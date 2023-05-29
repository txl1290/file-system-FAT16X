package app.command;

import app.command.base.Base;
import org.apache.commons.text.StringEscapeUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@CommandLine.Command(name = "echo", mixinStandardHelpOptions = true, description = "show the content in the terminal")
public class Echo extends Base {

    @CommandLine.Parameters(index = "0..*", description = "The content")
    private String[] contents;

    public Echo(String curDir) {
        super(curDir);
    }

    @Override
    protected void executeCommand() throws IOException {
        String content = String.join(" ", contents);
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
            content = content1 + unescape(content2) + content3;
        }

        out.write(content.getBytes(StandardCharsets.UTF_8));
    }

    private String unescape(String content) {
        if(content.startsWith("'") && content.endsWith("'")) {
            return content.substring(1, content.length() - 1);
        } else if(content.startsWith("\"") && content.endsWith("\"")) {
            return StringEscapeUtils.unescapeJava(content.substring(1, content.length() - 1));
        } else {
            return content;
        }
    }
}
