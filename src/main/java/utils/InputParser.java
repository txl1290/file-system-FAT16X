package utils;

import java.util.Arrays;
import java.util.Stack;

public class InputParser {

    public static String getCommand(String input) {
        String command = Arrays.stream(input.split("\\s+")).findFirst().get();
        //首字母大写
        return command.substring(0, 1).toUpperCase() + command.substring(1).toLowerCase();
    }

    public static String[] getArgs(String input) {
        String[] inputArr = input.split("\\s+");
        return Arrays.copyOfRange(inputArr, 1, inputArr.length);
    }

    public static String[] getFilePathArr(String path) {
        if(isAbsolutePath(path)) {
            path = path.substring(1);
        }
        if(path.equals("")) {
            return new String[0];
        } else {
            return path.split("/");
        }
    }

    public static boolean isAbsolutePath(String path) {
        return path.startsWith("/");
    }

    public static String getFileParentPath(String path) {
        String[] pathArr = path.split("/");
        if(isAbsolutePath(path)) {
            return "/" + String.join("/", Arrays.copyOfRange(pathArr, 1, pathArr.length - 1));
        } else {
            return String.join("/", Arrays.copyOfRange(pathArr, 0, pathArr.length - 1));
        }
    }

    public static String getFileName(String path) {
        String[] pathArr = path.split("/");
        return pathArr[pathArr.length - 1].split("\\.")[0];
    }

    public static String getFileExtension(String path) {
        String[] fileNameArr = path.split("\\.");
        if(fileNameArr.length == 1) {
            return "";
        }
        return fileNameArr[fileNameArr.length - 1];
    }

    public static boolean isRoot(String path) {
        return "/".equals(path);
    }

    /**
     * 格式化掉 ./ ../
     */
    public static String trimPath(String path) {
        String[] paths = InputParser.getFilePathArr(path);
        Stack<String> stack = new Stack<>();
        for (String s : paths) {
            if(".".equals(s)) {
                continue;
            } else if("..".equals(s)) {
                if(!stack.isEmpty()) {
                    stack.pop();
                }
            } else {
                stack.push(s);
            }
        }
        if(isAbsolutePath(path)) {
            return "/" + String.join("/", stack);
        } else {
            return String.join("/", stack);
        }
    }
}
