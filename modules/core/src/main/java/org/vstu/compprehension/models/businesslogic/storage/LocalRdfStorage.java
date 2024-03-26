package org.vstu.compprehension.models.businesslogic.storage;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.vfs2.FileSystemException;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;
import org.vstu.compprehension.models.repository.QuestionMetadataRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
    }

    public LocalRdfStorage(RemoteFileService remoteFileService,
                           QuestionMetadataRepository questionMetadataRepository,
                           QuestionMetadataManager questionMetadataManager) {
        super(remoteFileService,
                questionMetadataRepository,
                questionMetadataManager);
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
}
