package com.roxiun.mellow.feature.requestpopup;

public enum RequestType {
    FRIEND("Friend request from ", "/friend accept ", "/friend deny "),
    PARTY("Party request from ", "/party accept ", null);

    private final String displayPrefix;
    private final String acceptCommandPrefix;
    private final String denyCommandPrefix;

    RequestType(
        String displayPrefix,
        String acceptCommandPrefix,
        String denyCommandPrefix
    ) {
        this.displayPrefix = displayPrefix;
        this.acceptCommandPrefix = acceptCommandPrefix;
        this.denyCommandPrefix = denyCommandPrefix;
    }

    public String buildDisplayText(String fromPlayer) {
        return displayPrefix + fromPlayer;
    }

    public String buildCommand(boolean accept, String fromPlayer) {
        String commandPrefix = accept ? acceptCommandPrefix : denyCommandPrefix;
        if (commandPrefix == null || commandPrefix.isEmpty()) {
            return null;
        }
        return commandPrefix + fromPlayer;
    }
}
