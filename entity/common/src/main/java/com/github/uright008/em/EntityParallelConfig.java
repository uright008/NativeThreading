package com.github.uright008.em;

import com.google.gson.JsonObject;
import com.github.uright008.pc.ParallelConfig;

public final class EntityParallelConfig extends ParallelConfig {

    private static final EntityParallelConfig INSTANCE = new EntityParallelConfig();

    private volatile boolean enabled;
    private volatile int tickTimeoutSeconds;

    private EntityParallelConfig() { super("entity-parallelization"); }

    public static void init() { INSTANCE.initialize(); }

    @Override
    protected void applyDefaults() {
        enabled = false;
        tickTimeoutSeconds = 5;
    }

    @Override
    protected void read(JsonObject json) {
        applyDefaults();
        if (json.has("enabled")) enabled = json.get("enabled").getAsBoolean();
        if (json.has("tickTimeoutSeconds")) tickTimeoutSeconds = json.get("tickTimeoutSeconds").getAsInt();
        logger().info("Entity parallelization: {} | tickTimeout={}s",
                enabled ? "ON" : "OFF", tickTimeoutSeconds);
    }

    @Override
    protected JsonObject write() {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", enabled);
        json.addProperty("tickTimeoutSeconds", tickTimeoutSeconds);
        return json;
    }

    public static boolean isEnabled() { return INSTANCE.loaded && INSTANCE.enabled; }
    public static void setEnabled(boolean v) { INSTANCE.enabled = v; INSTANCE.save(); }
    public static int tickTimeoutSeconds() { return INSTANCE.tickTimeoutSeconds; }
    public static void setTickTimeoutSeconds(int v) { INSTANCE.tickTimeoutSeconds = v; INSTANCE.save(); }

    public static void reloadConfig() { INSTANCE.reload(); }
}
