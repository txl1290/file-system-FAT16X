package app.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.io.OutputStream;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Application {

    protected String name;

    protected String content;

    protected InputStream in;

    protected OutputStream out;

    protected OutputStream err;

    public Application(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String name() {
        return name;
    }

    public String content() {
        return content;
    }
}
