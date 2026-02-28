package com.roxiun.mellow.feature.stats.tab;

import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.config.MellowOneConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExtendedTabStatsColumns {

    public static final int COLUMN_GAP = 4;
    public static final int BEDWARS_NONE_INDEX = 10;
    public static final int BEDWARS_HP_INDEX = 11;
    public static final int SKYWARS_NONE_INDEX = 7;
    public static final int SKYWARS_HP_INDEX = 8;
    public static final int DUELS_NONE_INDEX = 10;
    public static final int DUELS_HP_INDEX = 11;

    private ExtendedTabStatsColumns() {}

    public static int[] getConfiguredStatsForScope(
        StatScope scope,
        MellowOneConfig config
    ) {
        if (config == null) {
            return new int[0];
        }

        if (scope == StatScope.SKYWARS) {
            return new int[] {
                config.skywarsCustomStat1,
                config.skywarsCustomStat2,
                config.skywarsCustomStat3,
                config.skywarsCustomStat4,
                config.skywarsCustomStat5,
                config.skywarsCustomStat6,
                config.skywarsCustomStat7,
                config.skywarsCustomStat8,
                config.skywarsCustomStat9,
                config.skywarsCustomStat10,
            };
        }

        if (scope == StatScope.DUELS) {
            return new int[] {
                config.duelsCustomStat1,
                config.duelsCustomStat2,
                config.duelsCustomStat3,
                config.duelsCustomStat4,
                config.duelsCustomStat5,
                config.duelsCustomStat6,
                config.duelsCustomStat7,
                config.duelsCustomStat8,
                config.duelsCustomStat9,
                config.duelsCustomStat10,
            };
        }

        return new int[] {
            config.customStat1,
            config.customStat2,
            config.customStat3,
            config.customStat4,
            config.customStat5,
            config.customStat6,
            config.customStat7,
            config.customStat8,
            config.customStat9,
            config.customStat10,
        };
    }

    public static List<Integer> getConfiguredColumns(
        StatScope scope,
        MellowOneConfig config
    ) {
        int[] configured = getConfiguredStatsForScope(scope, config);
        if (configured.length == 0) {
            return Collections.emptyList();
        }

        int noneIndex = getNoneIndex(scope);
        List<Integer> columns = new ArrayList<>(configured.length);
        for (int configuredIndex : configured) {
            if (configuredIndex == noneIndex) {
                continue;
            }
            if (!isSupportedColumn(scope, configuredIndex)) {
                continue;
            }
            columns.add(configuredIndex);
        }

        return columns;
    }

    public static int getNoneIndex(StatScope scope) {
        if (scope == StatScope.SKYWARS) {
            return SKYWARS_NONE_INDEX;
        }
        if (scope == StatScope.DUELS) {
            return DUELS_NONE_INDEX;
        }
        return BEDWARS_NONE_INDEX;
    }

    public static boolean isSupportedColumn(StatScope scope, int statIndex) {
        if (scope == StatScope.SKYWARS) {
            return (
                (statIndex >= 0 && statIndex < SKYWARS_NONE_INDEX) ||
                statIndex == SKYWARS_HP_INDEX
            );
        }
        if (scope == StatScope.DUELS) {
            return (
                (statIndex >= 0 && statIndex < DUELS_NONE_INDEX) ||
                statIndex == DUELS_HP_INDEX
            );
        }
        return (
            (statIndex >= 0 && statIndex < BEDWARS_NONE_INDEX) ||
            statIndex == BEDWARS_HP_INDEX
        );
    }

    public static boolean isHealthColumn(StatScope scope, int statIndex) {
        if (scope == StatScope.SKYWARS) {
            return statIndex == SKYWARS_HP_INDEX;
        }
        if (scope == StatScope.DUELS) {
            return statIndex == DUELS_HP_INDEX;
        }
        return statIndex == BEDWARS_HP_INDEX;
    }

    public static String getHeaderLabel(StatScope scope, int statIndex) {
        if (scope == StatScope.SKYWARS) {
            switch (statIndex) {
                case 0:
                    return "TEAM";
                case 1:
                    return "LEVEL";
                case 2:
                    return "NAME";
                case 3:
                    return "KDR";
                case 4:
                    return "WLR";
                case 5:
                    return "WINS";
                case 6:
                    return "KILLS";
                case SKYWARS_HP_INDEX:
                    return "HP";
                default:
                    return "";
            }
        }

        if (scope == StatScope.DUELS) {
            switch (statIndex) {
                case 0:
                    return "TEAM";
                case 1:
                    return "DIV";
                case 2:
                    return "NAME";
                case 3:
                    return "KDR";
                case 4:
                    return "WLR";
                case 5:
                    return "WINS";
                case 6:
                    return "LOSSES";
                case 7:
                    return "KILLS";
                case 8:
                    return "DEATHS";
                case 9:
                    return "WS";
                case DUELS_HP_INDEX:
                    return "HP";
                default:
                    return "";
            }
        }

        switch (statIndex) {
            case 0:
                return "TEAM";
            case 1:
                return "STARS";
            case 2:
                return "NAME";
            case 3:
                return "FKDR";
            case 4:
                return "WS";
            case 5:
                return "WLR";
            case 6:
                return "BBLR";
            case 7:
                return "WINS";
            case 8:
                return "BEDS";
            case 9:
                return "FINALS";
            case BEDWARS_HP_INDEX:
                return "HP";
            default:
                return "";
        }
    }

    public static int getMinimumColumnWidth(StatScope scope, int statIndex) {
        if (scope == StatScope.SKYWARS) {
            switch (statIndex) {
                case 0:
                    return 28; // TEAM
                case 1:
                    return 56; // LEVEL
                case 2:
                    return 120; // NAME
                case 3:
                    return 40; // KDR
                case 4:
                    return 40; // WLR
                case 5:
                    return 42; // WINS
                case 6:
                    return 42; // KILLS
                case SKYWARS_HP_INDEX:
                    return 24; // HP
                default:
                    return 36;
            }
        }

        if (scope == StatScope.DUELS) {
            switch (statIndex) {
                case 0:
                    return 28; // TEAM
                case 1:
                    return 56; // DIVISION
                case 2:
                    return 120; // NAME
                case 3:
                    return 40; // KDR
                case 4:
                    return 40; // WLR
                case 5:
                    return 42; // WINS
                case 6:
                    return 48; // LOSSES
                case 7:
                    return 42; // KILLS
                case 8:
                    return 50; // DEATHS
                case 9:
                    return 36; // WS
                case DUELS_HP_INDEX:
                    return 24; // HP
                default:
                    return 36;
            }
        }

        switch (statIndex) {
            case 0:
                return 28; // TEAM
            case 1:
                return 56; // STARS
            case 2:
                return 120; // NAME
            case 3:
                return 40; // FKDR
            case 4:
                return 42; // WS
            case 5:
                return 40; // WLR
            case 6:
                return 44; // BBLR
            case 7:
                return 42; // WINS
            case 8:
                return 42; // BEDS
            case 9:
                return 46; // FINALS
            case BEDWARS_HP_INDEX:
                return 24; // HP
            default:
                return 36;
        }
    }

    public static int estimateTotalWidth(StatScope scope, MellowOneConfig config) {
        List<Integer> columns = getConfiguredColumns(scope, config);
        if (columns.isEmpty()) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < columns.size(); i++) {
            total += getMinimumColumnWidth(scope, columns.get(i));
            if (i > 0) {
                total += COLUMN_GAP;
            }
        }

        return total;
    }
}
