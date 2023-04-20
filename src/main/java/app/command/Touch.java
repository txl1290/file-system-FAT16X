package app.command;

import app.command.base.Base;
import fs.io.File;
import picocli.CommandLine;

@CommandLine.Command(name = "touch", mixinStandardHelpOptions = true, description = "create a file")
public class Touch extends Base {

    @CommandLine.Parameters(index = "0", description = "The file path")
    private String path;

    public Touch(String curDir) {
        super(curDir);
    }

    @Override
    protected void executeCommand() {
        File file = new File(getAbsolutePath(path));

        file.create();
    }
}
