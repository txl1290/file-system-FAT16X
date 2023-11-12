package app.command.base;

import fs.io.File;
import fs.io.FileOutputStream;
import picocli.CommandLine;
import utils.InputParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Base implements Runnable {

    @CommandLine.Option(names = ">", description = "The redirect path")
    protected String redirectPath;

    @CommandLine.Option(names = ">>", description = "The redirect path append")
    protected String redirectPathAppend;

    protected InputStream in;

    protected OutputStream out;

    protected OutputStream err;

    @Override
    public void run() {
        try {
            if(redirectPath != null || redirectPathAppend != null) {
                redirect();
            } else {
                executeCommand();
            }
        } catch (Exception e) {
            String errMsg = this.getClass().getSimpleName().toLowerCase() + ": " + e.getMessage() + "\n";
            try {
                err.write(errMsg.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void setIn(InputStream in) {
        this.in = in;
    }

    public void setOut(OutputStream out) {
        this.out = out;
    }

    public void setErr(OutputStream out) {
        this.err = out;
    }

    public void setRedirectPath(String redirectPath) {
        this.redirectPath = redirectPath;
    }

    public OutputStream getOut() {
        return out;
    }

    public OutputStream getErr() {
        return err;
    }

    protected void executeCommand() throws IOException {
    }

    public void redirect() throws IOException {
        String path;
        boolean append = false;
        if(redirectPath != null) {
            path = redirectPath;
        } else {
            path = redirectPathAppend;
            append = true;
        }

        File file = new File(InputParser.getAbsolutePath(path));

        // 这里加了一个同步锁防止报重复创建文件的错误
        synchronized(File.fs) {
            if(!file.exist()) {
                file.create();
            }

            FileOutputStream outputStream = new FileOutputStream(file, append);

            setOut(outputStream);
            executeCommand();
            outputStream.close();
        }
    }

    public boolean isRedirect() {
        return redirectPath != null || redirectPathAppend != null;
    }

    public void writeOut(String data) throws IOException {
        if(!data.startsWith("\033") && !data.endsWith("\n") && !data.isEmpty() && !isRedirect()) {
            data += "\n";
        }
        out.write(data.getBytes(StandardCharsets.UTF_8));
    }
}
