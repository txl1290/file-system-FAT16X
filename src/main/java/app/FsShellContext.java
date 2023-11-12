package app;

import fs.io.File;

public class FsShellContext {

    static ThreadLocal<File> currentPath = new ThreadLocal<>();

    public static File getCurrentPath() {
        return currentPath.get();
    }

    public static void setCurrentPath(File path) {
        currentPath.set(path);
    }
}
