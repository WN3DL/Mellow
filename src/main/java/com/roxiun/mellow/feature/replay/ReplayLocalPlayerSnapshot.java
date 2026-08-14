package com.roxiun.mellow.feature.replay;

public class ReplayLocalPlayerSnapshot {

    private int timestampMs;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean sneaking;
    private boolean sprinting;

    public ReplayLocalPlayerSnapshot() {}

    public ReplayLocalPlayerSnapshot(
        int timestampMs,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        boolean sneaking,
        boolean sprinting
    ) {
        this.timestampMs = timestampMs;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.sneaking = sneaking;
        this.sprinting = sprinting;
    }

    public int getTimestampMs() {
        return timestampMs;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public boolean isSprinting() {
        return sprinting;
    }
}
