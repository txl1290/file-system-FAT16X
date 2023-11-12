package app.command;

import app.command.base.Base;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "clear", mixinStandardHelpOptions = true, description = "clear the screen")
public class Clear extends Base {

    @Override
    protected void executeCommand() throws IOException {
        writeOut("\033[H\033[2J");
    }
}
