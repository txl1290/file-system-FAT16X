package app.command;

import app.command.base.Base;
import fs.io.File;
import fs.io.FileInputStream;
import picocli.CommandLine;

@CommandLine.Command(name = "cat", mixinStandardHelpOptions = true, description = "show the file's content")
public class Cat extends Base {

    @CommandLine.Parameters(index = "0", description = "The file path")
    private String path;

    public Cat(String curDir) {
        super(curDir);
    }

    @Override
    protected void executeCommand() {
        // 处理无空格的重定向
        path = handleRedirect(path);

        File file = new File(getAbsolutePath(path));
        FileInputStream in = new FileInputStream(file);
        String data = in.read();
        in.close();
        out = data;
    }
}
