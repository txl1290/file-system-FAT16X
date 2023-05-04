package utils;

import java.io.IOException;
import java.io.OutputStream;

public class StreamUtil {

    public static void writeOutputStream(OutputStream out, String content) throws IOException {
        //把content中单独的\n替换成\r\n
        content = content.replaceAll("([^\r])\n", "$1\r\n");
        out.write(content.getBytes());
        out.flush();
    }
}
