package com.sabihismail.DiscordLastFMScrobbler.connection;

/**
 * This class's purpose is to contain account information about LastFM and Discord.
 *
 * @since 1.0
 */
public class AccountInfo {
    private String lastFMName = "";
    private String lastFMPassword = "";
    private String lastFMSessionKey = "";
    private String discordToken = "";

    public String getLastFMName() {
        return lastFMName;
    }

    public void setLastFMName(String lastFMName) {
        this.lastFMName = lastFMName;
    }

    public String getLastFMPassword() {
        return lastFMPassword;
    }

    public void setLastFMPassword(String lastFMPassword) {
        this.lastFMPassword = lastFMPassword;
    }

    public String getLastFMSessionKey() {
        return lastFMSessionKey;
    }

    public void setLastFMSessionKey(String lastFMSessionKey) {
        this.lastFMSessionKey = lastFMSessionKey;
    }

    public String getDiscordToken() {
        return discordToken;
    }

    public void setDiscordToken(String discordToken) {
        this.discordToken = discordToken;
    }
}
