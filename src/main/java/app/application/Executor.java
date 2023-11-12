package app.application;

import utils.InputParser;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 应用执行器
 */
public class Executor {

    public void execute(BaseApplication app, String[] args, OutputStream out, InputStream in) {
        app.setIn(in);
        app.setOut(out);
        app.setErr(out);
        // 解析重定向
        InputParser.RedirectParam redirectParam = InputParser.parseRedirect(args);
        app.setArgs(redirectParam.getArgs());
        app.setRedirectPath(redirectParam.getRedirectPath());
        app.setRedirectPathAppend(redirectParam.getRedirectPathAppend());
        app.run();
    }
}
