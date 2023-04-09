package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Dataset;
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
import org.apache.jena.vocabulary.RDF;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.LawFormulation;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.QuestionRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataDraftEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionRequestLogEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataBaseRepository;
import org.vstu.compprehension.models.repository.QuestionMetadataDraftRepository;
import org.vstu.compprehension.models.repository.QuestionRequestLogRepository;
import org.vstu.compprehension.utils.ApplicationContextProvider;
import org.vstu.compprehension.utils.Checkpointer;

import javax.annotation.Nullable;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.lang.Long.bitCount;

@Log4j2
public abstract class AbstractRdfStorage {
    /**
     * Default prefixes
     */
    final static NamespaceUtil NS_root = new NamespaceUtil("http://vstu.ru/poas/");
    public final static NamespaceUtil NS_code = new NamespaceUtil(NS_root.get("code#"));
    //// final static NamespaceUtil NS_namedGraph = new NamespaceUtil("http://named.graph/");
//    final static NamespaceUtil NS_file = new NamespaceUtil("ftp://plain.file/");
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
    // TODO: use as defaults only, open in constructor.
    public static String FTP_BASE = "file:///c:/data/compp/";  // local dir is supported too (for debugging)
    public static String FTP_DOWNLOAD_BASE = FTP_BASE;
    static Lang DEFAULT_RDF_SYNTAX = Lang.TURTLE;

    // todo: reassign constants and merge prod & draft tables for questions_meta
    public static int STAGE_TEMPLATE = 1;
    public static int STAGE_QUESTION = 2;
    public static int STAGE_READY = 3; // in this stage the generated question may be moved to the production bank
    public static int STAGE_EXPORTED = 4;
    public static int GENERATOR_VERSION = 10;


    Domain domain;

    /**
     * Temporary storage (cache) for RDF graphs from remote RDF DB (ex. Fuseki)
     * TODO: remove, since no rdf storage is in use any more
     */
    @Deprecated
    Dataset dataset = null;
    static boolean USE_RDF_STORAGE = false;


    RemoteFileService fileService = null;
    QuestionMetadataManager questionMetadataManager = null;
    @Setter
    @Getter
    QuestionMetadataDraftRepository questionMetadataDraftRepository;

    @NotNull
    public static QuestionMetadataDraftRepository getMetadataDraftRepositoryStatic() {
        return ApplicationContextProvider.getApplicationContext().getBean(QuestionMetadataDraftRepository.class);
    }


    QuestionMetadataManager getQuestionMetadataManager() {
        if (this.questionMetadataManager == null) {
            if (this.domain != null) {
                val repo = domain.getQuestionMetadataRepository();
                if (repo != null)
                    questionMetadataManager = new QuestionMetadataManager(domain, repo);
            }
        }
        return questionMetadataManager;
    }

    public static NamespaceUtil questionSubgraphPropertyFor(GraphRole role) {
        return new NamespaceUtil(NS_questions.get("has_graph_" + role.ns().base()));
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

    /**
     * Find question templates in the questions bank. If no questions satisfy the requirements exactly, finds ones of similar complexity. Denied concepts and laws (laws map to violations on DB) cannot present in result questions anyway.
     * @param qr QuestionRequest
     * @param limit maximum questions to return (must be > 0)
     * @return questions found or empty list if the requirements cannot be satisfied
     */
    public List<Question> searchQuestions(QuestionRequest qr, int limit) {

        Checkpointer ch = new Checkpointer(log);

        QuestionMetadataManager metaMgr = getQuestionMetadataManager();
        int nQuestionsInAttempt = Optional.ofNullable(qr.getDeniedQuestionNames()).map(List::size).orElse(0);
        int queryLimit = limit + nQuestionsInAttempt;
        int hardLimit = 25;

        double complexity = qr.getComplexity();
        complexity = metaMgr.wholeBankStat.complexityStat.rescaleExternalValue(complexity, 0, 1);

        long targetConceptsBitmask = qr.getConceptsTargetedBitmask();
        /*long allowedConceptsBitmask = conceptsToBitmask(qr.getAllowedConcepts(), metaMgr);  // unused */
        long deniedConceptsBitmask = qr.getConceptsDeniedBitmask();
        long unwantedConceptsBitmask = findLastNQuestionsMeta(qr, 4).stream()
                .mapToLong(QuestionMetadataEntity::getConceptBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetConceptsBitmask &= ~deniedConceptsBitmask;
        long targetConceptsBitmaskInPlan = qr.getConceptsTargetedInPlanBitmask();

        // use laws, for e.g. Expr domain
        long targetLawsBitmask = qr.getLawsTargetedBitmask();
        /*long allowedLawsBitmask= lawsToBitmask(qr.getAllowedLaws(), metaMgr);  // unused */
        long deniedLawsBitmask = qr.getLawsDeniedBitmask();
        long unwantedLawsBitmask = findLastNQuestionsMeta(qr, 4).stream()
                .mapToLong(QuestionMetadataEntity::getLawBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetLawsBitmask &= ~deniedLawsBitmask;
        long targetViolationsBitmaskInPlan = qr.getLawsTargetedInPlanBitmask();


        // use violations from all questions is exercise attempt
        long unwantedViolationsBitmask = qr.getExerciseAttempt().getQuestions().stream()
                .map(qd -> qd.getOptions().getMetadata())
                .filter(Objects::nonNull)
                .mapToLong(QuestionMetadataEntity::getViolationBits)
                .reduce((t, t2) -> t | t2).orElse(0);

        ch.hit("searchQuestionsAdvanced - bitmasks prepared");

        // (previous version with explicit parameters)
//        List<QuestionMetadataEntity> foundQuestionMetas = metaMgr.findQuestionsAroundComplexityWithoutTemplates(
//                complexity,
//                0.15,
//                qr.getStepsMin(), qr.getStepsMax(),
//                targetConceptsBitmask, deniedConceptsBitmask,
//                targetLawsBitmask, deniedLawsBitmask,
//                List.of()/*templatesInUse*/, questionsInUse,
//                queryLimit, 12);

        List<QuestionMetadataEntity> foundQuestionMetas = metaMgr.findQuestionsAroundComplexityWithoutQIds(qr, 0.15, queryLimit, 12);

        ch.hit("searchQuestionsAdvanced - query executed with " + foundQuestionMetas.size() + " candidates");

        foundQuestionMetas = filterQuestionMetas(foundQuestionMetas,
                complexity,
//                solutionSteps,
                targetConceptsBitmask,
                unwantedConceptsBitmask,
                unwantedLawsBitmask,
                unwantedViolationsBitmask,
                Math.min(limit, hardLimit)  // Note: queryLimit >= limit
            );

        ch.hit("searchQuestionsAdvanced - filtered up to " + foundQuestionMetas.size() + " candidates");

        // set concepts from request (for future reference via questions' saved metadata)
        for (QuestionMetadataEntity m : foundQuestionMetas) {
            m.setConceptBitsInPlan(targetConceptsBitmaskInPlan);
            m.setViolationBitsInPlan(targetViolationsBitmaskInPlan);
            // Save actual requested bits as well
            m.setConceptBitsInRequest(targetConceptsBitmask);
            m.setViolationBitsInRequest(targetLawsBitmask);

            /// debug check law bits
            if (targetLawsBitmask != 0 && (targetLawsBitmask & m.getViolationBits()) == 0) {
                log.warn("No LAW bits matched: " +targetLawsBitmask+ " " + m.getName());
            }
        }

        List<Question> loadedQuestions = loadQuestions(foundQuestionMetas);
        ch.hit("searchQuestionsAdvanced - files loaded");

        if (loadedQuestions.size() == 1) {
            // increment the question's usage counter
            val meta = loadedQuestions.get(0).getQuestionData().getOptions().getMetadata();
            meta.setUsedCount(Optional.ofNullable(meta.getUsedCount()).orElse(0L) + 1);
            meta.setLastAttemptId(qr.getExerciseAttempt().getId());
            meta.setDateLastUsed(new Date());
            metaMgr.getQuestionRepository().save(meta);

//            ch.hit("searchQuestionsAdvanced - Question usage +1");
        }

        ch.since_start("searchQuestionsAdvanced - completed with " + loadedQuestions.size() + " questions");

        return loadedQuestions;
    }

    private List<QuestionMetadataEntity> filterQuestionMetas(
            List<QuestionMetadataEntity> given,
            double scaledComplexity,
            /*double scaledSolutionLength,*/
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

        /*List<QuestionMetadataEntity> ranking2 = given.stream()
                .sorted(Comparator.comparingDouble(q -> q.getSolutionStepsAbsDiff(scaledSolutionLength)))
                .collect(Collectors.toList());*/

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
                        /*ranking2.indexOf(q) +*/
                        ranking3.indexOf(q) +
                        ranking6.indexOf(q)
                        )))
                .collect(Collectors.toList());

        return finalRanking.subList(0, limit);
    }

    private List<Question> loadQuestions(Collection<QuestionMetadataEntity> metas) {
        List<Question> list = new ArrayList<>();
        for (QuestionMetadataEntity meta : metas) {
            Question question = loadQuestion(meta);
            if (question != null) {
                list.add(question);
            }
        }
        return list;
    }


    private Question loadQuestion(@NotNull QuestionMetadataEntity qMeta) {
        Question q = loadQuestion(qMeta.getQDataGraph());
        if (q != null) {
            if (q.getQuestionData().getOptions() == null) {
                q.getQuestionData().setOptions(new QuestionOptionsEntity());
            }
            q.getQuestionData().getOptions().setTemplateId(qMeta.getTemplateId());
            q.getQuestionData().getOptions().setQuestionMetaId(qMeta.getId());
            q.getQuestionData().getOptions().setMetadata(qMeta);
            q.setMetadata(qMeta);
            // future todo: reflect any set data in Domain.makeQuestionCopy() as well
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

//    public abstract RDFConnection getConn();

//    /** Download, cache and return a graph */
//    @Nullable
//    Model getGraph(String name) {
//        if (fetchGraph(name)) {
//            return getLocalGraphByUri(name);
//        }
//        return null;
//    }

    /**
     * Download a file content from remote FTP, parse and return as Model
     */
    @Nullable
    Model fetchModel(String filepath) {
        Model m = ModelFactory.createDefaultModel();
        try (InputStream stream = fileService.getFileStream(filepath)) {
            if (stream == null)
                return null;
            RDFDataMgr.read(m, stream, DEFAULT_RDF_SYNTAX);
        } catch (IOException /*| NullPointerException*/ e) {
            e.printStackTrace();
            return null;
        }
        return m;
    }

//    /** Cache and send a graph to remote DB */
//    boolean sendGraph(String gUri, Model m) {
//        if (setLocalGraph(gUri, m)) {
//            return uploadGraph(gUri);
//        }
//        return false;
//    }

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

    /* **
     * Download and cache a graph if not cached yet
     */
//    boolean fetchGraph(String gUri) {
//        return fetchGraph(gUri, false);
//    }

////    abstract boolean fetchGraph(String gUri, boolean fetchAlways);

//    abstract boolean uploadGraph(String gUri);

//    boolean runQueriesWithConnection(RDFConnection connection, Collection<UpdateRequest> requests, boolean merge) {
//        try (RDFConnection conn = connection) {
//            conn.begin(TxnType.WRITE);
//
//            if (merge && requests.size() > 1) {
//                // join all
//                StringBuilder bigRequest = new StringBuilder();
//                for (UpdateRequest r : requests) {
//                    if (bigRequest.length() > 0)
//                        bigRequest.append("\n;\n");  // ";" is SPARQL separator
//                    bigRequest.append(r.toString());
//                }
//                String finalRequest = bigRequest.toString();
//                // run query once
//                conn.update(finalRequest);
//            } else {
//                for (UpdateRequest r : requests) {
//                    conn.update(r);
//                }
//            }
//            conn.commit();
//            return true;
//        } catch (JenaException exception) {
//            exception.printStackTrace();
//            // System.out.println();
//            return false;
//        }
//    }

//    abstract boolean runQueries(Collection<UpdateRequest> requests);

//    abstract boolean runQueries(Collection<UpdateRequest> requests, boolean merge);

    public String nameForQuestionGraph(String questionName, GraphRole role) {
        // look for <Question>.<subgraph> property in metadata first
        QuestionMetadataDraftEntity meta = findQuestionByName(questionName);
        String targetPath = null;
        if (meta != null)
            switch (role) {
                case QUESTION_TEMPLATE:
                    targetPath = meta.getQtGraphPath(); break;
                case QUESTION_TEMPLATE_SOLVED:
                    targetPath = meta.getQtSolvedGraphPath(); break;
                case QUESTION:
                    targetPath = meta.getQGraphPath(); break;
                case QUESTION_SOLVED:
                    targetPath = meta.getQSolvedGraphPath(); break;
                case QUESTION_DATA:
                    targetPath = meta.getQDataGraphPath(); break;
            }

        if (targetPath != null) {
            return targetPath;
        }

        // no known relation - get default for a new one
        String ext = "." + DEFAULT_RDF_SYNTAX.getFileExtensions().get(0);
        return fileService.prepareNameForFile(role.ns().get(questionName + ext), false);
        //// return role.ns(NS_namedGraph.get()).get(questionName);
    }

    /**
     * Find and return row of question or question template with given name from 'question_meta_draft' table
     */
    public QuestionMetadataDraftEntity findQuestionByName(String questionName) {
        val repo = getQuestionMetadataDraftRepository();
        if (repo == null)
            return null;

        val found = repo.findByName(questionName);
        if (found.isEmpty())
            return null;

        return found.get(0);
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
            else {
                log.info("Sub-graph not found!  q: " + questionName);
            }

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
     * @param questionName question/questionTemplate unqualified name
     * @return one of questionStages(), or null if the question/questionTemplate does not exist.
     */
    public GraphRole getQuestionStatus(String questionName) {
        val meta = findQuestionByName(questionName);
        if (meta == null) {
            return null;
        }

        GraphRole reachedRole = GraphRole.QUESTION_TEMPLATE;
        for  (GraphRole role : questionStages()) {
            String graphPath = null;
            switch (role) {
                case QUESTION_TEMPLATE:
                    graphPath = meta.getQtGraphPath();
                    break;
                case QUESTION_TEMPLATE_SOLVED:
                    graphPath = meta.getQtSolvedGraphPath();
                    break;
                case QUESTION:
                    graphPath = meta.getQGraphPath();
                    break;
                case QUESTION_SOLVED:
                    graphPath = meta.getQSolvedGraphPath();
                    break;
            }
            if (graphPath != null)
                reachedRole = role;
            else
                break;
        }
        return reachedRole;
    }

    /**
     * создание/обновление и отправка во внешнюю БД одного из 4-х видов подграфа вопроса (например, создание нового
     * вопроса или добавление solved-данных для него)
     *
     * @param questionName unqualified name of Question or QuestionTemplate
     * @param role question subgraph role
     * @param model data to store
     * @return saved metadata instance if saved successful, else null
     */
    public QuestionMetadataDraftEntity setQuestionSubgraph(String questionName, GraphRole role, Model model) {
        String qgSubPath = nameForQuestionGraph(questionName, role);
        boolean success = sendModel(qgSubPath, model);

        if (!success)
            return null;

        // update questions metadata
        QuestionMetadataDraftEntity meta = findQuestionByName(questionName);
        if (meta == null) {
            // проинициализировать метаданные шаблона вопроса, далее сохранить в БД
            if (role == GraphRole.QUESTION_TEMPLATE || role == GraphRole.QUESTION_TEMPLATE_SOLVED)
                meta = createQuestionTemplate(questionName);
            else {
                throw new IllegalStateException("setQuestionSubgraph(): Question metadata row must be initialized in advance!");
            }
        }

        switch (role) {
            case QUESTION_TEMPLATE:
                meta.setQtGraphPath(qgSubPath); break;
            case QUESTION_TEMPLATE_SOLVED:
                meta.setQtSolvedGraphPath(qgSubPath); break;
            case QUESTION:
                meta.setQGraphPath(qgSubPath); break;
            case QUESTION_SOLVED:
                meta.setQSolvedGraphPath(qgSubPath); break;
        }
        meta = saveMetadataDraftEntity(meta);

        return meta;
    }

    @NotNull
    public QuestionMetadataDraftEntity saveMetadataDraftEntity(QuestionMetadataDraftEntity meta) {
        return questionMetadataDraftRepository.save(meta);
    }

    /**
     * Create empty metadata row for QuestionTemplate, but not overwrite existing data.
     *
     * @param questionTemplateName unique identifier-like name of question template
     * @return fresh or existing QuestionMetadataDraftEntity instance
     */
    public QuestionMetadataDraftEntity createQuestionTemplate(String questionTemplateName) {
        // find template metadata
        QuestionMetadataDraftEntity templateMeta = findQuestionByName(questionTemplateName);

        if (templateMeta != null) {
            return templateMeta;
        }

        val builder = QuestionMetadataDraftEntity.builder();

        // проинициализировать метаданные вопроса, далее сохранить в БД
        templateMeta = builder.name(questionTemplateName)
                .domainShortname(Optional.ofNullable(domain).map(Domain::getShortName).orElse(""))
                .templateId(-1)
                .stage(STAGE_TEMPLATE)
                .version(GENERATOR_VERSION)
                .build();
        templateMeta = saveMetadataDraftEntity(templateMeta);

        return templateMeta;

    }

    /**
     * Create metadata representing empty Question, but not overwrite existing data if recreate == false.
     *
     * @param questionName unique identifier-like name of question
     * @return true on success
     */
    public QuestionMetadataDraftEntity createQuestion(String questionName, String questionTemplateName, boolean recreate) {

        // find template metadata
        QuestionMetadataDraftEntity templateMeta = findQuestionByName(questionTemplateName);


        QuestionMetadataDraftEntity meta;
        if (!recreate) {
            meta = findQuestionByName(questionName);
            if (meta != null) {
                if (templateMeta != null) {
                    meta.setTemplateId(templateMeta.getId());
                }
                return meta;
            }
        }
        QuestionMetadataDraftEntity.QuestionMetadataDraftEntityBuilder builder;
        int templateId;
        if (templateMeta != null) {
            // get builder to copy data from template
            builder = templateMeta.toBuilder()
                    .id(null); // reset id
            templateId = templateMeta.getId();
        } else {
            builder = QuestionMetadataDraftEntity.builder()
                    .domainShortname(Optional.ofNullable(domain).map(Domain::getShortName).orElse(""));
            templateId = -1;
        }

        // проинициализировать метаданные вопроса, далее сохранить в БД
        meta = builder.name(questionName)
                    .templateId(templateId)
                    .stage(STAGE_QUESTION)
                    .version(GENERATOR_VERSION)
                    .build();
        meta = saveMetadataDraftEntity(meta);

        return meta;
    }

    /** delete files and db entry for question (not for the template)
     * @param questionName name of question to delete
     */
    public void deleteQuestion(String questionName /*, boolean keepTemplate*/) {
        val meta = findQuestionByName(questionName);

        // delete files for this question (not for the template)
        try {
            String qgSubPath = meta.getQGraphPath();
            if (qgSubPath != null) {
                fileService.deleteFile(qgSubPath);
            }
            qgSubPath = meta.getQSolvedGraphPath();
            if (qgSubPath != null) {
                fileService.deleteFile(qgSubPath);
            }
            qgSubPath = meta.getQDataGraphPath();
            if (qgSubPath != null) {
                fileService.deleteFile(qgSubPath);
            }
        } catch (FileSystemException e) {
            e.printStackTrace();
        }

        // delete db entry
        questionMetadataDraftRepository.delete(meta);
    }

    /* *
     * Add metadata triples to a Question node. Only scalar values (Literals) are allowed as an object in a triple.
     * Tip: use {@link NodeFactory} to create property URIs and literals. Property URI is typically obtained via
     * NS_questions.getUri("...").
     * (QuestionTemplate is not supported here since NS_classQuestion namespace is hardcoded in this method).
     *
     * @param questionName local name of a Question node
     * @param propValPairs property-literal pairs for triples to be created
     * @return true on success
     */
    /*public boolean setQuestionMetadata(String questionName, Collection<Pair<Node, Node>> propValPairs) {
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
    }*/

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
//        Model qG = getGraph(NS_questions.base());
//        assert qG != null;

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
        return setQuestionSubgraph(questionName, desiredLevel, inferred) != null;
    }

    public static int getQrTooFewQuestions(int qrLogId) {
        if (qrLogId != 0) {
            // TODO: get the exercise the QR made from and fetch its expected number of students
        }
        return 100;
    }

    public static int getQrEnoughQuestions(int qrLogId) {
        int base = getQrTooFewQuestions(qrLogId);
        return base * 3;
    }

    /**
     * Find generated questions that suit unsatisfied question-requests
     * and send them to the production question bank.
     * Note that database is the same tor generator and production environment; but file storage may be physically different,
     * so sending files may require e.g. FTP write access.
     * @param qrLogsToProcess unsatisfied question-request log entries
     * @param enoughQuestionsPerQR if reached this number of questions reached for a QrLog then mark it as resolved
     * @param metadataDraftRepo jpa repository
     * @param qrLogRepo jpa repository
     */
    public static void exportGeneratedQuestionsToProductionBank(
        // List<QuestionRequestLogEntity> qrLogsToProcess, int enoughQuestionsPerQR,
        String domainShortname,
        QuestionMetadataBaseRepository metadataRepo, QuestionMetadataDraftRepository metadataDraftRepo,
        // QuestionRequestLogRepository qrLogRepo,
        String storage_src_dir, String storage_dst_dir, int storageDummyDirsForNewFile) {


        // determine questions are to export (really new; this query excludes duplicates)
        // find draft questions not yet exported with LEFT JOIN
        val toExport = metadataDraftRepo.findNotYetExportedQuestions(domainShortname);  // restricted to one Domain only. TODO: allow arbitrary domains ?

        List<QuestionMetadataEntity> toImport;
        if (toExport.isEmpty()) {
            System.out.println("All draft questions have already exported.");
        } else {
            // send ready questions data to production bank
            int nExported = RdfStorage.exportQDtaFilesToProductionBank(toExport, storage_src_dir, storage_dst_dir, storageDummyDirsForNewFile);

            // integrate question metadata into production metadata table
            // Process only those questions that were successfully exported in previous operation.
            toImport = new ArrayList<QuestionMetadataEntity>(nExported);
            for (val d : toExport.subList(0, nExported)) {
                val q = d.toMetadataEntity();
                // q.id is null (this q is inserted as new)
                q.setStage(STAGE_READY);
                toImport.add(q);
            }

            val saved = metadataRepo.saveAll(toImport);
            System.out.println("Saved " + toImport.size() + " new question metadata rows into production table.");
        }
    }


    /**
     * Find questions and/or question templates which have `unsolvedSubgraph` set to rdf:nil.
     *
     * @param unsolvedSubgraph QUESTION_TEMPLATE_SOLVED or QUESTION_SOLVED
     * @return list of names
     */
    public List<String> unsolvedQuestions(/*String classUri,*/ GraphRole unsolvedSubgraph) {
        if (!USE_RDF_STORAGE) {
            return List.of();
        }
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
        if (!USE_RDF_STORAGE) {
            return List.of();
        }
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

    public boolean localGraphExists(String gUri) {
        if (!USE_RDF_STORAGE) {
            return false;
        }
        return dataset.containsNamedModel(gUri);
    }

    public Model getLocalGraphByUri(String gUri) {
        if (USE_RDF_STORAGE && dataset != null) {
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
        if (/*USE_RDF_STORAGE &&*/ dataset != null) {
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
        if (!USE_RDF_STORAGE) {
            return getDomainSchemaForSolving();
        }
        String uri = NS_graphs.get(GraphRole.SCHEMA.ns().base());
//        if (!dataset.containsNamedModel(uri)) {
//            setLocalGraph(uri, getDomainSchemaForSolving());
//        }
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
            m2 = inferred;
        }
        /*m2 = getLocalGraphByUri(uri);*/
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

        // Note: changes done to inf are also applied to srcModel.
        InfModel inf = ModelFactory.createInfModel(reasoner, srcModel);
        inf.prepare();

        long estimatedTime = System.nanoTime() - startTime;
//        log.info
        log.info("Time Jena spent on reasoning: " + String.format("%.5f",
                (float) estimatedTime / 1000 / 1000 / 1000) + " seconds.");

        Model result;
        if (retainNewFactsOnly) {
            // make a true copy
            result = ModelFactory.createDefaultModel().add(inf);
            // cleanup the inferred results (inf) ...
            result.remove(srcModel);
        } else {
            result = inf;
        }
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
