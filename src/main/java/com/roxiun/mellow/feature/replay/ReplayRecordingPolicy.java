package com.roxiun.mellow.feature.replay;

import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.module.bedwars.BedwarsChatSignalParser;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class ReplayRecordingPolicy {

    private static final Pattern GENERIC_TIMER_PATTERN = Pattern.compile(
        "(?i).+\\s+(?:in\\s+)?\\d{1,2}:\\d{2}"
    );
    private static final Set<String> BEDWARS_STAGE_EVENTS = new HashSet<>();

    static {
        BEDWARS_STAGE_EVENTS.add("diamond ii");
        BEDWARS_STAGE_EVENTS.add("emerald ii");
        BEDWARS_STAGE_EVENTS.add("diamond iii");
        BEDWARS_STAGE_EVENTS.add("emerald iii");
        BEDWARS_STAGE_EVENTS.add("bed gone");
        BEDWARS_STAGE_EVENTS.add("beds gone");
        BEDWARS_STAGE_EVENTS.add("bed destroyed");
        BEDWARS_STAGE_EVENTS.add("beds destroyed");
        BEDWARS_STAGE_EVENTS.add("sudden death");
        BEDWARS_STAGE_EVENTS.add("game end");
        BEDWARS_STAGE_EVENTS.add("end game");
    }

    private ReplayRecordingPolicy() {}

    static boolean isRecordableMatch(GameSnapshot snapshot) {
        return snapshot != null && snapshot.isInBedwarsMatch();
    }

    static boolean isBedwarsSession(GameSnapshot snapshot) {
        return snapshot != null && snapshot.isInBedwars();
    }

    static boolean isChatConfirmedMatchStart(String message) {
        return
            BedwarsChatSignalParser.isBedwarsStartMessage(message) ||
            BedwarsChatSignalParser.isBedwarsRespawnMessage(message);
    }

    static boolean hasLiveMatchEvidence(GameSnapshot snapshot) {
        return isBedwarsSession(snapshot) && hasBedwarsStageTimer(snapshot.getScoreboardLines());
    }

    private static boolean hasBedwarsStageTimer(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }

        for (String line : lines) {
            String normalized = normalize(line);
            if (!GENERIC_TIMER_PATTERN.matcher(normalized).matches()) {
                continue;
            }

            String eventName = normalized;
            int inIndex = normalized.indexOf(" in ");
            if (inIndex >= 0) {
                eventName = normalized.substring(0, inIndex).trim();
            } else {
                int timerStart = normalized.lastIndexOf(' ');
                if (timerStart > 0) {
                    eventName = normalized.substring(0, timerStart).trim();
                }
            }
            if (eventName.startsWith("next event:")) {
                eventName = eventName.substring("next event:".length()).trim();
            }
            if (BEDWARS_STAGE_EVENTS.contains(eventName)) {
                return true;
            }
        }

        return false;
    }

    private static String normalize(String line) {
        return line == null
            ? ""
            : line.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
