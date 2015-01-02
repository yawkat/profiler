package at.yawk.profiler.web;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

/**
 * @author yawkat
 */
@Slf4j
class ComponentScanner {
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("[\\w/\\.]+");

    @Getter private static final ComponentScanner instance = new ComponentScanner();

    private final Reflections reflections = new Reflections("at.yawk.profiler.web");

    @SuppressWarnings("unchecked")
    public <T> Collection<T> makeComponents(Class<T> ofType) {
        List<T> components = new ArrayList<T>();
        reflections.getTypesAnnotatedWith(Component.class)
                .stream()
                .filter(ofType::isAssignableFrom)
                .forEach(c -> {
                    try {
                        components.add((T) c.newInstance());
                    } catch (InstantiationException | IllegalAccessException e) {
                        log.error("Failed to create component", e);
                    }
                });
        return components;
    }

    public <A extends Aspect> void loadAspects(Class<A> type, Consumer<A> decorator, ContextHandler contextHandler,
                                               String contextPath) {
        ComponentScanner.getInstance().makeComponents(type)
                .forEach(aspect -> {
                    decorator.accept(aspect);
                    Path annotation = aspect.getClass().getAnnotation(Path.class);
                    String path = annotation == null ? "" : annotation.value();
                    ContextHandler child = aspect.makeContextHandler(path);
                    contextHandler.addContext(child);
                });
        contextHandler.addContext((session, path) -> {
            if (RESOURCE_PATTERN.matcher(path).matches()) {
                URL url = ComponentScanner.class.getResource(contextPath + path);
                if (url != null) {
                    try {
                        session.data("text/html", url.openStream());
                        return true;
                    } catch (IOException e) {
                        log.warn("Failed to load resource " + url, e);
                    }
                }
            }
            return false;
        });
        contextHandler.addContext((session, path) -> {
            if (path.endsWith(".html")) {
                path = path.substring(0, path.length() - 5);
                try {
                    String data = TemplateManager.getInstance().compile(path, null);
                    session.data("text/html", data);
                    return true;
                } catch (IOException ignored) {}
            }
            return false;
        });
    }
}
