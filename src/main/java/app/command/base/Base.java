package app.command.base;

import fs.io.File;
import fs.io.FileOutputStream;
import picocli.CommandLine;
import utils.InputParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Base implements Runnable {

    @CommandLine.Option(names = ">", description = "The redirect path")
    protected String redirectPath;

    @CommandLine.Option(names = ">>", description = "The redirect path append")
    protected String redirectPathAppend;

    protected InputStream in;

    protected ByteArrayOutputStream out = new ByteArrayOutputStream();

    protected ByteArrayOutputStream err = new ByteArrayOutputStream();

    private String curDir;

    public Base(String curDir) {
        this.curDir = curDir;
    }

    @Override
    public void run() {
        try {
            executeCommand();
            if(redirectPath != null || redirectPathAppend != null) {
                redirect();
            }
        } catch (Exception e) {
            String errMsg = this.getClass().getSimpleName().toLowerCase() + ": " + e.getMessage();
            try {
                err.write(errMsg.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            out.reset();
        }
    }

    public void setIn(InputStream in) {
        this.in = in;
    }

    public void setOut(ByteArrayOutputStream out) {
        this.out = out;
    }

    public void setRedirectPath(String redirectPath) {
        this.redirectPath = redirectPath;
    }

    public ByteArrayOutputStream getOut() {
        return out;
    }

    public ByteArrayOutputStream getErr() {
        return err;
    }

    protected void executeCommand() throws IOException {
    }

    protected String getAbsolutePath(String path) {
        if(path == null) {
            return curDir;
        } else if(InputParser.isAbsolutePath(path)) {
            return InputParser.trimPath(path);
        } else {
            return InputParser.trimPath(curDir + "/" + path);
        }
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

        File file = new File(getAbsolutePath(path));

        // 这里加了一个同步锁防止报重复创建文件的错误
        synchronized(File.fs) {
            if(!file.exist()) {
                file.create();
            }

            FileOutputStream outputStream = new FileOutputStream(file, append);

            out.writeTo(outputStream);
            outputStream.close();
        }

        out.reset();
    }

    public boolean isRedirect() {
        return redirectPath != null || redirectPathAppend != null;
    }
}
