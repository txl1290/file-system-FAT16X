import command.Cat;
import command.Cd;
import command.Echo;
import command.Format;
import command.Ls;
import command.Mkdir;
import command.Pwd;
import command.Touch;
import dirven.DiskDriven;
import picocli.CommandLine;
import utils.InputParser;

import java.util.Scanner;

class Terminal {

    public static void main(String... args) {
        Scanner scanner = new Scanner(System.in);
        String input;
        do {
            System.out.print(DiskDriven.getDisk().getFs().getBootSector().getOemName() + "：");
            input = scanner.nextLine();
            String command = InputParser.getCommand(input);
            if("format".equals(command)) {
                new CommandLine(new Format()).execute();
            } else if("cd".equals(command)) {
                new CommandLine(new Cd()).execute(InputParser.getArgs(input));
            } else if("echo".equals(command)) {
                new CommandLine(new Echo()).execute(InputParser.getArgs(input));
            } else if("ls".equals(command)) {
                new CommandLine(new Ls()).execute(InputParser.getArgs(input));
            } else if("mkdir".equals(command)) {
                new CommandLine(new Mkdir()).execute(InputParser.getArgs(input));
            } else if("pwd".equals(command)) {
                new CommandLine(new Pwd()).execute(InputParser.getArgs(input));
            } else if("touch".equals(command)) {
                new CommandLine(new Touch()).execute(InputParser.getArgs(input));
            } else if("cat".equals(command)) {
                new CommandLine(new Cat()).execute(InputParser.getArgs(input));
            } else if("ll".equals(command)) {
                input += " -l";
                new CommandLine(new Ls()).execute(InputParser.getArgs(input));
            } else {
                System.out.println("command " + command + " not found : "
                        + "you can use 'cd', 'echo', 'ls', 'mkdir', 'pwd', 'touch', 'cat', 'll' command to operate the file system");
            }
        } while (!"exit".equals(input));
    }
}
