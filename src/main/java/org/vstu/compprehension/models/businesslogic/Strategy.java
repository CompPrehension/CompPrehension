package org.vstu.compprehension.models.businesslogic;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.util.Pair;
import org.vstu.compprehension.Service.DomainService;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.DisplayingFeedbackType;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.utils.DomainAdapter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.lang.Math.abs;

@Component @Log4j2
public class Strategy extends AbstractStrategy {

    @Autowired
    private DomainService domainService;

    public QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt) {

        ExerciseEntity exercise = exerciseAttempt.getExercise();
        Domain domain = DomainAdapter.getDomain(exercise.getDomain().getClassPath());
        HashMap<String, LawNode> tree = getTree(domain);
        // Отдельная ветка для старта (взять некоторую часть возможных законов упражнения) - вопрос из середины графа
        if(exerciseAttempt.getQuestions() == null || exerciseAttempt.getQuestions().size() == 0){


            ArrayList<String> startTasks = null;
            if(domain instanceof ProgrammingLanguageExpressionDomain) {
                startTasks = new ArrayList<>(Arrays.asList("a + b ? c + d : e + f ? g + h : i + j"));
            }else if(domain instanceof ControlFlowStatementsDomain){
                startTasks = new ArrayList<>(Arrays.asList("alt_i1", "while_2_110",
                        "do_10"));
            }
            Random random = new Random();
            int nextQuestion = random.ints(0, startTasks.size())
                    .findFirst()
                    .getAsInt();

            LawNode nextNode = tree.get(startTasks.get(nextQuestion));

            return getQuestionRequest(exerciseAttempt, nextNode);
        }
        // Для вопроса вытянуть все интеракции в змейку по дельте (изменения в правильно примененных правилах и ошибки)
        QuestionEntity qe = null;
        // Find last not supplementary question in exercise
        for (int questionNumber = exerciseAttempt.getQuestions().size() - 1; questionNumber >= 0; --questionNumber) {
            if (!exerciseAttempt.getQuestions().get(questionNumber).getQuestionDomainType().contains("Supplementary")) {
                qe = exerciseAttempt.getQuestions().get(questionNumber);
                break;
            }
        }

        InteractionEntity lastIE = null;
        ArrayList<Pair<Pair<Boolean, Integer>, String>> allLaws = new ArrayList<>();
        ArrayList<InteractionEntity> ies = new ArrayList<>();

        if(qe.getInteractions() != null){
            ies = new ArrayList<>(qe.getInteractions());
        }

        if(ies.size() == 0){
            LawNode currentNode = tree.get(qe.getQuestionName());
            return getQuestionRequest(exerciseAttempt, currentNode);
        }

        Collections.sort(ies, new InteractionOrderComparator());
        int iIndex = 0;
        for(InteractionEntity ie : ies){
            ArrayList<Pair<Boolean, String>> tmp = findInteractionsDelta(lastIE, ie);

            for(Pair<Boolean, String> i: tmp){
                allLaws.add(Pair.of(Pair.of(i.getFirst(), iIndex), i.getSecond()));
            }
            iIndex++;

            lastIE = ie;
        }
        // Для каждого закона проверить, действительно ли он не угадан
        ArrayList<String> correctLaws = new ArrayList<>();
        ArrayList<String> incorrectLaws = new ArrayList<>();
        ArrayList<String> lastErrors = new ArrayList<>();

        //// Вопрос считается угаданным, если между последним правильным ответом и верным применением текущего закона...
        ////...была ошибка применения этого закона
        for(int i = 0; i < allLaws.size(); i++){
            Pair<Pair<Boolean, Integer>, String> interResult = allLaws.get(i);
            //Если текущая интеракция верна
            if(interResult.getFirst().getFirst()){
                //Если с момента прошлого верного ответа были такие ошибки
                int lastIndex = interResult.getFirst().getSecond();
                while(i < allLaws.size() && interResult.getFirst().getSecond() == lastIndex) {
                    if (lastErrors.contains(interResult.getSecond())) {
                        //Если закон считался верно изученным
                        //Считать, что он не был понят
                        if (correctLaws.contains(interResult.getSecond())) {
                            correctLaws.remove(interResult.getSecond());
                        }
                        if (!incorrectLaws.contains(interResult.getSecond())) {
                            incorrectLaws.add(interResult.getSecond());
                        }

                    } else {
                        //Если ранее закон считался не изученным
                        //Считать закон изученным
                        if (incorrectLaws.contains(interResult.getSecond())) {
                            incorrectLaws.remove(interResult.getSecond());
                        }
                        if (!correctLaws.contains(interResult.getSecond())) {
                            correctLaws.add(interResult.getSecond());
                        }


                    }

                    i++;
                    if(i < allLaws.size()){
                        interResult = allLaws.get(i);
                    }
                }
                i--;

                lastErrors.clear();
            }else{
                lastErrors.add(interResult.getSecond());
                if (correctLaws.contains(interResult.getSecond())) {
                    correctLaws.remove(interResult.getSecond());
                }

                if (!incorrectLaws.contains(interResult.getSecond())) {
                    incorrectLaws.add(interResult.getSecond());
                }
            }
        }

        float correct = (float) correctLaws.size() / (float)(correctLaws.size() + incorrectLaws.size());

        LawNode currentNode = tree.get(qe.getQuestionName());
        // Если верно примененных законов в текущем вопросе достаточно (не менее 90%), то берется вопрос из больших
        if(correct > 0.9){
            //Если у узла есть "прямые" большие вопросы
            if(currentNode != null && currentNode.parentNodes != null && currentNode.parentNodes.size() > 0){
                Random random = new Random();
                int nextQuestion = 0;
                if(currentNode.parentNodes.size() > 1) {
                    nextQuestion = random.ints(0, currentNode.parentNodes.size() )
                            .findFirst()
                            .getAsInt();
                }

                LawNode nextNode = tree.get(currentNode.parentNodes.get(nextQuestion));

                return getQuestionRequest(exerciseAttempt, nextNode);
            } else {
                //// Если нет больших, проверить усвоенность всех целевых законов
                ArrayList<QuestionEntity> qes = new ArrayList<>(exerciseAttempt.getQuestions());
                Collections.sort(qes, new QuestionOrderComparator());



                /////////////////////////////////////////////////////////////

                ArrayList<Pair<Pair<Boolean, Integer>, String>> allLawsHistory = new ArrayList<>();
                int index = 0;
                for (QuestionEntity taskqe : qes){
                    ArrayList<InteractionEntity> taskies = new ArrayList<>(taskqe.getInteractions());

                    Collections.sort(taskies, new InteractionOrderComparator());
                    InteractionEntity tasklastIE = null;
                    for(InteractionEntity ie : taskies){
                        ArrayList<Pair<Boolean, String>> tmp = findInteractionsDelta(tasklastIE, ie);
                        for(Pair<Boolean, String> law : tmp){
                            allLawsHistory.add(Pair.of(Pair.of(law.getFirst(), index), law.getSecond()));
                        }
                        index++;

                        tasklastIE = ie;
                    }

                }

                correctLaws = new ArrayList<>();
                incorrectLaws = new ArrayList<>();
                lastErrors = new ArrayList<>();

                //// Вопрос считается угаданным, если между последним правильным ответом и верным применением текущего закона...
                ////...была ошибка применения этого закона
                for(int i = 0; i < allLawsHistory.size(); i++){
                    Pair<Pair<Boolean, Integer>, String> interResult = allLawsHistory.get(i);
                    //Если текущая интеракция верна
                    if(interResult.getFirst().getFirst()){
                        //Если с момента прошлого верного ответа были такие ошибки
                        int lastIndex = interResult.getFirst().getSecond();
                        while(i < allLawsHistory.size() && interResult.getFirst().getSecond() == lastIndex) {
                            if (lastErrors.contains(interResult.getSecond())) {
                                //Если закон считался верно изученным
                                //Считать, что он не был понят
                                if (correctLaws.contains(interResult.getSecond())) {
                                    correctLaws.remove(interResult.getSecond());
                                }
                                if (!incorrectLaws.contains(interResult.getSecond())) {
                                    incorrectLaws.add(interResult.getSecond());
                                }
                            } else {
                                //Если ранее закон считался не изученным
                                //Считать закон изученным
                                if (incorrectLaws.contains(interResult.getSecond())) {
                                    incorrectLaws.remove(interResult.getSecond());
                                }
                                if (!correctLaws.contains(interResult.getSecond())) {
                                    correctLaws.add(interResult.getSecond());
                                }
                            }

                            i++;
                            if(i < allLawsHistory.size()){
                                interResult = allLawsHistory.get(i);
                            }
                        }
                        i--;

                        lastErrors.clear();
                    }else{
                        lastErrors.add(interResult.getSecond());
                        if (correctLaws.contains(interResult.getSecond())) {
                            correctLaws.remove(interResult.getSecond());
                        }

                        if (!incorrectLaws.contains(interResult.getSecond())) {
                            incorrectLaws.add(interResult.getSecond());
                        }
                    }
                }


                ////////////////////////////////////////////////////////////

                ArrayList<LawNode>nextNodes = getNextNodes(tree, correctLaws);
                //Все законы усвоены
                if(nextNodes.size() == 0){
                    return new QuestionRequest();
                }

                Random random = new Random();
                int nextQuestion = 0;
                if(currentNode != null && currentNode.parentNodes.size() > 1) {
                    nextQuestion = random.ints(0, currentNode.parentNodes.size())
                            .findFirst()
                            .getAsInt();
                }

                LawNode nextNode = nextNodes.get(nextQuestion);

                return getQuestionRequest(exerciseAttempt, nextNode);

            }
        }else{
            // ... иначе берется вопрос из меньших по графу, включающих законы, в которых была ошибка
            if(currentNode != null && currentNode.childNodes.size() > 0){

                ArrayList<LawNode> nextNodes = new ArrayList<>();
                for(String lawName : currentNode.childNodes){
                    LawNode childNode = tree.get(lawName);
                    for(String currentError : incorrectLaws){
                        if(childNode.currentLows.contains(currentError) && !nextNodes.contains(childNode)){
                            nextNodes.add(childNode);
                        }
                    }
                }
                if(nextNodes.size() == 0){
                    return getQuestionRequest(exerciseAttempt, currentNode);
                }

                Random random = new Random();
                int nextQuestion = 0;
                if(nextNodes.size() > 1) {
                    nextQuestion = random.ints(0, nextNodes.size())
                            .findFirst()
                            .getAsInt();
                }

                LawNode nextNode = nextNodes.get(nextQuestion);

                return getQuestionRequest(exerciseAttempt, nextNode);
            }else{
                //// Если некуда больше дробить, используем текущий вопрос как референс целевых законов
                return getQuestionRequest(exerciseAttempt, currentNode);
            }
        }

    }

    @NotNull
    private QuestionRequest getQuestionRequest(@NotNull ExerciseAttemptEntity exerciseAttempt, @Nullable LawNode nextNode) {
        QuestionRequest qr = new QuestionRequest();
        qr.setExerciseAttempt(exerciseAttempt);
        ExerciseEntity exercise = exerciseAttempt.getExercise();
        Domain domain = DomainAdapter.getDomain(exercise.getDomain().getClassPath());
        assert domain != null;

        qr.setComplexity(1);
        qr.setSolvingDuration(30);
//        qr.setDeniedConcepts(new ArrayList<>());
        qr.setAllowedConcepts(new ArrayList<>());

        List<Law> laws = new ArrayList<>(domain.getNegativeLaws()); //domainEntity.getLaws();
        List<Law> targetLaws = new ArrayList<>();

        for (Law l : laws) {
            if(nextNode != null && nextNode.currentLows.contains(l.name)){
                targetLaws.add(l);
            }
        }
        qr.setTargetLaws(targetLaws);

        ArrayList<Concept> concepts = new ArrayList<>(domain.getConcepts());
        ArrayList<Concept> targetConcepts = new ArrayList<>();

        ArrayList<Concept> deniedConcepts = new ArrayList<>();
        ArrayList<String> denConc = new ArrayList<>(Arrays.asList("precedence_type",
                "operands_type",
                "type"));

        for (Concept c : concepts) {
            if(nextNode != null && nextNode.currentLows.contains(c.getName())){
                targetConcepts.add(c);
            }

            if(domain instanceof ProgrammingLanguageExpressionDomain && denConc.contains(c.getName())){
                deniedConcepts.add(c);
            }
        }
        qr.setTargetConcepts(targetConcepts);
        qr.setDeniedConcepts(deniedConcepts);

        String T = "\t";

        log.info("Желаемый вопрос:");
        log.info(T + (nextNode != null ? nextNode.nodeName : ""));
        log.info("Желаемые законы:");
        ArrayList<String> printOutList = new ArrayList<>();
        if (nextNode != null)
            printOutList.addAll(nextNode.currentLows);
        printOutList.sort(null);
        for(String str : printOutList){
            log.info(T + str);
        }

        log.info("Законы из домена в запросе:");
        ArrayList<Law> printOutLaws = new ArrayList<>(qr.getTargetLaws());
        printOutLaws.sort(Comparator.comparing(l -> l.name));
        for(Law str : printOutLaws){
            log.info(T + str.name);
        }

        log.info("Концепты из домена в запросе:");
        ArrayList<Concept> printOutConcepts = new ArrayList<>(qr.getTargetConcepts());
        printOutConcepts.sort(Comparator.comparing(Concept::getName));
        for(Concept str : printOutConcepts){
            log.info(T + str.getName());
        }

        log.info("Запрещённые концепты из домена в запросе:");
        printOutConcepts = new ArrayList<>(qr.getDeniedConcepts());
        printOutConcepts.sort(Comparator.comparing(Concept::getName));
        for(Concept str : printOutConcepts){
            log.info(T + str.getName());
        }

        return qr;
    }

    public DisplayingFeedbackType determineDisplayingFeedbackType(QuestionEntity question) {

        List<InteractionEntity> interactions = question.getInteractions();

        int interactionWithMistakes = 0;
        for (InteractionEntity i : interactions) {

            if (i.getViolations() != null || i.getViolations().size() != 0) {

                interactionWithMistakes++;
            }
        }

        if (interactionWithMistakes < 1) {
            return DisplayingFeedbackType.NOT_SHOW;
        } else if (interactionWithMistakes < 2) {
            return DisplayingFeedbackType.HOVER;
        } else {
            return DisplayingFeedbackType.SHOW;
        }
    }

    public FeedbackType determineFeedbackType(QuestionEntity question) {

        List<InteractionEntity> interactions = question.getInteractions();

        int interactionWithMistakes = 0;
        for (InteractionEntity i : interactions) {

            if (i.getViolations() != null || i.getViolations().size() != 0) {

                interactionWithMistakes++;
            }
        }

        if (interactionWithMistakes < 1) {
            return FeedbackType.DEGREE_OF_CORRECTNESS;
        } else {
            return FeedbackType.EXPLANATION;
        }
    }

    @Override
    public float grade(ExerciseAttemptEntity exerciseAttempt) {

        val res = getLawGrade(exerciseAttempt);
        if(res.keySet().isEmpty()){
            return (float)0;
        }

        float summary = 0;

        for(HashMap.Entry<String, Float> entry : res.entrySet()) {
            String key = entry.getKey();
            val value = entry.getValue();
            summary += value;
        }

        return summary/(float)res.keySet().stream().count();
    }

    @Override
    public Decision decide(ExerciseAttemptEntity exerciseAttempt) {
        // Должно быть задано не менее 3 вопросов и все вопросы должны быть завершены
        if(exerciseAttempt.getQuestions().stream().count() <= 3 ||
            exerciseAttempt.getQuestions().stream().anyMatch(q -> q.getId() == exerciseAttempt.getQuestions().get(exerciseAttempt.getQuestions().size() - 1).getId() && (q.getInteractions().size() == 0 || q.getInteractions().get(q.getInteractions().size() - 1).getFeedback().getInteractionsLeft() > 0))){
            return Decision.CONTINUE;
        }

        ExerciseEntity exercise = exerciseAttempt.getExercise();
        Domain domain = DomainAdapter.getDomain(exercise.getDomain().getClassPath());
        HashMap<String, LawNode> tree = getTree(domain);

        //// Если нет больших, проверить усвоенность всех целевых законов
        ArrayList<QuestionEntity> qes = new ArrayList<>(exerciseAttempt.getQuestions());
        Collections.sort(qes, new QuestionOrderComparator());

        ArrayList<Pair<Pair<Boolean, Integer>, String>> allLawsHistory = new ArrayList<>();
        int index = 0;
        for (QuestionEntity taskqe : qes){
            ArrayList<InteractionEntity> taskies = new ArrayList<>(taskqe.getInteractions());

            Collections.sort(taskies, new InteractionOrderComparator());
            InteractionEntity tasklastIE = null;
            for(InteractionEntity ie : taskies){
                ArrayList<Pair<Boolean, String>> tmp = findInteractionsDelta(tasklastIE, ie);
                for(Pair<Boolean, String> law : tmp){
                    allLawsHistory.add(Pair.of(Pair.of(law.getFirst(), index), law.getSecond()));
                }
                index++;

                tasklastIE = ie;
            }

        }

        ArrayList<String> correctLaws = new ArrayList<>();
        ArrayList<String> incorrectLaws = new ArrayList<>();
        ArrayList<String> lastErrors = new ArrayList<>();

        //// Вопрос считается угаданным, если между последним правильным ответом и верным применением текущего закона...
        ////...была ошибка применения этого закона
        for(int i = 0; i < allLawsHistory.size(); i++){
            Pair<Pair<Boolean, Integer>, String> interResult = allLawsHistory.get(i);
            //Если текущая интеракция верна
            if(interResult.getFirst().getFirst()){
                //Если с момента прошлого верного ответа были такие ошибки
                int lastIndex = interResult.getFirst().getSecond();
                while(i < allLawsHistory.size() && interResult.getFirst().getSecond() == lastIndex) {
                    if (lastErrors.contains(interResult.getSecond())) {
                        //Если закон считался верно изученным
                        //Считать, что он не был понят
                        if (correctLaws.contains(interResult.getSecond())) {
                            correctLaws.remove(interResult.getSecond());
                        }
                        if (!incorrectLaws.contains(interResult.getSecond())) {
                            incorrectLaws.add(interResult.getSecond());
                        }
                    } else {
                        //Если ранее закон считался не изученным
                        //Считать закон изученным
                        if (incorrectLaws.contains(interResult.getSecond())) {
                            incorrectLaws.remove(interResult.getSecond());
                        }
                        if (!correctLaws.contains(interResult.getSecond())) {
                            correctLaws.add(interResult.getSecond());
                        }
                    }

                    i++;
                    if(i < allLawsHistory.size()){
                        interResult = allLawsHistory.get(i);
                        if (correctLaws.contains(interResult.getSecond())) {
                            correctLaws.remove(interResult.getSecond());
                        }

                        if (!incorrectLaws.contains(interResult.getSecond())) {
                            incorrectLaws.add(interResult.getSecond());
                        }
                    }
                }
                i--;

                lastErrors.clear();
            }else{
                lastErrors.add(interResult.getSecond());
            }
        }

        ArrayList<LawNode> nextNodes = getNextNodes((HashMap<String, LawNode>) tree, correctLaws);
        //Все законы усвоены
        if(nextNodes.size() == 0){
            return Decision.FINISH;
        }

        return Decision.CONTINUE;
    }

    private ArrayList<LawNode> getNextNodes(HashMap<String, LawNode> tree, ArrayList<String> correctLaws) {
        ArrayList<String>allNodes = new ArrayList<>(tree.keySet());
        ArrayList<String>wrongLaws = new ArrayList<>();

        ArrayList<LawNode> nextNodes = new ArrayList<>();
        for(String lawName : allNodes){
            LawNode childNode = tree.get(lawName);
            //Собрать ноды с упущенными или некорректными законами
            for(String currentError : childNode.currentLows){
                //Если закон отсутствует в корректных, то он или ошибочен или не применялся
                if(!correctLaws.contains(currentError) && !nextNodes.contains(childNode)){
                    if(!wrongLaws.contains(currentError)) {
                        wrongLaws.add(currentError);
                    }
                    nextNodes.add(childNode);
                }
            }

        }

        log.info("Упущенные законы:");
        for(String s:wrongLaws){
            log.info(s);
        }

        return nextNodes;
    }

    protected HashMap<String, Float> getLawGrade(ExerciseAttemptEntity exerciseAttempt){
        HashMap<String, Float> res = new HashMap<>();

        HashMap<String, ArrayList<Boolean>> conceptAtempt = new HashMap<>();
        ArrayList<QuestionEntity> questions = new ArrayList<>();
        questions.addAll(exerciseAttempt.getQuestions());

        ArrayList<InteractionEntity> ies = new ArrayList<>();
        for(QuestionEntity qe : questions){

            val inter = qe.getInteractions();
            if(inter != null) {
                ies.addAll(inter);
            }
        }

        Collections.sort(ies, new InteractionOrderComparator());
        Collections.reverse(ies);

        for (InteractionEntity ie : ies){
            ArrayList<ViolationEntity> mistakes = new ArrayList<>();
            if (ie.getViolations() != null) {
                mistakes.addAll(ie.getViolations());
            }

            for(ViolationEntity me : mistakes){
                if(conceptAtempt.containsKey(me.getLawName())){
                    conceptAtempt.get(me.getLawName()).add(false);
                }else{
                    ArrayList<Boolean> newLaw = new ArrayList<>();
                    newLaw.add(false);
                    conceptAtempt.put(me.getLawName(), newLaw);
                }
            }

            ArrayList<CorrectLawEntity> correctLaws = new ArrayList<>();
            if(ie.getCorrectLaw() != null) {
                correctLaws.addAll(ie.getCorrectLaw());
            }

            for(CorrectLawEntity cle : correctLaws){
                if(conceptAtempt.containsKey(cle.getLawName())){
                    conceptAtempt.get(cle.getLawName()).add(true);
                }else{
                    ArrayList<Boolean> newLaw = new ArrayList<>();
                    newLaw.add(true);
                    conceptAtempt.put(cle.getLawName(), newLaw);
                }
            }

        }

        for(HashMap.Entry<String, ArrayList<Boolean>> entry : conceptAtempt.entrySet()) {
            String key = entry.getKey();
            val value = entry.getValue();
            res.put(key, calculateWeightedScore(value));
        }

        return res;
    }

    protected ArrayList<Pair<Boolean, String>> findInteractionsDelta(InteractionEntity last, InteractionEntity current){
        ArrayList<Pair<Boolean, String>> result = new ArrayList<>();

        ArrayList<String> lastCorrectLaws = new ArrayList<>();
        if(last != null) {
            for (CorrectLawEntity cle : last.getCorrectLaw()) {
                lastCorrectLaws.add(cle.getLawName());
            }
        }

        for(CorrectLawEntity cle : current.getCorrectLaw()){
            if(!lastCorrectLaws.contains(cle.getLawName())) {
                result.add(Pair.of(true, cle.getLawName()));
            }
        }

        for(ViolationEntity ve : current.getViolations()){
            result.add(Pair.of(false, ve.getLawName()));
        }

        return result;
    }

    private Float calculateWeightedScore(ArrayList<Boolean> value) {
        if(value.stream().count() == 0){
            return (float)0;
        }

        float trueCount = 0;
        float falseCount = 0;

        float signCount = 5;
        float curIteration = 0;

        for(boolean i: value){
            if(i){
                trueCount += 1;
            }else{
                falseCount += 1;
            }

            if(abs(curIteration-signCount) < 0.0001){
                break;
            }
        }

        return trueCount/(trueCount + falseCount);
    }

    public HashMap<String, LawNode> getTree(Domain de) {
        if(de instanceof ProgrammingLanguageExpressionDomain) {
            return getTree0();
        }else if(de instanceof ControlFlowStatementsDomain)
        {
            return getTree1();
        }
        return null;
    }

    public HashMap<String, LawNode> getTree0(){
        HashMap<String, LawNode> result = new HashMap<>();

        result.put("a + b [ c + d ] + e", new LawNode("a + b [ c + d ] + e",
                new ArrayList<>(Arrays.asList("error_base_student_error_in_complex", "error_base_higher_precedence_right",
                        "error_base_same_precedence_left_associativity_left", "error_base_higher_precedence_left")),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "a + b + c",
                        "a && b || c", "-- a + b", "a == b < c", "- -- a", "* ++ a", "a * b + c * d",
                        "* ++ a + b", "++ a - b + c", "* a [ b + c ] + e", "a + b + c * d", "a && ( b || c ) && d",
                        "a + ( b + c < d + e )", "a || f ( b || c , d || e )"))));


        result.put("a + ( b + c < d + e )", new LawNode("a + ( b + c < d + e )",
                new ArrayList<>(Arrays.asList("error_base_student_error_in_complex", "error_base_higher_precedence_left",
                        "error_base_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("++ a || b + c", "a + b ? c + d : e + f", "-- ( -- a )", "a && ( b || c )"))));


        result.put("a || ( b || c , d || e )", new LawNode("a || ( b || c , d || e )",
                new ArrayList<>(Arrays.asList("error_base_student_error_in_complex", "error_base_student_error_strict_operands_order_base")),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "a && b || c",
                        "-- a + b", "a == b < c", "- -- a", "* ++ a", "a * b + c * d",
                        "* ++ a + b", "* a [ b + c ] + e"))));

        result.put("a && ( b || c ) && d", new LawNode("a && ( b || c ) && d",
                new ArrayList<>(Arrays.asList("error_base_student_error_in_complex", "error_base_same_precedence_left_associativity_left")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "a + b + c"))));

        result.put("a + b * c || d + e * f", new LawNode("a + b * c || d + e * f",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_right", "error_base_student_error_strict_operands_order_base")),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList("a == b < c", "- -- a", "* ++ a", "++ a || b + c", "a + b ? c + d : e + f"))));

        result.put("a + b + c * d", new LawNode("a + b + c * d",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_right", "error_base_same_precedence_left_associativity_left")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("a == b < c", "- -- a", "* ++ a", "a + b + c"))));

        result.put("* a [ b + c ] + e", new LawNode("* a [ b + c ] + e",
                new ArrayList<>(Arrays.asList("error_base_student_error_in_complex", "error_base_higher_precedence_left")),
                new ArrayList<>(Arrays.asList("a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "-- a + b", "a && b || c"))));

        result.put("a || f ( b || c , d || e )", new LawNode("a || f ( b || c , d || e )",
                new ArrayList<>(Arrays.asList("error_base_student_error_in_complex", "error_base_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "a == b < c", "- -- a", "* ++ a"))));

        result.put("++ a - b + c", new LawNode("++ a - b + c",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_left", "error_base_same_precedence_left_associativity_left")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("-- a + b", "a && b || c", "a + b + c"))));

        result.put("a + b ? c + d : e + f ? g + h : i + j", new LawNode("a + b ? c + d : e + f ? g + h : i + j",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_left", "error_base_same_precedence_left_associativity_left")),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList("a = b = c", "++ a || b + c", "a + b ? c + d : e + f"))));

        result.put("* ++ a + b", new LawNode("* ++ a + b",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_left", "error_base_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("a && b || c", "-- a + b", "a == b < c", "- -- a", "* ++ a"))));

        result.put("a * b + c * d", new LawNode("a * b + c * d",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_left", "error_base_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("a && b || c", "-- a + b", "a == b < c", "- -- a", "* ++ a"))));

        result.put("a + b ? c + d : e + f", new LawNode("a + b ? c + d : e + f",
                new ArrayList<>(Arrays.asList("error_base_student_error_strict_operands_order_base")),
                new ArrayList<>(Arrays.asList("a + b ? c + d : e + f ? g + h : i + j", "a + b * c || d + e * f",
                        "a || ( b || c , d || e )")),
                new ArrayList<>()));

        result.put("++ a || b + c", new LawNode("++ a || b + c",
                new ArrayList<>(Arrays.asList("error_base_student_error_strict_operands_order_base")),
                new ArrayList<>(Arrays.asList("a + b ? c + d : e + f ? g + h : i + j", "a + b * c || d + e * f",
                        "a || ( b || c , d || e )")),
                new ArrayList<>()));

        result.put("a && ( b || c )", new LawNode("a && ( b || c )",
                new ArrayList<>(Arrays.asList("error_base_student_error_in_complex")),
                new ArrayList<>(Arrays.asList("a || f ( b || c , d || e )", "* a [ b + c ] + e",
                        "a && ( b || c ) && d", "a || ( b || c , d || e )", "a + ( b + c < d + e )",
                        "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("-- ( -- a )", new LawNode("-- ( -- a )",
                new ArrayList<>(Arrays.asList("error_base_student_error_in_complex")),
                new ArrayList<>(Arrays.asList("a || f ( b || c , d || e )", "* a [ b + c ] + e",
                        "a && ( b || c ) && d", "a || ( b || c , d || e )", "a + ( b + c < d + e )",
                        "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("a = b = c", new LawNode("a = b = c",
                new ArrayList<>(Arrays.asList("error_base_same_precedence_right_associativity_right")),
                new ArrayList<>(Arrays.asList("a + b ? c + d : e + f ? g + h : i + j")),
                new ArrayList<>()));

        result.put("a + b + c", new LawNode("a + b + c",
                new ArrayList<>(Arrays.asList("error_base_same_precedence_left_associativity_left")),
                new ArrayList<>(Arrays.asList("++ a - b + c", "a + b + c * d", "a && ( b || c ) && d", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put(" ++ a", new LawNode(" ++ a",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "a || f ( b || c , d || e )",
                        "a + b + c * d", "a + b * c || d + e * f", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("- -- a", new LawNode("- -- a",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "a || f ( b || c , d || e )",
                        "a + b + c * d", "a + b * c || d + e * f", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("a == b < c", new LawNode("a == b < c",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "a || f ( b || c , d || e )",
                        "a + b + c * d", "a + b * c || d + e * f", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("-- a + b", new LawNode("-- a + b",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_left")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "++ a - b + c",
                        "* a [ b + c ] + e", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("a && b || c", new LawNode("a && b || c",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_left")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "++ a - b + c",
                        "* a [ b + c ] + e", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("* ++ a", new LawNode("* ++ a",
                new ArrayList<>(Arrays.asList("error_base_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "a || f ( b || c , d || e )",
                        "a + b + c * d", "a + b * c || d + e * f", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        return result;
    }

    public HashMap<String, LawNode> getTree1(){
        HashMap<String, LawNode> result = new HashMap<>();


        result.put("seq1", new LawNode("seq1",
                new ArrayList<>(Arrays.asList("DuplicateOfAct")),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5", "alt_i1", "alt_iaa1", "alt_i0",
                        "alt_iaa0", "alt_ii", "alt_iiaa", "alt_iii_111", "alt_iiiaa_110",
                        "alt_iii_10", "alt_ie1", "alt_ie0", "alt_11", "alt_1", "alt_00", "alt_ieie_c_1",
                        "alt_ieie_c_01", "alt_ieix7e_c_00", "alt_ieix7e_c_1", "alt_ieix7e_c_001",
                        "alt_ieix7e_c_00001", "alt_ieie_c_0000001")),
                new ArrayList<>()));

        List<String>lows = Arrays.asList("DuplicateOfAct", "TooEarlyInSequence", "TooLateInSequence");

        List<String>parents = Arrays.asList("while_10", "while_110", "while_2_0", "while_2_10",
                "while_2_110", "while_while_0", "while_while_100", "while_while_1100", "while_while_aa_1100",
                "while_while_11100", "while_while_110100", "while_while_aa_110100", "do_0", "do_10");

        result.put("seq2", new LawNode("seq2",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("seq3", new LawNode("seq3",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("seq5", new LawNode("seq5",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "ConditionAfterBranch",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "LastFalseNoEnd",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch");
        parents = Arrays.asList("alt_iei_c_1", "alt_iei_c_01", "alt_iei_c_00");

        result.put("alt_i1", new LawNode("alt_i1",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("alt_iaa1", new LawNode("alt_iaa1",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("alt_i0", new LawNode("alt_i0",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("alt_iaa0", new LawNode("alt_iaa0",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("alt_ii", new LawNode("alt_ii",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("alt_iiaa", new LawNode("alt_iiaa",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));


        result.put("alt_iii_111", new LawNode("alt_iii_111",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("alt_iii_110", new LawNode("alt_iii_110",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));


        result.put("alt_iiiaa_110", new LawNode("alt_iiiaa_110",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("alt_iii_10", new LawNode("alt_iii_10",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));


        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "BranchNotNextToCondition",
                "ElseBranchAfterTrueCondition",
                "ConditionAfterBranch",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch",
                "LastConditionIsFalseButNoElse");

        parents = Arrays.asList("alt_11",
                "alt_1", "alt_00", "alt_ieie_c_1", "alt_ieie_c_01", "alt_ieie_c_00",
                "alt_ieix7e_c_00", "alt_ieix7e_c_1", "alt_ieix7e_c_001", "alt_ieix7e_c_00001",
                "alt_ieix7e_c_0000001", "do2ie_10", "do2ie_1100");

        result.put("alt_ie1", new LawNode("alt_ie1",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        result.put("alt_ie0", new LawNode("alt_ie0",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq1"))));

        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "BranchNotNextToCondition",
                "ElseBranchNotNextToLastCondition",
                "ElseBranchAfterTrueCondition",
                "CondtionNotNextToPrevCondition",
                "ConditionTooEarly",
                "ConditionTooLate",
                "ConditionAfterBranch",
                "DuplicateOfCondition",
                "NoNextCondition",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch",
                "LastConditionIsFalseButNoElse");
        parents = Arrays.asList("ido_eiwh_e_0",
                "ido_eiwh_e_110", "ido_eiwh_e_1110", "ido_eiwh_e_1010", "ido_eiwh_e_10110", "ido_eiwh_e_100");

        result.put("alt_11", new LawNode("alt_11",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));


        result.put("alt_1", new LawNode("alt_1",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));

        result.put("alt_00", new LawNode("alt_00",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));

        result.put("alt_ieie_c_1", new LawNode("alt_ieie_c_1",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));

        result.put("alt_ieie_c_01", new LawNode("alt_ieie_c_01",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));

        result.put("alt_ieie_c_00", new LawNode("alt_ieie_c_00",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));

        result.put("alt_ieix7e_c_00", new LawNode("alt_ieix7e_c_00",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));

        result.put("alt_ieix7e_c_1", new LawNode("alt_ieix7e_c_1",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));

        result.put("alt_ieix7e_c_001", new LawNode("alt_ieix7e_c_001",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));

        result.put("alt_ieix7e_c_00001", new LawNode("alt_ieix7e_c_00001",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));

        result.put("alt_ieix7e_c_0000001", new LawNode("alt_ieix7e_c_0000001",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "seq1"))));


        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "BranchNotNextToCondition",
                "ElseBranchAfterTrueCondition",
                "ConditionAfterBranch",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch",
                "LastConditionIsFalseButNoElse",
                "NoIterationAfterSuccessfulCondition",
                "LoopEndAfterSuccessfulCondition",
                "NoLoopEndAfterFailedCondition",
                "LoopEndsWithoutCondition",
                "LoopStartIsNotIteration",
                "LoopContinuedAfterFailedCondition",
                "IterationAfterFailedCondition",
                "NoConditionAfterIteration",
                "NoConditionBetweenIterations");

        parents = Arrays.asList("ido_ewh_10", "ido_ewh_00", "ido_ewh_010");

        result.put("do2ie_10", new LawNode("do2ie_10",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0"))));

        result.put("do2ie_1100", new LawNode("do2ie_1100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0"))));


        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "TooEarlyInSequence",
                "TooLateInSequence",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "BranchNotNextToCondition",
                "ElseBranchAfterTrueCondition",
                "ConditionAfterBranch",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch",
                "LastConditionIsFalseButNoElse",
                "NoIterationAfterSuccessfulCondition",
                "LoopEndAfterSuccessfulCondition",
                "NoLoopEndAfterFailedCondition",
                "LoopEndsWithoutCondition",
                "LoopStartIsNotCondition",
                "LoopContinuedAfterFailedCondition",
                "IterationAfterFailedCondition",
                "NoConditionAfterIteration",
                "NoConditionBetweenIterations");

        parents = Arrays.asList("ido_ewh_10", "ido_ewh_00", "ido_ewh_010");

        result.put("wie_100", new LawNode("wie_100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "while_10", "while_110", "while_2_0", "while_2_10",
                        "while_2_110", "while_while_0", "while_while_100", "while_while_1100", "while_while_aa_1100",
                        "while_while_11100", "while_while_110100", "while_while_aa_110100"))));

        result.put("wie_110", new LawNode("wie_110",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "while_10", "while_110", "while_2_0", "while_2_10",
                        "while_2_110", "while_while_0", "while_while_100", "while_while_1100", "while_while_aa_1100",
                        "while_while_11100", "while_while_110100", "while_while_aa_110100"))));

        result.put("wie_0", new LawNode("wie_0",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_ie1", "alt_ie0", "while_10", "while_110", "while_2_0", "while_2_10",
                        "while_2_110", "while_while_0", "while_while_100", "while_while_1100", "while_while_aa_1100",
                        "while_while_11100", "while_while_110100", "while_while_aa_110100"))));


        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "TooEarlyInSequence",
                "TooLateInSequence",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoIterationAfterSuccessfulCondition",
                "LoopEndAfterSuccessfulCondition",
                "NoLoopEndAfterFailedCondition",
                "LoopEndsWithoutCondition",
                "LoopStartIsNotCondition",
                "LoopContinuedAfterFailedCondition",
                "IterationAfterFailedCondition",
                "NoConditionAfterIteration",
                "NoConditionBetweenIterations");
        parents = Arrays.asList("wie_100", "wie_110", "wie_0", "do_then_w_100", "do_then_w_010", "do_then_w_0110",
                "wiei_1010", "wiei_1100", "wiei_1000", "wiei_0");

        result.put("while_10", new LawNode("while_10",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_110", new LawNode("while_110",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_2_0", new LawNode("while_2_0",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_2_10", new LawNode("while_2_10",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_2_110", new LawNode("while_2_110",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_while_0", new LawNode("while_while_0",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_while_100", new LawNode("while_while_100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_while_1100", new LawNode("while_while_1100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_while_aa_1100", new LawNode("while_while_aa_1100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_while_11100", new LawNode("while_while_11100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_while_110100", new LawNode("while_while_110100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("while_while_aa_110100", new LawNode("while_while_aa_110100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "TooEarlyInSequence",
                "TooLateInSequence",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoIterationAfterSuccessfulCondition",
                "LoopEndAfterSuccessfulCondition",
                "NoLoopEndAfterFailedCondition",
                "LoopEndsWithoutCondition",
                "LoopStartIsNotCondition",
                "LoopStartIsNotIteration",
                "LoopContinuedAfterFailedCondition",
                "IterationAfterFailedCondition",
                "NoConditionAfterIteration",
                "NoConditionBetweenIterations");
        parents = Arrays.asList("ido_ewh_10", "ido_ewh_00", "ido_ewh_010", "ido_eiwh_e_0",
                "ido_eiwh_e_110", "ido_eiwh_e_1110", "ido_eiwh_e_1010", "ido_eiwh_e_10110", "ido_eiwh_e_100");

        result.put("do_then_w_100", new LawNode("do_then_w_100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do_0", "do_10", "while_10", "while_110", "while_2_0", "while_2_10",
                        "while_2_110", "while_while_0", "while_while_100", "while_while_1100", "while_while_aa_1100",
                        "while_while_11100", "while_while_110100", "while_while_aa_110100"))));

        result.put("do_then_w_010", new LawNode("do_then_w_010",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do_0", "do_10", "while_10", "while_110", "while_2_0", "while_2_10",
                        "while_2_110", "while_while_0", "while_while_100", "while_while_1100", "while_while_aa_1100",
                        "while_while_11100", "while_while_110100", "while_while_aa_110100"))));

        result.put("do_then_w_0110", new LawNode("do_then_w_0110",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do_0", "do_10", "while_10", "while_110", "while_2_0", "while_2_10",
                        "while_2_110", "while_while_0", "while_while_100", "while_while_1100", "while_while_aa_1100",
                        "while_while_11100", "while_while_110100", "while_while_aa_110100"))));

        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "TooEarlyInSequence",
                "TooLateInSequence",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoIterationAfterSuccessfulCondition",
                "LoopEndAfterSuccessfulCondition",
                "NoLoopEndAfterFailedCondition",
                "LoopEndsWithoutCondition",
                "LoopStartIsNotIteration",
                "LoopContinuedAfterFailedCondition",
                "IterationAfterFailedCondition",
                "NoConditionAfterIteration",
                "NoConditionBetweenIterations");
        parents = Arrays.asList("do_then_w_100", "do_then_w_010", "do_then_w_0110");

        result.put("do_0", new LawNode("do_0",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        result.put("do_10", new LawNode("do_10",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("seq2", "seq3", "seq5"))));

        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "BranchNotNextToCondition",
                "CondtionNotNextToPrevCondition",
                "ConditionTooEarly",
                "ConditionTooLate",
                "ConditionAfterBranch",
                "DuplicateOfCondition",
                "NoNextCondition",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "LastFalseNoEnd",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch");
        parents = Arrays.asList("wiei_1010", "wiei_1100", "wiei_1000", "wiei_0", "do2_10", "do2_11000", "do2_11010");

        result.put("alt_iei_c_1", new LawNode("alt_iei_c_1",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_i1", "alt_iaa1", "alt_i0", "alt_iaa0", "alt_ii",
                        "alt_iiaa", "alt_iii_111", "alt_iii_110", "alt_iiiaa_110", "alt_iii_10"))));

        result.put("alt_iei_c_01", new LawNode("alt_iei_c_01",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_i1", "alt_iaa1", "alt_i0", "alt_iaa0", "alt_ii",
                        "alt_iiaa", "alt_iii_111", "alt_iii_110", "alt_iiiaa_110", "alt_iii_10"))));

        result.put("alt_iei_c_00", new LawNode("alt_iei_c_00",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_i1", "alt_iaa1", "alt_i0", "alt_iaa0", "alt_ii",
                        "alt_iiaa", "alt_iii_111", "alt_iii_110", "alt_iiiaa_110", "alt_iii_10"))));


        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "TooEarlyInSequence",
                "TooLateInSequence",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "BranchNotNextToCondition",
                "CondtionNotNextToPrevCondition",
                "ConditionTooEarly",
                "ConditionTooLate",
                "ConditionAfterBranch",
                "DuplicateOfCondition",
                "NoNextCondition",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "LastFalseNoEnd",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch",
                "NoIterationAfterSuccessfulCondition",
                "LoopEndAfterSuccessfulCondition",
                "NoLoopEndAfterFailedCondition",
                "LoopEndsWithoutCondition",
                "LoopStartIsNotCondition",
                "LoopContinuedAfterFailedCondition",
                "IterationAfterFailedCondition",
                "NoConditionAfterIteration",
                "NoConditionBetweenIterations");
        parents = Arrays.asList("ido_eiwh_e_0",
                "ido_eiwh_e_110", "ido_eiwh_e_1110", "ido_eiwh_e_1010", "ido_eiwh_e_10110", "ido_eiwh_e_100");

        result.put("wiei_1010", new LawNode("wiei_1010",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_iei_c_1", "alt_iei_c_01", "alt_iei_c_00",
                        "while_10", "while_110",
                        "while_2_0", "while_2_10", "while_2_110", "while_while_0", "while_while_100",
                        "while_while_1100", "while_while_aa_1100", "while_while_11100",
                        "while_while_110100", "while_while_aa_110100"))));

        result.put("wiei_1100", new LawNode("wiei_1100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_iei_c_1", "alt_iei_c_01", "alt_iei_c_00",
                        "while_10", "while_110",
                        "while_2_0", "while_2_10", "while_2_110", "while_while_0", "while_while_100",
                        "while_while_1100", "while_while_aa_1100", "while_while_11100",
                        "while_while_110100", "while_while_aa_110100"))));

        result.put("wiei_1000", new LawNode("wiei_1000",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_iei_c_1", "alt_iei_c_01", "alt_iei_c_00",
                        "while_10", "while_110",
                        "while_2_0", "while_2_10", "while_2_110", "while_while_0", "while_while_100",
                        "while_while_1100", "while_while_aa_1100", "while_while_11100",
                        "while_while_110100", "while_while_aa_110100"))));

        result.put("wiei_0", new LawNode("wiei_0",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_iei_c_1", "alt_iei_c_01", "alt_iei_c_00",
                        "while_10", "while_110",
                        "while_2_0", "while_2_10", "while_2_110", "while_while_0", "while_while_100",
                        "while_while_1100", "while_while_aa_1100", "while_while_11100",
                        "while_while_110100", "while_while_aa_110100"))));

        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "BranchNotNextToCondition",
                "CondtionNotNextToPrevCondition",
                "ConditionTooEarly",
                "ConditionTooLate",
                "ConditionAfterBranch",
                "DuplicateOfCondition",
                "NoNextCondition",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "LastFalseNoEnd",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch",
                "NoIterationAfterSuccessfulCondition",
                "LoopEndAfterSuccessfulCondition",
                "NoLoopEndAfterFailedCondition",
                "LoopEndsWithoutCondition",
                "LoopStartIsNotIteration",
                "LoopContinuedAfterFailedCondition",
                "IterationAfterFailedCondition",
                "NoConditionAfterIteration",
                "NoConditionBetweenIterations");

        result.put("do2_10", new LawNode("do2_10",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_iei_c_1", "alt_iei_c_01", "alt_iei_c_00"))));

        result.put("do2_11000", new LawNode("do2_11000",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_iei_c_1", "alt_iei_c_01", "alt_iei_c_00"))));

        result.put("do2_11010", new LawNode("do2_11010",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("alt_iei_c_1", "alt_iei_c_01", "alt_iei_c_00"))));

        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "TooEarlyInSequence",
                "TooLateInSequence",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "BranchNotNextToCondition",
                "ElseBranchAfterTrueCondition",
                "ConditionAfterBranch",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch",
                "LastConditionIsFalseButNoElse",
                "NoIterationAfterSuccessfulCondition",
                "LoopEndAfterSuccessfulCondition",
                "NoLoopEndAfterFailedCondition",
                "LoopEndsWithoutCondition",
                "LoopStartIsNotCondition",
                "LoopStartIsNotIteration",
                "LoopContinuedAfterFailedCondition",
                "IterationAfterFailedCondition",
                "NoConditionAfterIteration",
                "NoConditionBetweenIterations");


        result.put("ido_ewh_10", new LawNode("ido_ewh_10",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do_then_w_100", "do_then_w_010", "do_then_w_0110",
                        "wie_100", "wie_110", "wie_0", "do2ie_10", "do2ie_1100"))));

        result.put("ido_ewh_00", new LawNode("ido_ewh_00",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do_then_w_100", "do_then_w_010", "do_then_w_0110",
                        "wie_100", "wie_110", "wie_0", "do2ie_10", "do2ie_1100"))));

        result.put("ido_ewh_010", new LawNode("ido_ewh_010",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do_then_w_100", "do_then_w_010", "do_then_w_0110",
                        "wie_100", "wie_110", "wie_0", "do2ie_10", "do2ie_1100"))));

        lows = Arrays.asList("CorrespondingEndMismatched",
                "EndedDeeper",
                "EndedShallower",
                "WrongContext",
                "OneLevelShallower",
                "TooEarlyInSequence",
                "TooLateInSequence",
                "SequenceFinishedTooEarly",
                "SequenceFinishedNotInOrder",
                "DuplicateOfAct",
                "NoFirstCondition",
                "BranchNotNextToCondition",
                "ElseBranchNotNextToLastCondition",
                "ElseBranchAfterTrueCondition",
                "CondtionNotNextToPrevCondition",
                "ConditionTooEarly",
                "ConditionTooLate",
                "ConditionAfterBranch",
                "DuplicateOfCondition",
                "NoNextCondition",
                "BranchOfFalseCondition",
                "AnotherExtraBranch",
                "BranchWithoutCondition",
                "NoBranchWhenConditionIsTrue",
                "LastFalseNoEnd",
                "AlternativeEndAfterTrueCondition",
                "NoAlternativeEndAfterBranch",
                "LastConditionIsFalseButNoElse",
                "NoIterationAfterSuccessfulCondition",
                "LoopEndAfterSuccessfulCondition",
                "NoLoopEndAfterFailedCondition",
                "LoopEndsWithoutCondition",
                "LoopStartIsNotCondition",
                "LoopStartIsNotIteration",
                "LoopContinuedAfterFailedCondition",
                "IterationAfterFailedCondition",
                "NoConditionAfterIteration",
                "NoConditionBetweenIterations");

        parents = Arrays.asList();
        result.put("ido_eiwh_e_0", new LawNode("ido_eiwh_e_0",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do2_10", "do2_11000", "do2_11010",
                        "wiei_1010", "wiei_1100", "wiei_1000", "wiei_0",
                        "do_then_w_100", "do_then_w_010", "do_then_w_0110",
                        "ido_ewh_10", "ido_ewh_00", "ido_ewh_010", "alt_11",
                        "alt_1", "alt_00", "alt_ieie_c_1", "alt_ieie_c_01", "alt_ieie_c_00",
                        "alt_ieix7e_c_00", "alt_ieix7e_c_1", "alt_ieix7e_c_001", "alt_ieix7e_c_00001",
                        "alt_ieix7e_c_0000001"))));

        result.put("ido_eiwh_e_110", new LawNode("ido_eiwh_e_110",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do2_10", "do2_11000", "do2_11010",
                        "wiei_1010", "wiei_1100", "wiei_1000", "wiei_0",
                        "do_then_w_100", "do_then_w_010", "do_then_w_0110",
                        "ido_ewh_10", "ido_ewh_00", "ido_ewh_010", "alt_11",
                        "alt_1", "alt_00", "alt_ieie_c_1", "alt_ieie_c_01", "alt_ieie_c_00",
                        "alt_ieix7e_c_00", "alt_ieix7e_c_1", "alt_ieix7e_c_001", "alt_ieix7e_c_00001",
                        "alt_ieix7e_c_0000001"))));

        result.put("ido_eiwh_e_1110", new LawNode("ido_eiwh_e_1110",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do2_10", "do2_11000", "do2_11010",
                        "wiei_1010", "wiei_1100", "wiei_1000", "wiei_0",
                        "do_then_w_100", "do_then_w_010", "do_then_w_0110",
                        "ido_ewh_10", "ido_ewh_00", "ido_ewh_010", "alt_11",
                        "alt_1", "alt_00", "alt_ieie_c_1", "alt_ieie_c_01", "alt_ieie_c_00",
                        "alt_ieix7e_c_00", "alt_ieix7e_c_1", "alt_ieix7e_c_001", "alt_ieix7e_c_00001",
                        "alt_ieix7e_c_0000001"))));

        result.put("ido_eiwh_e_1010", new LawNode("ido_eiwh_e_1010",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do2_10", "do2_11000", "do2_11010",
                        "wiei_1010", "wiei_1100", "wiei_1000", "wiei_0",
                        "do_then_w_100", "do_then_w_010", "do_then_w_0110",
                        "ido_ewh_10", "ido_ewh_00", "ido_ewh_010", "alt_11",
                        "alt_1", "alt_00", "alt_ieie_c_1", "alt_ieie_c_01", "alt_ieie_c_00",
                        "alt_ieix7e_c_00", "alt_ieix7e_c_1", "alt_ieix7e_c_001", "alt_ieix7e_c_00001",
                        "alt_ieix7e_c_0000001"))));

        result.put("ido_eiwh_e_10110", new LawNode("ido_eiwh_e_10110",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do2_10", "do2_11000", "do2_11010",
                        "wiei_1010", "wiei_1100", "wiei_1000", "wiei_0",
                        "do_then_w_100", "do_then_w_010", "do_then_w_0110",
                        "ido_ewh_10", "ido_ewh_00", "ido_ewh_010", "alt_11",
                        "alt_1", "alt_00", "alt_ieie_c_1", "alt_ieie_c_01", "alt_ieie_c_00",
                        "alt_ieix7e_c_00", "alt_ieix7e_c_1", "alt_ieix7e_c_001", "alt_ieix7e_c_00001",
                        "alt_ieix7e_c_0000001"))));

        result.put("ido_eiwh_e_100", new LawNode("ido_eiwh_e_100",
                new ArrayList<>(lows),
                new ArrayList<>(parents),
                new ArrayList<>(Arrays.asList("do2_10", "do2_11000", "do2_11010",
                        "wiei_1010", "wiei_1100", "wiei_1000", "wiei_0",
                        "do_then_w_100", "do_then_w_010", "do_then_w_0110",
                        "ido_ewh_10", "ido_ewh_00", "ido_ewh_010", "alt_11",
                        "alt_1", "alt_00", "alt_ieie_c_1", "alt_ieie_c_01", "alt_ieie_c_00",
                        "alt_ieix7e_c_00", "alt_ieix7e_c_1", "alt_ieix7e_c_001", "alt_ieix7e_c_00001",
                        "alt_ieix7e_c_0000001"))));

        return result;
    }


    class LawNode {
        public String getNodeName() {
            return nodeName;
        }

        public ArrayList<String> getCurrentLows() {
            return currentLows;
        }

        public ArrayList<String> getParentNodes() {
            return parentNodes;
        }

        public ArrayList<String> getChildNodes() {
            return childNodes;
        }

        public LawNode(String nodeName, ArrayList<String> currentLows, ArrayList<String> parentNodes, ArrayList<String> childNodes) {
            this.nodeName = nodeName;
            this.currentLows = currentLows;
            this.parentNodes = parentNodes;
            this.childNodes = childNodes;
        }

        public String nodeName;

        public ArrayList<String> currentLows;
        public ArrayList<String> parentNodes;
        public ArrayList<String> childNodes;

    }

    class InteractionOrderComparator implements Comparator<InteractionEntity> {
        @Override
        public int compare(InteractionEntity a, InteractionEntity b) {
            return a.getOrderNumber() < b.getOrderNumber() ? -1 : a.getOrderNumber() == b.getOrderNumber() ? 0 : 1;
        }
    }

    class QuestionOrderComparator implements Comparator<QuestionEntity> {
        @Override
        public int compare(QuestionEntity a, QuestionEntity b) {
            return a.getId() < b.getId() ? -1 : a.getId() == b.getId() ? 0 : 1;
        }
    }
}
