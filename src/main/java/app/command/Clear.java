package app.command;

import app.command.base.Base;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@CommandLine.Command(name = "clear", mixinStandardHelpOptions = true, description = "clear the screen")
public class Clear extends Base {

    public Clear(String curDir) {
        super(curDir);
    }

    @Override
    protected void executeCommand() throws IOException {
        out.write("\033[H\033[2J".getBytes(StandardCharsets.UTF_8));
    }
}
