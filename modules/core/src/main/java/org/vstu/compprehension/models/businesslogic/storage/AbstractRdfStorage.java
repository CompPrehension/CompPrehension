package org.vstu.compprehension.models.businesslogic.storage;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.PrintUtil;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.LawFormulation;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.QuestionBankSearchRequest;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.ExerciseAttemptEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;
import org.vstu.compprehension.models.entities.QuestionMetadataEntity;
import org.vstu.compprehension.models.entities.QuestionOptions.QuestionOptionsEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.utils.Checkpointer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
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

    /**
     * Temporary storage (cache) for RDF graphs from remote RDF DB (ex. Fuseki)
     * TODO: remove, since no rdf storage is in use any more
     */
    @Deprecated
    Dataset dataset = null;

    @Getter
    private final RemoteFileService fileService;
    private final QuestionMetadataRepository questionMetadataRepository;
    private final QuestionMetadataManager questionMetadataManager;

    protected AbstractRdfStorage(
            RemoteFileService fileService,
            QuestionMetadataRepository questionMetadataRepository,
            QuestionMetadataManager questionMetadataManager) {
        this.fileService = fileService;
        this.questionMetadataRepository = questionMetadataRepository;
        this.questionMetadataManager = questionMetadataManager;
    }

    private List<QuestionMetadataEntity> findLastNQuestionsMeta(QuestionBankSearchRequest qr, ExerciseAttemptEntity exerciseAttempt, int n) {
        List<QuestionEntity> list = exerciseAttempt.getQuestions();
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
    public List<Question> searchQuestions(Domain domain, ExerciseAttemptEntity attempt, QuestionBankSearchRequest qr, int limit) {

        Checkpointer ch = new Checkpointer(log);

        QuestionMetadataManager metaMgr = this.questionMetadataManager;
        int nQuestionsInAttempt = Optional.ofNullable(qr.getDeniedQuestionNames()).map(List::size).orElse(0);
        int queryLimit = limit + nQuestionsInAttempt;
        int hardLimit = 25;

        double complexity = qr.getComplexity();
        complexity = metaMgr.getWholeBankStat().complexityStat.rescaleExternalValue(complexity, 0, 1);

        long targetConceptsBitmask = qr.targetConceptsBitmask();
        /*long allowedConceptsBitmask = conceptsToBitmask(qr.getAllowedConcepts(), metaMgr);  // unused */
        long deniedConceptsBitmask = qr.deniedConceptsBitmask();
        long unwantedConceptsBitmask = findLastNQuestionsMeta(qr, attempt, 4).stream()
                .mapToLong(QuestionMetadataEntity::getConceptBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetConceptsBitmask &= ~deniedConceptsBitmask;
        long targetConceptsBitmaskInPlan = qr.targetConceptsBitmask();

        // use laws, for e.g. Expr domain
        long targetLawsBitmask = qr.targetLawsBitmask();
        /*long allowedLawsBitmask= lawsToBitmask(qr.getAllowedLaws(), metaMgr);  // unused */
        long deniedLawsBitmask = qr.deniedLawsBitmask();
        long unwantedLawsBitmask = findLastNQuestionsMeta(qr, attempt, 4).stream()
                .mapToLong(QuestionMetadataEntity::getLawBits).
                reduce((t, t2) -> t | t2).orElse(0);
        // guard: don't allow overlapping of target & denied
        targetLawsBitmask &= ~deniedLawsBitmask;
        long targetViolationsBitmaskInPlan = qr.targetLawsBitmask();


        // use violations from all questions is exercise attempt
        long unwantedViolationsBitmask = attempt.getQuestions().stream()
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

        List<QuestionMetadataEntity> foundQuestionMetas = metaMgr.findQuestionsAroundComplexityWithoutQIds(qr, 0.1, queryLimit, 12);

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
        log.info("searchQuestionsAdvanced - candidates: {}", foundQuestionMetas.stream().map(QuestionMetadataEntity::getName).toList());

        // set concepts from request (for future reference via questions' saved metadata)
        for (QuestionMetadataEntity m : foundQuestionMetas) {
            m.setConceptBitsInPlan(targetConceptsBitmaskInPlan);
            m.setViolationBitsInPlan(targetViolationsBitmaskInPlan);
            // Save actual requested bits as well
            m.setConceptBitsInRequest(targetConceptsBitmask);
            m.setViolationBitsInRequest(targetLawsBitmask);

            /// debug check law bits
            if (targetLawsBitmask != 0 && (targetLawsBitmask & m.getViolationBits()) == 0) {
                log.warn("No LAW bits matched: {} {}", targetLawsBitmask, m.getName());
            }
        }

        List<Question> loadedQuestions = loadQuestions(domain, foundQuestionMetas);
        ch.hit("searchQuestionsAdvanced - files loaded");

        if (loadedQuestions.size() == 1) {
            // increment the question's usage counter
            val meta = loadedQuestions.get(0).getQuestionData().getOptions().getMetadata();
            meta.setUsedCount(Optional.ofNullable(meta.getUsedCount()).orElse(0L) + 1);
            meta.setLastAttemptId(attempt.getId());
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
//        List<QuestionMetadataEntity> ranking6 = given.stream()
//                .sorted(Comparator.comparingInt(q -> -q.getDistinctErrorsCount()))
//                .collect(Collectors.toList());

        List<QuestionMetadataEntity> finalRanking = given.stream()
                .sorted(Comparator.comparingInt(q -> (
                        ranking1.indexOf(q) +
                        /*ranking2.indexOf(q) +*/
                        ranking3.indexOf(q)
                        /*ranking6.indexOf(q)*/
                        )))
                .collect(Collectors.toList());

        return finalRanking.subList(0, limit);
    }

    private List<Question> loadQuestions(Domain domain, Collection<QuestionMetadataEntity> metas) {
        List<Question> list = new ArrayList<>();
        for (QuestionMetadataEntity meta : metas) {
            Question question = loadQuestion(domain, meta);
            if (question != null) {
                list.add(question);
            }
        }
        return list;
    }


    private Question loadQuestion(Domain domain, @NotNull QuestionMetadataEntity qMeta) {
        Question q = loadQuestion(domain, qMeta.getQDataGraph());
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

    private Question loadQuestion(Domain domain, String path) {
        Question q = null;
        try (InputStream stream = fileService.getFileStream(path)) {
            if (stream != null) {
                q = domain.parseQuestionTemplate(stream);
            } else {
                log.warn("File NOT found by storage: {}", path);
            }
        } catch (IOException | NullPointerException | IllegalStateException e) {
            log.error("Error loading question with path [{}] - {}", path, e.getMessage(), e);
        } finally {
            try {
                getFileService().closeConnections();
            } catch (FileSystemException e) {
                log.error("Error closing connection - {}", e.getMessage(), e);
            }
        }
        return q;
    }

    Model getDomainSchemaForSolving(Domain domain) {
        if (domain != null) {
            return domain.getSchemaForSolving();
        }

        // the default
        return ModelFactory.createDefaultModel();
    }

    public List<Rule> getDomainRulesForSolvingAtLevel(Domain domain, GraphRole level) {
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
                        log.error("Following error in rule: {}", lawFormulation.getFormulation(), e);
                        continue;
                    }
                    rules.add(rule);
                }
            }
        }

        return rules;
    }

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
            getFileService().closeConnections();
        } catch (IOException /*| NullPointerException*/ e) {
            log.error("Error reading model with path [{}] - {}", filepath, e.getMessage(), e);
            return null;
        }
        return m;
    }

    public String nameForQuestionGraph(String questionName, GraphRole role) {
        // look for <Question>.<subgraph> property in metadata first
        QuestionMetadataEntity meta = findQuestionByName(questionName);
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
                    targetPath = meta.getQDataGraph(); break;
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
    public QuestionMetadataEntity findQuestionByName(String questionName) {
        val repo = this.questionMetadataRepository;
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

    List<GraphRole> questionStages() {
        return List.of(
                GraphRole.QUESTION_TEMPLATE,
                GraphRole.QUESTION_TEMPLATE_SOLVED,
                GraphRole.QUESTION,
                GraphRole.QUESTION_SOLVED
        );
    }

    @NotNull
    public QuestionMetadataEntity saveMetadataDraftEntity(QuestionMetadataEntity meta) {
        if (!meta.isDraft())
            meta.setDraft(true);
        return questionMetadataRepository.save(meta);
    }

    @NotNull
    public QuestionMetadataEntity saveMetadataEntity(QuestionMetadataEntity meta) {
        if (meta.isDraft())
            meta.setDraft(false);
        return questionMetadataRepository.save(meta);
    }

    /**
     * Create metadata representing empty Question, but not overwrite existing data if recreate == false.
     *
     * @param questionName unique identifier-like name of question
     * @return true on success
     */
    public QuestionMetadataEntity createQuestion(Domain domain, String questionName, String questionTemplateName, boolean recreate) {

        // find template metadata
        QuestionMetadataEntity templateMeta = findQuestionByName(questionTemplateName);


        QuestionMetadataEntity meta;
        if (!recreate) {
            meta = findQuestionByName(questionName);
            if (meta != null) {
                if (templateMeta != null) {
                    meta.setTemplateId(templateMeta.getId());
                }
                return meta;
            }
        }
        QuestionMetadataEntity.QuestionMetadataEntityBuilder builder;
        int templateId;
        if (templateMeta != null) {
            // get builder to copy data from template
            builder = templateMeta.toBuilder()
                    .id(null); // reset id
            templateId = templateMeta.getId();
        } else {
            builder = QuestionMetadataEntity.builder()
                    .domainShortname(Optional.ofNullable(domain).map(Domain::getShortName).orElse(""));
            templateId = -1;
        }

        // проинициализировать метаданные вопроса, далее сохранить в БД
        meta = builder.name(questionName)
                    .templateId(templateId)
                    .isDraft(true)
                    .stage(STAGE_QUESTION)
                    .version(GENERATOR_VERSION)
                    .build();
        meta = saveMetadataDraftEntity(meta);

        return meta;
    }

    public static int getTooFewQuestionsForQR(int qrLogId) {
        if (qrLogId != 0) {
            // TODO: get the exercise the QR made from and fetch its expected number of students
        }
        return 500;
    }

    public static int getQrEnoughQuestions(int qrLogId) {
        if (qrLogId != 0) {
            // TODO: get the exercise the QR made from and fetch its expected number of students
        }
        return 500;
    }

    public boolean localGraphExists(String gUri) {
        return false;
    }

    public Model getLocalGraphByUri(String gUri) {
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
    public Model getSchema(Domain domain) {
        return getDomainSchemaForSolving(domain);
    }

    /**
     * Get solved domain schema (for reasoning purposes)
     * @return model both schema and solved schema (the most of what exists - may be empty)
     */
    public Model getFullSchema(Domain domain) {
        Model m = ModelFactory.createDefaultModel();
        Model m2 = getSchema(domain);
        if (m2 != null)
            m.add(m2);

        String uri = NS_graphs.get(GraphRole.SCHEMA_SOLVED.ns().base());
        if (dataset != null && !dataset.containsNamedModel(uri) && !m.isEmpty()) {
            Model inferred = runReasoning(m, getDomainRulesForSolvingAtLevel(domain, GraphRole.SCHEMA), true);
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

    public Model runReasoning(Model srcModel, List<Rule> rules, boolean retainNewFactsOnly) {
        GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);

        long startTime = System.nanoTime();

        // Note: changes done to inf are also applied to srcModel.
        InfModel inf = ModelFactory.createInfModel(reasoner, srcModel);
        inf.prepare();

        long estimatedTime = System.nanoTime() - startTime;
        if (estimatedTime > 100_000_000) {  // > 0.1 s
            log.printf(Level.INFO, "Time Jena spent on reasoning: %.5f seconds.", (float) estimatedTime / 1000_000_000);
        }

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
}
