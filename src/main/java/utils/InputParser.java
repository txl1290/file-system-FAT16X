package utils;

import org.apache.commons.text.StringEscapeUtils;

import java.util.Arrays;
import java.util.Stack;

public class InputParser {

    public static String getCommand(String input) {
        String command = Arrays.stream(input.split("\\s+")).findFirst().get();
        if(command.isEmpty()) {
            return "";
        } else {
            //首字母大写
            return command.substring(0, 1).toUpperCase() + command.substring(1).toLowerCase();
        }
    }

    public static String[] getArgs(String input) {
        String[] inputArr = parseQuote(input);
        return Arrays.copyOfRange(inputArr, 1, inputArr.length);
    }

    public static String[] getFilePathArr(String path) {
        if(isAbsolutePath(path)) {
            path = path.substring(1);
        }
        if("".equals(path)) {
            return new String[0];
        } else {
            return path.split("/");
        }
    }

    public static boolean isAbsolutePath(String path) {
        return path.startsWith("/");
    }

    public static boolean isRootPath(String path) {
        return "/".equals(path);
    }

    public static String getFileParentPath(String path) {
        String[] pathArr = path.split("/");
        if(isAbsolutePath(path)) {
            return "/" + String.join("/", Arrays.copyOfRange(pathArr, 1, pathArr.length - 1));
        } else {
            return String.join("/", Arrays.copyOfRange(pathArr, 0, pathArr.length - 1));
        }
    }

    public static String getDirName(String path) {
        String[] pathArr = path.split("/");
        return pathArr[pathArr.length - 1];
    }

    public static String getFileName(String path) {
        String[] pathArr = path.split("/");
        return pathArr[pathArr.length - 1].split("\\.")[0];
    }

    public static String getFileExtension(String path) {
        String[] pathArr = path.split("/");
        String[] fileNameArr = pathArr[pathArr.length - 1].split("\\.");
        if(fileNameArr.length == 1) {
            return "";
        } else {
            return fileNameArr[fileNameArr.length - 1];
        }
    }

    /**
     * 格式化掉 ./ ../
     */
    public static String trimPath(String path) {
        String[] paths = InputParser.getFilePathArr(path);
        Stack<String> stack = new Stack<>();
        for (String s : paths) {
            if(".".equals(s) || "".equals(s)) {
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

    private static String[] parseQuote(String content) {
        // 找到单引号或双引号包起来的内容，谁成对先出现就用谁
        int singleStart = content.indexOf('\'');
        int singleEnd = content.lastIndexOf('\'');
        int doubleStart = content.indexOf('"');
        int doubleEnd = content.lastIndexOf('"');

        if(singleStart == singleEnd) {
            singleStart = -1;
            singleEnd = -1;
        }
        if(doubleStart == doubleEnd) {
            doubleStart = -1;
            doubleEnd = -1;
        }

        int start = -1;
        int end = -1;
        if(singleStart != -1 && doubleStart != -1) {
            if(singleStart < doubleStart) {
                start = singleStart;
                end = singleEnd;
            } else {
                start = doubleStart;
                end = doubleEnd;
            }
        } else if(singleStart != -1) {
            start = singleStart;
            end = singleEnd;
        } else if(doubleStart != -1) {
            start = doubleStart;
            end = doubleEnd;
        }

        // 如果有单引号或双引号包起来的内容，就把它们转义
        if(start != -1 && end != -1) {
            String content1 = content.substring(0, start);
            String content2 = content.substring(start, end + 1);
            String content3 = content.substring(end + 1);
            String unescape = unescape(content2);
            String[] splits1 = content1.split("\\s+");
            String[] splits3 = content3.split("\\s+");
            if(!content1.endsWith(" ")) {
                unescape = splits1[splits1.length - 1] + unescape;
                splits1 = Arrays.copyOfRange(splits1, 0, splits1.length - 1);
            }

            unescape = unescape + splits3[0];
            splits3 = Arrays.copyOfRange(splits3, 1, splits3.length);

            return concat(splits1, new String[] { unescape }, splits3);
        }
        return content.split("\\s+");
    }

    private static String[] concat(String[] splits1, String[] strings, String[] splits3) {
        String[] result = new String[splits1.length + strings.length + splits3.length];
        System.arraycopy(splits1, 0, result, 0, splits1.length);
        System.arraycopy(strings, 0, result, splits1.length, strings.length);
        System.arraycopy(splits3, 0, result, splits1.length + strings.length, splits3.length);
        return result;
    }

    private static String unescape(String content) {
        if(content.startsWith("'") && content.endsWith("'")) {
            return content.substring(1, content.length() - 1);
        } else if(content.startsWith("\"") && content.endsWith("\"")) {
            return StringEscapeUtils.unescapeJava(content.substring(1, content.length() - 1));
        } else {
            return content;
        }
    }
}
