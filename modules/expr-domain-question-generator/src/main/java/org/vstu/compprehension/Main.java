package org.vstu.compprehension;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.vstu.compprehension.models.businesslogic.storage.RdfStorage;

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
        main.run();
    }

    @Parameter(names={"--source", "-s"}, description = "Path to directory with ttl", required = true)
    String sourcePath;
    @Parameter(names={"--sourceId", "-id"}, description = "Import id", required = true)
    String sourceId;
    @Parameter(names={"--output", "-o"}, description = "Path to output directory", required = true)
    String outputPath;

    public void run() {
        RdfStorage.generateQuestionsForExpressionsDomain(
                sourcePath,
                outputPath,
                2,
                sourceId);
    }
}
