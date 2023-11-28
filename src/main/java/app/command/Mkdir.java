package app.command;

import app.command.base.Base;
import fs.io.File;
import picocli.CommandLine;
import utils.InputParser;

@CommandLine.Command(name = "mkdir", mixinStandardHelpOptions = true, description = "make a directory")
public class Mkdir extends Base {

    @CommandLine.Parameters(index = "0", description = "The directory path")
    private String dirPath;

    @CommandLine.Option(names = "-p", description = "Create parent directories if they do not exist")
    private boolean createParent = false;

    @Override
    protected void executeCommand() {
        String absPath = InputParser.getAbsolutePath(dirPath);
        if(createParent) {
            createParent(absPath);
        } else {
            createDir(absPath);
        }
    }

    private void createDir(String path) {
        File dir = new File(path, true, false);
        if(createParent && dir.exist()) {
            return;
        }
        dir.mkdir();
    }

    private void createParent(String path) {
        if(InputParser.isRootPath(path)) {
            return;
        }

        String parentPath = InputParser.getFileParentPath(path);
        File parent = new File(parentPath, true, false);
        if(!parent.exist()) {
            createParent(parentPath);
        }
        createDir(path);
    }

}
