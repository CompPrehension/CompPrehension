package org.vstu.compprehension.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
   @see <a href="https://stackoverflow.com/a/4115082/12824563">Answer on: how to both read and write to & from process through pipe (stdin/stdout)</a>
*/
public class ExpressionSituationPythonCaller {
    public static BufferedReader inp = null;
    public static BufferedWriter out = null;
    public static Process process = null;

    public static boolean initSubProcess() {

        String pythonScript = "expr_operator_concepts.py";
        String resourcesDir = "modules/background-server/target/classes/";
        String ScriptFullPath;

        // ApplicationContext context = new ClassPathXmlApplicationContext();
        ApplicationContext context = new FileSystemXmlApplicationContext();
        Resource resource = context.getResource(resourcesDir + pythonScript);
        try {
            // absolute path to path starting with additional '/': /C:/data/...
            // System.out.println(ExpressionSituationPythonCaller.class.getClassLoader().getResource(pythonScript).getPath());
            // System.out.println(resource.getURI().getPath());

            // modules\background-server\target\classes\expr_operator_concepts.py
            // System.out.println(resource.getFile().getPath());
            ScriptFullPath = resource.getFile().getPath();
        } catch (IOException e) {
            System.out.println("Error locating Python3 script: " + pythonScript);
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        String cmd = "python \""+ScriptFullPath+"\" --interactive";

        process = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            close();
            System.out.println("Error initializing Python3 sub-process. cmd:" + cmd);
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        inp = new BufferedReader( new InputStreamReader(process.getInputStream()) );
        out = new BufferedWriter( new OutputStreamWriter(process.getOutputStream()) );

        return true;
    }

    public static List<String> pipe(String msg, int expectedOutputLines) {

        if (process == null) {
            initSubProcess();
        }

        List<String> ret = new ArrayList<>(expectedOutputLines);

        try {
            out.write( msg + "\n" );
            out.flush();
            for (int i = 0; i < expectedOutputLines; i++) {
                ret.add(inp.readLine());
            }
        }
        catch (IOException ignored) {
            close();
            return Collections.nCopies(expectedOutputLines, "");
        }

        return ret;
    }

    public static List<String> invoke(String exprString) {
            return pipe(exprString, 2);
    }

    public static void close() {

        if (process == null) {
            return;
        }

        try {
            // empty input means "exit"
            pipe("", 0);
            process = null;

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
            err.printStackTrace();
        }
    }
}
