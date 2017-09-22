package com.sabihismail.DiscordLastFMScrobbler.listener;

import com.sabihismail.DiscordLastFMScrobbler.tools.Constants;
import com.sabihismail.DiscordLastFMScrobbler.tools.GUITools;
import com.sabihismail.DiscordLastFMScrobbler.tools.Tools;
import javafx.beans.property.*;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * A {@link Plugin} is created to allow for the capturing of the artist and title from any active process's window
 * title. Each supported music application has it's own {@link Plugin} with the {@link #processName} matching that
 * application's process name.
 * <p>
 * Data parsed includes the {@link #processName} that will be matched against all {@link CMDProcess#processName},
 * {@link #regex} that will be used to identify the current song from the {@link CMDProcess#windowTitle}, the regex
 * group, {@link #artistGroup}, that corresponds to the regex group that will be capturing the select artist from
 * {@link CMDProcess#windowTitle}, and the regex group, {@link #titleGroup}, that corresponds to the regex group which
 * will capture the title from {@link CMDProcess#windowTitle}.
 * <p>
 * This class also contains whether the process is currently active, {@link #active}, the latest processed title,
 * {@link #title}, and the latest processed artist, {@link #artist}.
 *
 * @since 1.0
 */
public class Plugin {
    private File pluginFile;

    private StringProperty processName = new SimpleStringProperty();
    private StringProperty regex = new SimpleStringProperty();
    private IntegerProperty artistGroup = new SimpleIntegerProperty();
    private IntegerProperty titleGroup = new SimpleIntegerProperty();

    private BooleanProperty active = new SimpleBooleanProperty(false);
    private StringProperty title = new SimpleStringProperty();
    private StringProperty artist = new SimpleStringProperty();

    /**
     * Reads a plugin file and processes the information.
     *
     * @param pluginFile The file that is going to be read.
     * @throws IOException   Throws any exceptions caught when reading from the file.
     * @throws JSONException Throws any exceptions caught when parsing the JSON stored in that file.
     */
    public Plugin(File pluginFile) throws IOException, JSONException {
        this.pluginFile = pluginFile;

        List<String> lines = Files.readAllLines(Paths.get(pluginFile.getAbsolutePath()));

        JSONObject obj = new JSONObject(String.join("", lines));
        JSONObject regexObj = obj.getJSONObject("regex");

        processName.set(obj.getString("process"));
        regex.set(regexObj.getString("pattern"));
        artistGroup.set(regexObj.getInt("artistGroup"));
        titleGroup.set(regexObj.getInt("titleGroup"));
    }

    public void setActive(boolean active) {
        this.active.set(active);
    }

    public boolean isActive() {
        return active.get();
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    public int getTitleGroup() {
        return titleGroup.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public void setArtist(String artist) {
        this.artist.set(artist);
    }

    public StringProperty artistProperty() {
        return artist;
    }

    public int getArtistGroup() {
        return artistGroup.get();
    }

    public String getProcessName() {
        return processName.get();
    }

    public StringProperty processNameProperty() {
        return processName;
    }

    public String getRegex() {
        return regex.get();
    }

    public File getPluginFile() {
        return pluginFile;
    }

    /**
     * Creates a {@link Plugin} for the specified process.
     * <p>
     * A {@link JSONObject} is created storing the process name, the regex, the artist regex capture group, and the
     * title regex capture group. This {@link JSONObject} is stored in the file.
     *
     * @param processName The name of the process that the {@link Plugin} corresponds to.
     * @param regex       The regex that will be capturing the artist and title.
     * @param artistGroup The regex capture group corresponding to the capture of the artist.
     * @param titleGroup  The regex capture group corresdponding to the capture of the title.
     * @return Returns a created {@link Plugin} with the information passed in. Please view the {@link Plugin} class
     * information for more information.
     * @throws IOException Throws any exceptions caught when attempting to write the file.
     */
    public static Plugin create(String processName, String regex, int artistGroup, int titleGroup) throws IOException {
        JSONObject regexObj = new JSONObject();
        regexObj.put("pattern", regex);
        regexObj.put("artistGroup", artistGroup);
        regexObj.put("titleGroup", titleGroup);

        JSONObject fullObj = new JSONObject();
        fullObj.put("process", processName);
        fullObj.put("regex", regexObj);

        File pluginFile = new File(Constants.PLUGIN_DIR + Tools.escapeFile(processName) + Constants.FILE_EXTENSION);

        if (pluginFile.exists()) {
            int overwrite = GUITools.showOnTopConfirmDialog("A plugin was already found for " + processName + ". " +
                    "Would you like to overwrite it?", "Overwrite " + pluginFile.getName() + "?");

            if (overwrite == JOptionPane.YES_OPTION) {
                pluginFile.delete();
            }
        }

        pluginFile.createNewFile();

        Files.write(pluginFile.toPath(), fullObj.toString(6).getBytes());

        return new Plugin(pluginFile);
    }
}
