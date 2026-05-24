package org.vstu.compprehension.models.businesslogic.domains;

import its.model.DomainSolvingModel;
import org.jetbrains.annotations.NotNull;
import org.vstu.compprehension.models.businesslogic.Question;
import org.vstu.compprehension.models.businesslogic.Tag;
import org.vstu.compprehension.models.businesslogic.backend.DecisionTreeReasonerBackend;
import org.vstu.compprehension.models.entities.DomainEntity;
import org.vstu.compprehension.models.entities.ResponseEntity;
import org.vstu.compprehension.utils.RandomProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

public abstract class DecisionTreeReasoningDomain extends DomainBase {
    protected DecisionTreeReasoningDomain(DomainEntity domainEntity, RandomProvider randomProvider) {
        super(domainEntity, randomProvider);
    }

    public abstract List<DomainSolvingModel> getDomainSolvingModels();
    
    public abstract DecisionTreeReasonerBackend.Interface getBackendInterface();

    @NotNull
    public String getBackendId() {
        return DecisionTreeReasonerBackend.BACKEND_ID;
    }

    public Question solveQuestion(Question question, List<Tag> tags) {
        var backend = new DecisionTreeReasonerBackend();
        var backendInterface = getBackendInterface();
        backendInterface.updateQuestionAfterSolve(
            question,
            backend.solve(backendInterface.prepareBackendInfoForSolve(question, tags))
        );
        return question;
    }

    public InterpretSentenceResult judgeQuestion(Question question, List<ResponseEntity> responses, List<Tag> tags) {
        var backend = new DecisionTreeReasonerBackend();
        var backendInterface = getBackendInterface();
        var input = backendInterface.prepareBackendInfoForJudge(question, responses, tags);

        Integer repeatsOpt = readRepeatCountFromDebugFile();
        if (repeatsOpt == null) {
            var output = backend.judge(input);
            return backendInterface.interpretJudgeOutput(question, output);
        }

        int repeats = repeatsOpt;
        DecisionTreeReasonerBackend.Output lastOutput = null;
        for (int i = 0; i < repeats; i++) {
            long t0 = System.nanoTime();
            lastOutput = backend.judge(input);
            long t1 = System.nanoTime();
            appendTimingLogLine(t1 - t0, i + 1, repeats, question);
        }
        return backendInterface.interpretJudgeOutput(question, lastOutput);
    }

    private static Integer readRepeatCountFromDebugFile() {
        Path debugPath = Paths.get("cmppr_debug_run.txt");
        if (!Files.exists(debugPath)) {
            return null;
        }
        try {
            var lines = Files.readAllLines(debugPath);
            String first = lines.isEmpty() ? null : lines.get(0);
            if (first == null) {
                return null;
            }
            int n = Integer.parseInt(first.trim());
            return n > 0 ? n : null;
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }

    private static void appendTimingLogLine(long elapsedNanos, int runIndex, int totalRuns, Question question) {
        Path logPath = Paths.get("cmppr_runs_loqi.txt");
        double elapsedMs = elapsedNanos / 1_000_000.0;
        long epochMillis = System.currentTimeMillis();
        String questionId = Objects.requireNonNull(question.getMetadata()).getId() != null ? Objects.requireNonNull(question.getMetadata()).getId().toString() : "null";
        String line = String.format(
                "%d\trepeat=%d/%d\telapsed_ms=%.3f\tquestionId=%s%n",
                epochMillis, runIndex, totalRuns, elapsedMs, questionId
        );
        try (var writer = Files.newBufferedWriter(
                logPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(line);
        } catch (IOException e) {
            // Ignore logging errors in timing helper
        }
    }
}
