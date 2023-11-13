package app.application;

import fs.io.File;
import fs.io.FileInputStream;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Scanner {

    private Set<String> apps = new HashSet<>();

    /**
     * 扫描所有安装的应用
     */
    public void scanApp() {
        //简化PATH，这里特定指的是bin目录下的文件
        File binDir = new File("/bin", true, false);
        FileInputStream inputStream = new FileInputStream(binDir);
        List<File> appFiles = inputStream.listFiles();
        for (File appFile : appFiles) {
            String appName = appFile.getName();
            apps.add(appName);
        }
    }

    public Set<String> getApps() {
        return apps;
    }

    public boolean isApp(String appName) {
        return apps.contains(appName);
    }

    public BaseApplication getApplication(String appName) {
        File appFile = new File("/bin/" + appName);
        FileInputStream inputStream = new FileInputStream(appFile);
        byte[] buff = new byte[1024];
        int len;
        StringBuilder content = new StringBuilder();
        try {
            while ((len = inputStream.read(buff)) != -1) {
                content.append(new String(buff, 0, len));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new JavaApplication(appName, content.toString().trim());
    }

}
