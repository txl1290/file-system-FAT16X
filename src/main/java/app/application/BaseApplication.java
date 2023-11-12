package app.application;

import fs.io.File;
import fs.io.FileOutputStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import utils.InputParser;

import java.io.InputStream;
import java.io.OutputStream;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class BaseApplication implements Application {

    protected String name;

    protected String content;

    protected String[] args;

    protected InputStream in;

    protected OutputStream out;

    protected OutputStream err;

    protected String redirectPath;

    protected String redirectPathAppend;

    public BaseApplication(String name, String content) {
        this.name = name;
        this.content = content;
    }

    public String name() {
        return name;
    }

    public String content() {
        return content;
    }

    public void run() {
        // application的重定向部分可以和command重定向一起抽取成公共部分，考虑成本，这里先不做了
        if(redirectPath != null || redirectPathAppend != null) {
            redirect();
        } else {
            exec();
        }
    }

    protected void exec() {
        throw new UnsupportedOperationException();
    }

    private void redirect() {
        String path;
        boolean append = false;
        if(redirectPath != null) {
            path = redirectPath;
        } else {
            path = redirectPathAppend;
            append = true;
        }

        File file = new File(InputParser.getAbsolutePath(path));

        // 这里加了一个同步锁防止报重复创建文件的错误
        synchronized(File.fs) {
            if(!file.exist()) {
                file.create();
            }

            FileOutputStream outputStream = new FileOutputStream(file, append);

            setOut(outputStream);
            exec();
            outputStream.close();
        }
    }
}
