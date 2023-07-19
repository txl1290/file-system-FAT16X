package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamUtil {

    public static void writeOutputStream(OutputStream out, String content) throws IOException {
        //把content中单独的\n替换成\r\n
        content = content.replaceAll("([^\r])\n", "$1\r\n");
        out.write(content.getBytes());
        out.flush();
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        out.flush();
    }
}
