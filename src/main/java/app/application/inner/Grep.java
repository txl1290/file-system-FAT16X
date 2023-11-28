package app.application.inner;

import app.application.JavaApplication;

public class Grep extends JavaApplication {

    private static final String DEFAULT_CONTENT = "import fs.io.File;\n"
            + "import fs.io.FileInputStream;\n"
            + "import utils.InputParser;\n"
            + "\n"
            + "import java.io.BufferedReader;\n"
            + "import java.io.ByteArrayOutputStream;\n"
            + "import java.io.IOException;\n"
            + "import java.io.InputStream;\n"
            + "import java.io.InputStreamReader;\n"
            + "import java.nio.charset.StandardCharsets;\n"
            + "public class grep {\n"
            + "    public ByteArrayOutputStream run(InputStream in, String[] args) {\n"
            + "        String find = args[0];\n"
            + "        String path = null;\n"
            + "        if(args.length > 1) {\n"
            + "            path = args[1];\n"
            + "        }\n"
            + "        BufferedReader reader = new BufferedReader(new InputStreamReader(in));\n"
            + "        if(path != null) {\n"
            + "            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(InputParser.getAbsolutePath(path)))));\n"
            + "        } "
            + "        String line;\n"
            + "        ByteArrayOutputStream out = new ByteArrayOutputStream();\n"
            + "        while (true) {\n"
            + "            try {\n"
            + "                if(!((line = reader.readLine()) != null)) {\n"
            + "                    break;\n"
            + "                }\n"
            + "                line = line.trim();\n"
            + "                if(line.contains(find)) {\n"
            + "                    out.write(line.getBytes(StandardCharsets.UTF_8));\n"
            + "                    out.write(\"\\n\".getBytes(StandardCharsets.UTF_8));\n"
            + "                }\n"
            + "            } catch (IOException e) {\n"
            + "                throw new RuntimeException(e);\n"
            + "            }\n"
            + "        }\n"
            + "        return out;\n"
            + "    }\n"
            + "}";

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
