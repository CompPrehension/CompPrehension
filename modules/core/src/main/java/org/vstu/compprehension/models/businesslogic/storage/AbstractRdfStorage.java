package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.common.StringHelper;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.storage.stats.BitmaskStat;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataBaseRepository;
import org.vstu.compprehension.utils.Checkpointer;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Long.bitCount;

@Log4j2
public abstract class AbstractRdfStorage {
    /**
     * Default prefixes
     */
    final static NamespaceUtil NS_root = new NamespaceUtil("http://vstu.ru/poas/");
    public final static NamespaceUtil NS_code = new NamespaceUtil(NS_root.get("code#"));
    //// final static NamespaceUtil NS_namedGraph = new NamespaceUtil("http://named.graph/");
    final static NamespaceUtil NS_file = new NamespaceUtil("ftp://plain.file/");
    final static NamespaceUtil NS_graphs = new NamespaceUtil(NS_root.get("graphs/"));
    //    graphs:
    //    class NamedGraph
    //		modifiedAt: datetime   (1..1)
    //      dependsOn: NamedGraph  (0..*)
    public final static NamespaceUtil NS_questions = new NamespaceUtil(NS_root.get("questions/"));
    final static NamespaceUtil NS_classQuestionTemplate = new NamespaceUtil(NS_questions.get("QuestionTemplate#"));
    final static NamespaceUtil NS_classQuestion = new NamespaceUtil(NS_questions.get("Question#"));
    /* questions:
     * class QuestionTemplate:
     *   name: str   (1..1)
     *   has_graph_qt    \ NamedGraph or rdf:nil
     *   has_graph_qt_s  / (1..1)
     *   ...
     * class Question:
     *   name: str    (1..1)
     *   has_template (1..1)
     *   has_graph_qt   \
     *   has_graph_qt_s  | NamedGraph or rdf:nil
     *   has_graph_q     | (1..1)
     *   has_graph_q_s  /
     *
     *   solution_structural_complexity : int
     *   solution_steps : int
     *   distinct_errors_count : int
     *   integral_complexity: float (0 <= value <= 1)  <- универсальная оценка вопроса для использования в стратегии
     *	has_tag: domain:Tag   [1..*]
     *	has_law: domain:Law [1..*]
     *	has_violation: domain:Violation   [1..*]
     *   ...
     * */
    /*final static NamespaceUtil NS_oop = new NamespaceUtil(NS_root.get("oop/"));*/
    // hardcoded FTP location:
//    static String FTP_BASE = "ftp://poas:{6689596D2347FA1287A4FD6AB36AA9C8}@vds84.server-1.biz/ftp_dir/compp/";
//    static String FTP_DOWNLOAD_BASE = "http://vds84.server-1.biz/misc/ftp/compp/";
    public static String FTP_BASE = "file:///c:/data/compp/";  // local dir is supported too (for debugging)
    public static String FTP_DOWNLOAD_BASE = FTP_BASE;
    static Lang DEFAULT_RDF_SYNTAX = Lang.TURTLE;
    static Map<String, String> DOMAIN_TO_ENDPOINT;
    Domain domain;

    protected List<Double> complexityStatCache = null;
    protected List<Double> questionSolutionLengthStatCache = null;

    /**
     * Temporary storage (cache) for RDF graphs from remote RDF DB (ex. Fuseki)
     */
    Dataset dataset = null;
    RemoteFileService fileService = null;
    QuestionMetadataManager questionMetadataManager = null;

    QuestionMetadataManager getQuestionMetadataManager() {
        if (this.questionMetadataManager == null) {
            if (this.domain != null) {
                QuestionMetadataBaseRepository<? extends QuestionMetadataEntity> repo =
                        domain.getQuestionMetadataRepository();
                if (repo != null)
                    questionMetadataManager = new QuestionMetadataManager(repo);
            }
        }
        return questionMetadataManager;
    }

    /**
     * Make a SPARQL Update query that removes all (s, p, *) triples and inserts one (s, p, o) triple into a named
     * graph ng.
     *
     * @param ng named graph
     * @param s  subject
     * @param p  predicate / property
     * @param o  object
     * @return UpdateRequest object
     */
    static UpdateRequest makeUpdateTripleQuery(Object ng, Object s, Object p, Object o) {
        // delete & insert new triple
        UpdateBuilder ub3 = new UpdateBuilder();
        ub3.addInsert(ng, s, p, o); // quad
        Var obj = Var.alloc("obj");
        // quad
        ub3.addDelete(ng, s, p, obj); // quad
        ub3.addWhere(new WhereBuilder()
                // OPTIONAL allows inserting new triples without replacing
                .addOptional(new WhereBuilder()
                        .addGraph(ng, s, p, obj))
        );
        UpdateRequest ur = ub3.buildRequest();
        //// System.out.println(ur.toString());
        return ur;
    }

    public static NamespaceUtil questionSubgraphPropertyFor(GraphRole role) {
        return new NamespaceUtil(NS_questions.get("has_graph_" + role.ns().base()));
    }

    // could the two following methods be collapsed into one?? (replacing metaMgr with HashMap from it) - no, lists clash.
    private int conceptsToBitmask(List<Concept> concepts, QuestionMetadataManager metaMgr) {
        int conceptBitmask = 0; //
        for (Concept t : concepts) {
            String name = t.getName();
            long newBit = QuestionMetadataManager.namesToBitmask(List.of(name), metaMgr.conceptName2bit);
            if (newBit == 0) {
                // make use of children
                for (Concept child : domain.getChildrenOfConcept(name)) {
                    newBit = QuestionMetadataManager.namesToBitmask(List.of(child.getName()), metaMgr.conceptName2bit);
                    if (newBit != 0)
                        break;
                }
            }
            conceptBitmask |= newBit;
        }
        return conceptBitmask;
    }

    private int lawsToBitmask(List<Law> laws, QuestionMetadataManager metaMgr) {
        int lawBitmask = 0; //
        // !! violations are not positive laws!
//        for (Law t : laws) {
//            String name = t.getName();
//            int newBit = QuestionMetadataManager.namesToBitmask(List.of(name), metaMgr.violationName2bit);
//            if (newBit == 0) {
//                // make use of children
//                for (Law child : domain.getPositiveLawWithImplied(name)) {  // !! todo: Note: positive only.
//                    newBit = QuestionMetadataManager.namesToBitmask(List.of(child.getName()), metaMgr.violationName2bit);
//                    if (newBit != 0)
//                        break;
//                }
//            }
//            lawBitmask |= newBit;
//        }
        return lawBitmask;
    }

    private List<QuestionMetadataEntity> findLastNQuestionsMeta(QuestionRequest qr, int n) {
        List<QuestionEntity> list = qr.getExerciseAttempt().getQuestions();
        List<QuestionMetadataEntity> result = new ArrayList<>();
        long toSkip = Math.max(0, list.size() - n);
        for (QuestionEntity questionEntity : list) {
            if (toSkip > 0) {
                toSkip--;
                continue;
            }
            QuestionMetadataEntity meta = questionEntity.getOptions().getMetadata();
            if (meta != null)
                result.add(meta);
        }
        return result;
    }

    public List<Question> searchQuestionsWithAdvancedMetadata(QuestionRequest qr, int limit) {

        Checkpointer ch = new Checkpointer(log);

        QuestionMetadataManager metaMgr = getQuestionMetadataManager();
        int queryLimit = (limit * 3 + Optional.ofNullable(qr.getDeniedQuestionNames()).map(List::size).orElse(0));
        int hardLimit = 25;

        Random random = domain.getRandomProvider().getRandom();

        // 0.8 .. 1.2
        double changeCoeff = 0.85f + 0.3f * random.nextDouble();
        double complexity = qr.getComplexity() * changeCoeff;
        int solutionSteps = qr.getSolvingDuration();  // 0..10
        solutionSteps += random.nextInt(5) - 2;
        complexity = metaMgr.wholeBankStat.complexityStat.rescaleExternalValue(complexity, 0, 1);
        solutionSteps = (int)Math.round(metaMgr.wholeBankStat.solutionStepsStat.rescaleExternalValue(solutionSteps, 0, 10));

        List<QuestionMetadataEntity> foundQuestionMetas;
        List<Integer> templatesInUse = qr.getDeniedQuestionTemplateIds();

        long targetConceptsBitmask = conceptsToBitmask(qr.getTargetConcepts(), metaMgr);
        long allowedConceptsBitmask = conceptsToBitmask(qr.getAllowedConcepts(), metaMgr);
        long deniedConceptsBitmask = conceptsToBitmask(qr.getDeniedConcepts(), metaMgr);
        long unwantedConceptsBitmask = findLastNQuestionsMeta(qr, 4).stream()
                .mapToLong(QuestionMetadataEntity::getConceptBits).
                reduce((t, t2) -> t | t2).orElse(0);

        if (bitCount(unwantedConceptsBitmask) > 2) {
            // drop several bits from targets (randomly chosen from unwanted)
            targetConceptsBitmask &= ~(unwantedConceptsBitmask ^ (unwantedConceptsBitmask & random.nextLong()));
        }

        // TODO: use laws for expr Domain
        long targetLawsBitmask = lawsToBitmask(qr.getTargetLaws(), metaMgr);
//        long allowedLawsBitmask= lawsToBitmask(qr.getAllowedLaws(), metaMgr);
        long deniedLawsBitmask = lawsToBitmask(qr.getDeniedLaws(), metaMgr);
        long unwantedLawsBitmask = findLastNQuestionsMeta(qr, 4).stream()
                .mapToLong(QuestionMetadataEntity::getLawBits).
                reduce((t, t2) -> t | t2).orElse(0);

//        List<Integer> selectedLawKeys = fit3Bitmasks(targetLawsBitmask, allowedLawsBitmask,
//                deniedLawsBitmask, unwantedLawsBitmask, queryLimit * 3 / 2, (!->) metaMgr.wholeBankStat.getViolationStat(), random);
        // */

        // take violations from all questions is exercise attempt
        long unwantedViolationsBitmask = qr.getExerciseAttempt().getQuestions().stream()
                .map(qd -> qd.getOptions().getMetadata())
                .filter(Objects::nonNull)
                .mapToLong(QuestionMetadataEntity::getViolationBits)
                .reduce((t, t2) -> t | t2).orElse(0);

        ch.hit("searchQuestionsAdvanced - bitmasks prepared");

        foundQuestionMetas = metaMgr.findQuestionsAroundComplexityStepsWithoutTemplates(
                complexity, solutionSteps,
                targetConceptsBitmask, deniedConceptsBitmask,
                targetLawsBitmask, deniedLawsBitmask,
                templatesInUse,
                queryLimit);

        // TODO: use tags as well
        if (foundQuestionMetas.isEmpty()) {

            List<Long> selectedTraceConceptKeys = fit3Bitmasks(targetConceptsBitmask, allowedConceptsBitmask,
                    deniedConceptsBitmask, 0 /*unwantedConceptsBitmask*/, queryLimit, metaMgr.wholeBankStat.getTraceConceptStat(), random);

            foundQuestionMetas = metaMgr.findQuestionsByConceptEntriesLawBitmasksWithoutTemplates(
                    selectedTraceConceptKeys, deniedConceptsBitmask,
                    targetLawsBitmask, deniedLawsBitmask,
                    templatesInUse);
        }
//        foundQuestionMetas = metaMgr.findQuestionsByBitmasksWithoutTemplates(
//                targetConceptsBitmask, deniedConceptsBitmask,
//                targetLawsBitmask, deniedLawsBitmask,
//                templatesInUse);
//
//        if (templatesInUse.isEmpty()) {
//            foundQuestionMetas = metaMgr.findQuestionsByConcepts(selectedTraceConceptKeys /*, queryLimit*/);
//        } else {
//            foundQuestionMetas = metaMgr.findQuestionsByConceptsWithoutTemplates(selectedTraceConceptKeys, templatesInUse);
//        }
        ch.hit("searchQuestionsAdvanced - query executed with " + foundQuestionMetas.size() + " candidates");

        // handle empty result
        if (foundQuestionMetas.isEmpty() && limit < 1000) {
            ch.since_start("searchQuestionsAdvanced - recurse to search better solution, after");
            return searchQuestionsWithAdvancedMetadata(qr, limit * 4);
        }

        // too many entries, drop one by one
        int iterCount = 0;
        int iterLimit = foundQuestionMetas.size();
        int actualLimit = Math.min(hardLimit, queryLimit * 4);
        while (foundQuestionMetas.size() > actualLimit && iterCount < iterLimit) {
            int randIdx = random.nextInt(foundQuestionMetas.size());
            QuestionMetadataEntity qm = foundQuestionMetas.get(randIdx);
            if (foundQuestionMetas.size() > actualLimit * 2 || (qm.getConceptBits() & unwantedConceptsBitmask) > 0
                    && (qm.getViolationBits() & unwantedViolationsBitmask) > 0
            ) {
                foundQuestionMetas.remove(randIdx);
            }
            ++iterCount;
        }

        ch.hit("searchQuestionsAdvanced - reduced to " + foundQuestionMetas.size() + " candidates");

        // filter foundQuestionMetas, ranking by complexity, solution steps
        foundQuestionMetas = filterQuestionMetas(foundQuestionMetas,
                complexity,
                solutionSteps,
                targetConceptsBitmask,
                unwantedConceptsBitmask,
                unwantedLawsBitmask,
                unwantedViolationsBitmask,
                Math.min(limit, hardLimit)  // Note: queryLimit > limit
            );

        ch.hit("searchQuestionsAdvanced - filtered up to " + foundQuestionMetas.size() + " candidates");

        List<Question> loadedQuestions = loadQuestions(foundQuestionMetas);

        ch.hit("searchQuestionsAdvanced - files loaded");
        ch.since_start("searchQuestionsAdvanced - completed with " + loadedQuestions.size() + " questions");

        return loadedQuestions;
    }

    private List<QuestionMetadataEntity> filterQuestionMetas(
            List<QuestionMetadataEntity> given,
            double scaledComplexity,
            double scaledSolutionLength,
            long targetConceptsBitmask,
            long unwantedConceptsBitmask,
            long unwantedLawsBitmask,
            long unwantedViolationsBitmask,
            int limit) {
        if (given.size() <= limit)
            return given;

        List<QuestionMetadataEntity> ranking1 = given.stream()
                .sorted(Comparator.comparingDouble(q -> q.complexityAbsDiff(scaledComplexity)))
                .collect(Collectors.toList());

        List<QuestionMetadataEntity> ranking2 = given.stream()
                .sorted(Comparator.comparingDouble(q -> q.getSolutionStepsAbsDiff(scaledSolutionLength)))
                .collect(Collectors.toList());

        List<QuestionMetadataEntity> ranking3 = given.stream()
                .sorted(Comparator.comparingInt(
                        q -> bitCount(q.getConceptBits() & unwantedConceptsBitmask)
                        + bitCount(q.getLawBits() & unwantedLawsBitmask)
                        + bitCount(q.getViolationBits() & unwantedViolationsBitmask)
                        - bitCount(q.getConceptBits() & targetConceptsBitmask) * 3
                ))
                .collect(Collectors.toList());


        // want more diversity in question ?? count control values -> take more with '1' not with '0'
        List<QuestionMetadataEntity> ranking6 = given.stream()
                .sorted(Comparator.comparingInt(q -> -q.getDistinctErrorsCount()))
                .collect(Collectors.toList());

        List<QuestionMetadataEntity> finalRanking = given.stream()
                .sorted(Comparator.comparingInt(q -> (
                        ranking1.indexOf(q) +
                        ranking2.indexOf(q) +
                        ranking3.indexOf(q) +
                        ranking6.indexOf(q)
                        )))
                .collect(Collectors.toList());

        return finalRanking.subList(0, limit);
    }

    private List<Long> fit3Bitmasks(long targetBitmask, long allowedBitmask, long deniedBitmask, long unwantedBitmask,
                                       int resCountLimit, BitmaskStat bitStat, Random random) {
        // ensure no overlap
        targetBitmask &= ~deniedBitmask;
        allowedBitmask &= ~deniedBitmask;
        allowedBitmask &= ~targetBitmask;
//        unwantedBitmask &= ~deniedBitmask;


        int alwBits;  // how many optional bits can be added to target, keeping the sample's size big enough.
        int optionalBits = bitCount(allowedBitmask);
        List<Long> keysCriteria = null;
        for (alwBits = optionalBits /*- 1*/; alwBits >= 0; --alwBits) {
            keysCriteria = bitStat.keysWithBits(
                    targetBitmask,
                    allowedBitmask, alwBits,
                    deniedBitmask);
            if (bitStat.sumForKeys(keysCriteria) >= resCountLimit)
                break;
        }

//        List<Integer> keysCriteriaReducingTargets = null;
        if (alwBits == -1) {
            System.out.println("Target concepts can't be reached fully ...");
            int newTargetCount = Math.min(resCountLimit, 2);
            int tarBits;  // how many target bits to take, since current criteria is too strong.
            int targetBits = bitCount(targetBitmask);
            for (tarBits = targetBits - 1; tarBits >= 0 && (tarBits > 0 || keysCriteria.isEmpty()); --tarBits) {  // not: tarBits >= 0
                keysCriteria = bitStat.keysWithBits(
                        0,
                        targetBitmask, tarBits,
                        deniedBitmask);
                if (bitStat.sumForKeys(keysCriteria) >= newTargetCount)
                    break;
            }
            if (keysCriteria.isEmpty() /*tarBits < 1*/) {
                System.out.println("Target concepts are impossible ...");
                throw new RuntimeException("Denied [concepts] don't allow any questions to be found!.");
            }

            return keysCriteria;
        }


        // continuing with rich set of candidates ...
        // decide how to "extend" targets with optionals
        // play with deletion of keys, checking others to be still enough ...
        int currSum = bitStat.sumForKeys(keysCriteria);
        // init lookup
        HashMap<Long, Integer> key2count = new HashMap<>();
        for (long key : keysCriteria) {
            key2count.put(key, bitStat.getItems().get(key));
        }
        // remove keys randomly while curr sum is still enough
        boolean stop = false;
        while (!stop && currSum > 0) {
            stop = true;
            for (long key : List.copyOf(key2count.keySet())) {
                int count = key2count.get(key);
                if (currSum - count > resCountLimit) {  // we can try deleting this one
                    float chance = count / (float)(currSum - resCountLimit);
                    if (random.nextFloat() < chance) {  // with chance, drop it
                        key2count.remove(key);
                        currSum -= count;
                        stop = false;
                        /// log.info("Dropped key of bitmask: " + key);
                    }
                }
            }
        }
        Set<Long> selectedBitKeys = key2count.keySet();
        return List.copyOf(selectedBitKeys);
    }

    private List<Question> loadQuestions(Collection<QuestionMetadataEntity> metas) {
        List<Question> list = new ArrayList<>();
        for (QuestionMetadataEntity meta : metas) {
            Question question = loadQuestion(meta);
            if (question != null) {
                question.setMetadata(meta);
                question.getQuestionData().getOptions().setMetadata(meta);
                list.add(question);
            }
        }
        return list;
    }


    /**
     * Find question templates in the questions bank
     * @param qr QuestionRequest
     * @param limit maximum questions to return (set 0 or less to disable; note: this may be slow)
     * // (ignore randomSeed) param randomSeed influence randomized order (if selection from many candidates is required)
     * @return questions found or empty list if the requirements cannot be satisfied
     */
    public List<Question> searchQuestions(QuestionRequest qr, int limit) {
        if (getQuestionMetadataManager() != null) {
            // ...
            return searchQuestionsWithAdvancedMetadata(qr, limit);
        }

        int queryLimit = (limit * 2 + Optional.ofNullable(qr.getDeniedQuestionNames()).map(List::size).orElse(0));

        /*Model qG =*/ getGraph(NS_questions.base());

        Checkpointer ch = new Checkpointer(log);

        String complexity_cmp_op = "";
        if (qr.getComplexitySearchDirection() != null)
            complexity_cmp_op = (qr.getComplexitySearchDirection().getValue() < 0)? "<=" : ">=";
        //
        List<Double> complStat = complexityStat();  // [min, avg, max]
        double complexity = qr.getComplexity();  // 0..1
        double desiredComplexity = (complStat.get(1));  // avg
        if (complexity <= 0.5) {
            desiredComplexity = (
                    complStat.get(0) +
                            (complexity * 2) * (complStat.get(1) - complStat.get(0))
            );  // min + weight * (avg - min)
        }
        else if (complexity > 0.5) {
            desiredComplexity = (
                    complStat.get(1) +
                            ((complexity - 0.5) * 2) * (complStat.get(2) - complStat.get(1))
            );  // avg + weight * (max - avg)
        }
        String complexity_threshold = "" + desiredComplexity;


        List<Double> SolutionLengthStat = questionSolutionLengthStat();  // [min, avg, max]
        int duration = qr.getSolvingDuration();  // 1..10
        int desiredSolutionSteps = (int) Math.round(SolutionLengthStat.get(1));  // avg
        if (duration < 5) {
            desiredSolutionSteps = (int) Math.round(
                    SolutionLengthStat.get(0) +
                            ((duration - 1) / 4.0) * (SolutionLengthStat.get(1) - SolutionLengthStat.get(0))
            );  // min + weight * (avg - min)
        }
        else if (duration > 5) {
            desiredSolutionSteps = (int) Math.round(
                    SolutionLengthStat.get(1) +
                            ((duration - 5) / 5.0) * (SolutionLengthStat.get(2) - SolutionLengthStat.get(1))
            );  // avg + weight * (max - avg)
        }

        List<Concept> targetConcepts = qr.getTargetConcepts();
        String targetConceptStr = null;
        if (!targetConcepts.isEmpty()) {
            // "loop","break","concept3", ...
            targetConceptStr = targetConcepts.stream().map(c -> "\"" + c.getName() + "\"").collect(Collectors.joining(","));
        }

        StringBuilder query = new StringBuilder("select distinct ?name ?complexity ?solution_steps \n" +  /// (?s as ?qUri)
                //// "#(count(distinct ?law) as ?law_count) (count(distinct ?concept) as ?concept_count)\n" +
                "(group_concat(distinct ?law;separator=\"; \") as ?laws)\n" +
                "(group_concat(distinct ?concept;separator=\"; \") as ?concepts)\n" +
//                "(max(?concepts_exist) as ?concepts_here)\n" +
                " {  GRAPH <" + NS_questions.base() + "> {\n" +
                "\t?s a <" + NS_questions.get("Question") + "> .\n" +
                "    ?s <" + NS_questions.get("name") + "> ?name.\n" +
                "    ?s <" + NS_questions.get("integral_complexity") + "> ?complexity.\n" +
                "    ?s <" + NS_questions.get("solution_steps") + "> ?solution_steps.\n" +
                "    ?s <" + NS_questions.get("has_concept") + "> ?concept.\n" +
                "    ?s <" + NS_questions.get("has_violation") + "> ?law.\n" +
                (
                    complexity_cmp_op.isEmpty() ? "" :
                    "    filter(?complexity " + complexity_cmp_op + " " + complexity_threshold + ").\n"
                ) +
                "    bind(abs(" + complexity_threshold + " - ?complexity) as ?compl_diff).\n" +
                "    bind(abs(" + desiredSolutionSteps + " - ?solution_steps) as ?steps_diff).\n" +
                //  question data is present (not nil)
                (targetConceptStr != null ?
//                "    FILTER EXISTS{?s <" + NS_questions.get("has_concept") + "> ?cpt. FILTER (?cpt IN (" + targetConceptStr + "))}.\n" : "") +
                "    FILTER (?concept IN (" + targetConceptStr + ")).\n" : "") +
                ("    filter not exists { ?s <" + NS_questions.get("has_graph_q_data")
                     + "> () }.\n"  /// rdf:nil
                ));
        // add "denied" constraints
            //  "    filter not exists { ?s <has_concept> \"denied_concept\" }\n" +
            //  "    filter not exists { ?s <http://vstu.ru/poas/questions/has_law> \"denied_law\" }\n" +
        for (Concept concept : qr.getDeniedConcepts()) {
            String denied_concept = concept.getName();
            query.append("    filter not exists { ?s <")
                    .append(NS_questions.get("has_concept"))
                    .append("> \"")
                    .append(denied_concept).append("\" }\n");
        }
        if (qr.getDeniedLaws() != null)
        for (Law law : qr.getDeniedLaws()) {
            String denied_law = law.getName();
            query.append("    filter not exists { ?s <")
                    .append(NS_questions.get("has_violation"))
                    .append("> \"")
                    .append(denied_law).append("\" }\n");
        }
        // Laws (violations)
        if (!qr.getTargetLaws().isEmpty())
        {
            // older version: just require all laws
            //  "#    filter exists { ?s <http://vstu.ru/poas/questions/has_law> \"target_law\" }\n";

            /* newer version: allow omitting rightmost laws if all laws cannot be here at once
            filter exists { {
                ?s <.../has_violation> "LastFalseNoEnd" .
                ?s <.../has_violation> "NoConditionAfterIteration" .
                ?s <.../has_violation> "NoLoopEndAfterFailedCondition" .
                ?s <.../has_violation> "NoFirstCondition" .
                ?s <.../has_violation> "LoopStartIsNotIteration" .
              } UNION {
                ?s <.../has_violation> "LastFalseNoEnd" .
                ?s <.../has_violation> "NoConditionAfterIteration" .
                ?s <.../has_violation> "NoLoopEndAfterFailedCondition" .
                ?s <.../has_violation> "NoFirstCondition" .
              } UNION {
                ?s <.../has_violation> "LastFalseNoEnd" .
                ?s <.../has_violation> "NoConditionAfterIteration" .
                ?s <.../has_violation> "NoLoopEndAfterFailedCondition" .
              } UNION {
                ?s <.../has_violation> "LastFalseNoEnd" .
                ?s <.../has_violation> "NoConditionAfterIteration" .
              } UNION {
                ?s <.../has_violation> "LastFalseNoEnd" .
              }
            }
             */
            String has_violation = NS_questions.get("has_violation");
            query.append("    filter exists {{\n");
            int lawsN = qr.getTargetLaws().size();
            boolean allOnly = qr.getLawsSearchDirection() != null && qr.getLawsSearchDirection().getValue() > 0;

            for (int i = lawsN; i > 0; --i) {
                if (i != lawsN) {
                    query.append("      } UNION {\n");
                }
                for (Law law : qr.getTargetLaws().subList(0, i)) {
                    String target_law = law.getName();
                    query.append("        ?s <")
                            .append(has_violation)
                            .append("> \"")
                            .append(target_law).append("\" .\n");
                }
                if (allOnly)
                    break;
            }
            query.append("    }}");
        }
//                "#    filter exists { ?s <http://vstu.ru/poas/questions/has_concept> \"target_concept\" }\n" +
        // query footer
        query.append("  }}\n" +
                "group by ?name ?complexity ?solution_steps ?compl_diff\n" +
                "order by ?compl_diff ?steps_diff");
//                "order by DESC(max(?concepts_exist)) ?compl_diff ?steps_diff");
//                "order by DESC(?concepts_here) ?compl_diff ?steps_diff");
        if (limit > 0)
            query.append("\n  limit " + queryLimit);

        ch.hit("searchQuestions - query prepared");
        List<Question> questions = new ArrayList<>();

        try ( RDFConnection conn = RDFConnection.connect(dataset) ) {

            List<Question> finalQuestions = questions;  // copy the reference as lambda syntax requires

            // loop over query results
            conn.querySelect(query.toString(), querySolution -> {

                String questionName = querySolution.get("name").asLiteral().getString();
//                Resource qNode = querySolution.get("qUri").asResource();
//                Property prop = NS_questions.getPropertyOnModel(GraphRole.QUESTION_DATA.prefix, qG);
//                RDFNode qDataUri = qG.getProperty(qNode, prop).getObject();
                String name = nameForQuestionGraph(questionName, GraphRole.QUESTION_DATA);

                Question q = loadQuestion(name);
                if (q == null) {
                    return;
                }
                finalQuestions.add(q);

                q.getQuestionData().setQuestionName( questionName );
                /*
                QuestionEntity qe = new QuestionEntity();
                // todo: pass the kind of question here
                Question q = new Ordering(qe);
                q.getConcepts().addAll( List.of(querySolution.get("concepts").asLiteral().getString().split("; ")) );
                q.getNegativeLaws().addAll( List.of(querySolution.get("laws").asLiteral().getString().split("; ")) ); */

                if (limit > 0) {  // calc the score as filtering is required
                    int score = 0;  // how the question suits the request

                    if (qr.getDeniedQuestionNames() != null && (qr.getDeniedQuestionNames().contains(questionName) || qr.getDeniedQuestionNames().contains(Domain.NAME_PREFIX_IS_HUMAN + questionName))) {
                        score -= 10_000; // try to avoid returning the same question again
                    }
                    else
                    {
                        int prefixLen = lengthOfMaxCommonPrefixAmongStrings(questionName, qr.getDeniedQuestionNames());
                        if (prefixLen > 0) {
                            score -= 1000 * prefixLen / questionName.length();  // try to avoid similar questions (similar names indicate that these look close)
                        }
                    }

                    // inspect laws
                    if (qr.getLawsSearchDirection() == null || qr.getLawsSearchDirection().getValue() < 0) {
                        int i = 0;
                        int size = qr.getTargetLaws().size();
                        //// score += 1 << size;  // maximize possible score for existing laws
                        // set penalty for each missing law (earlier ones cost more)
                        for (Law target : qr.getTargetLaws()) {
                            i += 1;
                            if (q.getNegativeLaws().contains(target.getName()))
                                score += 1 << (size - i);
                        }
                    }
                    if (qr.getAllowedLaws() != null && !qr.getAllowedLaws().isEmpty()) {
                        // set penalty for each extra law that is not in allowed laws (if no allowed laws provided, allow everything)
                        ArrayList<String> extraLaws = new ArrayList<>(q.getNegativeLaws());
                        extraLaws.removeAll(qr.getTargetLaws().stream().map(Law::getName).collect(Collectors.toList()));
                        if (!extraLaws.isEmpty()) {
                            // found out how many laws are "not allowed"
                            extraLaws.removeAll(qr.getAllowedLaws().stream().map(Law::getName).collect(Collectors.toList()));
                        }
                        score -= extraLaws.size();  // -1 for each extra law
                    }


                    // inspect concepts
                    int i = 0;
                    int size = qr.getTargetConcepts().size();
                    score += size * size / 2;  // maximize possible score for existing concepts
                    // set penalty for each missing law (earlier ones cost more)
                    for (Concept target : qr.getTargetConcepts()) {
                        i += 1;
                        if (!q.getConcepts().contains(target.getName()))
                            score -= (size - i);
                    }

                    if (qr.getAllowedConcepts() != null && !qr.getAllowedConcepts().isEmpty()) {
                        // set penalty for each extra concept that is not in allowed concepts (if no allowed concepts provided, allow everything)
                        ArrayList<String> extraConcepts = new ArrayList<>(q.getConcepts());
                        extraConcepts.removeAll(qr.getTargetConcepts().stream().map(Concept::getName).collect(Collectors.toList()));
                        if (!extraConcepts.isEmpty()) {
                            // found out how many Concepts are "not allowed"
                            extraConcepts.removeAll(qr.getAllowedConcepts().stream().map(Concept::getName).collect(Collectors.toList()));
                            score -= extraConcepts.size();  // -1 for each extra concept
                        }
                    }

                    q.getQuestionData().setId((long) score);  // (ab)use ID for setting score temporarily
                }

                /*
                // add some data about this question
                qe.setStatementFacts(new ArrayList<>(List.of(new BackendFactEntity("<this_question>", "complexity", "" + querySolution.get("complexity").asLiteral().getDouble()))));
                qe.getStatementFacts().add(new BackendFactEntity("<this_question>", "solution_steps", "" + querySolution.get("solution_steps").asLiteral().getDouble()));
                 */

            });

        } catch (JenaException exception) {
            exception.printStackTrace();
        }

        ch.hit("searchQuestions - query executed");
        if (limit > 0 && !questions.isEmpty()) {
            // sort to ensure best score first
            questions.sort((o1, o2) -> -(int) (o1.getQuestionData().getId() - o2.getQuestionData().getId()));

            // retain requested number of questions
            questions = questions.subList(0, Math.min(limit, questions.size()));
        }

//        // insert to the questions some data (all we can here)
//        for (Question q : questions) {
//            QuestionEntity qe = q.getQuestionData();
//            qe.setDomainEntity(domain.getEntity());
//            qe.setExerciseAttempt(qr.getExerciseAttempt());
//            //// qe.setOptions(domain.?);
//
//            qe.setStatementFacts( modelToFacts(
//                    getQuestionModel(q.getQuestionName()),
//                    // TODO: adapt base URI for each domain
//                    NS_code.base()
//                    ));
//        }

        ch.since_start("searchQuestions - completed with " + questions.size() + " questions", false);
        return questions;
    }

    private Question loadQuestion(@NotNull QuestionMetadataEntity qMeta) {
        Question q = loadQuestion(qMeta.getQDataGraph());
        if (q != null) {
            if (q.getQuestionData().getOptions() == null) {
                q.getQuestionData().setOptions(new QuestionOptionsEntity());
            }
            q.getQuestionData().getOptions().setTemplateId(qMeta.getTemplateId());
        }
        return q;
    }

    private Question loadQuestion(String path) {
        Question q = null;
        try (InputStream stream = fileService.getFileStream(path)) {
            if (stream != null) {
                q = domain.parseQuestionTemplate(stream);
            } else {
                log.warn("File NOT found by storage: " + path);
            }
        } catch (IOException | NullPointerException | IllegalStateException e) {
            e.printStackTrace();
        }
        return q;
    }

    /** Find length of the longest prefix of `needle` with (one of) strings in `hive`
     * @param needle target string
     * @param hive set of strings to match `needle` with
     * @return length of common prefix or 0 if `hive` is empty or no common prefix found
     */
    public static int lengthOfMaxCommonPrefixAmongStrings(String needle, Collection<String> hive) {
        int max = 0;
        for(var s : hive) {
            int current = StringHelper.findCommonPrefixLength(needle, s);
            if (current > max)
                max = current;
        }
        return max;
    }

    /**
     * @return [min, avg, max] statistics of integral_complexity property
     */
    List<Double> complexityStat() {
        if (complexityStatCache == null) {
            // init caches
            questionSolutionLengthStat();
        }
        return complexityStatCache;
    }

    /**
     * @return [min, avg, max] statistics of solution_steps property
     */
    List<Double> questionSolutionLengthStat() {
        if (questionSolutionLengthStatCache != null) {
            return questionSolutionLengthStatCache;
        }

        String solutionLengthStatQuery = "select " +
                " (max(?solution_steps) as ?max)" +
                " (min(?solution_steps) as ?min)" +
                " (avg(?solution_steps) as ?avg)\n" +
                " (max(?ic) as ?ic_max)" +
                " (min(?ic) as ?ic_min)" +
                " (avg(?ic) as ?ic_avg)\n" +
                " {  GRAPH <" + NS_questions.base() + "> {\n" +
                "    ?s a <" + NS_questions.get("Question") + "> .\n" +
                "    ?s <" + NS_questions.get("solution_steps") + "> ?solution_steps .\n" +
                "    ?s <" + NS_questions.get("integral_complexity") + "> ?ic .\n" +
                "  }}";

        List<Double> lenStat = new ArrayList<>();
        List<Double> complStat = new ArrayList<>();
        try (RDFConnection conn = RDFConnection.connect(dataset)) {

            conn.querySelect(solutionLengthStatQuery, querySolution -> {
                lenStat  .add(querySolution.get("min").asLiteral().getDouble());
                lenStat  .add(querySolution.get("avg").asLiteral().getDouble());
                lenStat  .add(querySolution.get("max").asLiteral().getDouble());
                complStat.add(querySolution.get("ic_min").asLiteral().getDouble());
                complStat.add(querySolution.get("ic_avg").asLiteral().getDouble());
                complStat.add(querySolution.get("ic_max").asLiteral().getDouble());
            });

        } catch (JenaException exception) {
            exception.printStackTrace();
        }
        questionSolutionLengthStatCache = lenStat;  // save to cache
        complexityStatCache = complStat;  // save to cache
        return lenStat;
    }

    Model getDomainSchemaForSolving() {
        if (domain != null) {
            return domain.getSchemaForSolving();
        }

        // the default
        return ModelFactory.createDefaultModel();
    }

    List<Rule> getDomainRulesForSolvingAtLevel(GraphRole level) {
        assert domain != null;

        // get rules
        List<Rule> rules = new ArrayList<>();

        List<Law> laws = new ArrayList<>();

        // choose whose rules to return
        if (level.ordinal() < GraphRole.QUESTION_TEMPLATE.ordinal()) {
            laws.addAll(domain.getPositiveLaws());
        } else if (level.ordinal() <= GraphRole.QUESTION.ordinal()) {
            laws.addAll(domain.getQuestionPositiveLaws(domain.getDefaultQuestionType(), domain.getDefaultQuestionTags(domain.getDefaultQuestionType())));
        } else {
            laws.addAll(domain.getQuestionNegativeLaws(domain.getDefaultQuestionType(), new ArrayList<>()));
        }

        PrintUtil.registerPrefix("my", NS_code.get()); // as `my:` is used in rules


        for (Law law : laws) {
            for (LawFormulation lawFormulation : law.getFormulations()) {
                Rule rule;
                if (lawFormulation.getBackend().equals("Jena")) {
                    try {
                        rule = Rule.parseRule(lawFormulation.getFormulation());
                    } catch (Rule.ParserException e) {
                        log.error("Following error in rule: " + lawFormulation.getFormulation(), e);
                        continue;
                    }
                    rules.add(rule);
                }
            }
        }

        return rules;
    }

    public abstract RDFConnection getConn();

    /** Download, cache and return a graph */
    @Nullable
    Model getGraph(String name) {
        if (fetchGraph(name)) {
            return getLocalGraphByUri(name);
        }
        return null;
    }

    /**
     * Download a file content from remote FTP, parse and return as Model
     */
    @Nullable
    Model fetchModel(String name) {
        Model m = ModelFactory.createDefaultModel();
        try (InputStream stream = fileService.getFileStream(name)) {
            if (stream == null)
                return null;
            RDFDataMgr.read(m, stream, DEFAULT_RDF_SYNTAX);
        } catch (IOException /*| NullPointerException*/ e) {
            e.printStackTrace();
            return null;
        }
        return m;
    }

    /** Cache and send a graph to remote DB */
    boolean sendGraph(String gUri, Model m) {
        if (setLocalGraph(gUri, m)) {
            return uploadGraph(gUri);
        }
        return false;
    }

    /**
     * Send a model as file to remote FTP
     */
    boolean sendModel(String name, Model m) {
        try (OutputStream stream = fileService.saveFileStream(name)) {
            RDFDataMgr.write(stream, m, DEFAULT_RDF_SYNTAX);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Download and cache a graph if not cached yet
     */
    boolean fetchGraph(String gUri) {
        return fetchGraph(gUri, false);
    }

    abstract boolean fetchGraph(String gUri, boolean fetchAlways);

    abstract boolean uploadGraph(String gUri);

    boolean runQueriesWithConnection(RDFConnection connection, Collection<UpdateRequest> requests, boolean merge) {
        try (RDFConnection conn = connection) {
            conn.begin(TxnType.WRITE);

            if (merge && requests.size() > 1) {
                // join all
                StringBuilder bigRequest = new StringBuilder();
                for (UpdateRequest r : requests) {
                    if (bigRequest.length() > 0)
                        bigRequest.append("\n;\n");  // ";" is SPARQL separator
                    bigRequest.append(r.toString());
                }
                String finalRequest = bigRequest.toString();
                // run query once
                conn.update(finalRequest);
            } else {
                for (UpdateRequest r : requests) {
                    conn.update(r);
                }
            }
            conn.commit();
            return true;
        } catch (JenaException exception) {
            exception.printStackTrace();
            // System.out.println();
            return false;
        }

    }

    abstract boolean runQueries(Collection<UpdateRequest> requests);

    abstract boolean runQueries(Collection<UpdateRequest> requests, boolean merge);

    public String nameForQuestionGraph(String questionName, GraphRole role) {
        // look for <Question>-<subgraph> relation in metadata first
        Model qG = getGraph(NS_questions.base());
        // assert qG != null;
        RDFNode targetNamedGraph = null;

        if (qG != null) {
            targetNamedGraph = findQuestionByName(questionName)
                    .listProperties(AbstractRdfStorage.questionSubgraphPropertyFor(role).baseAsPropertyOnModel(qG))
                    .toList().stream()
                    .map(Statement::getObject)
                    .dropWhile(res -> res.equals(RDF.nil))
                    .reduce((first, second) -> first)
                    .orElse(null);
        }
        if (targetNamedGraph != null) {
            String qsgName = targetNamedGraph.asNode().getURI();
            return NS_file.localize(qsgName);
        }

        // no known relation - get default for a new one
        String ext = "." + DEFAULT_RDF_SYNTAX.getFileExtensions().get(0);
        return fileService.prepareNameForFile(role.ns().get(questionName + ext), false);
        //// return role.ns(NS_namedGraph.get()).get(questionName);
    }

    /**
     * Find Resource denoting question or question template node with given name from 'questions' graph
     */
    Resource findQuestionByName(String questionName) {
        Model qG = getGraph(NS_questions.base());  // questions Graph containing questions metadata
        if (qG != null) {
            List<Resource> qResources = qG.listSubjectsWithProperty(
                    NS_questions.getPropertyOnModel("name", qG),
                    questionName
            ).toList();
            if (!qResources.isEmpty())
                return qResources.get(0);
        }
        return null;
    }

    /**
     * Get Model with specified `role` for question or question template, if one exists, else `null` is returned.
     */
    public Model getQuestionSubgraph(String questionName, GraphRole role) {
        return fetchModel(nameForQuestionGraph(questionName, role));
    }

    /**
     * Get union of all models available for question or question template
     */
    public Model getQuestionModel(String questionName) {
        return getQuestionModel(questionName, GraphRole.QUESTION_SOLVED);
    }

    /**
     * Get union of all models under `topRole` (inclusive) available for question or question template
     */
    public Model getQuestionModel(String questionName, GraphRole topRole) {
        Model m = ModelFactory.createDefaultModel();
        for (GraphRole role : questionStages()) {
            Model gm = getQuestionSubgraph(questionName, role);
            if (gm != null)
                m.add(gm);

            if (role == topRole)
                break;
        }
        return m;
    }

    List<GraphRole> questionStages() {
        return List.of(
                GraphRole.QUESTION_TEMPLATE,
                GraphRole.QUESTION_TEMPLATE_SOLVED,
                GraphRole.QUESTION,
                GraphRole.QUESTION_SOLVED
        );
    }

    /**
     * Find what stage a question is in. Returned constant means which stage is reached by now.
     * (Using "questions" metadata graph only, no more graphs fetched from remote.)
     *
     * @param questionName question/questionTemplate unqualified name
     * @return one of questionStages(), or null if the question/questionTemplate does not exist.
     */
    public GraphRole getQuestionStatus(String questionName) {
        Resource questionNode = findQuestionByName(questionName);
        if (questionNode != null) {

            GraphRole approvedStatus = GraphRole.SCHEMA;  // below any valid question status

            for (GraphRole role : questionStages()) {
                /// boolean exists = fetchGraph(uriForQuestionGraph(questionName, role));

                boolean targetNamedGraphAbsent = questionNode
                        .listProperties(AbstractRdfStorage.questionSubgraphPropertyFor(role).baseAsPropertyOnModel(questionNode.getModel()))
                        .toList()
                        .stream()
                        .map(Statement::getObject)
                        .dropWhile(res -> res.equals(RDF.nil))
                        .findAny().isEmpty();

                if (targetNamedGraphAbsent) {
                    break;  // now return approvedStatus
                }
                // else ...
                approvedStatus = role;
            }
            return approvedStatus;
        }
        return null;
    }

    /**
     * создание/обновление и отправка во внешнюю БД одного из 4-х видов подграфа вопроса (например, создание нового
     * вопроса или добавление
     * *solved-данных для него)
     *
     * @param questionName unqualified name of Question or QuestionTemplate
     * @param role
     * @param model
     * @return
     */
    public boolean setQuestionSubgraph(String questionName, GraphRole role, Model model) {
        String qgUri = nameForQuestionGraph(questionName, role);
        boolean success = sendModel(qgUri, model);

        if (!success)
            return false;

        // update questions metadata
        Resource questionNode = findQuestionByName(questionName);
        Node qgNode = NS_file.getUri(qgUri);

        UpdateRequest upd_setGraph = AbstractRdfStorage.makeUpdateTripleQuery(
                NS_questions.baseAsUri(),
                questionNode,
                AbstractRdfStorage.questionSubgraphPropertyFor(role).baseAsUri(),
                qgNode
        );

        return runQueries(List.of(upd_setGraph));
    }

    /**
     * Create metadata representing empty QuestionTemplate, but not overwrite existing data.
     *
     * @param questionTemplateName unique identifier-like name of question template
     * @return true on success
     */
    public boolean createQuestionTemplate(String questionTemplateName) {
        Model qG = getGraph(NS_questions.base());  // questions Graph containing questions metadata

        if (qG != null) {
            Resource nodeClass = qG.createResource(NS_classQuestionTemplate.base(), OWL.Class);
            Resource qNode = findQuestionByName(questionTemplateName);

            // deal with existing node
            if (qNode != null) {
                // check if this node is indeed a question Template
                boolean rightType = qG.listStatements(qNode, RDF.type, nodeClass).hasNext();
                if (!rightType) {
                    throw new RuntimeException("Cannot create QuestionTemplate: uri '" + qNode.getURI() + "' is " +
                            "already in use.");
                }

                // simple decision: do nothing if metadata node exists
                return true;
            }

            Node ngNode = NS_questions.baseAsUri();
            qNode = NS_classQuestionTemplate.getResourceOnModel(questionTemplateName, qG);

            List<UpdateRequest> commands = new ArrayList<>();

            commands.add(AbstractRdfStorage.makeUpdateTripleQuery(ngNode, qNode, RDF.type, nodeClass));

            commands.add(AbstractRdfStorage.makeUpdateTripleQuery(ngNode,
                    qNode,
                    NS_questions.getUri("name"),
                    NodeFactory.createLiteral(questionTemplateName)));

            // initialize template's graphs as empty ...
            // using "template-only" roles
            for (GraphRole role : questionStages().subList(0, 2)) {
                commands.add(AbstractRdfStorage.makeUpdateTripleQuery(ngNode,
                        qNode,
                        AbstractRdfStorage.questionSubgraphPropertyFor(role).baseAsUri(),
                        RDF.nil));
            }

            boolean success = runQueries(commands);
            return success;
        }
        return false;
    }

    /**
     * Create metadata representing empty Question, but not overwrite existing data if recreate == false.
     *
     * @param questionName unique identifier-like name of question
     * @return true on success
     */
    public boolean createQuestion(String questionName, String questionTemplateName, boolean recreate) {
        Model qG = getGraph(NS_questions.base());  // questions Graph containing questions metadata

        if (qG != null) {
            Resource nodeClass = NS_classQuestion.baseAsResourceOnModel(qG);
            Resource qNode = findQuestionByName(questionName);

            // deal with existing node
            if (qNode != null) {
                // check if this node is indeed a Question
                boolean rightType = qG.listStatements(qNode, RDF.type, nodeClass).hasNext();
                if (!rightType) {
                    throw new RuntimeException("Cannot create Question: uri '" + qNode.getURI() + "' is already in " +
                            "use.");
                }

                // simple decision: do nothing if metadata node exists
                if (!recreate)
                    return true;
            }

            if (!createQuestionTemplate(questionTemplateName)) // check if template is valid
                return false;

            Resource qtemplNode = findQuestionByName(questionTemplateName);

            Node ngNode = NS_questions.baseAsUri();
            qNode = NS_classQuestion.getResourceOnModel(questionName, qG);

            List<UpdateRequest> commands = new ArrayList<>();

            commands.add(AbstractRdfStorage.makeUpdateTripleQuery(ngNode, qNode, RDF.type, nodeClass));

            commands.add(AbstractRdfStorage.makeUpdateTripleQuery(ngNode,
                    qNode,
                    NS_questions.getPropertyOnModel("name", qG),
                    NodeFactory.createLiteral(questionName)));

            commands.add(AbstractRdfStorage.makeUpdateTripleQuery(ngNode,
                    qNode,
                    NS_questions.getPropertyOnModel("has_template", qG),
                    qtemplNode));

            // copy references to the graphs from template as is ...
            // using "template-only" roles
            for (GraphRole role : questionStages().subList(0, 2)) {
                Property propOfRole = AbstractRdfStorage.questionSubgraphPropertyFor(role).baseAsPropertyOnModel(qG);
                RDFNode graphWithRole = qtemplNode.listProperties(propOfRole).nextStatement().getObject();
                commands.add(AbstractRdfStorage.makeUpdateTripleQuery(ngNode, qNode, propOfRole, graphWithRole));
            }

            // initialize question's graphs as empty ...
            // using "question-only" roles
            for (GraphRole role : questionStages().subList(2, 4)) {
                commands.add(AbstractRdfStorage.makeUpdateTripleQuery(ngNode,
                        qNode,
                        AbstractRdfStorage.questionSubgraphPropertyFor(role).baseAsPropertyOnModel(qG),
                        RDF.nil));
            }

            return runQueries(commands);
        }
        return false;
    }

    /**
     * Add metadata triples to a Question node. Only scalar values (Literals) are allowed as an object in a triple.
     * Tip: use {@link NodeFactory} to create property URIs and literals. Property URI is typically obtained via
     * NS_questions.getUri("...").
     * (QuestionTemplate is not supported here since NS_classQuestion namespace is hardcoded in this method).
     *
     * @param questionName local name of a Question node
     * @param propValPairs property-literal pairs for triples to be created
     * @return true on success
     */
    public boolean setQuestionMetadata(String questionName, Collection<Pair<Node, Node>> propValPairs) {
        Model qG = getGraph(NS_questions.base());  // questions Graph containing questions metadata

        if (qG != null) {
            Node ngNode = NS_questions.baseAsUri();
            Node qNode = NS_classQuestion.getUri(questionName);
            List<Triple> triples = new ArrayList<>();

            // make triples with question node as subject
            for (Pair<Node, Node> pair : propValPairs) {
                triples.add(new Triple(
                        qNode,  // Subject
                        pair.getLeft(),  // Property
                        pair.getRight()  // Value (Object)
                ));
            }

            // insert new triples
            UpdateBuilder ub2 = new UpdateBuilder();
            ub2.addPrefix("qs", NS_questions.get());
            ub2.addInsert(ngNode, triples);
            UpdateRequest insertQuery = ub2.buildRequest();

            return runQueries(List.of(insertQuery));
        }
        return false;
    }

    /**
     * Solve a question or question template: create new subgraph & send it to remote, update questions metadata.
     *
     * @param questionName              name of question or question template
     * @param desiredLevel              QUESTION_TEMPLATE_SOLVED or QUESTION_SOLVED
     * @return true on success
     */
    public boolean solveQuestion(String questionName, GraphRole desiredLevel) {
        return solveQuestion(questionName, desiredLevel, -1);
    }

    /**
     * Solve a question or question template: create new subgraph & send it to remote, update questions metadata.
     *
     * @param questionName              name of question or question template
     * @param desiredLevel              QUESTION_TEMPLATE_SOLVED or QUESTION_SOLVED
     * @param tooLargeTemplateThreshold if > 0, do not process question templates having the number of RDF subjects
     *                                  exceeding this number (maybe for reducing reasoner load).
     * @return true on success
     */
    public boolean solveQuestion(String questionName, GraphRole desiredLevel, int tooLargeTemplateThreshold) {
        Model qG = getGraph(NS_questions.base());
        assert qG != null;

        Model existingData = getQuestionModel(questionName, GraphRole.getPrevious(desiredLevel));

        // avoid processing too large templates
        if (tooLargeTemplateThreshold > 0 && desiredLevel == GraphRole.QUESTION_TEMPLATE_SOLVED) {
            int subjectCount = existingData.listSubjects().toList().size();
            if (subjectCount > tooLargeTemplateThreshold) {
                System.out.println("Skip too large template of size " + subjectCount + ": " + questionName);
                return false;
            }
        }

        Model inferred = runReasoning(
                getFullSchema().union(existingData),
                getDomainRulesForSolvingAtLevel(desiredLevel),
                true);

        if (inferred.isEmpty())
            log.warn("Solved to empty for question: " + questionName);

        // set graph
        return setQuestionSubgraph(questionName, desiredLevel, inferred);
    }

    /**
     * Find questions and/or question templates which have `unsolvedSubgraph` set to rdf:nil.
     *
     * @param unsolvedSubgraph QUESTION_TEMPLATE_SOLVED or QUESTION_SOLVED
     * @return list of names
     */
    public List<String> unsolvedQuestions(/*String classUri,*/ GraphRole unsolvedSubgraph) {
        // find question templates to solve
        Node ng = NS_questions.baseAsUri();
        String unsolvedTemplates = new SelectBuilder()
                .addVar("?name")
                .addWhere(
                        new WhereBuilder()
                                /*.addGraph(ng,
                                        "?node",
                                        RDF.type,
                                        NodeFactory.createURI(classUri)
                                )*/
                                .addGraph(ng,
                                        "?node",
                                        NS_questions.getUri("name"),
                                        "?name"
                                )
                                .addGraph(ng,
                                        "?node",
                                        AbstractRdfStorage.questionSubgraphPropertyFor(unsolvedSubgraph).baseAsUri(),
                                        RDF.nil
                                )
                )
                .toString();

        RDFConnection connection = RDFConnection.connect(dataset);
        // RDFConnection connection = getConn();
        List<String> names = new ArrayList<>();
        try (RDFConnection conn = connection) {

            conn.querySelect(unsolvedTemplates,
                    querySolution -> names.add(querySolution.get("name").asLiteral().getString()));

        } catch (JenaException exception) {
            exception.printStackTrace();
        }
        return names;
    }

    /**
     * Find questions or question templates within dataset.
     *
     * @param classUri full URI of class an instance should have under `rdf:type` relation
     * @return list of names
     */
    public List<String> findAllQuestions(String classUri, int limit) {
        // find question templates to solve
        Node ng = NS_questions.baseAsUri();
        String queryNames = new SelectBuilder()
                .addVar("?name")
                .addWhere(
                        new WhereBuilder()
                                .addGraph(ng,
                                        "?node",
                                        RDF.type,
                                        NodeFactory.createURI(classUri)
                                )
                                .addGraph(ng,
                                        "?node",
                                        NS_questions.getUri("name"),
                                        "?name"
                                )
                )
                .toString();

        if (limit > 0) {
            queryNames += "\nLIMIT " + limit;
        }

        RDFConnection connection = RDFConnection.connect(dataset);  // TODO: replace deprecated ???
        // RDFConnection connection = getConn();
        List<String> names = new ArrayList<>();
        try (RDFConnection conn = connection) {

            conn.querySelect(queryNames, querySolution -> names.add(querySolution.get("name").asLiteral().getString()));

        } catch (JenaException exception) {
            exception.printStackTrace();
        }
        return names;
    }

    /**
     * (Method overload) Find all questions or question templates within dataset.
     *
     * @param classUri full URI of `rdf:type` an instance should have
     * @return list of names
     */
    public List<String> findAllQuestions(String classUri) {
        return findAllQuestions(classUri, 0);
    }

    public boolean localGraphExists(String gUri) {
        return dataset.containsNamedModel(gUri);
    }

    public Model getLocalGraphByUri(String gUri) {
        if (dataset != null) {
            if (localGraphExists(gUri)) {
                return dataset.getNamedModel(gUri);
            } else log.warn(String.format("Graph not found - name: '%s'", gUri));
        } else {
            log.warn("Dataset was not initialized");
        }
        return null;
    }

    /**
     * запись/обновление подграфа триплетов в кэше
     * @param gUri graph URI
     * @param model model to set
     * @return true on success
     */
    public boolean setLocalGraph(String gUri, Model model) {
        if (dataset != null) {
            if (dataset.containsNamedModel(gUri))
                dataset.replaceNamedModel(gUri, model);
            else
                dataset.addNamedModel(gUri, model);

        } else {
            log.error("Dataset was not initialized");
            throw new RuntimeException("Dataset was not initialized");
        }
        return true;
    }

    /**
     * получение подграфа триплетов, хранящего базовую схему домена (необходимую для работы ризонера)
     * @return model of triples
     */
    public Model getSchema() {
        String uri = NS_graphs.get(GraphRole.SCHEMA.ns().base());
        if (!dataset.containsNamedModel(uri)) {
            setLocalGraph(uri, getDomainSchemaForSolving());
        }
        return getLocalGraphByUri(uri);
    }

    /**
     * Get solved domain schema (for reasoning purposes)
     * @return model both schema and solved schema (the most of what exists - may be empty)
     */
    public Model getFullSchema() {
        Model m = ModelFactory.createDefaultModel();
        Model m2 = getSchema();
        if (m2 != null)
            m.add(m2);

        String uri = NS_graphs.get(GraphRole.SCHEMA_SOLVED.ns().base());
        if (!dataset.containsNamedModel(uri) && !m.isEmpty()) {
            Model inferred = runReasoning(m, getDomainRulesForSolvingAtLevel(GraphRole.SCHEMA), true);
            if (inferred.isEmpty()) {
                // add anything to avoid re-calculation
                inferred.add(m.listStatements().nextStatement());
            }
            setLocalGraph(uri, inferred);
        }
        m2 = getLocalGraphByUri(uri);
        if (m2 != null)
            m.add(m2);

        return m;
    }

    public Model solveTemplate(Model srcModel, GraphRole desiredLevel, boolean retainNewFactsOnly) {
        return runReasoning(
                getFullSchema().union(srcModel),
                getDomainRulesForSolvingAtLevel(desiredLevel),  // SCHEMA role suits ProgrammingLanguageExpressionDomain here
                retainNewFactsOnly);
    }

    public Model runReasoning(Model srcModel, List<Rule> rules, boolean retainNewFactsOnly) {
        GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);

        long startTime = System.nanoTime();

//        long _key = srcModel.size();
//        debug_dump(srcModel,_key + "-stor_in");

        // Note: changes done to inf are also applied to srcModel.
        InfModel inf = ModelFactory.createInfModel(reasoner, srcModel);
        inf.prepare();

        long estimatedTime = System.nanoTime() - startTime;
//        log.info
        System.out.println("Time Jena spent on reasoning: " + String.format("%.5f",
                (float) estimatedTime / 1000 / 1000 / 1000) + " seconds.");

//        debug_dump(inf,_key + "-stor_out");

        Model result;
        if (retainNewFactsOnly) {
            // make a true copy
            result = ModelFactory.createDefaultModel().add(inf);
            // cleanup the inferred results (inf) ...
            result.remove(srcModel);
        } else {
            result = inf;
        }
//        debug_dump(result,_key + "-stor_end");
        return result;
    }

    private void debug_dump(Model model, String name) {
        if (true) {
            String out_rdf_path = "c:/temp/" + name + ".n3";
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(out_rdf_path);
                RDFDataMgr.write(out, model, Lang.NTRIPLES);  // Lang.NTRIPLES  or  Lang.RDFXML
                System.out.println("Debug written: " + out_rdf_path + ". N of of triples: " + model.size());
            } catch (FileNotFoundException e) {
                System.out.println("Cannot write to file: " + out_rdf_path + "  Error: " + e);
            }
        }
    }

}
