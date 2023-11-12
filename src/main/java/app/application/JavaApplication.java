package app.application;

import app.application.compiler.InnerJavaCompiler;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

@SuperBuilder
public class JavaApplication extends BaseApplication {

    private static InnerJavaCompiler compiler = new InnerJavaCompiler();

    public JavaApplication() {
    }

    public JavaApplication(String name, String content) {
        super(name, content);
    }

    @Override
    protected void exec() {
        try {
            compiler.compile(this);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException | IOException |
                 InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
