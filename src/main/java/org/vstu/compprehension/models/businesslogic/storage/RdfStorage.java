package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.arq.querybuilder.*;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
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
import org.vstu.compprehension.models.businesslogic.Law;
import org.vstu.compprehension.models.businesslogic.LawFormulation;
import org.vstu.compprehension.models.businesslogic.domains.ControlFlowStatementsDomain;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;

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
    final static NamespaceUtil NS_namedGraph = new NamespaceUtil("http://named.graph/");
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
    *   name: str    (1..1)
    *   has_template (1..1)
    *   has_graph_qt   \
    *   has_graph_qt_s  | NamedGraph or rdf:nil
    *   has_graph_q     | (1..1)
    *   has_graph_q_s  /
    *   formulation_structural_complexity : int
    *   ...
    * */
    final static NamespaceUtil NS_oop = new NamespaceUtil(NS_root.get("oop/"));

//    static String BASE_PREFIX = "http:/poas.ru/";

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

    /** Cache and send a graph to remote DB */
    boolean sendGraph(String gUri, Model m) {
        if (setLocalGraph(gUri, m)) {
            return uploadGraph(gUri);
        }
        return false;
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

        // update graph metadata - modifiedAt -> new date
        return actualizeUpdateTime(gUri);
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
                NodeFactory.createURI(NS_graphs.base()),
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


//    public void setUriPrefix(String prefix) {
//        this.uriPrefix = prefix;
//    }
//    public String uriPrefix() {
//        return this.uriPrefix;
//    }


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
     * @param gUri
     * @param model
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

    public String uriForQuestionGraph(String questionName, GraphRole role) {
        // look for <Question>-<subgraph> relation in metadata first
        Model qG = getGraph(NS_questions.base());
        // assert qG != null;
        RDFNode targetNamedGraph = null;

        if (qG != null) {
            targetNamedGraph = findQuestionByName(questionName)
                    .listProperties(qG.createProperty(questionSubgraphPropertyFor(role)))
                    .toList().stream()
                    .map(Statement::getObject)
                    .dropWhile(res -> res.equals(RDF.nil))
                    .reduce((first, second) -> first)
                    .orElse(null);
        }
        if (targetNamedGraph != null) {
            return targetNamedGraph.asNode().getURI();
        }

        // no known relation - get default for a new one
        return role.ns(NS_namedGraph.get()).get(questionName);
        //// return role.ns().get(questionName);
    }

    public String nameFromQuestionGraphUri(String questionUri, GraphRole role) {
        int sep_pos = questionUri.indexOf('#');  // assume the prefix is #-ended, see GraphRole prefixes
        if (sep_pos > -1) {
            return questionUri.substring(sep_pos + 1);
        }

        log.warn(String.format("Question IRI does not begin with recognizable prefix: '%s'", questionUri));
        return questionUri;
    }

    public static String questionSubgraphPropertyFor(GraphRole role) {
        return NS_questions.get("has_graph_" + role.ns().base());
    }

    Resource findQuestionByName(String questionName) {
        Model qG = getGraph(NS_questions.base());  // questions Graph containing questions metadata
        if (qG != null) {
            List<Resource> qResources = qG.listSubjectsWithProperty(
                    qG.createProperty(NS_questions.get(), "name"),
                    questionName
            ).toList();
            if (!qResources.isEmpty())
                return qResources.get(0);
        }
        return null;
    }

    /** ... */
    public Model getQuestionSubgraph(String questionName, GraphRole role) {
        return getGraph(uriForQuestionGraph(questionName, role));
    }

    /** ... */
    public Model getQuestionModel(String questionName) {
        return getQuestionModel(questionName, GraphRole.QUESTION_SOLVED);
    }
    /** ... */
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
                        .listProperties(questionNode.getModel().createProperty(questionSubgraphPropertyFor(role)))
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
        String qgUri = uriForQuestionGraph(questionName, role);
        boolean success = sendGraph(qgUri, model);

        if (!success)
            return false;

        // update questions metadata
        Resource questionNode = findQuestionByName(questionName);
//        Resource questionNode = getGraph(NS_questions.base()).createResource( NS_questions.get(questionName) );  // ?? the way to obtain Uri
        Node qgNode = NodeFactory.createURI(qgUri);

        UpdateRequest upd_setGraph = makeUpdateTripleQuery(
                NodeFactory.createURI(NS_questions.base()),
                questionNode,
                NodeFactory.createURI(questionSubgraphPropertyFor(role)),
                qgNode
        );

        return runQueries(List.of(upd_setGraph));
    }

    /**
     * Create metadata representing empty QuestionTemplate, but not overwrite existing data.
     * @param questionTemplateName unique Uri-conformant name of question template
     * @return true on success
     */
    public boolean createQuestionTemplate(String questionTemplateName) {
        Model qG = getGraph(NS_questions.base());  // questions Graph containing questions metadata

        if (qG != null) {
            Resource nodeClass = qG.createResource(NS_questions.get("QuestionTemplate"), OWL.Class);
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

            qNode = qG.createResource(NS_questions.get(questionTemplateName));
            Node ngNode = NodeFactory.createURI(NS_questions.base());

            List<UpdateRequest> commands = new ArrayList<>();

            commands.add(makeUpdateTripleQuery(ngNode, qNode, RDF.type, nodeClass));
            //// qNode.addProperty(RDF.type, nodeClass);

            commands.add(makeUpdateTripleQuery(ngNode,
                    qNode,
                    qG.createProperty(NS_questions.get(), "name"),
                    NodeFactory.createLiteral(questionTemplateName)));

            // initialize template's graphs as empty ...
            // using "template-only" roles
            for (GraphRole role : questionStages().subList(0, 2)) {
                commands.add(makeUpdateTripleQuery(ngNode,
                        qNode,
                        qG.createProperty(questionSubgraphPropertyFor(role)),
                        RDF.nil));
            }

            boolean success = runQueries(commands);

            return success;
        }
        return false;
    }

    /**
     * Create metadata representing empty Question, but not overwrite existing data.
     * @param questionName unique Uri-conformant name of question
     * @return true on success
     */
    public boolean createQuestion(String questionName, String questionTemplateName) {
        Model qG = getGraph(NS_questions.base());  // questions Graph containing questions metadata

        if (qG != null) {
            Resource nodeClass = qG.createResource(NS_questions.get("Question"), OWL.Class);
            Resource qNode = findQuestionByName(questionName);

            // deal with existing node
            if (qNode != null) {
                // check if this node is indeed a question Template
                boolean rightType = qG.listStatements(qNode, RDF.type, nodeClass).hasNext();
                if (!rightType) {
                    throw new RuntimeException("Cannot create Question: uri '" + qNode.getURI() + "' is already in use.");
                }

                // simple decision: do nothing if metadata node exists
                return true;
            }

            if (!createQuestionTemplate(questionTemplateName)) // check if template is valid
                return false;

            Resource qtemplNode = findQuestionByName(questionTemplateName);

            Node ngNode = NodeFactory.createURI(NS_questions.base());
            qNode = qG.createResource(NS_questions.get(questionName));

            List<UpdateRequest> commands = new ArrayList<>();

            commands.add(makeUpdateTripleQuery(ngNode, qNode, RDF.type, nodeClass));

            commands.add(makeUpdateTripleQuery(ngNode,
                    qNode,
                    qG.createProperty(NS_questions.get(), "name"),
                    NodeFactory.createLiteral(questionName)));

            commands.add(makeUpdateTripleQuery(ngNode,
                    qNode,
                    qG.createProperty(NS_questions.get(), "has_template"),
                    qtemplNode));

            // copy references to the graphs from template as is ...
            // using "template-only" roles
            for (GraphRole role : questionStages().subList(0, 2)) {
                Property propOfRole = qG.createProperty(questionSubgraphPropertyFor(role));
                RDFNode graphWithRole = qtemplNode.listProperties(propOfRole).nextStatement().getObject();
                commands.add(makeUpdateTripleQuery(ngNode, qNode, propOfRole, graphWithRole));
            }

            // initialize question's graphs as empty ...
            // using "question-only" roles
            for (GraphRole role : questionStages().subList(2, 4)) {
                commands.add(makeUpdateTripleQuery(ngNode,
                        qNode,
                        qG.createProperty(questionSubgraphPropertyFor(role)),
                        RDF.nil));
            }

            return runQueries(commands);
        }
        return false;
    }


    /**
     * Solve a question or question template: create new subgraph & send it to remote, update questions metadata.
     * @param questionName name of question or question template
     * @param desiredLevel QUESTION_TEMPLATE_SOLVED or QUESTION_SOLVED
     * @return true on success
     */
    public boolean solveQuestion(String questionName, GraphRole desiredLevel) {
        Model qG = getGraph(NS_questions.base());

//        Resource qNode = findQuestionByName(questionName);

        assert qG != null;
//        qNode = qG.createResource(qNode.asResource());

        Model existingData = getQuestionModel(questionName, GraphRole.getPrevious(desiredLevel));

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
        Node ng = NodeFactory.createURI(NS_questions.base());
        String unsolvedTemplates = new SelectBuilder()
                //// .addVar("?node")
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
                                        NodeFactory.createURI(NS_questions.get("name")),
                                        "?name"
                                )
                                .addGraph(ng,
                                        "?node",
                                        NodeFactory.createURI(questionSubgraphPropertyFor(unsolvedSubgraph)),
                                        RDF.nil
                                )
                )
                .toString();

        RDFConnection connection = RDFConnectionFactory.connect(dataset);  // ???
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
     * @param classUri full URI of `rdf:type` an instance should have
     * @return list of names
     */
    public List<String> findAllQuestions(String classUri, int limit) {
        // find question templates to solve
        Node ng = NodeFactory.createURI(NS_questions.base());
        String queryNames = new SelectBuilder()
                //// .addVar("?node")
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
                                        NodeFactory.createURI(NS_questions.get("name")),
                                        "?name"
                                )
                )
                .toString();

        if (limit > 0) {
            queryNames += "\nLIMIT " + limit;
        }

        RDFConnection connection = RDFConnectionFactory.connect(dataset);  // ???
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

    public static void main_4(boolean forceResolve) {
        // debug some things ...
        // solve <question template> graphs with domain

//        String sparql_endpoint = FUSEKI_ENDPOINT_BASE + DOMAIN_TO_ENDPOINT.get("ControlFlowStatementsDomain");

        ControlFlowStatementsDomain cfd = new ControlFlowStatementsDomain(new LocalizationService());


        RdfStorage rs = new RdfStorage(cfd);



        // find question templates to solve
        List<String> unsqts;
        if (!forceResolve)
            unsqts = rs.unsolvedQuestions(GraphRole.QUESTION_TEMPLATE_SOLVED);
        else
            unsqts = rs.findAllQuestions(NS_questions.get("QuestionTemplate"), 0);


        System.out.println("Unsolved question templates: " + unsqts.size());
        for (String name : unsqts) {
            System.out.println(name);
        }
        System.out.println();


        // Solving
        for (String name : unsqts) {
            System.out.println("Solving: " + name);
            rs.solveQuestion(name, GraphRole.QUESTION_TEMPLATE_SOLVED);
        }
    }

    public static void main_3(String[] args) {
        // debug some things ...
        // upload <question template> graphs from files

        String sparql_endpoint = FUSEKI_ENDPOINT_BASE + DOMAIN_TO_ENDPOINT.get("ControlFlowStatementsDomain");
        RdfStorage rs = new RdfStorage(sparql_endpoint);

        List<String> files = List.of(
                "c:/Temp2/cntrflowoutput_v4/1__memcpy_s__1639429224.rdf",
                "c:/Temp2/cntrflowoutput_v4/2__memmove_s__1639429224.rdf",
                // "c:/Temp2/cntrflowoutput_v4/3__wcsnlen_s__1639429224.rdf",
                // "c:/Temp2/cntrflowoutput_v4/4___wcstok__1639429224.rdf",
                // "c:/Temp2/cntrflowoutput_v4/5__strnlen_s__1639429224.rdf",
                "c:/Temp2/cntrflowoutput_v4/6__arraylist_new__1639429224.rdf",
                "c:/Temp2/cntrflowoutput_v4/7__arraylist_free__1639429224.rdf",
                "c:/Temp2/cntrflowoutput_v4/8__arraylist_enlarge__1639429224.rdf",
                "c:/Temp2/cntrflowoutput_v4/9__arraylist_insert__1639429224.rdf",
                "c:/Temp2/cntrflowoutput_v4/10__arraylist_append__1639429224.rdf",
                "c:/Temp2/cntrflowoutput_v4/437__trie_lookup_binary__1639429230.rdf",
                "c:/Temp2/cntrflowoutput_v4/438__trie_num_entries__1639429230.rdf"
        );

        for (String file : files) {
            String name = file.substring(27);
            name = name.substring(0, name.length() - 16);

            System.out.println(name + " ...");

            rs.createQuestionTemplate(name);

            System.out.println("    Set/upload model ...");
            Model m = ModelFactory.createDefaultModel();
            RDFDataMgr.read(m, file);

            rs.setQuestionSubgraph(name, GraphRole.QUESTION_TEMPLATE, m);
        }
    }

    public static void main(String[] args) {
        // main_3(args); // upload graphs as question templates
        main_4(true); // solve question templates


        System.out.println("Finished.");
    }
}


