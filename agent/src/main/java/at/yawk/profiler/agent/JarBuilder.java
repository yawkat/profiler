package at.yawk.profiler.agent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.HashMap;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class JarBuilder implements Closeable {
    private final FileSystem fileSystem;
    private final Path root;

    public static JarBuilder create(Path target) throws IOException {
        URI uri = URI.create("jar:file:" + target);
        FileSystem fs = FileSystems.newFileSystem(uri, ImmutableMap.of("create", "true"));
        Path root = Iterables.getOnlyElement(fs.getRootDirectories());
        return new JarBuilder(fs, root);
    }

    public void addClass(Class<?> clazz) throws IOException {
        String fileName = clazz.getName().replace('.', '/') + ".class";
        Path target = root.resolve(fileName);
        try {
            Files.createDirectories(target.getParent());
        } catch (FileAlreadyExistsException ignored) {}
        try (InputStream is = clazz.getClassLoader().getResourceAsStream(fileName)) {
            Files.copy(is, target);
        }
    }

    public void manifest(URL source) throws IOException {
        Path target = root.resolve("META-INF/MANIFEST.MF");
        try {
            Files.createDirectories(target.getParent());
        } catch (FileAlreadyExistsException ignored) {}
        try (InputStream is = source.openStream()) {
            Files.copy(is, target);
        }
    }

    @Override
    public void close() throws IOException {
        fileSystem.close();
    }
}
