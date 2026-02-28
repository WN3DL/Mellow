package com.roxiun.mellow.config;

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.annotations.Button;
import cc.polyfrost.oneconfig.config.annotations.Checkbox;
import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.HUD;
import cc.polyfrost.oneconfig.config.annotations.Info;
import cc.polyfrost.oneconfig.config.annotations.Number;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.data.InfoType;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.data.ModType;
import cc.polyfrost.oneconfig.config.data.OptionSize;
import cc.polyfrost.oneconfig.utils.NetworkUtils;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.hud.BedwarsUpgradesTrapsHUD;
import com.roxiun.mellow.hud.DiamondCounterHUD;
import com.roxiun.mellow.hud.EmeraldCounterHUD;

public class MellowOneConfig extends Config {

    @Switch(name = "Auto /who", subcategory = "General")
    public boolean autoWho = true;

    @Switch(name = "Show Tab Stats", subcategory = "General")
    public boolean tabStats = true;

    @Switch(name = "Show Tags", subcategory = "General")
    public boolean tags = false;

    @Switch(name = "Print Stats to Chat", subcategory = "General")
    public boolean printStats = false;

    @Switch(name = "Auto Update Check", subcategory = "General")
    public boolean autoUpdateCheck = true;

    // Tab Stats Configuration

    @Switch(name = "Show Stars with Brackets", category = "Tab Stats")
    public boolean showStarsWithBrackets = true;

    @Switch(name = "Show Nick with Brackets", category = "Tab Stats")
    public boolean showNickWithBrackets = true;

    @Switch(name = "Extended Tab Stats View", category = "Tab Stats")
    public boolean extendedTabStatsView = true;

    @Switch(
        name = "Extended View In Lobbies",
        category = "Tab Stats",
        description = "Allows Extended Tab Stats View while in game lobbies."
    )
    public boolean extendedTabStatsInLobbies = false;

    @Switch(
        name = "Extended View Player Heads",
        category = "Tab Stats",
        description = "Shows player heads in the Name column when using Extended Tab Stats View."
    )
    public boolean extendedTabStatsShowHeads = true;

    @Dropdown(
        name = "Extended Team Column Mode",
        options = {
            "Combine With Stars",
            "Own Column",
            "Hide Team Header",
            "Combine With Name",
        },
        category = "Tab Stats"
    )
    public int extendedTabStatsTeamColumnMode = 0;

    @Switch(
        name = "Show Ranks In-Game",
        category = "Tab Stats",
        description = "When enabled, Name stat includes rank prefix during games. Lobbies always show rank."
    )
    public boolean showRanksInGameTabStats = false;

    @Info(
        text = "Set the order of stats in the tab list",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats"
    )
    public static boolean ignoredStatsOrderInfo;

    @Dropdown(
        name = "First Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat1 = 0;

    @Dropdown(
        name = "Second Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat2 = 1; // Stars

    @Dropdown(
        name = "Third Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat3 = 2; // Name

    @Dropdown(
        name = "Fourth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat4 = 3; // FKDR

    @Dropdown(
        name = "Fifth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat5 = 4; // Winstreak

    @Dropdown(
        name = "Sixth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat6 = 11; // HP by default

    @Dropdown(
        name = "Seventh Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat7 = 10; // None by default

    @Dropdown(
        name = "Eighth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat8 = 10; // None by default

    @Dropdown(
        name = "Ninth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat9 = 10; // None by default

    @Dropdown(
        name = "Tenth Stat",
        options = {
            "Team",
            "Stars",
            "Name",
            "FKDR",
            "Winstreak",
            "WLR",
            "BBLR",
            "Wins",
            "Beds",
            "Finals",
            "None",
            "HP",
        },
        category = "Tab Stats"
    )
    public int customStat10 = 10; // None by default

    @Info(
        text = "Set the order of SkyWars stats in the tab list",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public static boolean ignoredSkywarsStatsOrderInfo;

    @Dropdown(
        name = "First SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat1 = 0;

    @Dropdown(
        name = "Second SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat2 = 1;

    @Dropdown(
        name = "Third SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat3 = 2;

    @Dropdown(
        name = "Fourth SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat4 = 3;

    @Dropdown(
        name = "Fifth SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat5 = 4;

    @Dropdown(
        name = "Sixth SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat6 = 8; // HP by default

    @Dropdown(
        name = "Seventh SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat7 = 7;

    @Dropdown(
        name = "Eighth SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat8 = 7;

    @Dropdown(
        name = "Ninth SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat9 = 7;

    @Dropdown(
        name = "Tenth SkyWars Stat",
        options = {
            "Team", "Level", "Name", "KDR", "WLR", "Wins", "Kills", "None", "HP",
        },
        category = "Tab Stats",
        subcategory = "SkyWars"
    )
    public int skywarsCustomStat10 = 7;

    @Info(
        text = "Set the order of Duels stats in the tab list",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public static boolean ignoredDuelsStatsOrderInfo;

    @Dropdown(
        name = "First Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat1 = 0;

    @Dropdown(
        name = "Second Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat2 = 1;

    @Dropdown(
        name = "Third Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat3 = 2;

    @Dropdown(
        name = "Fourth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat4 = 3;

    @Dropdown(
        name = "Fifth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat5 = 4;

    @Dropdown(
        name = "Sixth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat6 = 5;

    @Dropdown(
        name = "Seventh Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat7 = 6;

    @Dropdown(
        name = "Eighth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat8 = 7;

    @Dropdown(
        name = "Ninth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat9 = 8;

    @Dropdown(
        name = "Tenth Duels Stat",
        options = {
            "Team",
            "Division",
            "Name",
            "KDR",
            "WLR",
            "Wins",
            "Losses",
            "Kills",
            "Deaths",
            "Winstreak",
            "None",
            "HP",
        },
        category = "Tab Stats",
        subcategory = "Duels"
    )
    public int duelsCustomStat10 = 11; // HP by default

    @Info(
        text = "Toggle seperator between stats",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public static boolean ignoredDotsInfo;

    @Checkbox(
        name = "Between 1st and 2nd",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot12 = false;

    @Checkbox(
        name = "Between 2nd and 3rd",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot23 = false;

    @Checkbox(
        name = "Between 3rd and 4th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot34 = true;

    @Checkbox(
        name = "Between 4th and 5th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot45 = true;

    @Checkbox(
        name = "Between 5th and 6th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot56 = true;

    @Checkbox(
        name = "Between 6th and 7th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot67 = true;

    @Checkbox(
        name = "Between 7th and 8th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot78 = true;

    @Checkbox(
        name = "Between 8th and 9th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot89 = true;

    @Checkbox(
        name = "Between 9th and 10th",
        category = "Tab Stats",
        subcategory = "Seperator"
    )
    public boolean showDot910 = true;

    @HUD(name = "Emerald Counter HUD", category = "HUD")
    public EmeraldCounterHUD emeraldCounterHUD = new EmeraldCounterHUD();

    @HUD(name = "Diamond Counter HUD", category = "HUD")
    public DiamondCounterHUD diamondCounterHUD = new DiamondCounterHUD();

    @HUD(name = "Upgrades & Traps HUD", category = "HUD")
    public BedwarsUpgradesTrapsHUD upgradesTrapsHUD =
        new BedwarsUpgradesTrapsHUD();

    @Number(
        name = "Minimum FKDR to show",
        min = -1,
        max = 500,
        step = 1,
        subcategory = "General"
    )
    public int minFkdr = -1;

    @Dropdown(
        name = "Stats Provider",
        options = { "Hypixel Public API", "Nadeshiko", "Abyss" },
        subcategory = "Stats"
    )
    public int statsProvider = 2;

    @Info(
        text = "Hypixel provider requires an API key from developer.hypixel.net. Other providers do not require a key.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        subcategory = "Stats"
    )
    public static boolean ignoredHypixelApiInfo;

    @Text(
        name = "Hypixel API Key",
        subcategory = "Stats",
        secure = true,
        multiline = false
    )
    public String hypixelApiKey = "";

    @Switch(name = "Print Blacklist Tags in /who", subcategory = "General")
    public boolean printBlacklistTags = true;

    @Switch(name = "Auto Pregame Stats", subcategory = "Pregame")
    public boolean pregameStats = true;

    @Switch(
        name = "Mention Stats in BedWars Lobbies",
        subcategory = "Pregame",
        description = "Show sender stats when they mention your username in BedWars lobby chat."
    )
    public boolean mentionLobbyStats = false;

    @Switch(
        name = "Warn for Blacklisted Party Members",
        subcategory = "Pregame",
        description = "Shows a warning when your Hypixel party contains one or more blacklisted players."
    )
    public boolean partyBlacklistWarning = true;

    @Switch(
        name = "Auto Leave on Blacklisted Chat",
        subcategory = "Pregame",
        description = "Automatically runs /lobby in BedWars pregame when a blacklisted chatter is detected and more than 2 seconds remain."
    )
    public boolean autoLeaveBlacklistedPregameChat = false;

    @Text(
        name = "Auto Leave Command",
        subcategory = "Pregame",
        multiline = false
    )
    public String autoLeaveBlacklistedPregameCommand = "/lobby";

    @Switch(name = "Auto Skin Denicker", subcategory = "Denicker")
    public boolean autoSkinDenick = true;

    // Urchin Configs
    @Info(
        text = "Urchin is a community blacklist, allowing you to see potential cheaters in your game",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Urchin"
    )
    public static boolean ignoredUrchinDescription;

    @Switch(name = "Enable Urchin", category = "Urchin")
    public boolean urchin = false;

    @Switch(name = "Show Urchin Tags in Tab", category = "Urchin")
    public boolean showUrchinTagsInTab = true;

    @Info(
        text = "Enabling Urchin will send requests to them and be subject to their ToS, this could enable tracking of your data (IP, Urchin API Key, Game Info).",
        size = OptionSize.DUAL,
        type = InfoType.WARNING,
        category = "Urchin"
    )
    public static boolean ignoredUrchinWarning;

    @Info(
        text = "Urchin does not require a key to view tags, these settings are deprecated",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Urchin"
    )
    public static boolean ignoredUrchinDeprecated;

    @Text(
        name = "Urchin API Key",
        category = "Urchin",
        secure = true,
        multiline = false
    )
    public String urchinKey = "";

    // Seraph Configs
    @Info(
        text = "Seraph is a community blacklist, allowing you to see potential cheaters in your game",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Seraph"
    )
    public static boolean ignoredSeraphDescription;

    @Switch(name = "Enable Seraph", category = "Seraph")
    public boolean seraph = false;

    @Switch(name = "Show Seraph Tags in Tab", category = "Seraph")
    public boolean showSeraphTagsInTab = true;

    @Info(
        text = "Enabling Seraph will send requests to them and be subject to their ToS, this could enable tracking of your data (IP, Seraph API Key, Game Info).",
        size = OptionSize.DUAL,
        type = InfoType.WARNING,
        category = "Seraph"
    )
    public static boolean ignoredSeraphWarning;

    @Info(
        text = "Seraph does not require a key to view any tags older than 1 week old",
        size = OptionSize.DUAL,
        type = InfoType.INFO,
        category = "Seraph"
    )
    public static boolean ignoredSeraphInfo;

    @Text(
        name = "Seraph API Key",
        category = "Seraph",
        secure = true,
        multiline = false
    )
    public String seraphKey = "";

    // Ping Configs
    @Dropdown(
        name = "Ping Provider",
        category = "Ping",
        options = { "None", "Polsu", "Urchin" }
    )
    public int pingProvider = 0;

    @Info(
        text = "Polsu requires an API key to be able to be used, Urchin does not.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Ping"
    )
    public static boolean ignoredPolsuAPI;

    @Button(
        name = "Polsu API Key",
        text = "Get Key",
        category = "Ping",
        subcategory = "Polsu"
    )
    Runnable polsuLinkButton = () -> {
        NetworkUtils.browseLink("https://polsu.xyz/api/apikey");
    };

    @Text(
        name = "Polsu API Key",
        category = "Ping",
        subcategory = "Polsu",
        secure = true,
        multiline = false
    )
    public String polsuApiKey = "";

    // Number denicker
    @Info(
        text = "This module attempts to denick players based the number of finals and beds broken from chat messages.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Number Denicker"
    )
    public static boolean ignoredNumberDenickerInfo; // Useless. Java limitations with @annotation.

    @Button(
        name = "Run /api view on the bot to get your key",
        text = "Discord Bot",
        size = OptionSize.DUAL,
        category = "Number Denicker"
    )
    Runnable auroraLinkButton = () -> {
        NetworkUtils.browseLink(
            "https://discord.com/oauth2/authorize?client_id=1244205279697174539"
        );
    };

    @Switch(name = "Enable Number Denicker", category = "Number Denicker")
    public boolean numberDenicker = false;

    @Switch(name = "Print all potential players", category = "Number Denicker")
    public boolean numberDenickerFuzzy = true;

    @Info(
        text = "Turning all potential players off, will only print players with both matching beds and finals.",
        type = InfoType.INFO,
        size = OptionSize.DUAL,
        category = "Number Denicker"
    )
    public static boolean ignoredNumberDenickerFuzzyInfo;

    @Text(
        name = "Aurora API Key",
        placeholder = "Enter your Aurora API key",
        category = "Number Denicker",
        secure = true,
        multiline = false
    )
    public String auroraApiKey = "";

    @Dropdown(
        name = "Finals Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Number Denicker"
    )
    public int finalsRange = 3; // Index for 100

    @Dropdown(
        name = "Beds Range",
        options = { "0", "50", "100", "200", "500" },
        category = "Number Denicker"
    )
    public int bedsRange = 1; // Index for 50

    @Number(
        name = "Minimum Finals to Check",
        category = "Number Denicker",
        min = 0,
        max = 500000,
        step = 1000
    )
    public int minFinalsForDenick = 15000;

    @Dropdown(
        name = "Max Results",
        options = { "5", "10", "20" },
        category = "Number Denicker"
    )
    public int maxResults = 0; // Index for 5

    // Hitboxes
    @Switch(name = "Colored Hitboxes", category = "Hitboxes")
    public boolean coloredHitboxes = true;

    @Switch(name = "Affect Vanilla F3+B", category = "Hitboxes")
    public boolean coloredHitboxesAffectVanillaDebug = true;

    @Switch(name = "Affect PolyHitbox", category = "Hitboxes")
    public boolean coloredHitboxesAffectPolyHitbox = true;

    @Switch(name = "Colored Nametag Backgrounds", category = "Hitboxes")
    public boolean coloredNametagBackgrounds = false;

    @Switch(name = "Affect PolyNametag", category = "Hitboxes")
    public boolean coloredNametagAffectPolyNametag = true;

    @Dropdown(
        name = "Hue Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes",
        subcategory = "Hue"
    )
    public int hitboxHueMode = 0;

    @Number(
        name = "Hue Value",
        category = "Hitboxes",
        subcategory = "Hue",
        min = 0,
        max = 360
    )
    public int hitboxHueValue = 0;

    @Number(
        name = "Hue Offset",
        category = "Hitboxes",
        subcategory = "Hue",
        min = -180,
        max = 180
    )
    public int hitboxHueOffset = 0;

    @Dropdown(
        name = "Saturation Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes",
        subcategory = "Saturation"
    )
    public int hitboxSaturationMode = 0;

    @Number(
        name = "Saturation Value",
        category = "Hitboxes",
        subcategory = "Saturation",
        min = 0,
        max = 100
    )
    public int hitboxSaturationValue = 100;

    @Number(
        name = "Saturation Offset",
        category = "Hitboxes",
        subcategory = "Saturation",
        min = -100,
        max = 100
    )
    public int hitboxSaturationOffset = 0;

    @Dropdown(
        name = "Brightness Mode",
        options = { "Offset", "Static" },
        category = "Hitboxes",
        subcategory = "Brightness"
    )
    public int hitboxBrightnessMode = 0;

    @Number(
        name = "Brightness Value",
        category = "Hitboxes",
        subcategory = "Brightness",
        min = 0,
        max = 100
    )
    public int hitboxBrightnessValue = 100;

    @Number(
        name = "Brightness Offset",
        category = "Hitboxes",
        subcategory = "Brightness",
        min = -100,
        max = 100
    )
    public int hitboxBrightnessOffset = 0;

    @Switch(name = "Enable Anticheat", category = "Anticheat")
    public boolean anticheatEnabled = false;

    @Switch(name = "NoSlow Check", category = "Anticheat")
    public boolean noSlowCheckEnabled = true;

    @Switch(name = "AutoBlock Check", category = "Anticheat")
    public boolean autoBlockCheckEnabled = true;

    @Switch(name = "Eagle Check", category = "Anticheat")
    public boolean eagleCheckEnabled = false;

    @Switch(name = "Scaffold Check", category = "Anticheat")
    public boolean scaffoldCheckEnabled = false;

    @Switch(
        name = "Verbose Alerts",
        category = "Anticheat",
        description = "Show detailed anticheat info (debug reason + VL) in alerts."
    )
    public boolean anticheatVerbose = false;

    @Number(
        name = "Violation Level",
        category = "Anticheat",
        min = 1,
        max = 100
    )
    public int anticheatVl = 10;

    @Number(
        name = "Cooldown (seconds)",
        category = "Anticheat",
        min = 1,
        max = 60
    )
    public int anticheatCooldown = 5;

    public MellowOneConfig() {
        super(new Mod(Mellow.NAME, ModType.HYPIXEL), Mellow.MODID + ".json");
        initialize();
        sanitizeDropdownIndexes();

        hideIf("hitboxHueValue", () -> hitboxHueMode == 0);
        hideIf("hitboxHueOffset", () -> hitboxHueMode != 0);
        hideIf("hitboxSaturationValue", () -> hitboxSaturationMode == 0);
        hideIf("hitboxSaturationOffset", () -> hitboxSaturationMode != 0);
        hideIf("hitboxBrightnessValue", () -> hitboxBrightnessMode == 0);
        hideIf("hitboxBrightnessOffset", () -> hitboxBrightnessMode != 0);
    }

    private void sanitizeDropdownIndexes() {
        // BedWars tab stats dropdowns: Team..None (12 options)
        customStat1 = clampIndex(customStat1, 12);
        customStat2 = clampIndex(customStat2, 12);
        customStat3 = clampIndex(customStat3, 12);
        customStat4 = clampIndex(customStat4, 12);
        customStat5 = clampIndex(customStat5, 12);
        customStat6 = clampIndex(customStat6, 12);
        customStat7 = clampIndex(customStat7, 12);
        customStat8 = clampIndex(customStat8, 12);
        customStat9 = clampIndex(customStat9, 12);
        customStat10 = clampIndex(customStat10, 12);

        // SkyWars tab stats dropdowns: Team..None (9 options)
        skywarsCustomStat1 = clampIndex(skywarsCustomStat1, 9);
        skywarsCustomStat2 = clampIndex(skywarsCustomStat2, 9);
        skywarsCustomStat3 = clampIndex(skywarsCustomStat3, 9);
        skywarsCustomStat4 = clampIndex(skywarsCustomStat4, 9);
        skywarsCustomStat5 = clampIndex(skywarsCustomStat5, 9);
        skywarsCustomStat6 = clampIndex(skywarsCustomStat6, 9);
        skywarsCustomStat7 = clampIndex(skywarsCustomStat7, 9);
        skywarsCustomStat8 = clampIndex(skywarsCustomStat8, 9);
        skywarsCustomStat9 = clampIndex(skywarsCustomStat9, 9);
        skywarsCustomStat10 = clampIndex(skywarsCustomStat10, 9);

        // Duels tab stats dropdowns: Team..None (12 options)
        duelsCustomStat1 = clampIndex(duelsCustomStat1, 12);
        duelsCustomStat2 = clampIndex(duelsCustomStat2, 12);
        duelsCustomStat3 = clampIndex(duelsCustomStat3, 12);
        duelsCustomStat4 = clampIndex(duelsCustomStat4, 12);
        duelsCustomStat5 = clampIndex(duelsCustomStat5, 12);
        duelsCustomStat6 = clampIndex(duelsCustomStat6, 12);
        duelsCustomStat7 = clampIndex(duelsCustomStat7, 12);
        duelsCustomStat8 = clampIndex(duelsCustomStat8, 12);
        duelsCustomStat9 = clampIndex(duelsCustomStat9, 12);
        duelsCustomStat10 = clampIndex(duelsCustomStat10, 12);

        // Misc dropdowns
        extendedTabStatsTeamColumnMode = clampIndex(
            extendedTabStatsTeamColumnMode,
            4
        );
        statsProvider = clampIndex(statsProvider, 3);
        pingProvider = clampIndex(pingProvider, 3);
        finalsRange = clampIndex(finalsRange, 5);
        bedsRange = clampIndex(bedsRange, 5);
        maxResults = clampIndex(maxResults, 3);
        hitboxHueMode = clampIndex(hitboxHueMode, 2);
        hitboxSaturationMode = clampIndex(hitboxSaturationMode, 2);
        hitboxBrightnessMode = clampIndex(hitboxBrightnessMode, 2);
    }

    private int clampIndex(int value, int optionCount) {
        if (optionCount <= 0) {
            return 0;
        }
        if (value < 0) {
            return 0;
        }
        int maxIndex = optionCount - 1;
        return value > maxIndex ? maxIndex : value;
    }
}
