package at.yawk.profiler.attach.sun;

import at.yawk.profiler.attach.AttachmentProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;

/**
 * @author yawkat
 */
public class SunAttachmentProvider {
    private static final String PACKAGE_NAME = SunAttachmentProvider.class.getPackage().getName();

    @Getter private static final AttachmentProvider instance;

    static {
        AttachmentProvider impl;
        try {
            Class.forName("com.sun.tools.attach.spi.AttachProvider");
            impl = new SunAttachmentProviderImpl();
        } catch (ClassNotFoundException e) {
            String javaHome = System.getProperty("java.home");

            Path path = null;
            for (Path p : new Path[]{
                    Paths.get(javaHome, "lib", "tools.jar"),
                    Paths.get(javaHome, "tools.jar"),
                    Paths.get(javaHome, "..", "lib", "tools.jar"),
                    Paths.get(javaHome, "..", "tools.jar")
            }) {
                if (Files.isRegularFile(p)) {
                    path = p;
                    break;
                }
            }
            if (path == null) {
                throw new RuntimeException("Failed to load sun attachment provider, are you running a JDK?");
            }

            try {
                URLClassLoader classLoader = new URLClassLoader(new URL[]{ path.toUri().toURL() }) {
                    Map<String, Class<?>> cache = new ConcurrentHashMap<>();

                    @Override
                    public Class<?> loadClass(String name) throws ClassNotFoundException {
                        if (name.startsWith(PACKAGE_NAME)) {
                            Class<?> c = cache.get(name);
                            if (c == null) {
                                String fileName = '/' + name.replace('.', '/') + ".class";
                                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                try (InputStream in = SunAttachmentProvider.class.getResourceAsStream(fileName)) {
                                    byte[] buf = new byte[1024];
                                    int len;
                                    while ((len = in.read(buf)) > 0) {
                                        bos.write(buf, 0, len);
                                    }
                                } catch (IOException f) {
                                    throw new ClassNotFoundException();
                                }
                                byte[] data = bos.toByteArray();
                                c = defineClass(name, data, 0, data.length);
                                resolveClass(c);
                                Class<?> o = cache.putIfAbsent(name, c);
                                if (o != null) { c = o; }
                            }
                            return c;
                        }
                        return super.loadClass(name);
                    }
                };

                Class<?> cl = classLoader.loadClass(PACKAGE_NAME + ".SunAttachmentProviderImpl");
                Constructor<?> constructor = cl.getDeclaredConstructor();
                constructor.setAccessible(true);
                impl = (AttachmentProvider) constructor.newInstance();
            } catch (MalformedURLException | ReflectiveOperationException f) {
                throw new RuntimeException(f);
            }
        }
        instance = impl;
    }
}
