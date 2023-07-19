package app.command;

import app.command.base.Base;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@CommandLine.Command(name = "echo", mixinStandardHelpOptions = true, description = "show the content in the terminal")
public class Echo extends Base {

    @CommandLine.Parameters(index = "0..*", description = "The content")
    private String[] contents;

    public Echo(String curDir) {
        super(curDir);
    }

    @Override
    protected void executeCommand() throws IOException {
        String content = String.join(" ", contents);
        out.write(content.getBytes(StandardCharsets.UTF_8));
    }

}
