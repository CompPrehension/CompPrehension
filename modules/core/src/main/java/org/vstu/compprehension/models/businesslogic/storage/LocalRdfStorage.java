package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;
import org.vstu.compprehension.utils.Checkpointer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Log4j2
public class LocalRdfStorage extends AbstractRdfStorage {

    /**
     * Absolute path (ex. under FTP_BASE) to file containing domain-specific template/question metadata as RDF
     */
    String qGraph_filepath = null;

    public LocalRdfStorage(DomainEntity domain,
                           QuestionMetadataRepository questionMetadataRepository,
                           QuestionMetadataManager questionMetadataManager) throws URISyntaxException {
        this(new RemoteFileService(
                        domain.getOptions().getStorageUploadFilesBaseUrl(),
                        Optional.ofNullable(domain.getOptions().getStorageDownloadFilesBaseUrl())
                                .orElse(domain.getOptions().getStorageUploadFilesBaseUrl())),
                questionMetadataRepository,
                questionMetadataManager);

        // use options from Domain
        DomainOptionsEntity cnf = domain.getOptions();
        this.qGraph_filepath = Optional.ofNullable(cnf.getQuestionsGraphPath())
                .orElse(cnf.getStorageSPARQLEndpointUrl());

        //setDomain(domain);

        // test it
//        this.getQuestionMetadataManager();
//        log.info("getQuestionMetadataManager completed.");
    }

    public LocalRdfStorage(RemoteFileService remoteFileService,
                           QuestionMetadataRepository questionMetadataRepository,
                           QuestionMetadataManager questionMetadataManager) {
        super(remoteFileService,
                questionMetadataRepository,
                questionMetadataManager);
    }

/*
    public LocalRdfStorage(String qGraph_filepath,
                           RemoteFileService
                           QuestionMetadataDraftRepository questionMetadataDraftRepository,
                           QuestionMetadataManager questionMetadataManager) {

        super(fileService, questionMetadataDraftRepository, questionMetadataManager);
        this.qGraph_filepath = qGraph_filepath;

        // init FTP pointing to some remote dir
        this.fileService = new RemoteFileService(FTP_BASE + "tmp" + "/");

        initDB();
    }*/


    /*
    public void setDomain(Domain domain) {
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
            String name = domain.getShortName();
            assert name != null;  // Ensure you created a database file and mapped a domain to it in DOMAIN_TO_ENDPOINT map!
            this.qGraph_filepath = FTP_BASE + name + "." + DEFAULT_RDF_SYNTAX.getFileExtensions().get(0);
            log.info("qGraph_filepath is set to: " + qGraph_filepath);

            // init FTP pointing to domain-specific remote dir
            this.fileService = new RemoteFileService(FTP_BASE + name, FTP_DOWNLOAD_BASE + name);
            this.fileService.setDummyDirsForNewFile(2);  // 1 is the default
        }

        initDB();
    }

    protected void initDB() {

        try {
//            dataset = TDB2Factory.createDataset() ;  // directory
//              dataset = DatasetFactory.createTxnMem();  // an in-memory. transactional Dataset
            dataset = DatasetFactory.create();  // a simple Dataset
            log.debug("local dataset initialised");
        }
        catch(JenaException ex) {
            log.error("dataset initialisation failed:");
            log.error(ex.getMessage());
            ex. printStackTrace();
        }

        log.info("LocalRdfStorage: init completed for: " + this.fileService.getBaseDownloadUri());
    }


     */


//    @Override
    boolean fetchGraph(String gUri, boolean fetchAlways) {
        if (!fetchAlways && localGraphExists(gUri))
            return true;

        if (!gUri.equals(NS_questions.base())) {
            // just make an empty model.
            setLocalGraph(gUri, ModelFactory.createDefaultModel());
            return true;
        }

        // Handle the special case using local file with the graph

        Checkpointer ch = new Checkpointer(log);

        Model model = ModelFactory.createDefaultModel();
        // just load the whole graph from file

        // find first existing path, then get it's IN stream.

        String ext_ttl = "." + DEFAULT_RDF_SYNTAX.getFileExtensions().get(0);


//        InputStream in = null;
        String path = null;
        Lang syntax = DEFAULT_RDF_SYNTAX;
        for (String p : List.of(
                qGraph_filepath,
                qGraph_filepath + ".bak"
        )) {
//            path = Paths.get(p);
            File f = new File(p);
            if (f.exists() && !f.isDirectory()) {
                path = p;
                // found file successfully, exit loop.
                break;
            }
        }

        if (path == null) {
            log.error("Cannot read questions data from file: {}", qGraph_filepath);
            throw new RuntimeException("Cannot find questions data file: " + qGraph_filepath);
        }

        ch.hit("fetchGraph() - found file on disk");

        // read & replace the model
        // RDFDataMgr.read(model, in, syntax);
        RDFDataMgr.read(model, path, syntax);
        ch.hit("fetchGraph() - model read");
        setLocalGraph(gUri, model);
        ch.hit("fetchGraph() - local graph set");
        ch.since_start("fetchGraph() - total time");

        return true;
    }

    public boolean saveToFilesystem() {
        return uploadGraph(NS_questions.base());
    }

    public String saveQuestionData(String basePath, String questionName, String data) throws IOException {
        var rawQuestionPath = Path.of(basePath, questionName + ".json");
        return saveQuestionDataImpl(rawQuestionPath.toString(), data);
    }

    public String saveQuestionData(String questionName, String data) throws IOException {
        return saveQuestionDataImpl(questionName + ".json", data);
    }

    private String saveQuestionDataImpl(String rawQuestionPath, String data) throws IOException {
        var questionPath = getFileService().prepareNameForFile(rawQuestionPath, false);
        try (OutputStream stream = getFileService().openForWrite(questionPath)) {
            stream.write(data.getBytes(StandardCharsets.UTF_8));
            return questionPath;
        } finally {
            try {
                getFileService().closeConnections();
            } catch (FileSystemException e) {
                log.error("Error closing file service connection: {}", e.getMessage(), e);
            }
        }
    }

    // @Override
    boolean uploadGraph(String gUri) {
        if (!USE_RDF_STORAGE || !gUri.equals(NS_questions.base())) {
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
            log.error("Error uploading graph with uri [{}] - {}", gUri, e.getMessage(), e);
        }
        return true;
    }

     /*public static void main(String [] args) {

        // convert Turtle to RDF/Binary (.rt)
        // results: binary format takes more disk space than Turtle
        String filePath = "c:/data/compp/control_flow.ttl";
        String filePathOut = "c:/data/compp/control_flow." + Lang.RDFTHRIFT.getFileExtensions().get(0);

        Model model = ModelFactory.createDefaultModel();
        RDFDataMgr.read(model, filePath, Lang.TURTLE);


        try {
            System.out.println("Saving dataset to disk...");
            try (OutputStream out = Files.newOutputStream(Paths.get(filePathOut))) {
                RDFDataMgr.write(out, model, Lang.RDFTHRIFT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/
}
