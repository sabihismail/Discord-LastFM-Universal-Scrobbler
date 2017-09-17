package com.arkazeen.DiscordLastFMScrobbler.tools;

import com.arkazeen.DiscordLastFMScrobbler.connection.Main;

/**
 * General program constants that are accessed from multiple classes, for example the name of the program and the
 * global save file extension.
 *
 * Keeping these constants in one file allows for ease of editing constants at once.
 *
 * @since 1.0
 */
public class Constants {
    /**
     * Base directory locations used by other variables.
     */
    private static final String HOME_DIR = System.getProperty("user.home").replace("\\", "/");
    private static final String PROJECT_HOME_DIR = HOME_DIR + "/ArkaPrime/DiscordLastFMScrobbler/";

    /**
     * Old save directory. On startup, will copy from this directory to the new directory automatically.
     *
     * Please view {@link Main#moveOldFolderToNew()} to view the code for this directory moving.
     */
    public static final String SETTINGS_DIR_OLD = HOME_DIR + "/AppData/Local/ArkaZeen/DiscordLastFMScrobbler/";

    /**
     * The designated file extension for all files created by this program.
     */
    public static final String FILE_EXTENSION = ".dlfs";

    /**
     * The save file for the settings file used by {@link Settings}.
     */
    public static final String SETTINGS_FILE = PROJECT_HOME_DIR + "settings" + FILE_EXTENSION;

    /**
     * The save directory of all plugins.
     */
    public static final String PLUGIN_DIR = PROJECT_HOME_DIR + "plugins/";

    /**
     * General bot and creator information.
     */
    public static final String NAME = "Discord Last.FM Universal Scrobbler";
    private static final String VERSION = "0.2";
    public static final String USER_AGENT = NAME + " (N/A, v" + VERSION + ")";
    public static final String GITHUB = "https://github.com/sabihismail/Discord-LastFM-Universal-Scrobbler/";
}
