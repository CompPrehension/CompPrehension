package org.vstu.compprehension;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.vstu.compprehension.adapters.FakeDomainRepository;
import org.vstu.compprehension.adapters.FakeLocalizationService;
import org.vstu.compprehension.adapters.FakeQuestionMetadataRepository;
import org.vstu.compprehension.adapters.FakeRandomProvider;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.businesslogic.storage.LocalRdfStorage;
import org.vstu.compprehension.models.businesslogic.storage.QuestionMetadataManager;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        Main main = new Main();
        try {
            JCommander.newBuilder()
                    .addObject(main)
                    .build()
                    .parse(args);
        } catch (ParameterException e) {
            System.out.println("CLI parameters error:");
            System.out.println(e.getMessage());
            return;
        }
        main.generateQuestionsForExpressionsDomain();
    }

    @Parameter(names={"--source", "-s"}, description = "Path to directory with ttl", required = true)
    String sourcePath;
    @Parameter(names={"--sourceId", "-id"}, description = "Import id", required = true)
    String sourceId;
    @Parameter(names={"--output", "-o"}, description = "Path to output directory", required = true)
    String outputPath;

    public void generateQuestionsForExpressionsDomain() {
        //val df = ApplicationContextProvider.getApplicationContext().getBean(DomainFactory.class);

        //ProgrammingLanguageExpressionDomain domain = (ProgrammingLanguageExpressionDomain) df.getDomain("ProgrammingLanguageExpressionDomain");

        var domainEntity = new FakeDomainRepository().findById("").orElseThrow();
        var domain = new ProgrammingLanguageExpressionDomain(
                domainEntity,
                new FakeLocalizationService(),
                new FakeRandomProvider(),
                new FakeQuestionMetadataRepository()
        );

//        String rdf_dir = "c:\\Temp2\\exprdata_v7\\";

        // set configuration for storage creation into domain options:
        // FTP_BASE = storage_base_dir;
        // FTP_DOWNLOAD_BASE = storage_base_dir;
        DomainOptionsEntity cnf = domain.getEntity().getOptions();
        cnf.setStorageDownloadFilesBaseUrl(outputPath);
        cnf.setStorageUploadFilesBaseUrl(outputPath);
        cnf.setStorageDummyDirsForNewFile(2);
        // TODO: using LocalRdfStorage (while the code is) in RdfStorage. Move something?
        LocalRdfStorage rs = new LocalRdfStorage(domain.getDomainEntity(),
                new FakeQuestionMetadataRepository(),
                new QuestionMetadataManager(domain, new FakeQuestionMetadataRepository()));

        // Find files in local directory
        List<String> files = new ArrayList<>();
        try {
            files = listFullFilePathsInDir(sourcePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(files.size() + " parsed files to generate questions from");

        int qCountLimit = 1000;

        // treat leaf directory name of source path as questions' origin name
        String leafDir = Path.of(sourcePath).getFileName().toString();

        domain.generateManyQuestions(files, outputPath, qCountLimit, leafDir);
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
}
