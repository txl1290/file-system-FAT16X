import app.application.JavaApplication;
import app.application.compiler.InnerJavaCompiler;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class CompilerTest {

    @Test
    public void test() {
        InnerJavaCompiler compiler = new InnerJavaCompiler();
        JavaApplication app = JavaApplication.builder()
                .name("hello")
                .content("import java.io.ByteArrayOutputStream;\n"
                        + "import java.io.IOException;"
                        + "import java.io.InputStream;"
                        + " public class hello { public ByteArrayOutputStream run(InputStream in, String[] args) "
                        + "{ ByteArrayOutputStream out = new ByteArrayOutputStream();\n"
                        + "        try {\n"
                        + "            out.write(\"hello world\".getBytes());\n"
                        + "        } catch (IOException e) {\n"
                        + "            throw new RuntimeException(e);\n"
                        + "        }\n"
                        + "        return out; } }")
                .build();
        app.setOut(System.out);
        try {
            compiler.compile(app);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException | InstantiationException |
                 IOException e) {
            throw new RuntimeException(e);
        }
    }
}
