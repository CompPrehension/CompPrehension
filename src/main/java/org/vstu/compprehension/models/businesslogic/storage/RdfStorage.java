package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.modify.request.UpdateClear;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.vstu.compprehension.Service.LocalizationService;
import org.vstu.compprehension.models.businesslogic.*;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;
import org.vstu.compprehension.models.entities.QuestionEntity;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


enum GraphRole {
    SCHEMA("schema#"), // all static assertions important for reasoning
    SCHEMA_SOLVED("schema_s#"), // inferences from schema itself
//    QUESTIONS("questions#"),  // all question metadata


    QUESTION_TEMPLATE("qt#"), // template - a backbone for a question
    QUESTION_TEMPLATE_SOLVED("qt_s#"), // inferences from template itself

    QUESTION("q#"),  // data complementing template to complete question
    QUESTION_SOLVED("q_s#"), // inferences from whole question

//    QUESTION_TEMPLATE_FULL("qt_f#", List.of(QUESTION_TEMPLATE, QUESTION_TEMPLATE_SOLVED)), // template + its inferences
//    QUESTION_FULL("q_f#", List.of(QUESTION, QUESTION_SOLVED, QUESTION_TEMPLATE, QUESTION_TEMPLATE_SOLVED)), // question + its inferences
    ;

    public final String prefix;

    private GraphRole(String prefix) {
        this.prefix = prefix;
    }

    /** Convert to NamespaceUtil instance */
    public NamespaceUtil ns() {
        return new NamespaceUtil(prefix);
    }

    /** Convert to NamespaceUtil instance based on provided prefix */
    public NamespaceUtil ns(String basePrefix) {
        return new NamespaceUtil(basePrefix + prefix);
    }

    static GraphRole getNext(GraphRole role) {
        int ordIndex = role.ordinal() + 1;
        for (GraphRole other : GraphRole.values()) {
            if (other.ordinal() == ordIndex)
                return other;
        }
        return null;
    }
    static GraphRole getPrevious(GraphRole role) {
        int ordIndex = role.ordinal() - 1;
        for (GraphRole other : GraphRole.values()) {
            if (other.ordinal() == ordIndex)
                return other;
        }
        return null;
    }
}


/**
 * Use separate RdfStorage for each domain.
 */
@Log4j2
public class RdfStorage {

    /**
     * Default prefixes
     */
    final static NamespaceUtil NS_root = new NamespaceUtil("http://vstu.ru/poas/");
    final static NamespaceUtil NS_code = new NamespaceUtil(NS_root.get("code#"));
    //// final static NamespaceUtil NS_namedGraph = new NamespaceUtil("http://named.graph/");
    final static NamespaceUtil NS_file = new NamespaceUtil("ftp://plain.file/");
    final static NamespaceUtil NS_graphs = new NamespaceUtil(NS_root.get("graphs/"));
    //    graphs:
    //    class NamedGraph
    //		modifiedAt: datetime   (1..1)
    //      dependsOn: NamedGraph  (0..*)
    final static NamespaceUtil NS_questions = new NamespaceUtil(NS_root.get("questions/"));
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
    final static NamespaceUtil NS_oop = new NamespaceUtil(NS_root.get("oop/"));

    // hardcoded FTP location:
    static String FTP_BASE = "ftp://poas:{6689596D2347FA1287A4FD6AB36AA9C8}@vds84.server-1.biz/ftp_dir/compp/";
    String FTP_DOWNLOAD_BASE = "http://vds84.server-1.biz/misc/ftp/compp/";
    //// static String FTP_BASE = "file:///c:/Temp2/compp/";  // local dir is supported too (for debugging)
    static Lang DEFAULT_RDF_SYNTAX = Lang.TURTLE;


    // hardcoded Fuseki endpoint:
    static String FUSEKI_ENDPOINT_BASE = "http://vds84.server-1.biz:6515/";
    //// static String FUSEKI_ENDPOINT_BASE = "http://localhost:6515/";


    static Map<String, String> DOMAIN_TO_ENDPOINT;
    static {
        DOMAIN_TO_ENDPOINT = new HashMap<>(2);
        DOMAIN_TO_ENDPOINT.put("ControlFlowStatementsDomain", "control_flow"); // not "control_flow/update"
        DOMAIN_TO_ENDPOINT.put("ProgrammingLanguageExpressionDomain", "expression"); // not "expression/update"
    }

    /**
     * name of directory under BASE_DB_PATH to store domain-specific database in
     */
    String sparql_endpoint = null;

    Domain domain;

    /** Temporary storage (cache) for RDF graphs from remote RDF DB (ex. Fuseki) */
    Dataset dataset = null;

    RemoteFileService fileService = null;

    public RdfStorage(Domain domain) {

        assert domain != null;
        this.domain = domain;

        if (domain.getEntity() != null) {
            // use options from Domain
            DomainOptionsEntity cnf = domain.getEntity().getOptions();
            this.sparql_endpoint = cnf.getStorageSPARQLEndpointUrl();

            // init FTP pointing to domain-specific remote dir
            this.fileService = new RemoteFileService(
                    cnf.getStorageUploadFilesBaseUrl(),
                    Optional.ofNullable(cnf.getStorageDownloadFilesBaseUrl())
                            .orElse(cnf.getStorageUploadFilesBaseUrl()));  // use upload Url for download by default
            this.fileService.setDummyDirsForNewFile(cnf.getStorageDummyDirsForNewFile());
        } else {
            // default settings (if not available via domain)
            String name = DOMAIN_TO_ENDPOINT.get(domain.getName());
            assert name != null;  // Ensure you created a database in Fuseki and mapped a domain to it in DOMAIN_TO_ENDPOINT map!
            this.sparql_endpoint = FUSEKI_ENDPOINT_BASE + name;

            // init FTP pointing to domain-specific remote dir
            this.fileService = new RemoteFileService(FTP_BASE + name, FTP_DOWNLOAD_BASE + name);
            this.fileService.setDummyDirsForNewFile(2);  // 1 is the default
        }

        initDB();
    }

    public RdfStorage(String sparql_endpoint) {

        this.domain = null;
        this.sparql_endpoint = sparql_endpoint;

        // init FTP pointing to some remote dir
        this.fileService = new RemoteFileService(FTP_BASE + "tmp" + "/");

        initDB();
    }

    protected void initDB() {
        try {
//            dataset = TDB2Factory.createDataset() ;  // directory
            dataset = DatasetFactory.createTxnMem();  // an in-memory. transactional Dataset
            log.debug("local dataset initialised");
        }
        catch(JenaException ex) {
            log.error("dataset initialisation failed:");
            log.error(ex.getMessage());
            ex. printStackTrace();
        }

        // init some named graphs
        setLocalGraph(NS_graphs.base(), ModelFactory.createDefaultModel());
        fetchGraph(NS_graphs.base(), true);

        if (!fetchGraph(NS_questions.base(), true)) {

            Model qG = ModelFactory.createDefaultModel();
            Resource classQuestion = qG.createResource(NS_questions.get("Question"));
            Resource classQuestionTpl = qG.createResource(NS_questions.get("QuestionTemplate"));
            qG.add(new StatementImpl(classQuestion, RDF.type, OWL.Class));
            qG.add(new StatementImpl(classQuestionTpl, RDF.type, OWL.Class));

            setLocalGraph(NS_questions.base(), qG);
            uploadGraph(NS_questions.base());
        }
    }

    /**
     * Find questions in current questions bank
     * @param qr QuestionRequest
     * @param limit maximum questions to return (set 0 or less to disable; note: this may be slow)
     * // (ignore randomSeed) param randomSeed influence randomized order (if selection from many candidates is required)
     * @return questions found or empty list if the requirements cannot be satisfied
     */
    List<Question> searchQuestions(QuestionRequest qr, int limit) {
        Model qG = getGraph(NS_questions.base());

        String complexity_threshold = "" + qr.getComplexity();
        String complexity_cmp_op = (qr.getComplexitySearchDirection().getValue() < 0)? "<=" : ">=";

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

        StringBuilder query = new StringBuilder("select distinct ?name ?complexity ?solution_steps \n" +
                //// "#(count(distinct ?law) as ?law_count) (count(distinct ?concept) as ?concept_count)\n" +
                "(group_concat(distinct ?law;separator=\"; \") as ?laws)\n" +
                "(group_concat(distinct ?concept;separator=\"; \") as ?concepts)\n" +
                " {  GRAPH <" + NS_questions.base() + "> {\n" +
                "\t?s a <" + NS_questions.get("Question") + "> .\n" +
                "    ?s <" + NS_questions.get("name") + "> ?name.\n" +
                "    ?s <" + NS_questions.get("integral_complexity") + "> ?complexity.\n" +
                "    ?s <" + NS_questions.get("solution_steps") + "> ?solution_steps.\n" +
                "    ?s <" + NS_questions.get("has_concept") + "> ?concept.\n" +
                "    ?s <" + NS_questions.get("has_violation") + "> ?law.\n" +
                "    filter(?complexity " + complexity_cmp_op + " " + complexity_threshold + ").\n" +
                "    bind(abs(" + complexity_threshold + " - ?complexity) as ?compl_diff).\n" +
                "    bind(abs(" + desiredSolutionSteps + " - ?solution_steps) as ?steps_diff).\n");
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
        for (Law law : qr.getDeniedLaws()) {
            String denied_law = law.getName();
            query.append("    filter not exists { ?s <")
                    .append(NS_questions.get("has_violation"))
                    .append("> \"")
                    .append(denied_law).append("\" }\n");
        }
        if (qr.getLawsSearchDirection().getValue() > 0) {
            //  "#    filter exists { ?s <http://vstu.ru/poas/questions/has_law> \"target_law\" }\n";
            for (Law law : qr.getTargetLaws()) {
                String target_law = law.getName();
                query.append("    filter exists { ?s <")
                        .append(NS_questions.get("has_violation"))
                        .append("> \"")
                        .append(target_law).append("\" }\n");
            }
        }
//                "#    filter exists { ?s <http://vstu.ru/poas/questions/has_concept> \"target_concept\" }\n" +
        // query footer
        query.append("  }}\n" +
                "group by ?name ?complexity ?solution_steps ?compl_diff\n" +
                "order by ?compl_diff ?steps_diff");
        if (limit > 0)
            query.append("\n  limit " + limit * 2);

        List<Question> questions = new ArrayList<>();
        try ( RDFConnection conn = RDFConnection.connect(dataset) ) {

            List<Question> finalQuestions = questions;  // copy reference as the lambda syntax requires
            conn.querySelect(query.toString(), querySolution -> {
                QuestionEntity qe = new QuestionEntity();
                qe.setQuestionName( querySolution.get("name").asLiteral().getString() );
                // todo: pass the kind of question here
                Question q = new Ordering(qe);
                q.getConcepts().addAll( List.of(querySolution.get("concepts").asLiteral().getString().split("; ")) );
                q.getNegativeLaws().addAll( List.of(querySolution.get("laws").asLiteral().getString().split("; ")) );

                if (limit > 0) {  // don't calc score if no filtering is required
                    int score = 0;  // how the question suits the request

                    // inspect laws
                    if (qr.getLawsSearchDirection().getValue() < 0) {
                        int i = 0;
                        int size = qr.getTargetLaws().size();
                        score += size * size / 2;  // max possible score for existing
                        // set penalty for each missing law (earlier ones cost more)
                        for (Law target : qr.getTargetLaws()) {
                            i += 1;
                            if (!q.getNegativeLaws().contains(target.getName()))
                                score -= (size - i);
                        }
                    }
                    // set penalty for each extra law that is not in allowed laws
                    ArrayList<String> extraLaws = new ArrayList<>(q.getNegativeLaws());
                    extraLaws.removeAll(qr.getTargetLaws().stream().map(Law::getName).collect(Collectors.toList()));
                    if (!extraLaws.isEmpty()) {
                        // found out how many laws are "not allowed"
                        extraLaws.removeAll(qr.getAllowedLaws().stream().map(Law::getName).collect(Collectors.toList()));
                        score -= extraLaws.size();  // -1 for each extra law
                    }

                    // inspect concepts
                    int i = 0;
                    int size = qr.getTargetConcepts().size();
                    score += size * size / 2;  // max possible score for existing
                    // set penalty for each missing law (earlier ones cost more)
                    for (Concept target : qr.getTargetConcepts()) {
                        i += 1;
                        if (!q.getConcepts().contains(target.getName()))
                            score -= (size - i);
                    }

                    // set penalty for each extra Concepts that is not in allowed laws
                    ArrayList<String> extraConcepts = new ArrayList<>(q.getConcepts());
                    extraConcepts.removeAll(qr.getTargetConcepts().stream().map(Concept::getName).collect(Collectors.toList()));
                    if (!extraConcepts.isEmpty()) {
                        // found out how many Concepts are "not allowed"
                        extraConcepts.removeAll(qr.getAllowedConcepts().stream().map(Concept::getName).collect(Collectors.toList()));
                        score -= extraConcepts.size();  // -1 for each extra concept
                    }

                    qe.setId((long) score);  // (ab)use ID for setting score temporarily
                }

                // add some data about this question
                qe.getStatementFacts().add(new BackendFactEntity("<this_question>", "complexity", "" + querySolution.get("complexity").asLiteral().getDouble()));
                qe.getStatementFacts().add(new BackendFactEntity("<this_question>", "solution_steps", "" + querySolution.get("solution_steps").asLiteral().getDouble()));

                finalQuestions.add(q);
            });

        } catch (JenaException exception) {
            exception.printStackTrace();
        }

        if (limit > 0) {
            // sort to ensure best score first
            questions.sort((o1, o2) -> -(int) (o1.getQuestionData().getId() - o2.getQuestionData().getId()));
            // retain requested number of questions
            questions = questions.subList(0, limit);
        }

        // insert to the questions some data (all we can here)
        for (Question q : questions) {
            QuestionEntity qe = q.getQuestionData();
            qe.setDomainEntity(domain.getEntity());
            qe.setExerciseAttempt(qr.getExerciseAttempt());
            //// qe.setOptions(domain.?);

            qe.setStatementFacts( modelToFacts(
                    getQuestionModel(q.getQuestionName()),
                    // TODO: adapt base URI for each domain
                    NS_code.base()
                    ));
        }

        return questions;
    }

    /**
     * @return [min, avg, max] statistics of solution_steps property
     */
    List<Double> questionSolutionLengthStat() {
        // TODO: cache result?
        String solutionLengthStatQuery = "select (max(?solution_steps) as ?max)" +
                " (min(?solution_steps) as ?min)" +
                " (avg(?solution_steps) as ?avg)\n" +
                " {  GRAPH <" + NS_questions.base() + "> {\n" +
                "    ?s a <" + NS_questions.get("Question") + "> .\n" +
                "    ?s <" + NS_questions.get("solution_steps") + "> ?solution_steps.\n" +
                "  }}";

        List<Double> result = new ArrayList<>();
        try ( RDFConnection conn = RDFConnection.connect(dataset) ) {

            conn.querySelect(solutionLengthStatQuery, querySolution -> {
                result.add((double) querySolution.get("min").asLiteral().getInt());
                result.add(         querySolution.get("avg").asLiteral().getDouble());
                result.add((double) querySolution.get("max").asLiteral().getInt());
            });

        } catch (JenaException exception) {
            exception.printStackTrace();
        }
        return result;
    }

    public static List<BackendFactEntity> modelToFacts(Model factsModel, String baseUri) {
        JenaBackend jback = new JenaBackend();
        jback.createOntology(baseUri);

        // fill model
        OntModel model = jback.getModel();
        model.add(factsModel);

        List<String> verbs = factsModel.listStatements().toList().stream()
                .map(Statement::getPredicate)
                .map(Property::getLocalName).distinct().collect(Collectors.toList());
        return jback.getFacts(verbs);
    }


    Model getDomainSchemaForSolving() {
        if (domain instanceof ControlFlowStatementsDomain) {
            Model schemaModel = ModelFactory.createDefaultModel();
            return schemaModel.read(ControlFlowStatementsDomain.VOCAB_SCHEMA_PATH);

        } else if (domain instanceof ProgrammingLanguageExpressionDomain) {
            // TODO: revise what is schema for this domain
            throw new NotImplementedException("schema for " + domain.getName());
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
        if (level.ordinal() >= GraphRole.QUESTION_TEMPLATE.ordinal() && level.ordinal() <= GraphRole.QUESTION_TEMPLATE_SOLVED.ordinal()) {
            laws.addAll(domain.getPositiveLaws());
        } else if (level.ordinal() >= GraphRole.QUESTION.ordinal()) {
            laws.addAll(domain.getNegativeLaws());
        } else {
            // passed not-a-question role -- get all
            laws.addAll(domain.getPositiveLaws());
            laws.addAll(domain.getNegativeLaws());
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

    /*
            REMOTE DATABASE COMMUNICATION
     */

    /** get connection to remote RDF DB (Fuseki) */
    public RDFConnection getConn() {
        RDFConnectionRemoteBuilder cb = RDFConnectionFuseki.create()
                .destination(sparql_endpoint);
        return cb.build();
    }

    /** Download, cache and return a graph */
    @Nullable
    Model getGraph(String name) {
        if (fetchGraph(name)) {
            return getLocalGraphByUri(name);
        }
        return null;
    }

    /** Download a file content from remote FTP, parse and return as Model */
    @Nullable
    Model fetchModel(String name) {
        Model m = ModelFactory.createDefaultModel();
        try (InputStream stream = fileService.getFileStream(name)) {
            RDFDataMgr.read(m, stream, DEFAULT_RDF_SYNTAX);
        } catch (IOException | NullPointerException e) {
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

    /** Send a model as file to remote FTP */
    boolean sendModel(String name, Model m) {
        try (OutputStream stream = fileService.saveFileStream(name)) {
            RDFDataMgr.write(stream, m, DEFAULT_RDF_SYNTAX);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /** Download and cache a graph if not cached yet */
    boolean fetchGraph(String gUri) {
        return fetchGraph(gUri, false);
    }

    /** Download and cache a graph
     * @param gUri graph name (an uri)
     * @return true on success
     */
    boolean fetchGraph(String gUri, boolean fetchAlways) {
        // check if "graphs" data loaded (as a single graph)
        if (! gUri.equals(NS_graphs.base()) && ! localGraphExists(NS_graphs.base())) {
            fetchGraph(NS_graphs.base());
        }
        if (!fetchAlways && localGraphExists(gUri))
            return true;

//        // TODO: check if graph is up-to-date
//        if (graphExists(g)) {
//            Date localTime = null;
//            Date remoteTime = null;
//            // ...
//
//            int timeDiff = remoteTime.compareTo(localTime);
//            if (timeDiff <= 0) {
//                return true;
//            } else {
//                // TODO: update local metadata, too
//            }
//        }

        // load desired graph
        boolean remoteGraphExists = false;  // false is default for the case of any error
        AskBuilder ab = new AskBuilder()
                .from(gUri)
                .addWhere("?s", "?p", "?o");
        Query s = ab.build();
        try ( RDFConnection conn = getConn() ) {
            remoteGraphExists = conn.queryAsk(s);
        }

        if (remoteGraphExists) {
            Model graphModel = null;
            // Use CONSTRUCT query to copy a graph
            ConstructBuilder sb = new ConstructBuilder()
                     .addConstruct("?s", "?p", "?o")
                    .from(gUri)
                    .addWhere("?s", "?p", "?o");
            try ( RDFConnection conn = getConn() ) {
                try (QueryExecution qExec = conn.query(sb.build())) {
                    graphModel = qExec.execConstruct();
                    // qExec.close();  // is done by try-resources block
                }
            }

            if (graphModel != null) {
                // remove & set obtained graph locally
                dataset.replaceNamedModel(gUri, graphModel);
                return true;
            }
        }

        return false;
    }

    /** Write whole local graph to remote storage (replace if one exists)
     * @param gUri graph uri
     * @return true on success
     */
    boolean uploadGraph(String gUri) {
        // String errMsg = "Graph doesn't exist locally: " + gUri;
        assert localGraphExists(gUri);

        // check if graphs data loaded (as a single graph)
        if (! gUri.equals(NS_graphs.base()) && ! localGraphExists(NS_graphs.base())) {
            fetchGraph(NS_graphs.base());
        }

        Model localGraph = getLocalGraphByUri(gUri);

        // clear old remote graph + insert new data there...
//        String clearGraphSparql = "CLEAR SILENT GRAPH <" + gUri + ">";
        UpdateRequest clearGraphSparql = new UpdateRequest(new UpdateClear(gUri, true));

        UpdateBuilder builder = new UpdateBuilder();
        builder.addPrefixes( localGraph );
        builder.addInsert( NodeFactory.createURI(gUri), localGraph );
        UpdateRequest insertGraphQuery = builder.buildRequest();

        runQueriesOnRemoteDB(List.of(clearGraphSparql, insertGraphQuery), false /*may be slow but safe?*/);

        if (false) {
            // update graph metadata - modifiedAt -> new date
            return actualizeUpdateTime(gUri);
        }
        return true;
    }

    /** Set UpdatedAt time for both remote and local versions of graph
     * @param gUri graph uri
     * @return success
     */
    boolean actualizeUpdateTime(String gUri) {
        // update graph metadata:
        // modifiedAt -> new date
        String dateNowStr = Instant.now().toString();
        Literal dateLiteral = dataset.getDefaultModel().createTypedLiteral(dateNowStr, XSD.dateTime.getURI());

        Node gNode = NodeFactory.createURI(gUri);

        UpdateRequest upd_modifiedAt = makeUpdateTripleQuery(
                NS_graphs.baseAsUri(),
                gNode,
                NS_graphs.getUri("modifiedAt"),
                dateLiteral
        );

        return runQueries(List.of(upd_modifiedAt));
    }

    /** Make a SPARQL Update query that removes all (s, p, *) triples and inserts one (s, p, o) triple into a named graph ng.
     * @param ng named graph
     * @param s subject
     * @param p predicate / property
     * @param o object
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

    boolean runQueriesWithConnection(RDFConnection connection, Collection<UpdateRequest> requests, boolean merge) {
        try ( RDFConnection conn = connection ) {
            conn.begin( TxnType.WRITE );

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

    boolean runQueriesOnRemoteDB(Collection<UpdateRequest> requests, boolean merge) {
        return runQueriesWithConnection(getConn(), requests, merge);
    }

    boolean runQueriesLocally(Collection<UpdateRequest> requests, boolean merge) {
        return runQueriesWithConnection(RDFConnectionFactory.connect(dataset), requests, merge);
    }

    boolean runQueries(Collection<UpdateRequest> requests) {
       return runQueriesOnRemoteDB(requests, true)
                &&
               runQueriesLocally(requests, true);
    }
    boolean runQueries(Collection<UpdateRequest> requests, boolean merge) {
        return runQueriesOnRemoteDB(requests, merge)
                &&
               runQueriesLocally(requests, merge);
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

//    /**
//     * получение подграфа триплетов, как он есть в хранилище
//     * @param localName graph name
//     * @return model of triples
//     */
//    public Model getGraphByName(String localName) {
//        return getGraphByIri(local2iri(localName));
//    }



    /*
            QUESTION DATA MANIPULATION
            (Name of QuestionTemplate can be used instead of Question name in many methods)
     */

    public String nameForQuestionGraph(String questionName, GraphRole role) {
        // look for <Question>-<subgraph> relation in metadata first
        Model qG = getGraph(NS_questions.base());
        // assert qG != null;
        RDFNode targetNamedGraph = null;

        if (qG != null) {
            targetNamedGraph = findQuestionByName(questionName)
                    .listProperties(questionSubgraphPropertyFor(role).baseAsPropertyOnModel(qG))
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

    //public String nameFromQuestionGraphUri(String questionUri, GraphRole role) {
    //    int sep_pos = questionUri.indexOf('#');  // assume the prefix is #-ended, see GraphRole prefixes
    //    if (sep_pos > -1) {
    //        return questionUri.substring(sep_pos + 1);
    //    }
    //
    //    log.warn(String.format("Question IRI does not begin with recognizable prefix: '%s'", questionUri));
    //    return questionUri;
    //}

    public static NamespaceUtil questionSubgraphPropertyFor(GraphRole role) {
        return new NamespaceUtil(NS_questions.get("has_graph_" + role.ns().base()));
    }

    /** Find Resource denoting question or question template node with given name from 'questions' graph */
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

    /** Get Model with specified `role` for question or question template, if one exists, else `null` is returned. */
    public Model getQuestionSubgraph(String questionName, GraphRole role) {
        return fetchModel(nameForQuestionGraph(questionName, role));
    }

    /** Get union of all models available for question or question template */
    public Model getQuestionModel(String questionName) {
        return getQuestionModel(questionName, GraphRole.QUESTION_SOLVED);
    }
    /** Get union of all models under `topRole` (inclusive) available for question or question template */
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

    /** Find what stage a question is in. Returned constant means which stage is reached now.
     * (Using "questions" metadata graph only, no more graphs fetched from remote.)
     * @param questionName question/questionTemplate unqualified name
     * @return one of questionStages(), or null if the question/questionTemplate does not exist. */
    public GraphRole getQuestionStatus(String questionName) {
        Resource questionNode = findQuestionByName(questionName);
        if (questionNode != null) {

            GraphRole approvedStatus = GraphRole.SCHEMA;  // below any valid question status

            for (GraphRole role : questionStages()) {
                /// boolean exists = fetchGraph(uriForQuestionGraph(questionName, role));

                boolean targetNamedGraphAbsent = !questionNode
                        .listProperties(questionSubgraphPropertyFor(role).baseAsPropertyOnModel(questionNode.getModel()))
                        .toList()
                        .stream()
                        .map(Statement::getObject)
                        .dropWhile(res -> res.equals(RDF.nil))
                        .findAny().isPresent();

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
     * создание/обновление и отправка во внешнюю БД одного из 4-х видов подграфа вопроса (например, создание нового вопроса или добавление
     * *solved-данных для него)
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

        UpdateRequest upd_setGraph = makeUpdateTripleQuery(
                NS_questions.baseAsUri(),
                questionNode,
                questionSubgraphPropertyFor(role).baseAsUri(),
                qgNode
        );

        return runQueries(List.of(upd_setGraph));
    }

    /**
     * Create metadata representing empty QuestionTemplate, but not overwrite existing data.
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
                    throw new RuntimeException("Cannot create QuestionTemplate: uri '" + qNode.getURI() + "' is already in use.");
                }

                // simple decision: do nothing if metadata node exists
                return true;
            }

            Node ngNode = NS_questions.baseAsUri();
            qNode = NS_classQuestionTemplate.getResourceOnModel(questionTemplateName, qG);

            List<UpdateRequest> commands = new ArrayList<>();

            commands.add(makeUpdateTripleQuery(ngNode, qNode, RDF.type, nodeClass));

            commands.add(makeUpdateTripleQuery(ngNode,
                    qNode,
                    NS_questions.getUri("name"),
                    NodeFactory.createLiteral(questionTemplateName)));

            // initialize template's graphs as empty ...
            // using "template-only" roles
            for (GraphRole role : questionStages().subList(0, 2)) {
                commands.add(makeUpdateTripleQuery(ngNode,
                        qNode,
                        questionSubgraphPropertyFor(role).baseAsUri(),
                        RDF.nil));
            }

            boolean success = runQueries(commands);
            return success;
        }
        return false;
    }

    /**
     * Create metadata representing empty Question, but not overwrite existing data if recreate == false.
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
                    throw new RuntimeException("Cannot create Question: uri '" + qNode.getURI() + "' is already in use.");
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

            commands.add(makeUpdateTripleQuery(ngNode, qNode, RDF.type, nodeClass));

            commands.add(makeUpdateTripleQuery(ngNode,
                    qNode,
                    NS_questions.getPropertyOnModel("name", qG),
                    NodeFactory.createLiteral(questionName)));

            commands.add(makeUpdateTripleQuery(ngNode,
                    qNode,
                    NS_questions.getPropertyOnModel("has_template", qG),
                    qtemplNode));

            // copy references to the graphs from template as is ...
            // using "template-only" roles
            for (GraphRole role : questionStages().subList(0, 2)) {
                Property propOfRole = questionSubgraphPropertyFor(role).baseAsPropertyOnModel(qG);
                RDFNode graphWithRole = qtemplNode.listProperties(propOfRole).nextStatement().getObject();
                commands.add(makeUpdateTripleQuery(ngNode, qNode, propOfRole, graphWithRole));
            }

            // initialize question's graphs as empty ...
            // using "question-only" roles
            for (GraphRole role : questionStages().subList(2, 4)) {
                commands.add(makeUpdateTripleQuery(ngNode,
                        qNode,
                        questionSubgraphPropertyFor(role).baseAsPropertyOnModel(qG),
                        RDF.nil));
            }

            return runQueries(commands);
        }
        return false;
    }


    /**
     * Add metadata triples to a Question node. Only scalar values (Literals) are allowed as an object in a triple.
     * Tip: use {@link NodeFactory} to create property URIs and literals. Property URI is typically obtained via NS_questions.getUri("...").
     * (QuestionTemplate is not supported here since NS_classQuestion namespace is hardcoded in this method).
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
            for (Pair<Node, Node>pair : propValPairs) {
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
     * @param questionName name of question or question template
     * @param desiredLevel QUESTION_TEMPLATE_SOLVED or QUESTION_SOLVED
     * @param tooLargeTemplateThreshold if > 0, do not process question templates having the number of RDF subjects exceeding this number (maybe for reducing reasoner load).
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
                System.out.println("Skip too large template of size "+ subjectCount +": " + questionName);
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
                                        questionSubgraphPropertyFor(unsolvedSubgraph).baseAsUri(),
                                        RDF.nil
                                )
                )
                .toString();

        RDFConnection connection = RDFConnection.connect(dataset);
        // RDFConnection connection = getConn();
        List<String> names = new ArrayList<>();
        try ( RDFConnection conn = connection ) {

            conn.querySelect(unsolvedTemplates, querySolution -> names.add(querySolution.get("name").asLiteral().getString()));

        } catch (JenaException exception) {
            exception.printStackTrace();
        }
        return names;
    }

    /**
     * Find questions or question templates within dataset.
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

        RDFConnection connection = RDFConnectionFactory.connect(dataset);  // TODO: replace deprecated ???
        // RDFConnection connection = getConn();
        List<String> names = new ArrayList<>();
        try ( RDFConnection conn = connection ) {

            conn.querySelect(queryNames, querySolution -> names.add(querySolution.get("name").asLiteral().getString()));

        } catch (JenaException exception) {
            exception.printStackTrace();
        }
        return names;
    }

    /**
     * (Method overload) Find all questions or question templates within dataset.
     * @param classUri full URI of `rdf:type` an instance should have
     * @return list of names
     */
    public List<String> findAllQuestions(String classUri) {
        return findAllQuestions(classUri, 0);
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

    /* *
     * получить модель, объединённую из всех подграфов для вопроса
     * @param ensureFull if true and any of subgraphs of question does not exist then return null
     * * /
    public Model getQuestionFull(String questionName, boolean ensureFull, boolean includeSchema) {
        List<SubgraphRole> requested = List.of(
                SubgraphRole.QUESTION_TEMPLATE,
                SubgraphRole.QUESTION_TEMPLATE_SOLVED,
                SubgraphRole.QUESTION,
                SubgraphRole.QUESTION_SOLVED
        );
        Model model = ModelFactory.createDefaultModel();

        for (SubgraphRole role : requested) {
            Model graph = getQuestionSubgraph(questionName, role);
            if (graph != null) {
                model.add(graph);
            } else if (ensureFull) {
                return null;
            }
        }

        if (model.isEmpty())
            return null;

        if (includeSchema) {
            model.add(getSchema());
        }
        return model;
    }

    List<String> listQuestions(SubgraphRole minLevel) {
        List<String> names = new ArrayList<>();

        List<SubgraphRole> levels = List.of(
                SubgraphRole.QUESTION_TEMPLATE,
                SubgraphRole.QUESTION_TEMPLATE_SOLVED,
                SubgraphRole.QUESTION,
                SubgraphRole.QUESTION_SOLVED
        );

        for (Iterator<String> it = dataset.listNames(); it.hasNext(); ) {
            String name = it.next();
            for (SubgraphRole level : levels) {
                String qPrefix = level.label + "#";
                if (name.startsWith(qPrefix))
                    names.add(name.substring(qPrefix.length()));

                if (level.ordinal() >= minLevel.ordinal())
                    break;
            }
        }

        return names;
    }  // */


    private Model runReasoning(Model srcModel, List<Rule> rules, boolean retainNewFactsOnly) {
        GenericRuleReasoner reasoner = new GenericRuleReasoner(rules);

        long startTime = System.nanoTime();

        // Note: changes done to inf are also applied to srcModel.
        InfModel inf = ModelFactory.createInfModel(reasoner, srcModel);
        inf.prepare();

        long estimatedTime = System.nanoTime() - startTime;
        log.info("Time Jena spent on reasoning: " + String.format("%.5f", (float)estimatedTime / 1000 / 1000 / 1000) + " seconds.");

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


    /*
basePrefix: String

Методы:
getGraphByUri(String uri) : Model
getGraphByName(String localName) : Model
получение подграфа триплетов, как он есть в хранилище
getSchema() : Model
получение подграфа триплетов, хранящего базовую схему домена (необходимую для работы ризонера)
getQuestionSubgraph(String questionName, enum QuestionSubgraphRole r) : Model
получение одного из 4-х видов подграфа вопроса

getQuestionFull(String questionName, boolean includeSchema = true) : Model
получить модель, объединённую из всех подграфов для вопроса

listQuestions(boolean solvedOnly = false) : StringList
список доступных вопросов

findQuestions(List<> targetConcepts, List<> deniedConcepts, List<> targetViolations, List<> deniedViolations, float targetComplexity, int targetSteps,  ..., int limit) : List<String>
findQuestion(List<> targetConcepts, List<> deniedConcepts, List<> targetViolations, List<> deniedViolations, float targetComplexity, int targetSteps, ..., int randomSeed) : String

findQuestionsLike(String questionName,  List<> targetViolations, List<> deniedViolations, float targetComplexity, int limit) : List<String>


setNamedGraph(String localName, Model m) : boolean
запись/обновление подграфа триплетов в хранилище
setQuestionSubgraph(String questionName, enum QuestionSubgraphRole r, Model m) : boolean
запись/обновление одного из 4-х видов подграфа вопроса (например, создание нового вопроса или добавление *solved-данных для него)

TODO:
API для автоматического (фонового) заполнения БД
RdfStorage.StartBackgroundDBFillUp(int proirity)
RdfStorage.StopBackgroundDBFillUp()


    *
    * */


    public static void main_2(String[] args) {
        // debug some things ...

        String sparql_endpoint = FUSEKI_ENDPOINT_BASE + DOMAIN_TO_ENDPOINT.get("ControlFlowStatementsDomain");
        RdfStorage rs = new RdfStorage(sparql_endpoint);

//        AskBuilder ab = new AskBuilder();
//        ab.from(NS_graphs.base());
//        ab.addWhere("?s", "?p", "?o");
//        Query s = ab.build();
//        System.out.println(s.toString());

        Instant d = Instant.now();
        String dateStr = d.toString();
        // dateStr = "\"" + dateStr + "\"^^xsd:dateTime";
        Literal dl = rs.dataset.getDefaultModel().createTypedLiteral(dateStr, XSD.dateTime.getURI());

        Resource gr = new ResourceImpl(NS_graphs.base());
        String SubjName = "my_Graph";

        // delete old triple
        UpdateBuilder ub = new UpdateBuilder();
        ub.addPrefix("graphs", NS_graphs.get());
        Node subj = NS_graphs.getUri(SubjName);
        Node pred = NS_graphs.getUri("modifiedAt");
        Var obj = Var.alloc("t");
        // quad
        ub.addDelete(gr, subj, pred, obj);
        /// ub.addWhere(gr, subj, "graphs:modifiedAt", obj);
        ub.addWhere(new WhereBuilder()
                .addGraph(gr, subj, pred, obj)
        );
//        ub.addWhere("?s", "graphs:name", gname);  // ?
        UpdateRequest s = ub.buildRequest();
        System.out.println(s.toString());


        // insert new triple
        UpdateBuilder ub2 = new UpdateBuilder();
        ub2.addPrefix("graphs", NS_graphs.get());
        // quad
        ub2.addInsert(gr, "graphs:my_Graph", "graphs:modifiedAt", dl);
//        ub2.addWhere("?s", "graphs:name", gname);  // ?
        UpdateRequest s2 = ub2.buildRequest();
        System.out.println(s2.toString());


        // variant II
        // delete & insert new triple
        UpdateBuilder ub3 = new UpdateBuilder();
        ub3.addPrefix("graphs", NS_graphs.get());
        // quad
        ub3.addInsert(gr, "graphs:my_Graph", "graphs:modifiedAt", dl);
//        Node subj = NS_graphs.getAsUri(SubjName);
//        Node pred = NS_graphs.getAsUri("modifiedAt");
//        Var obj = Var.alloc("t");
        // quad
        ub3.addDelete(gr, subj, pred, obj);
        ub3.addWhere(new WhereBuilder()
                .addGraph(gr, subj, pred, obj)
        );
        UpdateRequest s3 = ub3.buildRequest();
        System.out.println(s3.toString());

        // ok
        UpdateRequest s4 = makeUpdateTripleQuery(gr, subj, pred, dl);


        try ( RDFConnection conn = rs.getConn() ) {
            conn.begin( TxnType.WRITE );
//            conn.update( s );
//            conn.update( s2 );
//            conn.update( s3 );
            conn.update( s4 );

//            conn.update( insertGraphQuery );
            conn.commit();
            // return true;
        } catch (JenaException exception) {
        }
    }

    public static void main_4(boolean forceResolve) {
        // debug some things ...
        // solve <question template> graphs with domain

//        String sparql_endpoint = FUSEKI_ENDPOINT_BASE + DOMAIN_TO_ENDPOINT.get("ControlFlowStatementsDomain");
        ControlFlowStatementsDomain cfd = new ControlFlowStatementsDomain(new LocalizationService());
        RdfStorage rs = new RdfStorage(cfd);


        if (false) {
            _setQuestionAttributesExample(rs);
            return;
        }


        // find question templates to solve
        List<String> unsqts;
        if (!forceResolve)
            unsqts = rs.unsolvedQuestions(GraphRole.QUESTION_TEMPLATE_SOLVED);
        else
            unsqts = rs.findAllQuestions(NS_questions.get("QuestionTemplate"), 0);


        int toSolve = unsqts.size();
        System.out.println("Unsolved question templates: " + toSolve);
//        for (String name : unsqts) {
//            System.out.println(name);
//        }
//        System.out.println();


        // Solving
        int i = 1;
        for (String name : unsqts) {
            System.out.println("[" + i + " / " + toSolve + "] Solving: " + name);
            rs.solveQuestion(name, GraphRole.QUESTION_TEMPLATE_SOLVED, 80);
            i += 1;
        }
    }

    private static void _setQuestionAttributesExample(RdfStorage rs) {
        rs.setQuestionMetadata("44__binary_heap_free_v_nocond_q94",
                List.of(
                        Pair.of(NS_questions.getUri("has_tag"),
                                NodeFactory.createLiteral("C++")),
                        Pair.of(NS_questions.getUri("distinct_errors_count"),
                                NodeFactory.createLiteralByValue(3, XSDDatatype.XSDinteger)),
                        Pair.of(NS_questions.getUri("integral_complexity"),
                                NodeFactory.createLiteralByValue(0.0921, XSDDatatype.XSDdouble))
                ));
    }

    public static List<String> listFullFilePathsInDir(String dir) throws IOException {
        try (Stream<Path> stream = java.nio.file.Files.list(Paths.get(dir))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    // .map(Path::getFileName)  // this makes name relative
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    public static void main_3(String[] args) {
        // debug some things ...
        // upload <question template> graphs from files

        ControlFlowStatementsDomain cfd = new ControlFlowStatementsDomain(new LocalizationService());
        RdfStorage rs = new RdfStorage(cfd);

//        String rdf_dir = "c:\\Temp2\\cntrflowoutput_v6\\";
        String rdf_dir = "c:\\Temp2\\cntrflowoutput_v7_rdf\\";

        // Find files in local directory
        List<String> files = new ArrayList<>();
        try {
            files = listFullFilePathsInDir(rdf_dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (true) {  // optional filter by extension and file names
            String[] unwantedNameSubstrings = {"snlen", "stok", "strnlen", "strlen", "printf", "scanf", "acosl", };
            String wantedExt = ".rdf";
            files = files.stream()
                    .filter(s -> s.endsWith(wantedExt))
                    .filter(s -> Arrays.stream(unwantedNameSubstrings).noneMatch(s::contains))
                    .collect(Collectors.toList());
        }


        for (String file : files) {
            String name = file.substring(27);  // cut directory path
            List<String> nameParts = Arrays.stream(name.split("__")).collect(Collectors.toList());
            nameParts = nameParts.subList(0, nameParts.size() - 2);
            name = String.join("_", nameParts);

            System.out.print(name + " ...\t");

            rs.createQuestionTemplate(name);

            System.out.println("    Upload model ...");
            Model m = ModelFactory.createDefaultModel();
            RDFDataMgr.read(m, file);

            rs.setQuestionSubgraph(name, GraphRole.QUESTION_TEMPLATE, m);
        }
    }

    public static void main(String[] args) {
        main_3(args); // upload graphs as question templates
        main_4(false); // solve question templates


        System.out.println("Finished.");
    }
}


