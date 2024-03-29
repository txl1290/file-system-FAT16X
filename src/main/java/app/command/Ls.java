package app.command;

import app.command.base.Base;
import fs.io.File;
import fs.io.FileInputStream;
import picocli.CommandLine;
import utils.InputParser;

import java.io.IOException;
import java.util.List;

@CommandLine.Command(name = "ls", mixinStandardHelpOptions = true, description = "show the files and dirs in a designated path")
public class Ls extends Base {

    @CommandLine.Parameters(arity = "0..1", description = "The path")
    private String path;

    @CommandLine.Option(names = "-l", description = "Show the details of the files and dirs")
    private boolean showDetails;

    @Override
    protected void executeCommand() throws IOException {
        path = InputParser.getAbsolutePath(path);

        File dir = new File(path, true, false);

        FileInputStream in = new FileInputStream(dir);
        String data = showFiles(in.listFiles());
        writeOut(data);
    }

    private String showFiles(List<File> files) {
        StringBuilder content = new StringBuilder();
        for (File file : files) {
            String colorName = file.getName();

            // 文件夹终端颜色
            if(!isRedirect() && file.isDirectory()) {
                colorName = "\033[34m" + file.getName() + "\033[0m";
            }
            String fileDetail = colorName;

            if(showDetails) {
                StringBuilder builder = new StringBuilder();
                if(file.isDirectory()) {
                    builder.append("d");
                } else {
                    builder.append("-");
                }
                if(file.isReadOnly()) {
                    builder.append("r-x");
                } else {
                    builder.append("rwx");
                }
                builder.append("\t").append(file.getHumanReadableFileSize()).append("\t").append(file.getLastWriteTime()).append("\t")
                        .append(colorName);
                fileDetail = builder.toString();
            }

            content.append(fileDetail).append("\r\n");
        }
        return content.toString();
    }
}
