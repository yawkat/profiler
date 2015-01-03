package at.yawk.profiler.web;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yawkat
 */
public abstract class Aspect {
    private static final Gson GSON = new Gson();

    public ContextHandler makeContextHandler(String path) {
        ContextHandler contextHandler = new ContextHandler(path);
        for (Method m : getClass().getMethods()) {
            m.setAccessible(true);
            Page annotation = m.getAnnotation(Page.class);
            if (annotation != null) {
                Pattern basePattern = Pattern.compile(annotation.pattern());
                Pattern layoutPattern;
                String renderedBy = annotation.renderedBy();
                if (!renderedBy.isEmpty()) {
                    layoutPattern = Pattern.compile(annotation.pattern() + Pattern.quote(".html"));
                } else {
                    layoutPattern = null;
                }

                boolean useRequest = m.getParameterCount() > 0 && m.getParameterTypes()[0] == HttpSession.class;

                contextHandler.addContext((session, p) -> {
                    Matcher matcher = basePattern.matcher(p);
                    boolean layout = false;
                    if (!matcher.matches()) {
                        if (layoutPattern == null) { return false; }

                        matcher = layoutPattern.matcher(p);
                        if (!matcher.matches()) { return false; }

                        layout = true;
                    }

                    int start = useRequest ? 1 : 0;
                    Object[] args = new Object[matcher.groupCount() + start];
                    if (useRequest) { args[0] = session; }
                    for (int i = 0; i < matcher.groupCount(); i++) {
                        args[i + start] = matcher.group(i + 1);
                    }
                    Object result;
                    try {
                        result = m.invoke(this, args);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        HttpError error;
                        if (e.getCause() instanceof HttpError) {
                            error = (HttpError) e.getCause();
                        } else {
                            error = new HttpError(500, e.getCause().getMessage());
                        }
                        session.status(error.code);
                        Object message = error.message;
                        if (message instanceof String) {
                            message = ImmutableMap.of("error", message);
                        }
                        session.data("application/json", GSON.toJson(message));
                        return true;
                    }
                    String mime;
                    String data;
                    if (layout) {
                        mime = "text/html";
                        try {
                            data = TemplateManager.getInstance().compile(renderedBy, result);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    } else {
                        mime = annotation.mime();
                        if (result instanceof String) {
                            data = (String) result;
                        } else {
                            data = GSON.toJson(result);
                        }
                    }
                    session.data(mime, data);
                    return true;
                });
            }
        }
        return contextHandler;
    }
}
