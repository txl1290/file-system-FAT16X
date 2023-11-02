package app.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Application {

    protected String name;

    protected String content;

    public String name() {
        return name;
    }

    public String content() {
        return content;
    }
}
