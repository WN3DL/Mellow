package com.roxiun.mellow.module.bedwars;

import com.roxiun.mellow.gamestate.GameSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedwarsTimerService {

    private static final Pattern TIMER_PATTERN = Pattern.compile(
        "(\\d{1,2}):(\\d{2})"
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
        STAGE_SCHEDULE.put("钻石生成点ii级", 6 * 60);
        STAGE_SCHEDULE.put("绿宝石生成点ii级", 12 * 60);
        STAGE_SCHEDULE.put("钻石生成点iii级", 18 * 60);
        STAGE_SCHEDULE.put("绿宝石生成点iii级", 24 * 60);
        STAGE_SCHEDULE.put("床自毁", 30 * 60);
        STAGE_SCHEDULE.put("床已被破坏", 30 * 60);
        STAGE_SCHEDULE.put("绝杀模式", 40 * 60);
        STAGE_SCHEDULE.put("游戏结束", 50 * 60);
    }

    private BedwarsTimerState state = BedwarsTimerState.empty();

    public BedwarsTimerState getState() {
        return state;
    }

    public void reset() {
        state = BedwarsTimerState.empty();
    }

    public void update(GameSnapshot snapshot) {
        if (snapshot == null || !snapshot.isInBedwars()) {
            reset();
            return;
        }

        List<String> sidebarLines = snapshot.getScoreboardLines();
        StageTimer stage = parseStageTimer(sidebarLines);
        if (stage == null) {
            return;
        }

        int elapsed = stage.scheduledSeconds - stage.secondsLeft;
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
        if (lines == null || lines.isEmpty()) {
            return null;
        }

        for (String line : lines) {
            StageTimer parsed = parseStageLine(line);
            if (parsed == null) {
                continue;
            }
            return parsed;
        }

        return null;
    }

    private StageTimer parseStageLine(String line) {
        if (line == null) {
            return null;
        }

        String raw = line.trim();
        if (raw.isEmpty()) {
            return null;
        }

        Matcher timer = TIMER_PATTERN.matcher(raw);
        if (!timer.find()) {
            return null;
        }

        int minutes;
        int seconds;
        try {
            minutes = Integer.parseInt(timer.group(1));
            seconds = Integer.parseInt(timer.group(2));
        } catch (NumberFormatException ignored) {
            return null;
        }

        int timerStart = timer.start();
        String lower = raw.toLowerCase(Locale.ROOT);
        String eventName;

        int explicitIn = lower.indexOf(" in ");
        int explicitDash = lower.indexOf(" - ");
        if (explicitIn >= 0 && explicitIn < timerStart) {
            eventName = raw.substring(0, explicitIn).trim();
        } else if (explicitDash >= 0 && explicitDash < timerStart) {
            eventName = raw.substring(0, explicitDash).trim();
        } else {
            int looseIn = lower.indexOf("in ");
            int looseDash = lower.indexOf("- ");
            if (looseIn >= 0 && looseIn < timerStart) {
                eventName = raw.substring(0, looseIn).trim();
            } else if (looseDash >= 0 && looseDash < timerStart) {
                eventName = raw.substring(0, looseDash).trim();
            } else {
                eventName = raw.substring(0, timerStart).trim();
            }
        }

        Integer scheduled = resolveScheduledSeconds(normalizeEvent(eventName));
        if (scheduled == null) {
            return null;
        }

        return new StageTimer(scheduled, minutes * 60 + seconds);
    }

    private String normalizeEvent(String eventName) {
        String normalized = eventName
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.startsWith("next event:")) {
            normalized = normalized.substring("next event:".length()).trim();
        }
        if (normalized.startsWith("下个事件:")) {
            normalized = normalized.substring("下个事件:".length()).trim();
        }
        return normalized;
    }

    private Integer resolveScheduledSeconds(String eventName) {
        Integer direct = STAGE_SCHEDULE.get(eventName);
        if (direct != null) {
            return direct;
        }

        String normalized = eventName
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ")
            .trim();

        if (normalized.contains("diamond") || normalized.contains("钻石")) {
            if (containsTier(normalized, 2)) {
                return 6 * 60;
            }
            if (containsTier(normalized, 3)) {
                return 18 * 60;
            }
        }

        if (normalized.contains("emerald") || normalized.contains("绿宝石")) {
            if (containsTier(normalized, 2)) {
                return 12 * 60;
            }
            if (containsTier(normalized, 3)) {
                return 24 * 60;
            }
        }

        if (normalized.contains("sudden death") || normalized.contains("绝杀模式")) {
            return 40 * 60;
        }
        if (
            normalized.contains("game end") || normalized.contains("end game") || normalized.contains("游戏结束")
        ) {
            return 50 * 60;
        }
        if (
            normalized.contains("bed gone") ||
            normalized.contains("beds gone") ||
            normalized.contains("bed destroyed") ||
            normalized.contains("beds destroyed") ||
            normalized.contains("床自毁") ||
            normalized.contains("床已被破坏")
        ) {
            return 30 * 60;
        }

        return null;
    }

    private boolean containsTier(String value, int tier) {
        if (tier == 2) {
            return (
                value.matches(".*\\bii\\b.*") ||
                value.matches(".*\\b2\\b.*")
            );
        }
        if (tier == 3) {
            return (
                value.matches(".*\\biii\\b.*") ||
                value.matches(".*\\b3\\b.*")
            );
        }
        return false;
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
            if (line.contains("Pink:") || line.contains("粉队:")) {
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

        private final int scheduledSeconds;
        private final int secondsLeft;

        private StageTimer(int scheduledSeconds, int secondsLeft) {
            this.scheduledSeconds = scheduledSeconds;
            this.secondsLeft = secondsLeft;
        }
    }
}
