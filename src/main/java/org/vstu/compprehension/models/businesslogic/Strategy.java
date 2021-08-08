package org.vstu.compprehension.models.businesslogic;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.util.Pair;
import org.vstu.compprehension.Service.DomainService;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.Decision;
import org.vstu.compprehension.models.entities.EnumData.DisplayingFeedbackType;
import org.vstu.compprehension.models.entities.EnumData.FeedbackType;
import org.vstu.compprehension.models.entities.EnumData.RoleInExercise;
import org.vstu.compprehension.utils.DomainAdapter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.lang.Math.abs;

@Component
public class Strategy extends AbstractStrategy {

    @Autowired
    private DomainService domainService;

    public QuestionRequest generateQuestionRequest(ExerciseAttemptEntity exerciseAttempt) {

        // Отдельная ветка для старта (взять некоторую часть возможных законов упражнения) - вопрос из середины графа
        if(exerciseAttempt.getQuestions() == null || exerciseAttempt.getQuestions().size() == 0){

            //TODO: Подготовить для разных деревьев
            HashMap<String, LawNode> tree = getTree();
            ArrayList<String> startTasks = new ArrayList<>(Arrays.asList("a + b + c * d", "* ++ a + b",
                    "a && ( b || c ) && d"));
            Random random = new Random();
            int nextQuestion = random.ints(0, startTasks.size()-1)
                    .findFirst()
                    .getAsInt();

            LawNode nextNode = tree.get(startTasks.get(nextQuestion));

            return getQuestionRequest(exerciseAttempt, nextNode);
        }
        // Для вопроса вытянуть все интеракции в змейку по дельте (изменения в правилльно примененных правилах и ошибки)
        QuestionEntity qe = exerciseAttempt.getQuestions().get(exerciseAttempt.getQuestions().size() - 1);
        InteractionEntity lastIE = null;
        ArrayList<Pair<Boolean, String>> allLaws = new ArrayList<>();
        ArrayList<InteractionEntity> ies = new ArrayList<>();

        if(qe.getInteractions() != null){
            ies = new ArrayList<>(qe.getInteractions());
        }

        Collections.sort(ies, new InteractionOrderComparator());

        for(InteractionEntity ie : ies){
            allLaws.addAll(findInteractionsDelta(lastIE, ie));

            lastIE = ie;
        }
        // Для каждого закона проверить, действительно ли он не угадан
        ArrayList<String> correctLaws = new ArrayList<>();
        ArrayList<String> incorrectLaws = new ArrayList<>();
        ArrayList<String> lastErrors = new ArrayList<>();

        //// Вопрос считается угаданным, если между последним правильным ответом и верным применением текущего закона...
        ////...была ошибка применения этого закона
        for(Pair<Boolean, String> interResult : allLaws){
            //Если текущая интеракция верна
            if(interResult.getFirst()){
                //Если с момента прошлого верного ответа были такие ошибки
                if(lastErrors.contains(interResult.getSecond())){
                    //Если закон считался верно изученным
                    //Считать, что он не был понят
                    if(correctLaws.contains(interResult.getSecond())){
                        correctLaws.remove(interResult.getSecond());
                    }
                    incorrectLaws.add(interResult.getSecond());
                }else{
                    //Если ранее закон считался не изученным
                    //Считать закон изученным
                    if(incorrectLaws.contains(interResult.getSecond())){
                        incorrectLaws.remove(interResult.getSecond());
                    }
                    correctLaws.add(interResult.getSecond());
                }

                lastErrors.clear();
            }else{
                lastErrors.add(interResult.getSecond());
            }
        }

        float correct = (float) correctLaws.size() / (float)(correctLaws.size() + incorrectLaws.size());
        // Получить граф вопросов и типичных ошибок к ним
        HashMap<String, LawNode> tree = getTree();
        LawNode currentNode = tree.get(qe.getQuestionText());
        // Если верно примененных законов в текущем вопросе достаточно (не менее 90%), то берется вопрос из больших
        if(correct > 0.9){
            //Если у узла есть "прямые" большие вопросы
            if(currentNode.parentNodes.size() > 0){
                Random random = new Random();
                int nextQuestion = random.ints(0, currentNode.parentNodes.size()-1)
                        .findFirst()
                        .getAsInt();

                LawNode nextNode = tree.get(currentNode.parentNodes.get(nextQuestion));

                return getQuestionRequest(exerciseAttempt, nextNode);
            }else{
                //// Если нет больших, проверить усвоенность всех целевых законов
                ArrayList<QuestionEntity> qes = new ArrayList<>(exerciseAttempt.getQuestions());
                Collections.sort(qes, new QuestionOrderComparator());

                ArrayList<Pair<Boolean, String>> allLawsHistory = new ArrayList<>();

                for (QuestionEntity taskqe : qes){
                    ArrayList<InteractionEntity> taskies = new ArrayList<>(taskqe.getInteractions());

                    Collections.sort(taskies, new InteractionOrderComparator());
                    InteractionEntity tasklastIE = null;
                    for(InteractionEntity ie : taskies){
                        allLawsHistory.addAll(findInteractionsDelta(tasklastIE, ie));
                        tasklastIE = ie;
                    }

                }

                correctLaws = new ArrayList<>();
                incorrectLaws = new ArrayList<>();
                lastErrors = new ArrayList<>();

                //// Вопрос считается угаданным, если между последним правильным ответом и верным применением текущего закона...
                ////...была ошибка применения этого закона
                for(Pair<Boolean, String> interResult : allLaws){
                    //Если текущая интеракция верна
                    if(interResult.getFirst()){
                        //Если с момента прошлого верного ответа были такие ошибки
                        if(lastErrors.contains(interResult.getSecond())){
                            //Если закон считался верно изученным
                            //Считать, что он не был понят
                            if(correctLaws.contains(interResult.getSecond())){
                                correctLaws.remove(interResult.getSecond());
                            }
                            incorrectLaws.add(interResult.getSecond());
                        }else{
                            //Если ранее закон считался не изученным
                            //Считать закон изученным
                            if(incorrectLaws.contains(interResult.getSecond())){
                                incorrectLaws.remove(interResult.getSecond());
                            }
                            correctLaws.add(interResult.getSecond());
                        }

                        lastErrors.clear();
                    }else{
                        lastErrors.add(interResult.getSecond());
                    }
                }

                ArrayList<String>allNodes = new ArrayList<>(tree.keySet());

                ArrayList<LawNode> nextNodes = new ArrayList<>();
                for(String lawName : allNodes){
                    LawNode childNode = tree.get(lawName);
                    //Собрать ноды с упущенными или некорректными законами
                    for(String currentError : childNode.currentLows){
                        //Если закон отсутствует в корректных, то он или ошибочен или не применялся
                        if(!correctLaws.contains(currentError) && !nextNodes.contains(childNode)){
                            nextNodes.add(childNode);
                        }
                    }

                }
                //Все законы усвоены
                if(nextNodes.size() == 0){
                    return new QuestionRequest();
                }

                Random random = new Random();
                int nextQuestion = random.ints(0, nextNodes.size()-1)
                        .findFirst()
                        .getAsInt();

                LawNode nextNode = nextNodes.get(nextQuestion);

                return getQuestionRequest(exerciseAttempt, nextNode);

            }
        }else{
            // ... иначе берется вопрос из меньших по графу, включающих законы, в которых была ошибка
            if(currentNode.childNodes.size() > 0){

                ArrayList<LawNode> nextNodes = new ArrayList<>();
                for(String lawName : currentNode.childNodes){
                    LawNode childNode = tree.get(lawName);
                    for(String currentError : incorrectLaws){
                        if(childNode.currentLows.contains(currentError) && !nextNodes.contains(childNode)){
                            nextNodes.add(childNode);
                        }
                    }
                }

                Random random = new Random();
                int nextQuestion = random.ints(0, nextNodes.size()-1)
                        .findFirst()
                        .getAsInt();

                LawNode nextNode = nextNodes.get(nextQuestion);

                return getQuestionRequest(exerciseAttempt, nextNode);
            }else{
                //// Если некуда больше дробить, используем текущий вопрос как референс целевых законов
                return getQuestionRequest(exerciseAttempt, currentNode);
            }
        }

    }

    @NotNull
    private QuestionRequest getQuestionRequest(ExerciseAttemptEntity exerciseAttempt, LawNode nextNode) {
        QuestionRequest qr = new QuestionRequest();
        qr.setExerciseAttempt(exerciseAttempt);
        ExerciseEntity exercise = exerciseAttempt.getExercise();
        Domain domain = DomainAdapter.getDomain(domainService.getDomainEntity(exercise.getDomain().getName()).getName());

        qr.setComplexity(1);
        qr.setSolvingDuration(30);

        List<Law> laws = new ArrayList<>(domain.getNegativeLaws()); //domainEntity.getLaws();
        List<Law> targetLaws = new ArrayList<>();

        for (Law l : laws) {
            if(nextNode.currentLows.contains(l.name)){
                targetLaws.add(l);
            }
        }
        qr.setTargetLaws(targetLaws);

        ArrayList<Concept> concepts = new ArrayList<>(domain.getConcepts());
        ArrayList<Concept> targetConcepts = new ArrayList<>();
        for (Concept c : concepts) {
            if(nextNode.currentLows.contains(c.getName())){
                targetConcepts.add(c);
            }
        }
        qr.setTargetConcepts(targetConcepts);

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
        if(exerciseAttempt.getQuestions().stream().count() >= 15){
            return Decision.FINISH;
        }
        return Decision.CONTINUE;
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

        for(ViolationEntity ve : current.getViolations()){
            result.add(Pair.of(false, ve.getLawName()));
        }

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

    public HashMap<String, LawNode> getTree(){
        HashMap<String, LawNode> result = new HashMap<>();

        result.put("a + b [ c + d ] + e", new LawNode("a + b [ c + d ] + e",
                new ArrayList<>(Arrays.asList("error_student_error_in_complex", "error_higher_precedence_right",
                        "error_same_precedence_left_associativity_left", "error_higher_precedence_left")),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "a + b + c",
                        "a && b || c", "-- a + b", "a == b < c", "- -- a", "* ++ a", "a * b + c * d",
                        "* ++ a + b", "++ a - b + c", "* a [ b + c ] + e", "a + b + c * d", "a && ( b || c ) && d",
                        "a + ( b + c < d + e )", "a || f ( b || c , d || e )"))));


        result.put("a + ( b + c < d + e )", new LawNode("a + ( b + c < d + e )",
                new ArrayList<>(Arrays.asList("error_student_error_in_complex", "error_higher_precedence_left",
                        "error_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("++ a || b + c", "a + b ? c + d : e + f", "-- ( -- a )", "a && ( b || c )"))));


        result.put("a || ( b || c , d || e )", new LawNode("a || ( b || c , d || e )",
                new ArrayList<>(Arrays.asList("error_student_error_in_complex", "error_student_error_strict_operands_order")),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "a && b || c",
                        "-- a + b", "a == b < c", "- -- a", "* ++ a", "a * b + c * d",
                        "* ++ a + b", "* a [ b + c ] + e"))));

        result.put("a && ( b || c ) && d", new LawNode("a && ( b || c ) && d",
                new ArrayList<>(Arrays.asList("error_student_error_in_complex", "error_same_precedence_left_associativity_left")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "a + b + c"))));

        result.put("a + b * c || d + e * f", new LawNode("a + b * c || d + e * f",
                new ArrayList<>(Arrays.asList("error_higher_precedence_right", "error_student_error_strict_operands_order")),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList("a == b < c", "- -- a", "* ++ a", "++ a || b + c", "a + b ? c + d : e + f"))));

        result.put("a + b + c * d", new LawNode("a + b + c * d",
                new ArrayList<>(Arrays.asList("error_higher_precedence_right", "error_same_precedence_left_associativity_left")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("a == b < c", "- -- a", "* ++ a", "a + b + c"))));

        result.put("* a [ b + c ] + e", new LawNode("* a [ b + c ] + e",
                new ArrayList<>(Arrays.asList("error_student_error_in_complex", "error_higher_precedence_left")),
                new ArrayList<>(Arrays.asList("a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "-- a + b", "a && b || c"))));

        result.put("a || f ( b || c , d || e )", new LawNode("a || f ( b || c , d || e )",
                new ArrayList<>(Arrays.asList("error_student_error_in_complex", "error_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("-- ( -- a )", "a && ( b || c )", "a == b < c", "- -- a", "* ++ a"))));

        result.put("++ a - b + c", new LawNode("++ a - b + c",
                new ArrayList<>(Arrays.asList("error_higher_precedence_left", "error_same_precedence_left_associativity_left")),
                new ArrayList<>(Arrays.asList("a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("-- a + b", "a && b || c", "a + b + c"))));

        result.put("a + b ? c + d : e + f ? g + h : i + j", new LawNode("a + b ? c + d : e + f ? g + h : i + j",
                new ArrayList<>(Arrays.asList("error_higher_precedence_left", "error_same_precedence_left_associativity_left")),
                new ArrayList<>(),
                new ArrayList<>(Arrays.asList("-- -- a", "++ a || b + c", "a + b ? c + d : e + f"))));

        result.put("* ++ a + b", new LawNode("* ++ a + b",
                new ArrayList<>(Arrays.asList("error_higher_precedence_left", "error_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("a && b || c", "-- a + b", "a == b < c", "- -- a", "* ++ a"))));

        result.put("a * b + c * d", new LawNode("a * b + c * d",
                new ArrayList<>(Arrays.asList("error_higher_precedence_left", "error_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>(Arrays.asList("a && b || c", "-- a + b", "a == b < c", "- -- a", "* ++ a"))));

        result.put("a + b ? c + d : e + f", new LawNode("a + b ? c + d : e + f",
                new ArrayList<>(Arrays.asList("error_student_error_strict_operands_order")),
                new ArrayList<>(Arrays.asList("a + b ? c + d : e + f ? g + h : i + j", "a + b * c || d + e * f",
                        "a || ( b || c , d || e )")),
                new ArrayList<>()));

        result.put("++ a || b + c", new LawNode("++ a || b + c",
                new ArrayList<>(Arrays.asList("error_student_error_strict_operands_order")),
                new ArrayList<>(Arrays.asList("a + b ? c + d : e + f ? g + h : i + j", "a + b * c || d + e * f",
                        "a || ( b || c , d || e )")),
                new ArrayList<>()));

        result.put("a && ( b || c )", new LawNode("a && ( b || c )",
                new ArrayList<>(Arrays.asList("error_student_error_in_complex")),
                new ArrayList<>(Arrays.asList("a || f ( b || c , d || e )", "* a [ b + c ] + e",
                        "a && ( b || c ) && d", "a || ( b || c , d || e )", "a + ( b + c < d + e )",
                        "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("-- ( -- a )", new LawNode("-- ( -- a )",
                new ArrayList<>(Arrays.asList("error_student_error_in_complex")),
                new ArrayList<>(Arrays.asList("a || f ( b || c , d || e )", "* a [ b + c ] + e",
                        "a && ( b || c ) && d", "a || ( b || c , d || e )", "a + ( b + c < d + e )",
                        "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("-- -- a", new LawNode("-- -- a",
                new ArrayList<>(Arrays.asList("error_same_precedence_right_associativity_right")),
                new ArrayList<>(Arrays.asList("a + b ? c + d : e + f ? g + h : i + j")),
                new ArrayList<>()));

        result.put("a + b + c", new LawNode("a + b + c",
                new ArrayList<>(Arrays.asList("error_same_precedence_left_associativity_left")),
                new ArrayList<>(Arrays.asList("++ a - b + c", "a + b + c * d", "a && ( b || c ) && d", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put(" ++ a", new LawNode(" ++ a",
                new ArrayList<>(Arrays.asList("error_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "a || f ( b || c , d || e )",
                        "a + b + c * d", "a + b * c || d + e * f", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("- -- a", new LawNode("- -- a",
                new ArrayList<>(Arrays.asList("error_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "a || f ( b || c , d || e )",
                        "a + b + c * d", "a + b * c || d + e * f", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("a == b < c", new LawNode("a == b < c",
                new ArrayList<>(Arrays.asList("error_higher_precedence_right")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "a || f ( b || c , d || e )",
                        "a + b + c * d", "a + b * c || d + e * f", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("-- a + b", new LawNode("-- a + b",
                new ArrayList<>(Arrays.asList("error_higher_precedence_left")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "++ a - b + c",
                        "* a [ b + c ] + e", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

        result.put("a && b || c", new LawNode("a && b || c",
                new ArrayList<>(Arrays.asList("error_higher_precedence_left")),
                new ArrayList<>(Arrays.asList("a * b + c * d", "* ++ a + b", "++ a - b + c",
                        "* a [ b + c ] + e", "a + ( b + c < d + e )", "a + b [ c + d ] + e")),
                new ArrayList<>()));

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
