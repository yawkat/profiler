package at.yawk.profiler.web.heapdump;

import at.yawk.hdr.index.ProgressCounter;
import com.google.gson.JsonObject;

/**
 * @author yawkat
 */
class JsonProgressCounter extends ProgressCounter {
    private volatile long progress;
    private volatile long max;

    public JsonProgressCounter(int granularity) {
        super(granularity);
    }

    @Override
    protected void updateValue(long progress, long max) {
        this.progress = progress;
        this.max = max;
    }

    public void end() {
        progress = max;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("progress", progress);
        obj.addProperty("max", max);
        return obj;
    }
}
