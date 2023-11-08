import app.application.compiler.InnerJavaCompiler;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class CompilerTest {

    @Test
    public void test() {
        InnerJavaCompiler compiler = new InnerJavaCompiler();
        try {
            compiler.compile("hello", "public class hello { public static void main(String[] args) { System.out.println(\"hello world\"); } }");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
