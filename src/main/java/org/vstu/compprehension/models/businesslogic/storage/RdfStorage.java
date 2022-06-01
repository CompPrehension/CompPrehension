package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFuseki;
import org.apache.jena.rdfconnection.RDFConnectionRemoteBuilder;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.modify.request.UpdateClear;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Storage of templates & questions for a domain.
 * Metadata is available from SPARQL endpoint,
 * Actual questions data is accessed as files by FTP/HTTP/local filesystem.
 */
@Log4j2
public class RdfStorage extends AbstractRdfStorage {


    // hardcoded Fuseki endpoint:
    //// static String FUSEKI_ENDPOINT_BASE = "http://vds84.server-1.biz:6515/";
    static String FUSEKI_ENDPOINT_BASE = "http://localhost:6515/";


    static {
        DOMAIN_TO_ENDPOINT = new HashMap<>(2);
        DOMAIN_TO_ENDPOINT.put("ControlFlowStatementsDomain", "control_flow"); // not "control_flow/update"
        DOMAIN_TO_ENDPOINT.put("ProgrammingLanguageExpressionDomain", "expression"); // not "expression/update"
    }

    /**
     * Relative path under FUSEKI_ENDPOINT_BASE to store domain-specific database in
     */
    String sparql_endpoint = null;

    public RdfStorage(Domain domain) {

        assert domain != null;
        this.domain = domain;

        if (false && domain.getEntity() != null) {
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


    /*
            REMOTE DATABASE COMMUNICATION
     */

    /** get connection to remote RDF DB (Fuseki) */
    @Override
    public RDFConnection getConn() {
        RDFConnectionRemoteBuilder cb = RDFConnectionFuseki.create()
                .destination(sparql_endpoint);
        return cb.build();
    }

    /** Download and cache a graph
     * @param gUri graph name (an uri)
     * @return true on success
     */
    @Override
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
    @Override
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

    boolean runQueriesOnRemoteDB(Collection<UpdateRequest> requests, boolean merge) {
        return runQueriesWithConnection(getConn(), requests, merge);
    }

    boolean runQueriesLocally(Collection<UpdateRequest> requests, boolean merge) {
        return runQueriesWithConnection(RDFConnection.connect(dataset), requests, merge);
    }

    @Override
    boolean runQueries(Collection<UpdateRequest> requests) {
       return runQueriesOnRemoteDB(requests, true)
                &&
               runQueriesLocally(requests, true);
    }
    @Override
    boolean runQueries(Collection<UpdateRequest> requests, boolean merge) {
        return runQueriesOnRemoteDB(requests, merge)
                &&
               runQueriesLocally(requests, merge);
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

    //public String nameFromQuestionGraphUri(String questionUri, GraphRole role) {
    //    int sep_pos = questionUri.indexOf('#');  // assume the prefix is #-ended, see GraphRole prefixes
    //    if (sep_pos > -1) {
    //        return questionUri.substring(sep_pos + 1);
    //    }
    //
    //    log.warn(String.format("Question IRI does not begin with recognizable prefix: '%s'", questionUri));
    //    return questionUri;
    //}



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

//        String sparql_endpoint = FUSEKI_ENDPOINT_BASE + DOMAIN_TO_ENDPOINT.get("ControlFlowStatementsDomain");
        String sparql_endpoint = FUSEKI_ENDPOINT_BASE + DOMAIN_TO_ENDPOINT.get("ProgrammingLanguageExpressionDomain");

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

        JenaBackend.registerBuiltins();

//        String sparql_endpoint = FUSEKI_ENDPOINT_BASE + DOMAIN_TO_ENDPOINT.get("ControlFlowStatementsDomain");

//        ControlFlowStatementsDomain cfd = new ControlFlowStatementsDomain(new LocalizationService());
//        RdfStorage rs = new RdfStorage(cfd);
        ProgrammingLanguageExpressionDomain pled = new ProgrammingLanguageExpressionDomain();
        RdfStorage rs = new RdfStorage(pled);


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

    public static void main_5(boolean forceResolve) {
        // create question(s) from template

        ProgrammingLanguageExpressionDomain pled = new ProgrammingLanguageExpressionDomain();
        RdfStorage rs = new RdfStorage(pled);

        // find question templates to solve
        List<String> sqts;
        sqts = rs.unsolvedQuestions(GraphRole.QUESTION_TEMPLATE_SOLVED);

        System.out.println("Unsolved question templates: " + sqts.size());
        for (String name : sqts) {
            System.out.println(name);
        }
        System.out.println();

        // Solving
        for (String templateName : sqts) {
            System.out.println("Creating question: " + templateName);
            Model solvedTemplateModel = rs.getQuestionModel(templateName, GraphRole.getPrevious(GraphRole.QUESTION_TEMPLATE));
            for (Map.Entry<String, Model> question : pled.generateDistinctQuestions(templateName, solvedTemplateModel, ModelFactory.createDefaultModel(), 1024).entrySet()) {
                // create metadata entry
                rs.createQuestion(question.getKey(), templateName, true);
                // set basic data of the question
                rs.setQuestionSubgraph(question.getKey(), GraphRole.QUESTION, question.getValue());
                // solve the question (using the data uploaded above as QUESTION)
                rs.solveQuestion(templateName, GraphRole.QUESTION_SOLVED);
            }
        }
    }

    static class Stats {
        int min = 100000000;
        int max = 0;
        float mean;
        int size = 0;

        Stats (List<Integer> a) {
            for (Integer x : a) {
                min = Math.min(min, x);
                max = Math.max(max, x);
                size++;
                mean += x;
            }
            mean /= size;
        }

        public void print(String name) {
           System.out.println(name + " min=" + min + " max=" + max + " count=" + size + " mean=" + mean);
        }
    }

    public static void main_3(String[] args) {
        // debug some things ...
        // upload <question template> graphs from files

//        ControlFlowStatementsDomain domain = new ControlFlowStatementsDomain(new LocalizationService());
        Domain domain = new ProgrammingLanguageExpressionDomain();

        RdfStorage rs = new RdfStorage(domain);

//        String rdf_dir = "c:\\Temp2\\cntrflowoutput_v7_rdf\\";
        String rdf_dir = "c:\\Temp2\\exprdata_v7\\";

        // Find files in local directory
        List<String> files = new ArrayList<>();
        try {
            files = listFullFilePathsInDir(rdf_dir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (domain instanceof ControlFlowStatementsDomain) {  // optional filter by extension and file names
            String[] unwantedNameSubstrings = {"snlen", "stok", "strnlen", "strlen", "printf", "scanf", "acosl", };
            String wantedExt = ".rdf";
            files = files.stream()
                    .filter(s -> s.endsWith(wantedExt))
                    .filter(s -> Arrays.stream(unwantedNameSubstrings).noneMatch(s::contains))
                    .collect(Collectors.toList());
        }


        for (String file : files) {  //// .subList(540, 1338) .subList(0, 32)  .subList(32, 438)  .subList(120, 140)
            int path_len = rdf_dir.length();
            String name = file.substring(path_len);  // cut directory path

            if (name.endsWith(".ttl"))
                name = name.substring(0, name.length() - ".ttl".length());

            if (domain instanceof ControlFlowStatementsDomain) {
                // cut last two sections with digits
                List<String> nameParts = Arrays.stream(name.split("__")).collect(Collectors.toList());
                nameParts = nameParts.subList(0, nameParts.size() - 2);
                name = String.join("_", nameParts);
            }
            else if (domain instanceof ProgrammingLanguageExpressionDomain) {
                name = name.replaceAll("[^a-zA-Z0-9_]", "");
            }

            System.out.print(name + " ...\t");

            rs.createQuestionTemplate(name);

            System.out.println("    Upload model ...");
            Model m = ModelFactory.createDefaultModel();
            RDFDataMgr.read(m, file);

            rs.setQuestionSubgraph(name, GraphRole.QUESTION_TEMPLATE, m);

            if (domain instanceof ProgrammingLanguageExpressionDomain) {
                Model solved = rs.solveTemplate(m, false);
                rs.setQuestionSubgraph(name, GraphRole.QUESTION_TEMPLATE_SOLVED, solved);
            }


        }
    }

    public static void main(String[] args) {
        main_3(args); // upload graphs as question templates
//        main_4(false); // solve question templates
//        main_4(true); // solve question templates (force re-solve)
        
        //// main_5(false); // make questions from templates

        System.out.println("Finished.");
    }
}


