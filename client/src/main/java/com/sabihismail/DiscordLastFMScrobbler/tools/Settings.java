package com.sabihismail.DiscordLastFMScrobbler.tools;

import com.sabihismail.DiscordLastFMScrobbler.listener.Plugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains all settings data relevant to the usage of the main functions of the program.
 * <p>
 * All settings data saves to {@link Constants#SETTINGS_FILE} which by default is named "settings" and is saved with the
 * extension {@link Constants#FILE_EXTENSION}.
 *
 * @since 1.0
 */
public class Settings {
    private boolean discordConnectionEnabled = true;
    private boolean scrobblingEnabled = true;

    private String salt;

    private String lastFMName = "";
    private String lastFMPassword = "";
    private String sessionKey = "";
    private String token = "";
    private List<String> enabledPlugins = new ArrayList<>();

    private boolean encryptionMessage = false;

    /**
     * Checks if settings data file already exists. If the file does exist, the default values will be replaced by the
     * data contained in the file.
     * <p>
     * If the file does not exist then a file will be created storing the default values.
     */
    public Settings() {
        File settingsFile = new File(Constants.SETTINGS_FILE);

        StringBuilder stringBuilder = new StringBuilder();
        try {
            if (!settingsFile.exists()) {
                settingsFile.getParentFile().mkdirs();
                settingsFile.createNewFile();

                salt = new String(Encryption.generateRandomSalt());

                saveSettings();
            } else {
                BufferedReader r = new BufferedReader(new FileReader(settingsFile));
                String s;
                while ((s = r.readLine()) != null) {
                    stringBuilder.append(s);
                }

                try {
                    JSONObject json = new JSONObject(stringBuilder.toString());

                    salt = json.getString("salt");

                    SecretKeySpec key = Encryption.createSecretKey(salt.getBytes());

                    encryptionMessage = !json.isNull("encryptionMessage") && json.getBoolean("encryptionMessage");

                    JSONObject discord = json.getJSONObject("discord");
                    discordConnectionEnabled = discord.getBoolean("discordConnectionEnabled");
                    token = Encryption.decrypt(discord.getString("token"), key);

                    JSONObject lastFM = json.getJSONObject("lastFM");
                    scrobblingEnabled = lastFM.getBoolean("scrobblingEnabled");
                    lastFMName = lastFM.getString("lastFMName");
                    lastFMPassword = Encryption.decrypt(lastFM.getString("lastFMPassword"), key);
                    sessionKey = Encryption.decrypt(lastFM.getString("sessionKey"), key);

                    JSONArray acceptedPluginJSONArray = !json.isNull("enabledPlugins") ?
                            json.getJSONArray("enabledPlugins") : null;
                    if (acceptedPluginJSONArray != null) {
                        for (int i = 0; i < acceptedPluginJSONArray.length(); i++) {
                            enabledPlugins.add(acceptedPluginJSONArray.getString(i));
                        }
                    }
                } catch (JSONException ex) {
                    GUITools.showOnTopMessageDialog("Your settings were corrupt and uneditable. " +
                            "All settings will be reset.");

                    salt = new String(Encryption.generateRandomSalt());

                    discordConnectionEnabled = true;
                    scrobblingEnabled = true;
                    lastFMName = "";
                    lastFMPassword = "";
                    token = "";
                    sessionKey = "";
                    enabledPlugins = new ArrayList<>();

                    encryptionMessage = false;
                }
            }
        } catch (IOException | GeneralSecurityException e) {
            Logging.logError(new String[]{stringBuilder.toString()}, e);
        }
    }

    /**
     * This method will create a {@link JSONObject} that contains all data in an easy to read format. The data will then
     * be saved to the file designated by {@link Constants#SETTINGS_FILE}.
     * <p>
     * All sensitive data will then be encrypted using the {@link Encryption} class.
     */
    public void saveSettings() {
        JSONObject json = new JSONObject();

        try {
            SecretKeySpec key = Encryption.createSecretKey(salt.getBytes());

            json.put("salt", salt);
            json.put("encryptionMessage", encryptionMessage);
            json.put("enabledPlugins", new JSONArray(enabledPlugins));

            JSONObject discord = new JSONObject();
            discord.put("discordConnectionEnabled", discordConnectionEnabled);
            discord.put("token", Encryption.encrypt(token, key));

            JSONObject lastFM = new JSONObject();
            lastFM.put("scrobblingEnabled", scrobblingEnabled);
            lastFM.put("lastFMName", lastFMName);
            lastFM.put("lastFMPassword", Encryption.encrypt(lastFMPassword, key));
            lastFM.put("sessionKey", Encryption.encrypt(sessionKey, key));

            json.put("discord", discord);
            json.put("lastFM", lastFM);

            BufferedWriter w = new BufferedWriter(new FileWriter(Constants.SETTINGS_FILE));
            w.write(json.toString(4));
            w.newLine();
            w.close();
        } catch (IOException | GeneralSecurityException e) {
            Logging.logError(new String[]{enabledPlugins.toString()}, e);
        }
    }

    /**
     * Returns a {@link List<Plugin>} that are considered enabled based on the list of enabled plugins designated by
     * {@link #enabledPlugins}. The list order is the same as {@link #enabledPlugins}.
     *
     * @param disabled The {@link List<Plugin>} that have been read from the folder designated by
     *                 {@link Constants#PLUGIN_DIR}.
     * @return Returns a {@link List<Plugin>} that match the list of plugins designated by {@link #enabledPlugins}.
     */
    public List<Plugin> getEnabledPlugins(List<Plugin> disabled) {
        List<Plugin> enabled = new ArrayList<>();

        outer:
        for (String enabledPlugin : enabledPlugins) {
            for (Plugin disabledPlugin : disabled) {
                if (enabledPlugin.equals(disabledPlugin.getProcessName())) {
                    enabled.add(disabledPlugin);

                    continue outer;
                }
            }
        }

        return enabled;
    }

    public void setViewedEncryptionMessage() {
        encryptionMessage = true;
    }

    public boolean hasViewedEncryptionMessage() {
        return encryptionMessage;
    }

    public boolean isDiscordConnectionEnabled() {
        return discordConnectionEnabled;
    }

    public void setDiscordConnectionEnabled(boolean discordConnectionEnabled) {
        this.discordConnectionEnabled = discordConnectionEnabled;
    }

    public boolean isScrobblingEnabled() {
        return scrobblingEnabled;
    }

    public void setScrobblingEnabled(boolean scrobblingEnabled) {
        this.scrobblingEnabled = scrobblingEnabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

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

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void addPlugin(Plugin plugin) {
        enabledPlugins.add(plugin.getProcessName());
    }

    public void removePlugin(Plugin plugin) {
        enabledPlugins.remove(plugin.getProcessName());
    }
}
