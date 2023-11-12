package app.application;

import fs.fat.FatFileSystem;
import fs.io.File;
import fs.io.FileOutputStream;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用安装器
 */
public class Installer {

    List<BaseApplication> applications = new ArrayList<>();

    public Installer() {
        init();
    }

    public void init() {
        File dir = new File(FatFileSystem.APP_PATH, true, false);
        if(!dir.exist()) {
            dir.mkdir();
        }
    }

    public void install() {
        applications.forEach(this::install);
        reset();
    }

    public void addApplication(BaseApplication app) {
        applications.add(app);
    }

    public void reset() {
        applications.clear();
    }

    /**
     * 安装内置应用
     */
    public void installInnerApp() {
        // 加载app/application/inner内的应用
        String path = "app/application/inner";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resourceUrl = classLoader.getResource(path);
        java.io.File[] innerAppFiles = new java.io.File(resourceUrl.getFile()).listFiles();
        for (java.io.File file : innerAppFiles) {
            String name = file.getName();
            try {
                Class<?> clazz = Class.forName("app.application.inner." + name.substring(0, name.lastIndexOf(".")));
                BaseApplication app = (BaseApplication) clazz.newInstance();
                addApplication(app);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("load inner app error", e);
            }
        }
        install();
    }

    private void install(BaseApplication app) {
        String name = app.name();
        String content = app.content();
        // 把应用写到磁盘的bin目录下
        File file = new File(FatFileSystem.APP_PATH + "/" + name);
        if(!file.exist()) {
            file.create();

            FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(content.getBytes());
            } catch (IOException e) {
                throw new IllegalStateException("install app error", e);
            }
            out.close();
        }
    }

}
