package app.command;

import app.command.base.Base;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "echo", mixinStandardHelpOptions = true, description = "show the content in the terminal")
public class Echo extends Base {

    @CommandLine.Parameters(index = "0..*", description = "The content")
    private String[] contents;

    @Override
    protected void executeCommand() throws IOException {
        String content = String.join(" ", contents);
        writeOut(content);
    }

}
