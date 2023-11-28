package app.application.inner;

import app.application.JavaApplication;

public class Echo extends JavaApplication {

    private static final String DEFAULT_CONTENT = "import java.io.ByteArrayOutputStream;\n"
            + "import java.io.IOException;\n"
            + "import java.io.InputStream;\n"
            + "public class echo { \n"
            + "\tpublic ByteArrayOutputStream run(InputStream in, String[] args) { \n"
            + "\t\tString content = String.join(\" \", args);\n"
            + "\t\tByteArrayOutputStream out = new ByteArrayOutputStream();\n"
            + "\t\ttry {\n"
            + "\t\t\tout.write(content.getBytes());\n"
            + "\t\t\treturn out;\n"
            + "\t\t} catch (IOException e) {\n"
            + "\t\t\tthrow new RuntimeException(e);\n"
            + "\t\t}\n"
            + "\t}\n"
            + "}";

    public Echo() {
    }

    public Echo(String name, String content) {
        super(name, content);
    }

    @Override
    public String name() {
        return "echo";
    }

    @Override
    public String content() {
        return content != null ? content : DEFAULT_CONTENT;
    }
}
