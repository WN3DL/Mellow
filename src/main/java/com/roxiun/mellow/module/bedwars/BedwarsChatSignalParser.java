package com.roxiun.mellow.module.bedwars;

public final class BedwarsChatSignalParser {

    private BedwarsChatSignalParser() {}

    public static boolean isBedwarsStartMessage(String message) {
        return
            message.contains("Protect your bed and destroy the enemy beds.") &&
            !message.contains(":") &&
            !message.contains("SHOUT");
    }

    public static boolean isBedwarsRespawnMessage(String message) {
        return
            message.contains("You will respawn because you still have a bed!") &&
            !message.contains(":") &&
            !message.contains("SHOUT");
    }

    public static boolean isPregameCountdownMessage(String message) {
        return
            message.startsWith("The game starts in ") &&
            message.contains(" seconds!");
    }

    public static boolean isPurchaseMessage(String message) {
        return message.toLowerCase().contains("purchased");
    }

    public static boolean isTrapSignalMessage(String message) {
        String lower = message.toLowerCase();
        return (
            lower.contains("trap was set off!") ||
            lower.contains("reveal trap set off") ||
            (lower.contains("removed") && lower.contains("trap from the queue"))
        );
    }
}
