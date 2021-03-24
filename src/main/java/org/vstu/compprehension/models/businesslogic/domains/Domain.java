package org.vstu.compprehension.models.businesslogic.domains;

import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.repository.QuestionRepository;
import org.vstu.compprehension.utils.HyperText;

import java.util.*;

public abstract class Domain {
    protected List<PositiveLaw> positiveLaws;
    protected List<NegativeLaw> negativeLaws;
    protected List<Concept> concepts;

    protected DomainEntity domainEntity;
    protected QuestionRepository questionRepository;

    protected String name = "";
    protected String version = "";

    public abstract void update();

    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }

    public List<PositiveLaw> getPositiveLaws() {
        return positiveLaws;
    }
    public List<NegativeLaw> getNegativeLaws() {
        return negativeLaws;
    }

    public List<Concept> getConcepts() {
        return concepts;
    }

    public PositiveLaw getPositiveLaw(String name) {
        for (PositiveLaw law : positiveLaws) {
            if (name.equals(law.getName())) {
                return law;
            }
        }
        return null;
    }

    public NegativeLaw getNegativeLaw(String name) {
        for (NegativeLaw law : negativeLaws) {
            if (name.equals(law.getName())) {
                return law;
            }
        }
        return null;
    }

    public Concept getConcept(String name) {
        for (Concept concept : concepts) {
            if (name.equals(concept.getName())) {
                return concept;
            }
        }
        return null;
    }

    public Domain() {
    }
    
    public abstract List<HyperText> getFullSolutionTrace(Question question);

    public abstract ExerciseForm getExerciseForm();
    
    public abstract ExerciseEntity processExerciseForm(ExerciseForm ef);
    
    public abstract Question makeQuestion(QuestionRequest questionRequest, List<Tag> tags, Language userLanguage);
    
    public abstract ArrayList<HyperText> makeExplanation(List<MistakeEntity> mistakes, FeedbackType feedbackType);

    public List<Law> getQuestionLaws(String questionDomainType, List<Tag> tags) {
        List<PositiveLaw> positiveLaws = getQuestionPositiveLaws(questionDomainType, tags);
        List<NegativeLaw> negativeLaws = getQuestionNegativeLaws(questionDomainType, tags);
        List<Law> laws = new ArrayList<>();
        laws.addAll(positiveLaws);
        laws.addAll(negativeLaws);
        return laws;
    }

    public abstract List<PositiveLaw> getQuestionPositiveLaws(String questionDomainType, List<Tag> tags);
    public abstract List<NegativeLaw> getQuestionNegativeLaws(String questionDomainType, List<Tag> tags);

    public abstract List<String> getSolutionVerbs(String questionDomainType, List<BackendFactEntity> statementFacts);
    public abstract List<String> getViolationVerbs(String questionDomainType, List<BackendFactEntity> statementFacts);

    /**
     * Сформировать из ответов (которые были ранее добавлены к вопросу)
     * студента факты в универсальной форме
     * @return - факты в универсальной форме
     */
    public abstract List<BackendFactEntity> responseToFacts(String questionDomainType, List<ResponseEntity> responses, List<AnswerObjectEntity> answerObjects);

    public class ProcessSolutionResult {
        public int CountCorrectOptions;
        public int IterationsLeft;
    }
    public class InterpretSentenceResult extends ProcessSolutionResult {
        public List<MistakeEntity> mistakes;
        public List<String> correctlyAppliedLaws;
    }

    /**
     * Сформировать из найденных Backend'ом фактов объекты нарушений
     * */
    public abstract InterpretSentenceResult interpretSentence(List<BackendFactEntity> violations);

    public abstract ProcessSolutionResult processSolution(List<BackendFactEntity> solution);

    protected abstract List<Question> getQuestionTemplates();
    public Question findQuestion(List<Tag> tags, Question q) {
        return findQuestion(tags, new HashSet<>(q.getConcepts()), new HashSet<>(), new HashSet<>(), new HashSet<>(Set.of(q.getQuestionText().getText())));
    }

    public Question findQuestion(List<Tag> tags, HashSet<String> targetConcepts, HashSet<String> allowedConcepts, HashSet<String> deniedConcepts, HashSet<String> forbiddenQuestions) {
        List<Question> questions = new ArrayList<>();
        int maxSuitCount = 0;
        for (Question q : getQuestionTemplates()) {
            int targetConceptCount = 0;
            boolean suit = true;
            if (forbiddenQuestions.contains(q.getQuestionText().getText())) {
                continue;
            }
            for (Tag tag : tags) {
                if (!q.getTags().contains(tag.getName())) {
                    suit = false;
                    break;
                }
            }
            for (String concept : q.getConcepts()) {
                if (deniedConcepts.contains(concept)) {
                    suit = false;
                    break;
                }
                if (targetConcepts.contains(concept)) {
                    targetConceptCount++;
                }
            }

            if (suit && targetConceptCount >= maxSuitCount) {
                if (targetConceptCount > maxSuitCount) {
                    questions.clear();
                    maxSuitCount = targetConceptCount;
                }
                questions.add(q);
            }
        }
        if (questions.isEmpty()) {
            return null;
        } else {
            return questions.get(new Random().nextInt(questions.size()));
        }
    }
}
