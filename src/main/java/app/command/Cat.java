package app.command;

import app.command.base.Base;
import fs.io.File;
import fs.io.FileInputStream;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "cat", mixinStandardHelpOptions = true, description = "show the file's content")
public class Cat extends Base {

    @CommandLine.Parameters(index = "0", description = "The file path")
    private String path;

    public Cat(String curDir) {
        super(curDir);
    }

    @Override
    protected void executeCommand() throws IOException {
        File file = new File(getAbsolutePath(path));
        in = new FileInputStream(file);
        byte[] data = new byte[file.getFileSize()];
        in.read(data);
        writeOut(new String(data));
    }
}
