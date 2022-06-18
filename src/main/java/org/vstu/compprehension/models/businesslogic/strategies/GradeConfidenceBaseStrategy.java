package org.vstu.compprehension.models.businesslogic.strategies;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.NegativeLaw;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.DomainFactory;
import org.vstu.compprehension.models.businesslogic.strategies.AbstractStrategy;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.DisplayingFeedbackType;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.SearchDirections;

import javax.inject.Singleton;
import java.util.*;

@Component @Singleton @Primary
@Log4j2
public class GradeConfidenceBaseStrategy implements AbstractStrategy {

    private DomainFactory domainFactory;

    @Autowired
    public GradeConfidenceBaseStrategy(DomainFactory domainFactory) {
        this.domainFactory = domainFactory;
    }


    protected int WINDOW_TO_GRADE = 7;
    protected float TARGET_GRADE = (float)0.8;
    protected int DEFAULT_LAW_COUNT = 5;

    @NotNull
    @Override
    public String getStrategyId() {
        return "GradeConfidenceBaseStrategy";
    }

    @Override
    public QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt) {

        ExerciseEntity exercise = exerciseAttempt.getExercise();
        Domain domain = domainFactory.getDomain(exercise.getDomain().getName());

        QuestionRequest result = new QuestionRequest();
        result.setTargetConcepts(new ArrayList<>());
        result.setAllowedConcepts(new ArrayList<>());
        result.setAllowedLaws(new ArrayList<>());
        result.setDeniedConcepts(new ArrayList<>());
        result.setDeniedLaws(new ArrayList<>());
        HashMap<String, List<Boolean>> allLaws = getTargetLawsInteractions(exerciseAttempt, 0);
        HashMap<String, List<Boolean>> allLawsBeforeLastQuestion = getTargetLawsInteractions(exerciseAttempt, 1);

        HashMap<String, List<Boolean>> difference = countDifference(allLaws, allLawsBeforeLastQuestion);

        boolean isFirstQuestion = exerciseAttempt.getQuestions().size() == 0;


        result.setComplexity(grade(exerciseAttempt));//TODO
        //result.setComplexity(1);//TODO
        result.setSolvingDuration(30);
        result.setExerciseAttempt(exerciseAttempt);

        SearchDirections lawsDirections = isFirstQuestion ? SearchDirections.TO_SIMPLE: countLawsSearchDirections(difference);
        result.setLawsSearchDirection(lawsDirections);
        result.setAllowedConcepts(new ArrayList<>());

        //Вычислить количество законов в следующем вопросе

        //Вычислить тип студента: начинающий = -1, средний = 0, знаток = +1
        //        Для начинающего требуется высокая скорость уменьшения количества законов, низкая сложность
        //        Для среднего пытаться увеличить количество шагов на вопрос, небольшой или никакой рост количества законов
        //        Для знатока взрывной рост количества законов в вопросе
        int studentType = classifyStudent(exerciseAttempt);

        //оценка прохождения прошлого вопроса -1, 0, 1
        //Мера того, как справляется студент на текущем уровне сложности (-1 - сложность слишком велика, 0 - граничная сложность, 1 - слишком легко)
        int studentsComplexity = isFirstQuestion ? 0: countComplexityByDifference(difference);

        ArrayList<QuestionEntity> questions = new ArrayList<>();
        questions.addAll(exerciseAttempt.getQuestions());

        Collections.sort(questions, new QuestionOrderComparator());

        int lastLawCount = 0;
        int summarizedABSLawDeltaCount = 0;
        int summarizedLawDeltaCount = 0;
        int lastLawDeltaCount = 0;
        for (QuestionEntity qe : questions) {
            int currentLawCount = countQuestionLaws(qe).size();
            lastLawDeltaCount = currentLawCount - lastLawCount;
            summarizedABSLawDeltaCount += Math.abs(lastLawDeltaCount);
            summarizedLawDeltaCount += lastLawDeltaCount;

            lastLawCount = currentLawCount;
        }


        int countOfLaw = nextLawCount(studentType, studentsComplexity, lastLawCount,
                                        summarizedLawDeltaCount, lastLawDeltaCount, questions.size());
        if (studentsComplexity == -1) {
            //Директивное упрощение
            ArrayList<InteractionEntity> inters = new ArrayList<>();
            inters.addAll(questions.get(questions.size()-1).getInteractions());
            HashMap<String, List<Boolean>> allLawsError = new HashMap<>();

            for (InteractionEntity inter: inters) {
                for (ViolationEntity vio: inter.getViolations()) {
                    allLawsError.put(vio.getLawName(), allLaws.get(vio.getLawName()));
                }
            }

            allLaws = allLawsError;
        }

        result.setTargetLaws(countNextTargetLaws(allLaws, domain, countOfLaw));
        result.setDeniedQuestionNames(listQuestionsOfAttempt(exerciseAttempt));


        loggingParams(studentType, studentsComplexity, lawsDirections);
        loggingRequest(result);

        return result;
    }

    @Override
    public DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question) {
        return null;
    }

    @Override
    public FeedbackType determineFeedbackType(QuestionEntity question) {
        return null;
    }

    @Override
    public float grade(ExerciseAttemptEntity exerciseAttempt) {

        HashMap<String, List<Boolean>> allLaws = getTargetLawsInteractions(exerciseAttempt, 0);

        List<String> targetLaws = new ArrayList<>(basicLawsUsage(exerciseAttempt).keySet());

        float resultGrade = 0;

        for (String currentLaw : targetLaws) {
            List<Boolean> laws = new ArrayList<>();
            //laws.addAll(allLaws.get(currentLaw.getLawName()));
            laws.addAll(allLaws.get(currentLaw));
            Collections.reverse(laws);

            resultGrade += countGradeByUsage(laws, (float)1.2) * countConfidence(laws);
        }

//        if(targetLaws.size() == 0){
//            return 0;
//        }
        resultGrade = resultGrade / targetLaws.size();

        return resultGrade;
    }

    @Override
    public Decision decide(ExerciseAttemptEntity exerciseAttempt) {

        if(grade(exerciseAttempt) > countTargetGrade() && isAllLawsUsedWindowCount(exerciseAttempt)){
            return Decision.FINISH;
        }
        return Decision.CONTINUE;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private void loggingRequest(QuestionRequest result) {

        String T = "\t";

        log.info("Законы из домена в запросе:");
        ArrayList<Law> printOutLaws = new ArrayList<>(result.getTargetLaws());
        printOutLaws.sort(Comparator.comparing(Law::getName));
        for(Law str : printOutLaws){
            log.info(T + str.getName());
        }
    }

    private void loggingParams(int studentType, int studentsComplexity, SearchDirections lawsDirections) {

        String T = "\t";

        log.info("Параметры при расчёте стратегии:");
        log.info(T + "Тип студента: " + (studentType == -1 ? "начинающий" : studentType == 0 ? "средний" : "знаток"));
        log.info(T + "Сложность прошлого вопроса: " + (studentsComplexity == -1 ? "слишком сложно" : studentType == 0 ? "норма" : "слишком легко"));
        log.info(T + "Рекомендованное направление поиска для домена: " + (lawsDirections == SearchDirections.TO_SIMPLE ? "упрощение" : "усложнение"));

    }

    private int nextLawCount(int studentType, int studentsComplexity, int lastLawCount,
                             int summarizedLawDeltaCount, int lastLawDeltaCount,
                             int questionsCount) {

        if(questionsCount == 0){
            return getDefaultLawCount();
        }

        int lawCount = lastLawCount <= 0 ? 1 : lastLawCount;
        if(studentsComplexity == 0){

            if(studentType == 1){
                lawCount = lawCount + Math.abs(lastLawDeltaCount/2) + 1;
            }else if(studentType == 0){
                lawCount = lawCount + 1;
            }else if(studentType == -1){
                lawCount = lawCount - 1;
            }

        }

        if(studentsComplexity == -1){

            if(studentType == 1){
                lawCount = lawCount - Math.abs(lastLawDeltaCount/2) + 1;
            }else if(studentType == 0){
                lawCount = lawCount - Math.abs(lastLawDeltaCount/2);
            }else if(studentType == -1){
                lawCount = lawCount - Math.abs(lastLawDeltaCount);
            }

        }

        if(studentsComplexity == 1){

            if(studentType == 1){
                lawCount = lawCount + Math.abs(lastLawDeltaCount + summarizedLawDeltaCount/questionsCount) + 1;
            }else if(studentType == 0){
                lawCount = lawCount + Math.abs(lastLawDeltaCount + 1);
            }else if(studentType == -1){
                lawCount = lawCount + Math.abs(lastLawDeltaCount + 1);
            }

        }
        return lawCount <= 0 ? 1 : lawCount;
    }

    private int getDefaultLawCount() {
        return DEFAULT_LAW_COUNT;
    }


    private int classifyStudent(ExerciseAttemptEntity exerciseAttempt){
        int result = 0;

        HashMap<String, List<Boolean>> targetLawsInteractions = getTargetLawsInteractions(exerciseAttempt, 0);
        int errorCount = 0;
        int correctCount = 0;

        for(Map.Entry<String, List<Boolean>> entry : targetLawsInteractions.entrySet()) {
            String key = entry.getKey();
            List<Boolean> valueFirst = entry.getValue();

            for(Boolean currentVal : valueFirst){
                if(currentVal == Boolean.TRUE){
                    correctCount++;
                } else{
                    errorCount++;
                }
            }
        }


        //В зависимости от частости ошибок студента выбирается его тип (условный начинающий, средний, знаток)
        if((correctCount + errorCount) == 0){
            result = 0;
        } else {

            if ((float)correctCount / (correctCount + errorCount) <= 0.6) {
                result = -1;
            } else if ((float)correctCount / (correctCount + errorCount) <= 0.9) {
                result = 0;
            } else {
                result = 1;
            }
        }

        return result;
    }

    private List<String> countQuestionLaws(QuestionEntity questionEntity){

        List<String> result = new ArrayList<>();
        ArrayList<InteractionEntity> interactions = new ArrayList<>();
        interactions.addAll(questionEntity.getInteractions());
        for(int i = 0; i < interactions.size(); i++){

            ArrayList<CorrectLawEntity> correctLaws = new ArrayList<>();
            correctLaws.addAll(interactions.get(i).getCorrectLaw());
            for(int j = 0; j < correctLaws.size(); j++){
                if(!result.contains(correctLaws.get(j).getLawName())){
                    result.add(correctLaws.get(j).getLawName());
                }
            }
        }

        return result;

    }

    private List<Law> countNextTargetLaws(HashMap<String, List<Boolean>> allLaws, Domain domain, int countOfLaws) {
        // proxy to overloaded method
        return countNextTargetLaws(allLaws, new ArrayList<>(domain.getNegativeLaws()), countOfLaws);
    }

    private List<Law> countNextTargetLaws(HashMap<String, List<Boolean>> allLaws, List<Law> targetLaws, int countOfLaws) {
        ArrayList<Law> result = new ArrayList<>();

        float meanOfUsage = 0;

        //Подсчитать оценку каждого закона
        ArrayList<Pair<String, Float>> allLawsGrade = new ArrayList<>();
        for(Map.Entry<String, List<Boolean>> entry : allLaws.entrySet()) {

            String key = entry.getKey();
            List<Boolean> valueFirst = entry.getValue();

            List<Boolean> laws = new ArrayList<>();

            if (valueFirst != null) {
                meanOfUsage += valueFirst.size();
                laws.addAll(valueFirst);
            }

            allLawsGrade.add(0, Pair.of(key, countGradeByUsage(laws, (float)1.2) * countConfidence(laws)));
        }

        meanOfUsage = meanOfUsage / allLaws.keySet().size();

        Collections.sort(allLawsGrade, new LawGradeComparator());
        //////Проверить в каком порядке сортируется

        //Выбрать минимально изученные законы в количестве countOfLaws
        ArrayList<String> targetLawsName = new ArrayList<>();
        for (int i = 0; i < allLawsGrade.size() && i < countOfLaws; i++){
            targetLawsName.add(allLawsGrade.get(i).getFirst());
        }

        for(String lName : targetLawsName){
            Law lawToAdd = targetLaws.stream()
                    .filter(law -> lName.equals(law.getName()))
                    .findFirst()
                    .orElse(null);

            if(lawToAdd != null && (meanOfUsage == 0 || allLaws.get(lName).size() < (meanOfUsage / 2))) {
                result.add(lawToAdd);
            }
        }


        return result;
    }

    private boolean isAllLawsUsedWindowCount(ExerciseAttemptEntity exerciseAttempt) {

        HashMap<String, List<Boolean>> allLawsUsage = getTargetLawsInteractions(exerciseAttempt, 0);


        List<String> targetLaws = new ArrayList<>(basicLawsUsage(exerciseAttempt).keySet());

        Integer minimumLawUsageCount = null;
        for (String currentTargetLaw : targetLaws) {
            if(minimumLawUsageCount == null){
                //minimumLawUsageCount = allLawsUsage.get(currentTargetLaw.getLawName()).size();
                minimumLawUsageCount = allLawsUsage.get(currentTargetLaw).size();
            }else{
                //minimumLawUsageCount = Math.min(minimumLawUsageCount, allLawsUsage.get(currentTargetLaw.getLawName()).size());
                minimumLawUsageCount = Math.min(minimumLawUsageCount, allLawsUsage.get(currentTargetLaw).size());
            }
        }

        if(minimumLawUsageCount == null || minimumLawUsageCount > countGradeWindow()){
            return true;
        }

        return false;
    }

    protected HashMap<String, List<Boolean>> getTargetLawsInteractions(ExerciseAttemptEntity exerciseAttempt, int removeLastCount){
        List<QuestionEntity> allQuestions = exerciseAttempt.getQuestions();
        HashMap<String, List<Boolean>> allLawsUsage = basicLawsUsage(exerciseAttempt);

        Collections.sort(allQuestions, new QuestionOrderComparator());

        for(int i = 0; i < removeLastCount; i++){
            if(allQuestions.size() > 0) {
                allQuestions.remove(allQuestions.size() - 1);
            }
        }

        return getQuestionsLawConceptUsage(allQuestions, allLawsUsage);
    }





    private HashMap<String, List<Boolean>> basicLawsUsage(ExerciseAttemptEntity exerciseAttempt){
        HashMap<String, List<Boolean>> allLawsUsage = new HashMap<>();

        ExerciseEntity exercise = exerciseAttempt.getExercise();

        if (exercise.getExerciseLaws().isEmpty()) {

            // получить законы из домена (все подряд)
            Domain domain = domainFactory.getDomain(exerciseAttempt.getExercise().getDomain().getName());

            List<NegativeLaw> targetLaws = domain.getNegativeLaws();
            for (NegativeLaw currentTargetLaw : targetLaws) {
                allLawsUsage.put(currentTargetLaw.getName(), new ArrayList<>());
            }

        } else {
            // получить законы из упражнения
            List<ExerciseLawsEntity> targetLaws = exerciseAttempt.getExercise().getExerciseLaws();
            for (ExerciseLawsEntity currentTargetLaw : targetLaws) {
                allLawsUsage.put(currentTargetLaw.getLawName(), new ArrayList<>());
            }
        }


        return allLawsUsage;
    }

    private HashMap<String, List<Boolean>> getQuestionsLawConceptUsage(List<QuestionEntity> allQuestions, HashMap<String, List<Boolean>> allLawsUsage) {
        for (QuestionEntity currentQuestion : allQuestions) {
            List<InteractionEntity> allInteractions = currentQuestion.getInteractions();
            Collections.sort(allInteractions, new InteractionOrderComparator());

            for (InteractionEntity currentInteraction : allInteractions) {

                List<ViolationEntity> allViolations = currentInteraction.getViolations();
                for (ViolationEntity currentViolation : allViolations) {

                    if(allLawsUsage.containsKey(currentViolation.getLawName())){
                        allLawsUsage.get(currentViolation.getLawName()).add(Boolean.FALSE);
                    }

                }

                List<CorrectLawEntity> allLaws = currentInteraction.getCorrectLaw();
                for (CorrectLawEntity currentLaw : allLaws) {

                    if (allLawsUsage.containsKey(currentLaw.getLawName())) {
                        allLawsUsage.get(currentLaw.getLawName()).add(Boolean.TRUE);
                    }

                }

            }

        }

        return allLawsUsage;
    }

    private HashMap<String, List<Boolean>> countDifference(HashMap<String, List<Boolean>> first, HashMap<String, List<Boolean>>second){
        HashMap<String, List<Boolean>> result = new HashMap<>();

        for(Map.Entry<String, List<Boolean>> entry : first.entrySet()) {
            String key = entry.getKey();
            List<Boolean> valueFirst = entry.getValue();
            List<Boolean> valueSecond = second.get(key);

            List<Boolean> resValue = new ArrayList<>();

            if(valueFirst.size() == valueSecond.size()){
                result.put(key, resValue);
                continue;
            }

            List<Boolean> longestArray = new ArrayList<>();
            List<Boolean> shortestArray = new ArrayList<>();
            if(valueFirst.size() > valueSecond.size()){
                longestArray.addAll(valueFirst);
                shortestArray.addAll(valueSecond);
            }else {
                longestArray.addAll(valueSecond);
                shortestArray.addAll(valueFirst);
            }

            for(int i = shortestArray.size(); i < longestArray.size(); i++){
                resValue.add(longestArray.get(i));
            }

            result.put(key, resValue);

        }

        return result;
    }



    protected SearchDirections countLawsSearchDirections(HashMap<String, List<Boolean>> difference){

        Pair<Integer, Integer> counts = countMaximumCountOfErrorAndErrorCount(difference);
        int maximumCountOfError = counts.getFirst();
        int errorCount = counts.getSecond();

        if(maximumCountOfError > 2 || errorCount > 4){
            return SearchDirections.TO_SIMPLE;
        }
        return SearchDirections.TO_COMPLEX;
    }

    protected int countComplexityByDifference(HashMap<String, List<Boolean>> difference){

        Pair<Integer, Integer> counts = countMaximumCountOfErrorAndErrorCount(difference);
        int maximumCountOfError = counts.getFirst();
        int errorCount = counts.getSecond();

        if(maximumCountOfError > 2 || errorCount > 4){
            return -1;
        }

        if(errorCount > 2){
            return 0;
        }

        return 1;
    }

    protected Pair<Integer, Integer> countMaximumCountOfErrorAndErrorCount(HashMap<String, List<Boolean>> difference){
        int maximumCountOfError = 0;
        int errorCount = 0;

        for(Map.Entry<String, List<Boolean>> entry : difference.entrySet()) {
            String key = entry.getKey();
            List<Boolean> valueFirst = entry.getValue();

            int currentErrorCount = 0;
            for (Boolean currentAnswer : valueFirst) {
                if(currentAnswer == Boolean.FALSE){
                    currentErrorCount++;
                    errorCount++;
                }
            }

            maximumCountOfError = Math.max(maximumCountOfError, currentErrorCount);

        }

        return Pair.of(maximumCountOfError, errorCount);
    }




    protected int countGradeWindow(){
        return WINDOW_TO_GRADE;
    }

    protected float countTargetGrade(){ return TARGET_GRADE; }

    //List отсортированных по времени (от последних до первых) верных и неверных применений концепта
    private float countGradeByUsage(List<Boolean> conceptUsageResult, float k){
        float maximumGrade = 0;
        float realGrade = 0;

        ArrayList<Boolean> conceptUsageToAnalise = convertConceptUsageResultBeforeAnalise(conceptUsageResult);
        Collections.reverse(conceptUsageToAnalise);
        for (int i = 0; i < conceptUsageToAnalise.size(); i++) {
            float weightOfElement = (i * k)+ 1;
            maximumGrade += weightOfElement;

            if(conceptUsageToAnalise.get(i) == Boolean.TRUE){
                realGrade += weightOfElement;
            }
        }

        return realGrade / maximumGrade;
    }

    //Рассчитывает меру уверенности стратегии в оценке знаний по концепту
    //List отсортированных по времени (от последних до первых) верных и неверных применений концепта
    //Уверенность - мера однородности массива. Чем более однороден массив, тем выше уверенность
    private float countConfidence(List<Boolean> conceptUsageResult){

        int componentCount = 2;

        ArrayList<Boolean> conceptUsageToAnalise = convertConceptUsageResultBeforeAnalise(conceptUsageResult);
        ArrayList<Integer> reducedUsage = reduceUsage(conceptUsageToAnalise);

        int numberOfChanges = reducedUsage.size();
        int numberOfConceptUsage = conceptUsageToAnalise.size();
        int countOfMaxAnswer = 0;

        for (Boolean currentElement : conceptUsageToAnalise) {
            if (currentElement == Boolean.TRUE) {
                countOfMaxAnswer++;
            }
        }

        countOfMaxAnswer = Math.max(countOfMaxAnswer, numberOfConceptUsage - countOfMaxAnswer);

        int maxLenInReduce = 0;
        for (Integer currentLen : reducedUsage) {
            maxLenInReduce = Math.max(maxLenInReduce, currentLen);
        }

        return (((float)countOfMaxAnswer / numberOfConceptUsage)
                + (((float)maxLenInReduce / numberOfConceptUsage) / (float)numberOfChanges)) / componentCount;

    }

    private ArrayList<Integer> reduceUsage(ArrayList<Boolean> conceptUsageToAnalise){
        ArrayList<Integer> reducedUsage = new ArrayList<>();

        Boolean lastElement = null;

        for (Boolean currentElement : conceptUsageToAnalise) {
            if(currentElement != lastElement){
                reducedUsage.add(1);
                lastElement = currentElement;
            } else {
                reducedUsage.set(reducedUsage.size() - 1, reducedUsage.get(reducedUsage.size() - 1) + 1);
            }
        }

        return reducedUsage;
    }

    private ArrayList<Boolean> convertConceptUsageResultBeforeAnalise(List<Boolean> conceptUsageResult){
        int countToCheck = countGradeWindow();
        ArrayList<Boolean> conceptUsageToAnalise = new ArrayList<>();

        int i = 0;
        for (i = 0; i < countToCheck && i < conceptUsageResult.size(); i++) {
            conceptUsageToAnalise.add(conceptUsageResult.get(i));
        }

        for (; i < countToCheck; i++) {
            conceptUsageToAnalise.add(Boolean.FALSE);
        }

        return conceptUsageToAnalise;
    }

    class QuestionOrderComparator implements Comparator<QuestionEntity> {
        @Override
        public int compare(QuestionEntity a, QuestionEntity b) {
            return a.getId() < b.getId() ? -1 : a.getId() == b.getId() ? 0 : 1;
        }
    }

    class InteractionOrderComparator implements Comparator<InteractionEntity> {
        @Override
        public int compare(InteractionEntity a, InteractionEntity b) {
            return a.getOrderNumber() < b.getOrderNumber() ? -1 : a.getOrderNumber() == b.getOrderNumber() ? 0 : 1;
        }
    }

    class LawGradeComparator implements Comparator<Pair<String, Float>> {
        @Override
        public int compare(Pair<String, Float> a, Pair<String, Float> b) {
            return a.getSecond() < b.getSecond() ? -1 : a.getSecond() == b.getSecond() ? 0 : 1;
        }
    }


}
