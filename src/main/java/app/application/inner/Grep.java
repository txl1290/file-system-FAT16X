package app.application.inner;

import app.application.JavaApplication;

public class Grep extends JavaApplication {

    private static final String DEFAULT_CONTENT = "grep";

    public Grep() {
    }

    public Grep(String name, String content) {
        super(name, content);
    }

    @Override
    public String name() {
        return "grep";
    }

    @Override
    public String content() {
        return content != null ? content : DEFAULT_CONTENT;
    }

}
