package command;

import dirven.DiskDriven;
import picocli.CommandLine;

import java.nio.charset.StandardCharsets;

@CommandLine.Command(name = "echo", mixinStandardHelpOptions = true, description = "show the content in the terminal")
public class Echo implements Runnable {

    @CommandLine.Parameters(index = "0..*", description = "The content")
    private String[] contents;

    @CommandLine.Option(names = ">", description = "The redirect path")
    private String redirectPath;

    @Override
    public void run() {
        try {
            String content = String.join(" ", contents);
            if(redirectPath == null) {
                System.out.println(content);
            } else {
                DiskDriven.writeFileContent(redirectPath, content.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            System.out.println("echo: " + e.getMessage());
        }
    }
}
