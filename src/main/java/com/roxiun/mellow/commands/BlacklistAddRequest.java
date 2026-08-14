package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.seraph.SeraphBlacklistReportType;
import java.util.Arrays;

final class BlacklistAddRequest {

    private static final String DEFAULT_REASON = "(none)";
    private static final String SERAPH_DIRECTIVE = "seraph";

    private final String playerName;
    private final String localReason;
    private final SeraphBlacklistReportType seraphReportType;
    private final String seraphReason;

    private BlacklistAddRequest(
        String playerName,
        String localReason,
        SeraphBlacklistReportType seraphReportType,
        String seraphReason
    ) {
        this.playerName = playerName;
        this.localReason = localReason;
        this.seraphReportType = seraphReportType;
        this.seraphReason = seraphReason;
    }

    public static BlacklistAddRequest parse(String commandPrefix, String[] args) {
        String playerName = args[1];
        if (args.length < 3) {
            return new BlacklistAddRequest(
                playerName,
                DEFAULT_REASON,
                null,
                null
            );
        }

        if (!SERAPH_DIRECTIVE.equalsIgnoreCase(args[2])) {
            return new BlacklistAddRequest(
                playerName,
                join(args, 2),
                null,
                null
            );
        }

        if (args.length < 4) {
            throw new IllegalArgumentException(
                "Missing Seraph report type. Use " +
                buildSeraphUsage(commandPrefix)
            );
        }

        SeraphBlacklistReportType reportType = SeraphBlacklistReportType.fromToken(
            args[3]
        );
        if (reportType == null) {
            throw new IllegalArgumentException(
                "Invalid Seraph report type. Valid types: " +
                SeraphBlacklistReportType.getUsageOptions()
            );
        }

        if (args.length < 5) {
            throw new IllegalArgumentException(
                "Seraph reports require a reason. Use " +
                buildSeraphUsage(commandPrefix)
            );
        }

        String reason = join(args, 4);
        if (reason.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Seraph reports require a reason. Use " +
                buildSeraphUsage(commandPrefix)
            );
        }

        return new BlacklistAddRequest(playerName, reason, reportType, reason);
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getLocalReason() {
        return localReason;
    }

    public boolean shouldSubmitToSeraph() {
        return seraphReportType != null;
    }

    public SeraphBlacklistReportType getSeraphReportType() {
        return seraphReportType;
    }

    public String getSeraphReason() {
        return seraphReason;
    }

    public static String buildSeraphUsage(String commandPrefix) {
        return (
            commandPrefix +
            " add <username> seraph <" +
            SeraphBlacklistReportType.getUsageOptions() +
            "> <reason...>"
        );
    }

    private static String join(String[] args, int startInclusive) {
        if (args == null || startInclusive >= args.length) {
            return DEFAULT_REASON;
        }

        String joined = String.join(
            " ",
            Arrays.copyOfRange(args, startInclusive, args.length)
        ).trim();
        return joined.isEmpty() ? DEFAULT_REASON : joined;
    }
}
