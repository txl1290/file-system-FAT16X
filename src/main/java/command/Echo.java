package command;

import command.base.Base;
import picocli.CommandLine;

@CommandLine.Command(name = "echo", mixinStandardHelpOptions = true, description = "show the content in the terminal")
public class Echo extends Base {

    @CommandLine.Parameters(index = "0..*", description = "The content")
    private String[] contents;

    @Override
    public String executeCommand() {
        return String.join(" ", contents);
    }
}
