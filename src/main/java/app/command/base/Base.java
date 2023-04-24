package app.command.base;

import fs.io.File;
import fs.io.FileOutputStream;
import picocli.CommandLine;
import utils.InputParser;

public class Base implements Runnable {

    @CommandLine.Option(names = ">", description = "The redirect path")
    protected String redirectPath;

    @CommandLine.Option(names = ">>", description = "The redirect path append")
    protected String redirectPathAppend;

    protected String out = "";

    protected String err = "";

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
            err = this.getClass().getSimpleName().toLowerCase() + ": " + e.getMessage() + "\n";
            out = "";
        }
    }

    public String getOut() {
        return out;
    }

    public String getErr() {
        return err;
    }

    protected void executeCommand() {
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

    /**
     * 处理无空格的重定向
     */
    protected String handleRedirect(String path) {
        if(path.contains(">>")) {
            String[] pathArr = path.split(">>");
            redirectPathAppend = pathArr[1].trim();
            path = pathArr[0].trim();
        } else if(path.contains(">")) {
            String[] pathArr = path.split(">");
            redirectPath = pathArr[1].trim();
            path = pathArr[0].trim();
        }
        return path;
    }

    private void redirect() {
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

            outputStream.write(out);
            outputStream.close();
        }

        out = "";
    }
}
