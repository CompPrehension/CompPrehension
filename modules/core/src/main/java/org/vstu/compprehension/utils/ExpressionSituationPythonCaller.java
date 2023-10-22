package org.vstu.compprehension.utils;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;
import org.vstu.compprehension.common.ResourcesHelper;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
   @see <a href="https://stackoverflow.com/a/4115082/12824563">Answer on: how to both read and write to & from process through pipe (stdin/stdout)</a>
*/
@Log4j2
public class ExpressionSituationPythonCaller implements AutoCloseable {
    private BufferedReader inp = null;
    private BufferedWriter out = null;
    private Process process = null;

    static {
        try {
            getOrExtractPathToPyFolder(true);
        } catch (Exception e) {
            log.warn("Error locating Python3 script expr_operator_concepts.py: {}", e.getMessage(), e);
        }
    }

    public ExpressionSituationPythonCaller() {
        @Nullable String pathToPython;
        try {
            var extractedPath = getOrExtractPathToPyFolder(false);
            pathToPython = Path.of(extractedPath, "expr_operator_concepts.py").toString();
        } catch (Exception e) {
            log.warn("Error locating Python3 script expr_operator_concepts.py: {}", e.getMessage(), e);
            return;
        }

        String[] cmd = new String[] { "python", pathToPython, "--interactive" };
        process = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            close();
            log.warn("Error initializing Python3 sub-process. cmd: {}, ex: {}", cmd, e.getMessage(), e);
            return;
        }

        inp = new BufferedReader(new InputStreamReader(process.getInputStream()));
        out = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
    }

    private static String getOrExtractPathToPyFolder(boolean override) {
        var pathToResourcesFolder = "org/vstu/compprehension/models/businesslogic/domains/programming-language-expression-domain-model/generator";
        return ResourcesHelper.ensureFolderExtracted(ExpressionSituationPythonCaller.class, pathToResourcesFolder, "generator_py", override);
    }

    private @Nullable List<String> pipe(String msg, int expectedOutputLines) {
        List<String> ret;
        try {
            if (process == null) {
                return null;
            }

            ret = new ArrayList<>(expectedOutputLines);

            out.write(msg);
            out.write("\n");
            out.flush();
            for (int i = 0; i < expectedOutputLines; i++) {
                ret.add(inp.readLine());
            }
        }
        catch (IOException | NullPointerException ignored) {
            log.warn("cannot access/use python sub-process.");
            return null;
        }

        return ret;
    }

    public @Nullable List<String> invoke(String exprString) {
        return pipe(exprString, 2);
    }

    public void close() {
        try {
            if (process != null) {
                // empty input means "exit"
                pipe("", 0);
                process = null;
            }
            if (inp != null) {
                inp.close();
                inp = null;
            }
            if (out != null) {
                out.close();
                out = null;
            }
        }
        catch (IOException err) {
            log.warn("error closing python caller: {}", err.getMessage(), err);
        }
    }
}
