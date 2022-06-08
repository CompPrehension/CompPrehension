package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.JenaException;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.vstu.compprehension.models.businesslogic.domains.Domain;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

@Log4j2
public class LocalRdfStorage extends AbstractRdfStorage  {

    //// static Lang RDF_DATASET_SYNTAX = Lang.TRIG;

    static {
        DOMAIN_TO_ENDPOINT = new HashMap<>(2);
        DOMAIN_TO_ENDPOINT.put("ControlFlowStatementsDomain", "control_flow"); // not "control_flow/update"
        DOMAIN_TO_ENDPOINT.put("ProgrammingLanguageExpressionDomain", "expression"); // not "expression/update"
    }

    /**
     * Absolute path (ex. under FTP_BASE) to file containing domain-specific template/question metadata as RDF
     */
    String qGraph_filepath = null;

    public LocalRdfStorage(Domain domain) {

        assert domain != null;
        this.domain = domain;

        if (domain.getEntity() != null) {
            // use options from Domain
            DomainOptionsEntity cnf = domain.getEntity().getOptions();
            this.qGraph_filepath = Optional.ofNullable(cnf.getQuestionsGraphPath()).orElse(cnf.getStorageSPARQLEndpointUrl());

            // init FTP pointing to domain-specific remote dir
            this.fileService = new RemoteFileService(
                    cnf.getStorageUploadFilesBaseUrl(),
                    Optional.ofNullable(cnf.getStorageDownloadFilesBaseUrl())
                            .orElse(cnf.getStorageUploadFilesBaseUrl()));  // use upload Url for download by default
            this.fileService.setDummyDirsForNewFile(cnf.getStorageDummyDirsForNewFile());
        } else {
            log.warn("LocalRdfStorage: cannot get config from domain! Using default paths...");

            // default settings (if not available via domain)
            String name = DOMAIN_TO_ENDPOINT.get(domain.getName());
            assert name != null;  // Ensure you created a database file and mapped a domain to it in DOMAIN_TO_ENDPOINT map!
            this.qGraph_filepath = FTP_BASE + name + "." + DEFAULT_RDF_SYNTAX.getFileExtensions().get(0);
            log.info("qGraph_filepath is set to: " + qGraph_filepath);

            // init FTP pointing to domain-specific remote dir
            this.fileService = new RemoteFileService(FTP_BASE + name, FTP_DOWNLOAD_BASE + name);
            this.fileService.setDummyDirsForNewFile(2);  // 1 is the default
        }

        initDB();
    }

    public LocalRdfStorage(String qGraph_filepath, String templatesDir) {

        this.domain = null;
        this.qGraph_filepath = qGraph_filepath;

        // init fileService pointing to specified dir as plain filesystem
        if (! templatesDir.startsWith("file:///")) {
            // force prefix indicating filesystem type
            templatesDir = templatesDir + "file:///";
        }
        this.fileService = new RemoteFileService(templatesDir);

        initDB();
    }

    public LocalRdfStorage(String qGraph_filepath) {

        this.domain = null;
        this.qGraph_filepath = qGraph_filepath;

        // init FTP pointing to some remote dir
        this.fileService = new RemoteFileService(FTP_BASE + "tmp" + "/");

        initDB();
    }

    protected void initDB() {

        // strip prefix if present
        if (this.qGraph_filepath.startsWith("file:///")) {
            this.qGraph_filepath = this.qGraph_filepath.substring("file:///".length());
        }

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
//        setLocalGraph(NS_graphs.base(), ModelFactory.createDefaultModel());
//        fetchGraph(NS_graphs.base(), true);

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



    @Override
    public RDFConnection getConn() {
        throw new NotImplementedException("No remote connection can be returned for the LOCAL storage.");
        //// return null;
    }

    @Override
    boolean fetchGraph(String gUri, boolean fetchAlways) {
        if (!fetchAlways && localGraphExists(gUri))
            return true;

        if (!gUri.equals(NS_questions.base())) {
            // just make an empty model.
            setLocalGraph(gUri, ModelFactory.createDefaultModel());
            return true;
        }

        // Handle the special case using local file with the graph

        Model model = ModelFactory.createDefaultModel();
        // just load the whole graph from file
        Path path = Paths.get(qGraph_filepath);
        if (Files.notExists(path)) {
            path = Paths.get(qGraph_filepath + ".bak");
            if (Files.notExists(path)) {
                log.error("Cannot read questions data from file: " + qGraph_filepath);
                throw new RuntimeException("Cannot find questions data file: " + qGraph_filepath);
            }
        }
        InputStream in = null;
        try {
            in = Files.newInputStream(path);
        } catch (IOException e) {
            // try fallback to .bak version
            if (!path.endsWith(".bak")) {
                path = Paths.get(qGraph_filepath + ".bak");
                try {
                    in = Files.newInputStream(path);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else
                e.printStackTrace();
        }
        // read & replace the model
        RDFDataMgr.read(model, in, DEFAULT_RDF_SYNTAX);
        try {
            assert in != null;
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setLocalGraph(gUri, model);

        return true;
    }

    @Override
    boolean uploadGraph(String gUri) {
        if (!gUri.equals(NS_questions.base())) {
            // just do nothing.
            return true;
        }

        // Handle the special case using local file with the graph

        // just save the whole graph back to file
        try {
            // Backup the original file before rewriting it
            Files.move(Paths.get(qGraph_filepath), Paths.get(qGraph_filepath + ".bak"), StandardCopyOption.REPLACE_EXISTING);

            log.info("Saving dataset to disk...");
            try (OutputStream out = Files.newOutputStream(Paths.get(qGraph_filepath))) {
                RDFDataMgr.write(out, getLocalGraphByUri(gUri), DEFAULT_RDF_SYNTAX);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    boolean runQueries(Collection<UpdateRequest> requests) {
        // same as runQueriesLocally():
        return runQueriesWithConnection(RDFConnection.connect(dataset), requests, true);
    }

    @Override
    boolean runQueries(Collection<UpdateRequest> requests, boolean merge) {
        // same as runQueriesLocally():
        return runQueriesWithConnection(RDFConnection.connect(dataset), requests, merge);
    }

}
