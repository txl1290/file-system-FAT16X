package app.application.compiler;

import app.application.JavaApplication;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class InnerJavaCompiler {

    private JavaCompiler compiler;

    private JavaFileManager fileManager;

    private DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

    public InnerJavaCompiler() {
        compiler = ToolProvider.getSystemJavaCompiler();
        // 创建一个内存中的虚拟文件系统
        fileManager = new InMemoryFileManager(compiler.getStandardFileManager(diagnostics, null, null));
    }

    public void compile(JavaApplication app)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, ClassNotFoundException, InstantiationException,
            IOException {

        // 创建一个Java文件对象，用于包装代码字符串
        JavaFileObject fileObject = new DynamicJavaObject(app.name(), app.content());

        // 编译Java代码
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, null, null, Arrays.asList(fileObject));
        boolean success = task.call();

        if(success) {
            // 加载并执行编译后的类
            CustomClassLoader classLoader = new CustomClassLoader(fileManager);
            Class<?> compiledClass = classLoader.loadClass(app.name());
            ByteArrayOutputStream out = (ByteArrayOutputStream) compiledClass.getDeclaredMethod("run", InputStream.class, String[].class)
                    .invoke(compiledClass.newInstance(), app.getIn(), app.getArgs());
            out.writeTo(app.getOut());
        } else {
            // 处理编译错误
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                System.err.format(Locale.ENGLISH, "Error on line %d in %s%n", diagnostic.getLineNumber(), diagnostic.getSource().toUri());
                System.err.println(diagnostic.getMessage(Locale.ENGLISH));
            }
        }
    }

    static class CustomClassLoader extends ClassLoader {

        private JavaFileManager fileManager;

        CustomClassLoader(JavaFileManager fileManager) {
            super(CustomClassLoader.class.getClassLoader());
            this.fileManager = fileManager;
        }

        @Override
        protected Class<?> findClass(String name) {
            byte[] bytes = DynamicClassLoader.getClassBytes(fileManager, name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    static class DynamicJavaObject extends SimpleJavaFileObject {
        private final String code;

        DynamicJavaObject(String name, String code) {
            super(DynamicClassLoader.toUri(name), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    static class DynamicClassLoader {
        private static byte[] getClassBytes(JavaFileManager fileManager, String className) {
            // 返回编译后的类字节码，可以根据需要自行实现
            try {
                JavaFileObject javaFileObject = fileManager.getJavaFileForInput(null, className, JavaFileObject.Kind.CLASS);
                InMemoryJavaFileObject memoryJavaFileObject = (InMemoryJavaFileObject) javaFileObject;
                return memoryJavaFileObject.getBytes();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static java.net.URI toUri(String name) {
            return java.net.URI.create(name + JavaFileObject.Kind.SOURCE.extension);
        }
    }

    static class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<URI, JavaFileObject> fileObjects = new HashMap<>();

        InMemoryFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            JavaFileObject fileObject = new InMemoryJavaFileObject(URI.create(className), kind);
            fileObjects.put(URI.create(className), fileObject);
            return fileObject;
        }

        @Override
        public JavaFileObject getJavaFileForInput(Location location,
                String className,
                JavaFileObject.Kind kind) {
            return fileObjects.get(URI.create(className));
        }

        @Override
        public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse) throws
                IOException {
            List<JavaFileObject> result = new ArrayList<>();
            for (JavaFileObject fileObject : fileObjects.values()) {
                if(fileObject.getKind() == JavaFileObject.Kind.SOURCE && fileObject.getName().startsWith(packageName)) {
                    result.add(fileObject);
                }
            }
            Iterable<JavaFileObject> superList = super.list(location, packageName, kinds, recurse);
            if(superList != null) {
                for (JavaFileObject f : superList) {
                    result.add(f);
                }
            }
            return result;
        }
    }

    static class InMemoryJavaFileObject extends SimpleJavaFileObject {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        InMemoryJavaFileObject(URI uri, Kind kind) {
            super(uri, kind);
        }

        @Override
        public OutputStream openOutputStream() {
            return outputStream;
        }

        byte[] getBytes() {
            return outputStream.toByteArray();
        }
    }
}
