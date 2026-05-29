package com.github.uright008.tester;

import com.google.gson.JsonObject;
import com.github.uright008.pc.ParallelConfig;

public final class ProfileConfig extends ParallelConfig {

    private static final ProfileConfig INSTANCE = new ProfileConfig();

    private volatile boolean autoProfile;
    private volatile int warmupSeconds;
    private volatile int recordSeconds;
    private volatile int botX;
    private volatile int botY;
    private volatile int botZ;
    private volatile String postSpawnCommand;

    private ProfileConfig() {
        super("profiler-monitor");
    }

    public static void init() {
        INSTANCE.initialize();
    }

    @Override
    protected void applyDefaults() {
        autoProfile = false;
        warmupSeconds = 5;
        recordSeconds = 30;
        botX = 0;
        botY = 64;
        botZ = 0;
        postSpawnCommand = "";
    }

    @Override
    protected void read(JsonObject json) {
        applyDefaults();
        if (json.has("autoProfile")) autoProfile = json.get("autoProfile").getAsBoolean();
        if (json.has("warmupSeconds")) warmupSeconds = Math.max(1, json.get("warmupSeconds").getAsInt());
        if (json.has("recordSeconds")) recordSeconds = Math.max(10, json.get("recordSeconds").getAsInt());
        if (json.has("botX")) botX = json.get("botX").getAsInt();
        if (json.has("botY")) botY = json.get("botY").getAsInt();
        if (json.has("botZ")) botZ = json.get("botZ").getAsInt();
        if (json.has("postSpawnCommand")) postSpawnCommand = json.get("postSpawnCommand").getAsString();
        logger().info("Profiler auto-profile: {} (bot at {},{},{})",
                autoProfile ? "ON" : "OFF", botX, botY, botZ);
    }

    @Override
    protected JsonObject write() {
        JsonObject json = new JsonObject();
        json.addProperty("autoProfile", autoProfile);
        json.addProperty("warmupSeconds", warmupSeconds);
        json.addProperty("recordSeconds", recordSeconds);
        json.addProperty("botX", botX);
        json.addProperty("botY", botY);
        json.addProperty("botZ", botZ);
        json.addProperty("postSpawnCommand", postSpawnCommand);
        return json;
    }

    public static boolean isAutoProfile() { return INSTANCE.loaded && INSTANCE.autoProfile; }
    public static int warmupSeconds() { return INSTANCE.warmupSeconds; }
    public static int recordSeconds() { return INSTANCE.recordSeconds; }
    public static int botX() { return INSTANCE.botX; }
    public static int botY() { return INSTANCE.botY; }
    public static int botZ() { return INSTANCE.botZ; }
    public static String postSpawnCommand() { return INSTANCE.postSpawnCommand; }
}
