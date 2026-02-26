package com.roxiun.mellow.module.bedwars;

import com.roxiun.mellow.gamestate.GameSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedwarsTimerService {

    private static final Pattern STAGE_TIMER_PATTERN = Pattern.compile(
        "(?i)(.+?)\\s+in\\s+(\\d{1,2}):(\\d{2})"
    );

    private static final Map<String, Integer> STAGE_SCHEDULE = new HashMap<>();

    static {
        STAGE_SCHEDULE.put("diamond ii", 6 * 60);
        STAGE_SCHEDULE.put("emerald ii", 12 * 60);
        STAGE_SCHEDULE.put("diamond iii", 18 * 60);
        STAGE_SCHEDULE.put("emerald iii", 24 * 60);
        STAGE_SCHEDULE.put("bed gone", 30 * 60);
        STAGE_SCHEDULE.put("beds gone", 30 * 60);
        STAGE_SCHEDULE.put("bed destroyed", 30 * 60);
        STAGE_SCHEDULE.put("beds destroyed", 30 * 60);
        STAGE_SCHEDULE.put("sudden death", 40 * 60);
        STAGE_SCHEDULE.put("game end", 50 * 60);
        STAGE_SCHEDULE.put("end game", 50 * 60);
    }

    private BedwarsTimerState state = BedwarsTimerState.empty();

    public BedwarsTimerState getState() {
        return state;
    }

    public void reset() {
        state = BedwarsTimerState.empty();
    }

    public void update(GameSnapshot snapshot) {
        List<String> sidebarLines = snapshot.getScoreboardLines();
        StageTimer stage = parseStageTimer(sidebarLines);
        if (!snapshot.isInBedwarsMatch() && stage == null) {
            reset();
            return;
        }
        if (stage == null) {
            return;
        }

        int elapsed = Math.max(0, stage.scheduledSeconds - stage.secondsLeft);
        String modeGroup = resolveModeGroup(snapshot.getMode(), sidebarLines);

        SpawnState emeraldState = calculateSpawns(
            elapsed + 1,
            31,
            new int[] { 12 * 60, 24 * 60 },
            new IntervalProvider() {
                @Override
                public int intervalForTier(int tier) {
                    return getEmeraldInterval(modeGroup, tier);
                }
            }
        );

        SpawnState diamondState = calculateSpawns(
            elapsed + 1,
            1,
            new int[] { 6 * 60, 18 * 60 },
            new IntervalProvider() {
                @Override
                public int intervalForTier(int tier) {
                    return getDiamondInterval(tier);
                }
            }
        );

        state = new BedwarsTimerState(
            emeraldState.count,
            emeraldState.next,
            diamondState.count,
            diamondState.next
        );
    }

    private StageTimer parseStageTimer(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = STAGE_TIMER_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue;
            }

            String eventName = normalizeEvent(matcher.group(1));
            Integer scheduled = STAGE_SCHEDULE.get(eventName);
            if (scheduled == null) {
                continue;
            }

            int minutes;
            int seconds;
            try {
                minutes = Integer.parseInt(matcher.group(2));
                seconds = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException ignored) {
                continue;
            }

            return new StageTimer(eventName, scheduled, minutes * 60 + seconds);
        }

        return null;
    }

    private String normalizeEvent(String eventName) {
        String normalized = eventName
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.startsWith("next event:")) {
            normalized = normalized.substring("next event:".length()).trim();
        }
        return normalized;
    }

    private String resolveModeGroup(String locationMode, List<String> lines) {
        String mode = locationMode == null ? "" : locationMode.toUpperCase(Locale.ROOT);

        if (mode.contains("_EIGHT_")) {
            return "eight";
        }
        if (mode.contains("_FOUR_")) {
            return "four";
        }

        for (String line : lines) {
            if (line.contains("Pink:")) {
                return "eight";
            }
        }

        return "four";
    }

    private int getEmeraldInterval(String modeGroup, int tier) {
        if ("eight".equals(modeGroup)) {
            if (tier == 1) {
                return 65;
            }
            if (tier == 2) {
                return 50;
            }
            return 35;
        }

        if (tier == 1) {
            return 55;
        }
        if (tier == 2) {
            return 40;
        }
        return 27;
    }

    private int getDiamondInterval(int tier) {
        if (tier == 1) {
            return 30;
        }
        if (tier == 2) {
            return 23;
        }
        return 12;
    }

    private SpawnState calculateSpawns(
        int elapsedTime,
        int firstSpawn,
        int[] upgrades,
        IntervalProvider intervalProvider
    ) {
        if (elapsedTime < firstSpawn) {
            return new SpawnState(0, firstSpawn - elapsedTime);
        }

        int count = 1;
        int lastSpawn = firstSpawn;
        int upgradeIndex = 0;
        int nextUpgrade = upgrades.length > 0 ? upgrades[0] : Integer.MAX_VALUE;

        while (true) {
            int tier = getTier(lastSpawn, upgrades);
            int interval = intervalProvider.intervalForTier(tier);
            int nextSpawn = lastSpawn + interval;

            if (nextUpgrade <= elapsedTime && nextUpgrade < nextSpawn) {
                count++;
                lastSpawn = nextUpgrade;
                upgradeIndex++;
                nextUpgrade = upgradeIndex < upgrades.length
                    ? upgrades[upgradeIndex]
                    : Integer.MAX_VALUE;
                continue;
            }

            if (nextSpawn > elapsedTime) {
                return new SpawnState(count, Math.max(1, nextSpawn - elapsedTime));
            }

            count++;
            lastSpawn = nextSpawn;
        }
    }

    private int getTier(int time, int[] upgrades) {
        if (upgrades.length > 1 && time >= upgrades[1]) {
            return 3;
        }
        if (upgrades.length > 0 && time >= upgrades[0]) {
            return 2;
        }
        return 1;
    }

    private interface IntervalProvider {
        int intervalForTier(int tier);
    }

    private static class SpawnState {

        private final int count;
        private final int next;

        private SpawnState(int count, int next) {
            this.count = count;
            this.next = next;
        }
    }

    private static class StageTimer {

        private final String event;
        private final int scheduledSeconds;
        private final int secondsLeft;

        private StageTimer(String event, int scheduledSeconds, int secondsLeft) {
            this.event = event;
            this.scheduledSeconds = scheduledSeconds;
            this.secondsLeft = secondsLeft;
        }
    }
}
