package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.rdfconnection.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.JenaException;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.modify.request.UpdateClear;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.XSD;
import org.vstu.compprehension.models.businesslogic.domains.Domain;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;


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
    public final List<GraphRole> components;

    private GraphRole(String prefix) {
        this.prefix = prefix;
        this.components = List.of();
    }
    private GraphRole(String prefix, List<GraphRole> components) {
        this.prefix = prefix;
        this.components = components;
    }

    /** Convert to NamespaceUtil instance */
    public NamespaceUtil ns() {
        return new NamespaceUtil(prefix);
    }

    /** Convert to NamespaceUtil instance based on provided prefix */
    public NamespaceUtil ns(String basePrefix) {
        return new NamespaceUtil(basePrefix + prefix);
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
    final static NamespaceUtil NS_code = new NamespaceUtil(NS_root.get("code/"));
    final static NamespaceUtil NS_graphs = new NamespaceUtil(NS_root.get("graphs/"));
    //    graphs:
    //    class NamedGraph
    //		modifiedAt: datetime   (1..1)
    //      dependsOn: NamedGraph  (0..*)
    final static NamespaceUtil NS_questions = new NamespaceUtil(NS_root.get("questions/"));
    /* questions:
    * class QuestionTemplate:
    *   name: str   (1..1)
    *   has_graph_qt    \ NamedGraph or rdf:nil
    *   has_graph_qt_s  / (1..1)
    *   ...
    * class Question:
    *   name: str   (1..1)
    *   has_graph_qt   \
    *   has_graph_qt_s  | NamedGraph or rdf:nil
    *   has_graph_q     | (1..1)
    *   has_graph_q_s  /
    *   formulation_structural_complexity : int
    *   ...
    * */
    final static NamespaceUtil NS_oop = new NamespaceUtil(NS_root.get("oop/"));

//    static String BASE_DB_PATH = "tdb/";
//    static String BASE_PREFIX = "http:/poas.ru/";
    static String FUSEKI_ENDPOINT_BASE = "http://vds84.server-1.biz:6515/";
    static Map<String, String> DOMAIN_TO_ENDPOINT;
    static {
        DOMAIN_TO_ENDPOINT = new HashMap<>(2);
        DOMAIN_TO_ENDPOINT.put("ControlFlowStatementsDomain", "control_flow/update");
        DOMAIN_TO_ENDPOINT.put("ProgrammingLanguageExpressionDomain", "expression/update");
    }

    /**
     * name of directory under BASE_DB_PATH to store domain-specific database in
     */
    String sparql_endpoint = null;
    String uriPrefix = NS_code.get();

    Domain domain;

    /** Temporary storage (cache) for RDF graphs from remote RDF DB (ex. Fuseki) */
    Dataset dataset = null;

    public RdfStorage(Domain domain) {

        assert domain != null;
        this.domain = domain;

        String name = DOMAIN_TO_ENDPOINT.get(domain.getName());
        assert name != null;  // Ensure you created a database in Fuseki and mapped a domain to it in DOMAIN_TO_ENDPOINT map!
        this.sparql_endpoint = FUSEKI_ENDPOINT_BASE + name;

        initDB();
    }

    public RdfStorage(String sparql_endpoint) {

        this.domain = null;
        this.sparql_endpoint = sparql_endpoint;
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
//        finally {
//            System.out.println("Finally clause");
//        }
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
            return getLocalGraphByName(name);
        }
        return null;
    }

    /** Cache and send a graph to remote DB */
    boolean sendGraph(String name, Model m) {
        if (setLocalGraph(name, m)) {
            return uploadGraph(name);
        }
        return false;
    }

    /** Download and cache a graph if not cached yet */
    boolean fetchGraph(String name) {
        return fetchGraph(name, false);
    }

    /** Download and cache a graph
     * @param g graph name (an uri)
     * @return true on success
     */
    boolean fetchGraph(String g, boolean fetchAlways) {
        // check if graphs data loaded (as a single graph)
        if (! g.equals(NS_graphs.base()) && ! localGraphExists(NS_graphs.base())) {
            fetchGraph(NS_graphs.base());
        }
        if (!fetchAlways && localGraphExists(g))
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
                .from(g)
                .addWhere("?s", "?p", "?o");
        Query s = ab.build();
        try ( RDFConnection conn = getConn() ) {
            remoteGraphExists = conn.queryAsk(s);
        }

        if (remoteGraphExists) {
            Model graphModel = null;
            // Use CONSTRUCT query to copy a graph
            ConstructBuilder sb = new ConstructBuilder()
                    // .addVar("*")
                    .from(NS_graphs.base())
                    .addWhere("?s", "?p", "?o");
            try ( RDFConnection conn = getConn() ) {
                try (QueryExecution qExec = conn.query(sb.build())) {
                    graphModel = qExec.execConstruct();
                    // qExec.close();  // is done by try-resources block
                }
            }

            if (graphModel != null) {
                // remove & set obtained graph locally
                dataset.replaceNamedModel(g, graphModel);
                return true;
            }
        }

        return false;
    }

    /** Write whole local graph to remote storage (replace if one exists)
     * @param g graph name (an uri)
     * @return true on success
     */
    boolean uploadGraph(String g) {
        // String errMsg = "Graph doesn't exist locally: " + g;
        assert localGraphExists(g);

        // check if graphs data loaded (as a single graph)
        if (! g.equals(NS_graphs.base()) && ! localGraphExists(NS_graphs.base())) {
            fetchGraph(NS_graphs.base());
        }

        Model localGraph = getLocalGraphByName(g);

        // clear old remote graph + insert new data there...
//        String clearGraphSparql = "CLEAR SILENT GRAPH <" + g + ">";
        UpdateRequest clearGraphSparql = new UpdateRequest(new UpdateClear(g, true));

        UpdateBuilder builder = new UpdateBuilder();
        builder.addPrefixes( localGraph );
        builder.addInsert( g, localGraph );
        UpdateRequest insertGraphQuery = builder.buildRequest();

        runQueriesOnRemoteDB(List.of(clearGraphSparql, insertGraphQuery));

        /*try ( RDFConnection conn = getConn() ) {
            conn.begin( TxnType.WRITE );
            conn.update( clearGraphSparql );

            conn.update( insertGraphQuery );
            conn.commit();
            // return true;
        } catch (JenaException exception) {
            return false;
        }*/

        // update graph metadata - modifiedAt -> new date
        return actualizeUpdateTime(g);
    }

    /** Set Update time for both remote and local versions of graph g
     * @param g graph uri
     * @return success
     */
    boolean actualizeUpdateTime(String g) {
        // update graph metadata?
        // modifiedAt -> new date
        String dateNowStr = Instant.now().toString();
        Literal dateLiteral = dataset.getDefaultModel().createTypedLiteral(dateNowStr, XSD.dateTime.getURI());

        Node gNode = NodeFactory.createURI(g);

        UpdateRequest upd_modifiedAt = makeUpdateTripleQuery(
                gNode,
                gNode,
                NodeFactory.createURI(NS_graphs.get("modifiedAt")),
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
        //// ub3.addPrefix("graphs", NS_graphs.get());
        ub3.addInsert(ng, s, p, o); // quad
//        Node subj = NodeFactory.createURI(NS_graphs.get(SubjName));
//        Node pred = NodeFactory.createURI(NS_graphs.get("modifiedAt"));
        Var obj = Var.alloc("obj");
        // quad
        ub3.addDelete(ng, s, p, obj); // quad
        ub3.addWhere(new WhereBuilder()
                .addGraph(ng, s, p, obj)
        );
        UpdateRequest ur = ub3.buildRequest();
        //// System.out.println(ur.toString());
        return ur;
    }

    boolean runQueriesWithConnection(RDFConnection connection, Collection<UpdateRequest> requests) {
        try ( RDFConnection conn = connection ) {
            conn.begin( TxnType.WRITE );

            for (UpdateRequest r : requests) {
                conn.update( r );
            }

            conn.commit();
            return true;
        } catch (JenaException exception) {
            return false;
        }

    }

    boolean runQueriesOnRemoteDB(Collection<UpdateRequest> requests) {
        return runQueriesWithConnection(getConn(), requests);
    }

    boolean runQueriesLocally(Collection<UpdateRequest> requests) {
        return runQueriesWithConnection(RDFConnectionFactory.connect(dataset), requests);
    }

    boolean runQueries(Collection<UpdateRequest> requests) {
        return runQueriesOnRemoteDB(requests)
                &&
               runQueriesLocally(requests);
    }


//    public void setUriPrefix(String prefix) {
//        this.uriPrefix = prefix;
//    }

    public String uriPrefix() {
        return this.uriPrefix;
    }


/*    public String local2iri(String localName) {
        return uriPrefix() + localName;
    }

    public String iri2local(String iri) {
        String localName = iri;
        if (iri.startsWith(uriPrefix()))
            localName = iri.substring(uriPrefix().length());
        else
            log.warn(String.format("IRI does not begin with expected prefix: '%s'", iri));

        return localName;
    }

//    public String getName() {
//        return name;
//    }*/

    public boolean localGraphExists(String name) {
        return dataset.containsNamedModel(name);
    }

    public Model getLocalGraphByName(String name) {
        if (dataset != null) {
            if (localGraphExists(name)) {
                return dataset.getNamedModel(name);
            } else log.warn(String.format("Graph not found - name: '%s'", name));
        } else {
            log.warn("Dataset was not initialized");
        }
        return null;
    }

    /**
     * запись/обновление подграфа триплетов в хранилище
     * @param name
     * @param model
     * @return true on success
     */
    public boolean setLocalGraph(String name, Model model) {
        if (dataset != null) {
            dataset.replaceNamedModel(name, model);
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
     */

    public String uriForQuestionGraph(String questionName, GraphRole role) {
        return role.ns(NS_questions.get())
                .get(questionName);
    }

    public String nameFromQuestionGraphUri(String questionUri, GraphRole role) {
        int sep_pos = questionUri.indexOf('#');  // assume the prefix is #-ended
        if (sep_pos > -1) {
            return questionUri.substring(sep_pos + 1);
        }

        log.warn(String.format("Question IRI does not begin with recognizable prefix: '%s'", questionUri));
        return questionUri;
    }

    /** ... */
    public Model getQuestionSubgraph(String questionName, GraphRole role) {
        return getGraph(uriForQuestionGraph(questionName, role));
    }

    List<GraphRole> questionStages() {
        return List.of(
                GraphRole.QUESTION_TEMPLATE,
                GraphRole.QUESTION_TEMPLATE_SOLVED,
                GraphRole.QUESTION,
                GraphRole.QUESTION_SOLVED
        );
    }

    /** ... */
    public GraphRole getQuestionStatus(String questionName) {
        Model qG = getGraph(NS_questions.base());  // questions Graph containing questions metadata
        if (qG != null) {

//            ResIterator qResources = qG.listSubjectsWithProperty(
//                    qG.createProperty(NS_questions.base()),
//                    );
//
//            for (GraphRole role : questionStages()) {
//
//            }
//            getGraph(uriForQuestionGraph(questionName, role));
        }
        return null;
    }

    /**
     * запись/обновление одного из 4-х видов подграфа вопроса (например, создание нового вопроса или добавление
     * *solved-данных для него)
     * @param questionName unqualified name of Question or QuestionTemplate
     * @param role
     * @param model
     * @return
     */
    public boolean setQuestionSubgraph(String questionName, GraphRole role, Model model) {
        String qgUri = uriForQuestionGraph(questionName, role);
        boolean success = sendGraph(qgUri, model);

        // update questions metadata

        return success;
    }

    /**
     * получение подграфа триплетов, хранящего базовую схему домена (необходимую для работы ризонера)
     * @return model of triples
     */
    public Model getSchema() {
        return getLocalGraphByName(GraphRole.SCHEMA.prefix);
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


    public static void main(String[] args) {
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
        Node subj = NodeFactory.createURI(NS_graphs.get(SubjName));
        Node pred = NodeFactory.createURI(NS_graphs.get("modifiedAt"));
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
//        Node subj = NodeFactory.createURI(NS_graphs.get(SubjName));
//        Node pred = NodeFactory.createURI(NS_graphs.get("modifiedAt"));
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
            return;
        }

    }

}


