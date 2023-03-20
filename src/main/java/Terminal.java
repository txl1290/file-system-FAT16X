import command.Ls;
import dirven.DiskDriven;
import picocli.CommandLine;
import utils.InputParser;

import java.util.Scanner;

class Terminal {

    public static void main(String... args) {
        Scanner scanner = new Scanner(System.in);
        String input;
        while (true) {
            System.out.print(DiskDriven.getDisk().getFs().getBootSector().getOemName() + "：");
            input = scanner.nextLine().trim();
            String command = InputParser.getCommand(input);
            try {
                if("ll".equalsIgnoreCase(command)) {
                    new CommandLine(new Ls()).execute(InputParser.getArgs(input + " -l"));
                } else if("exit".equalsIgnoreCase(command)) {
                    System.out.println("Bye!");
                    break;
                } else {
                    Class<?> clazz = Class.forName("command." + command);
                    if(clazz.isAnnotationPresent(CommandLine.Command.class)) {
                        CommandLine commandLine = new CommandLine(clazz.newInstance());
                        commandLine.execute(InputParser.getArgs(input));
                    }
                }
            } catch (ClassNotFoundException e) {
                System.out.println("command " + command + " not found : "
                        + "you can use 'cd', 'echo', 'ls', 'mkdir', 'pwd', 'touch', 'cat', 'll' command to operate the file system or use 'exit' command to exit the terminal");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
