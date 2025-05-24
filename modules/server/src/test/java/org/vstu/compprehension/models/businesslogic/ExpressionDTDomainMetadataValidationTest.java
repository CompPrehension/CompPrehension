package org.vstu.compprehension.models.businesslogic;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.Assert;
import org.vstu.compprehension.Service.QuestionService;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDTDomain;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeRDFHelper;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.MeaningTreeUtils;
import org.vstu.compprehension.models.businesslogic.domains.helpers.meaningtree.QuestionDynamicDataAppender;
import org.vstu.compprehension.models.businesslogic.storage.QuestionBank;
import org.vstu.compprehension.models.entities.AnswerObjectEntity;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseOptionsEntity;
import org.vstu.compprehension.models.entities.exercise.ExerciseStageEntity;
import org.vstu.compprehension.models.repository.ExerciseAttemptRepository;
import org.vstu.compprehension.models.repository.ExerciseRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.models.repository.UserRepository;
import org.vstu.meaningtree.SupportedLanguage;
import org.vstu.meaningtree.exceptions.MeaningTreeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@Transactional
public class ExpressionDTDomainMetadataValidationTest {
    @Autowired
    DomainFactory domainFactory;
    @Autowired
    private ExerciseAttemptRepository exerciseAttemptRepository;
    @Autowired
    private ExerciseRepository exerciseRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private QuestionMetadataRepository qMetaRepo;
    @Autowired
    private QuestionBank qBank;

    private ExerciseAttemptEntity attempt;
    private ExerciseEntity exercise;
    private ProgrammingLanguageExpressionDTDomain domain;

    public static final String domainId = "ProgrammingLanguageExpressionDTDomain";

    @BeforeAll
    public void tearUp() {
        domain = (ProgrammingLanguageExpressionDTDomain) domainFactory.getDomain(domainId);
        exercise = new ExerciseEntity();
        exercise.setDomain(domain.getDomainEntity());
        exercise.setBackendId("DTReasoner");
        exercise.setTags("");
        exercise.setOptions(new ExerciseOptionsEntity(null, true,
                true, true, true,
                true, true, 7));
        exercise.setName("test");
        exercise.setStages(Collections.singletonList(new ExerciseStageEntity()));
        exercise.setStrategyId("StaticStrategy");
        exercise.getStages().getFirst();
        exerciseRepository.save(exercise);
        attempt = new ExerciseAttemptEntity();
        attempt.setQuestions(List.of());
        attempt.setExercise(exercise);
        attempt.setUser(userRepository.findAll().iterator().next());
        exerciseAttemptRepository.save(attempt);
    }

    @AfterAll
    public void tearDown() {
        exerciseAttemptRepository.delete(attempt);
        exerciseRepository.delete(exercise);
    }

    @Test
    public void verify() {
        int count = 0;
        for (QuestionMetadataEntity meta : qMetaRepo.findAll()) {
            try {
                boolean qOk = true;
                if (!meta.getDomainShortname().equals("expression_dt")) {
                    continue;
                }
                Question q = prepareQuestion(meta);
                SupportedLanguage lang = MeaningTreeUtils.detectLanguageFromTags(meta.getTagBits(), domain);
                String text;
                try {
                    text = MeaningTreeUtils.viewExpression(MeaningTreeRDFHelper.backendFactsToMeaningTree(q.getStatementFacts()), lang);
                } catch (MeaningTreeException e) {
                    text = meta.getName();
                }
                String qInfo = String.format("[Question metadata id %d, lang %s, text: %s]: ", meta.getId(), lang.toString(), text);
                System.err.println(qInfo.concat("Processing"));
                List<AnswerObjectEntity> ansObj = q.getAnswerObjects().stream()
                        .filter((AnswerObjectEntity obj) -> !obj.getDomainInfo().equals("end_token")).toList();
                List<List<AnswerObjectEntity>> combinations = generateAllCombinations(ansObj);
                HashSet<Skill> allSkills = new HashSet<>();
                HashSet<NegativeLaw> allLaws = new HashSet<>();

                HashSet<Skill> questionSkills = new HashSet<>(domain.skillsFromBitmask(q.getMetadata().getSkillBits()));
                for (Skill skill : new HashSet<>(questionSkills)) {
                    if (!skill.baseSkills.isEmpty()) {
                        questionSkills.remove(skill);
                        questionSkills.addAll(skill.baseSkills);
                    }
                }
                HashSet<NegativeLaw> questionLaws = new HashSet<>(domain.negativeLawFromBitmask(q.getMetadata().getSkillBits()).stream().filter(
                        (NegativeLaw nLaw) -> nLaw.getLawsImplied() != null && !nLaw.getLawsImplied().isEmpty()).toList());
                questionLaws.remove(domain.getNegativeLaw("error_base_student_error_early_finish"));
                boolean foundCorrectSolution = false;
                for (List<AnswerObjectEntity> ans : combinations) {
                    var result = solve(q, lang, ans);
                    if (result.IterationsLeft == 0) {
                        foundCorrectSolution = true;
                    }
                    for (String rawSkill : result.domainSkills) {
                        Skill skill = domain.getSkill(rawSkill);
                        if (skill == null) {
                            System.err.println(qInfo.concat("Unknown skill in domain " + rawSkill));
                            qOk = false;
                            continue;
                        }
                        if (skill.baseSkills.isEmpty()) {
                            allSkills.add(skill);
                        } else {
                            allSkills.addAll(skill.baseSkills);
                        }
                    }
                    for (String rawLaw : result.domainNegativeLaws) {
                        NegativeLaw nLaw = domain.getNegativeLaw(rawLaw);
                        if (nLaw == null) {
                            System.err.println(qInfo.concat("Unknown law in domain " + rawLaw));
                            qOk = false;
                            continue;
                        }
                        allLaws.add(nLaw);
                    }
                }

                if (!foundCorrectSolution) {
                    System.err.println(qInfo.concat("Correct solution wasn't found for this question in all combinations of answer object"));
                    qOk = false;
                }

                HashSet<Skill> missingSkills = distinct(questionSkills, allSkills);
                HashSet<Skill> extraSkills = distinct(allSkills, questionSkills);
                HashSet<NegativeLaw> missingLaws = distinct(questionLaws, allLaws);
                HashSet<NegativeLaw> extraLaws = distinct(allLaws, questionLaws);

                if (!missingSkills.isEmpty() || !extraSkills.isEmpty() || !missingLaws.isEmpty() || !extraLaws.isEmpty()) {
                    qOk = false;
                }

                if (!missingSkills.isEmpty()) {
                    System.err.println(qInfo.concat("These skills are missing while solving: ").concat(
                            missingSkills.stream().map(Skill::getName).toList().toString()));
                }
                if (!extraSkills.isEmpty()) {
                    System.err.println(qInfo.concat("These skills are missing in question metadata: ").concat(
                            extraSkills.stream().map(Skill::getName).toList().toString()));
                }
                if (!missingLaws.isEmpty()) {
                    System.err.println(qInfo.concat("These negative laws are missing while solving: ").concat(
                            missingLaws.stream().map(Law::getName).toList().toString()));
                }
                if (!extraLaws.isEmpty()) {
                    System.err.println(qInfo.concat("These negative laws are missing in question metadata: ").concat(
                            extraLaws.stream().map(Law::getName).toList().toString()));
                }
                count += qOk ? 0 : 1;
            } catch (Exception e) {
                e.printStackTrace();
                count++;
            }
        }
        if (count > 0) {
            Assertions.fail("There are differences between question metadata and solving skills/laws in " + count + " questions");
        }
    }

    public static <T> HashSet<T> distinct(HashSet<T> a, HashSet<T> b) {
        HashSet<T> set = new HashSet<>(a);
        set.removeAll(b);
        return set;
    }

    public Question prepareQuestion(QuestionMetadataEntity meta) {
        SupportedLanguage lang = MeaningTreeUtils.detectLanguageFromTags(meta.getTagBits(), domain);
        Question q = meta.getQuestionData().getData().toQuestion(domain, meta);
        return QuestionDynamicDataAppender.appendQuestionData(q, attempt, qBank, lang, domain, Language.ENGLISH);
    }

    public Domain.InterpretSentenceResult solve(Question q, SupportedLanguage language, List<AnswerObjectEntity> answerSequence) {
        String outLangStr = language.toString().substring(0, 1).toUpperCase() + language.toString().substring(1);

        // Check metadata
        Assert.isTrue(q.getMetadata() != null
                && q.getMetadata().getIntegralComplexity() >= 0
                && q.getMetadata().getIntegralComplexity() <= 1, String.format(
                "Invalid integral complexity %f, possibleErrors=%d, solutionLength=%d",
                q.getMetadata().getIntegralComplexity(),
                q.getMetadata().getDistinctErrorsCount(),
                q.getMetadata().getSolutionSteps()));

        List<ResponseEntity> responses = new ArrayList<>();
        for (AnswerObjectEntity answerObject : answerSequence) {
            responses.add(ResponseEntity.builder().leftAnswerObject(answerObject).rightAnswerObject(answerObject).build());
        }
        return questionService.judgeQuestion(q, responses, List.of(domain.getTag(outLangStr)));
    }

    // Метод для получения всех комбинаций
    public static <T> List<List<T>> generateAllCombinations(List<T> inputList) {
        List<List<T>> result = new ArrayList<>();
        for (int size = 1; size <= inputList.size(); size++) {
            generateCombinations(inputList, new ArrayList<>(), result, size);
        }
        return result;
    }

    // Рекурсивный метод для генерации комбинаций заданного размера
    private static <T> void generateCombinations(List<T> inputList, List<T> current, List<List<T>> result, int size) {
        if (current.size() == size) {
            generatePermutations(current, 0, result);
            return;
        }
        for (int i = 0; i < inputList.size(); i++) {
            List<T> remaining = new ArrayList<>(inputList.subList(i + 1, inputList.size()));
            current.add(inputList.get(i));
            generateCombinations(remaining, current, result, size);
            current.removeLast();
        }
    }

    // Метод для генерации всех перестановок текущей комбинации
    private static <T> void generatePermutations(List<T> list, int start, List<List<T>> result) {
        if (start == list.size() - 1) {
            result.add(new ArrayList<>(list));
            return;
        }
        for (int i = start; i < list.size(); i++) {
            swap(list, i, start);
            generatePermutations(list, start + 1, result);
            swap(list, i, start); // Возврат к исходному состоянию
        }
    }

    // Метод для обмена элементов в списке
    private static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
