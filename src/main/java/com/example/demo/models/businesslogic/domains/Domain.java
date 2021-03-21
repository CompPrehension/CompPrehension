package com.example.demo.models.businesslogic.domains;

import com.example.demo.models.businesslogic.*;
import com.example.demo.models.businesslogic.Question;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.FeedbackType;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.repository.QuestionRepository;
import com.example.demo.utils.HyperText;
import com.google.common.collect.TreeMultimap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

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
    
    public abstract Question makeQuestion(QuestionRequest questionRequest, Language userLanguage);
    
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
    public Question findQuestion(HashSet<String> targetConcepts, HashSet<String> allowedConcepts, HashSet<String> deniedConcepts) {
        List<Question> questions = new ArrayList<>();
        int maxSuitCount = 0;
        for (Question q : getQuestionTemplates()) {
            int targetConceptCount = 0;
            boolean suit = true;
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
        if (questions.isEmpty() || maxSuitCount == 0) {
            return null;
        } else {
            return questions.get(new Random().nextInt(questions.size()));
        }
    }
}
