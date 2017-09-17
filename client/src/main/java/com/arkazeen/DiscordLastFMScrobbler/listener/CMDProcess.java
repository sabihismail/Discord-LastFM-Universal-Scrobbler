package com.arkazeen.DiscordLastFMScrobbler.listener;

/**
 * This class is dedicated to storing data about a certain active process.
 *
 * @since 1.0
 */
public class CMDProcess {
    private String processName;
    private String windowTitle;

    /**
     * The constructor accepts a line of text that is returned by the 'tasklist' command in command prompt. The data is
     * separated by commas and is parsed.
     *
     * @param line The comma separated line of text.
     */
    public CMDProcess(String line) {
        String[] split = line.split(",");

        processName = split[0].substring(1, split[0].length() - 1);
        windowTitle = split[split.length - 1].substring(1, split[split.length - 1].length() - 1);
    }

    public String getProcessName() {
        return processName;
    }

    public String getWindowTitle() {
        return windowTitle;
    }
}
