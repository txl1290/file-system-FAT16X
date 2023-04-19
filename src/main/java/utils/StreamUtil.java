package utils;

import java.io.IOException;
import java.io.OutputStream;

public class StreamUtil {

    public static void writeOutputStream(OutputStream out, String content) throws IOException {
        out.write(content.getBytes());
        out.flush();
    }
}
