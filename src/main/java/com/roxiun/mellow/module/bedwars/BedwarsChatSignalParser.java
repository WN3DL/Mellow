package com.roxiun.mellow.module.bedwars;

public final class BedwarsChatSignalParser {

    private BedwarsChatSignalParser() {}

    public static boolean isBedwarsStartMessage(String message) {
        return
            message.contains("Protect your bed and destroy the enemy beds.") ||
            message.contains("保护你的床并摧毁敌人的床。") &&
            !message.contains(":") &&
            !message.contains("SHOUT") &&
            !message.contains("喊话");
    }

    public static boolean isBedwarsRespawnMessage(String message) {
        return
            message.contains("You will respawn because you still have a bed!") &&
            !message.contains(":") &&
            !message.contains("SHOUT") &&
            !message.contains("喊话");
    }

    public static boolean isPregameCountdownMessage(String message) {
        return
            message.startsWith("The game starts in ") &&
            message.contains(" seconds!") ||
            message.startsWith("游戏将在") &&
            message.contains("秒后开始！");
    }

    public static boolean isPurchaseMessage(String message) {
        return message.toLowerCase().contains("purchased") ||
               message.contains("购买");
    }

    public static boolean isTrapSignalMessage(String message) {
        String lower = message.toLowerCase();
        return (
            lower.contains("trap was set off!") ||
            lower.contains("陷阱已被触发！") ||
            lower.contains("reveal trap set off") ||
            lower.contains("触发了显形陷阱") ||
            (lower.contains("removed") && lower.contains("trap from the queue")) ||
            (lower.contains("已将") && lower.contains("移出队列"))
        );
    }
}
