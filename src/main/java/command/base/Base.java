package command.base;

import dirven.DiskDriven;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;

public class Base implements Runnable {

    @CommandLine.Option(names = ">", description = "The redirect path")
    protected String redirectPath;

    @CommandLine.Option(names = ">>", description = "The redirect path append")
    protected String redirectPathAppend;

    @Override
    public void run() {
        try {
            // get absolute path，防止cd切换目录后，重定向路径不正确
            if(redirectPath != null) {
                redirectPath = DiskDriven.getAbsolutePath(redirectPath);
            } else if(redirectPathAppend != null) {
                redirectPathAppend = DiskDriven.getAbsolutePath(redirectPathAppend);
            }
            
            String content = executeCommand();
            writeRedirect(content);
        } catch (Exception e) {
            System.out.println(this.getClass().getSimpleName().toLowerCase() + ": " + e.getMessage());
        }
    }

    protected String executeCommand() {
        return "";
    }

    protected void writeRedirect(String content) {
        if(redirectPath == null && redirectPathAppend == null) {
            if(content != null && !content.isEmpty()) {
                System.out.println(content);
            }
        } else {
            if(redirectPath != null) {
                DiskDriven.writeFileContent(redirectPath, content.getBytes(StandardCharsets.UTF_8));
            } else {
                DiskDriven.writeFileContentAppend(redirectPathAppend, content.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}
