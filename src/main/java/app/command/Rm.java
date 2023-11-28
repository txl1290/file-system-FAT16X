package app.command;

import app.command.base.Base;
import fs.io.File;
import picocli.CommandLine;
import utils.InputParser;

@CommandLine.Command(name = "rm", mixinStandardHelpOptions = true, description = "remove a file or directory")
public class Rm extends Base {

    @CommandLine.Parameters(index = "0", description = "The file path")
    private String path;

    @Override
    protected void executeCommand() {
        File file = new File(InputParser.getAbsolutePath(path));

        file.remove();
    }

}
