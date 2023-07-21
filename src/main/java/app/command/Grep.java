package app.command;

import app.command.base.Base;
import fs.io.File;
import fs.io.FileInputStream;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@CommandLine.Command(name = "grep", mixinStandardHelpOptions = true, description = "find the file's content")
public class Grep extends Base {

    @CommandLine.Parameters(index = "0", description = "The content to find")
    private String find;

    @CommandLine.Parameters(arity = "0..1", description = "The file path")
    private String path;

    public Grep(String curDir) {
        super(curDir);
    }

    @Override
    protected void executeCommand() throws IOException {
        if(path != null) {
            in = new FileInputStream(new File(getAbsolutePath(path)));
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null) {
            if(line.contains(find)) {
                // 输出时把找到的内容用红色标记，可能会有多个
                out.write(line.replaceAll(find, "\033[31m" + find + "\033[0m").getBytes());
                out.write("\n".getBytes());
            }
        }
    }

}
