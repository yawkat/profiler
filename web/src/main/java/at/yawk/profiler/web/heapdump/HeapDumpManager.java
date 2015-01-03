package at.yawk.profiler.web.heapdump;

import at.yawk.profiler.web.App;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/**
 * @author yawkat
 */
@RequiredArgsConstructor
public class HeapDumpManager {
    static final Pattern DUMP_PATTERN = Pattern.compile("[\\w-]+\\.hprof");

    private static final DateTimeFormatter FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.YEAR, 4)
                    .appendLiteral('-')
                    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                    .appendLiteral('-')
                    .appendValue(ChronoField.DAY_OF_MONTH, 2)
                    .appendLiteral('-')
                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
                    .appendLiteral('-')
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .appendLiteral('-')
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                    .toFormatter();

    private final App app;
    private final Path directory;

    public HeapDumpManager(App app) {
        this(app, app.getStorageDirectory().resolve("heapdumps"));
    }

    public List<Path> listHeapDumps() throws IOException {
        try {
            return Files.list(directory)
                    .filter(f -> DUMP_PATTERN.matcher(f.getFileName().toString()).matches())
                    .collect(Collectors.toList());
        } catch (NoSuchFileException e) {
            return Collections.emptyList();
        }
    }

    public Path createNewDumpTarget() throws IOException {
        try {
            Files.createDirectories(directory);
        } catch (FileAlreadyExistsException ignored) {}

        LocalDateTime dateTime = LocalDateTime.now(Clock.systemUTC());
        String timeString = dateTime.format(FORMATTER);
        return resolveDump(timeString + ".hprof");
    }

    public Path resolveDump(String name) {
        if (!DUMP_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid dump file name");
        }
        return directory.resolve(name);
    }

    public void deleteHeapDump(Path path) throws IOException {
        Files.deleteIfExists(path);
    }
}
