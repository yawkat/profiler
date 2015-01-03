package at.yawk.profiler.web;

import com.github.jknack.handlebars.Handlebars;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;

/**
 * @author yawkat
 */
class TemplateManager {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(AtomicInteger.class, new TypeAdapter<AtomicInteger>() {
                @Override
                public void write(JsonWriter out, AtomicInteger value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.intValue());
                    }
                }

                @Override
                public AtomicInteger read(JsonReader in) throws IOException {
                    JsonToken type = in.peek();
                    if (type == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    } else {
                        return new AtomicInteger(in.nextInt());
                    }
                }
            })
            .registerTypeAdapter(AtomicLong.class, new TypeAdapter<AtomicLong>() {
                @Override
                public void write(JsonWriter out, AtomicLong value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.longValue());
                    }
                }

                @Override
                public AtomicLong read(JsonReader in) throws IOException {
                    JsonToken type = in.peek();
                    if (type == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    } else {
                        return new AtomicLong(in.nextLong());
                    }
                }
            })
            .create();

    @Getter private static final TemplateManager instance = new TemplateManager();

    private final Handlebars handlebars = new Handlebars();

    public String compile(String path, Object data) throws IOException {
        return handlebars.compile(path).apply(contextualize(data));
    }

    private static Object contextualize(Object object) {
        JsonElement ele = GSON.toJsonTree(object);
        return toObj(ele);
    }

    private static Object toObj(JsonElement element) {
        if (element instanceof JsonObject) {
            Map<String, Object> hm = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : ((JsonObject) element).entrySet()) {
                hm.put(entry.getKey(), toObj(entry.getValue()));
            }
            return hm;
        } else if (element instanceof JsonArray) {
            return ImmutableList.copyOf(Iterables.transform((JsonArray) element, TemplateManager::toObj));
        } else if (element instanceof JsonPrimitive) {
            JsonPrimitive primitive = (JsonPrimitive) element;
            if (primitive.isBoolean()) { return primitive.getAsBoolean(); }
            if (primitive.isNumber()) { return primitive.getAsNumber(); }
            return primitive.getAsString();
        } else {
            return null;
        }
    }
}
