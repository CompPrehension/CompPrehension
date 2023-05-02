package org.vstu.compprehension;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import lombok.val;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.vstu.compprehension.adapters.*;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.backend.JenaBackend;
import org.vstu.compprehension.models.businesslogic.domains.ProgrammingLanguageExpressionDomain;
import org.vstu.compprehension.models.businesslogic.storage.GraphRole;
import org.vstu.compprehension.models.businesslogic.storage.LocalRdfStorage;
import org.vstu.compprehension.models.businesslogic.storage.QuestionMetadataManager;
import org.vstu.compprehension.models.entities.BackendFactEntity;
import org.vstu.compprehension.models.entities.DomainOptionsEntity;
import org.vstu.compprehension.utils.ExpressionSituationPythonCaller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.vstu.compprehension.models.businesslogic.storage.AbstractRdfStorage.NS_code;

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
                new FakeQuestionMetadataRepository(),
                new FakeQuestionRequestLogRepository()
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
        int path_len = sourcePath.length();
        int count = 0;
        int qcount = 0;
        int qcountLimit = 35;

        for (String file : files) {
            try {
                String name = file.substring(path_len);  // cut directory path

                if (name.endsWith(".ttl")) {
                    name = name.substring(0, name.length() - ".ttl".length());
                    name = name.replaceAll("[^a-zA-Z0-9_=+-]", "");
                } else {
                    continue; //skip all other files
                }

                if (rs.getQuestionStatus(name) == GraphRole.QUESTION_TEMPLATE_SOLVED) {
                    System.out.println("Skip solved template: " + name);
                    continue;
                }
                // System.out.println(name + " ...\t");

                count++;
                if (qcount > qcountLimit) break;
//                if (count % 100 == 0) {
//                    rs.saveToFilesystem();
//                    System.out.println("Dump metadata on disk");
//                }

                // Create template and save it and metadata
                System.out.println(name + " \tUpload model number " + count);
                /*rs.createQuestionTemplate(name);*/
                Model m = ModelFactory.createDefaultModel();
                RDFDataMgr.read(m, file);
                val templateMeta = rs.setQuestionSubgraph(domain, name, GraphRole.QUESTION_TEMPLATE, m);
                // set more info to the metadata
                templateMeta.setOrigin(sourceId);
                rs.saveMetadataDraftEntity(templateMeta);

                // Create solved template and save it and metadata
                rs.solveQuestion(domain, name, GraphRole.QUESTION_TEMPLATE_SOLVED);

                System.out.println("Creating questions for template: " + name);
                Model solvedTemplateModel = rs.getQuestionModel(name, GraphRole.QUESTION_TEMPLATE_SOLVED);
                Set<Set<String>> possibleViolations = new HashSet<>();
                for (Map.Entry<String, Model> question : domain.generateDistinctQuestions(name, solvedTemplateModel, ModelFactory.createDefaultModel(), 12).entrySet()) {
                    qcount++;
                    if (qcount >= qcountLimit) break;
                    // Create question model (with positive laws)
                    Model questionInitModel = rs.getQuestionModel(name, GraphRole.getPrevious(GraphRole.QUESTION)).add(question.getValue());
                    Model questionModel = rs.solveTemplate(domain, questionInitModel, GraphRole.QUESTION, true);
                    questionModel.add(question.getValue());
                    // Find potential errors
                    Model solvedQuestionModel = rs.solveTemplate(domain, questionInitModel.add(questionModel), GraphRole.QUESTION_SOLVED, true);

                    // Generate only questions with different error sets
                    List<BackendFactEntity> facts = JenaBackend.modelToFacts(solvedQuestionModel, NS_code.get());
                    Set<String> violations = domain.possibleViolations(facts, null);
                    if (possibleViolations.contains(violations)) {
                        System.out.println("Skip question with same violations: " + question.getKey());
                        continue;
                    }
                    possibleViolations.add(violations);

                    // (note! names of template and question must differ)
                    String questionName = question.getKey();
                    if (questionName.equals(name)) {
                        // guard for the case when the name was not changed
                        questionName += "_v";
                    }
                    // create metadata entry
                    rs.createQuestion(domain, questionName, name, false);
                    // set basic data of the question
                    rs.setQuestionSubgraph(domain, questionName, GraphRole.QUESTION, questionModel);
                    // set solved data of the question
                    rs.setQuestionSubgraph(domain, questionName, GraphRole.QUESTION_SOLVED, solvedQuestionModel);

                    // Save question data for domain in JSON
                    System.out.println("Generating question: " + questionName);
                    Question domainQuestion = domain.createQuestionFromModel(questionName, rs.getQuestionModel(questionName, GraphRole.QUESTION_SOLVED), rs);

                    if (domainQuestion == null) {
                        System.out.println("--  Cancelled inappropriate question: " + questionName);
                        // don't complete this question, generation aborted
                        rs.deleteQuestion(questionName);
                        continue;
                    }

                    // Save question data for domain in JSON
                    System.out.println("++  Saving question: " + questionName);
                    String filename = rs.saveQuestionData(questionName, domain.questionToJson(domainQuestion));
                    // save metadata row
                    var metaDraft = rs.findQuestionByName(questionName);
                    metaDraft.setQDataGraphPath(filename);
                    rs.saveMetadataDraftEntity(metaDraft);
                    // save data to question's metadata instance, too
                    val meta = domainQuestion.getQuestionData().getOptions().getMetadata();
                    meta.setQDataGraphPath(filename);
                }
            } catch (Exception e) {
                e.printStackTrace();
                rs.saveToFilesystem();
            }
        }
        ExpressionSituationPythonCaller.close();
        rs.saveToFilesystem();
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
