package com.sabihismail.DiscordLastFMScrobbler.connection;

import com.sabihismail.DiscordLastFMScrobbler.discord.Discord;
import com.sabihismail.DiscordLastFMScrobbler.discord.DiscordRichPresense;
import com.sabihismail.DiscordLastFMScrobbler.discord.DiscordSocket;
import com.sabihismail.DiscordLastFMScrobbler.lastFM.LastFM;
import com.sabihismail.DiscordLastFMScrobbler.lastFM.LastFMManager;
import com.sabihismail.DiscordLastFMScrobbler.listener.PluginManager;
import com.sabihismail.DiscordLastFMScrobbler.tools.Constants;
import com.sabihismail.DiscordLastFMScrobbler.tools.GUITools;
import com.sabihismail.DiscordLastFMScrobbler.tools.Logging;
import com.sabihismail.DiscordLastFMScrobbler.tools.Settings;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.JSONException;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Main class which contains the GUI created in JavaFX.
 * <p>
 * This class contains the objects {@link Discord} and {@link LastFMManager} which manage retrieving and sending
 * information to Discord and LastFM. The {@link PluginManager} for active music identification is also contained
 * in this class. {@link Settings} is the object which contains all information about bot preferences and user
 * information.
 * <p>
 * Other classes frequently utilize these above classes which is why the classes are statically accessible.
 *
 * @since 1.0
 */
public class Main extends Application {
    public static boolean BOT_ENABLED = true;

    public static Settings SETTINGS;
    public static Discord DISCORD;
    public static DiscordRichPresense DISCORD_RICH_PRESENSE;
    public static PluginManager PLUGIN_MANAGER;
    public static LastFMManager LAST_FM_MANAGER;

    private Stage stage;

    private double stageHeight = 0;
    private double songHeight = 0;

    /**
     * All major class object components ({@link Settings}, {@link Discord}, {@link PluginManager}, and
     * {@link LastFMManager}) will all be initialized in this method.
     *
     * @throws Exception JavaFX's {@link Application#init()} throws all {@link Exception} by default.
     */
    @Override
    public void init() throws Exception {
        super.init();

        // preparation (set UI and check if user has used bot before and is now updated)
        matchSwingUIWithWindowsUI();
        moveOldFolderToNew();

        SETTINGS = new Settings();

        oneTimeConfiguration();

        // verify existing account info located in settings file
        AccountInfo accountInfo = verificationOfExistingFiles(SETTINGS.getLastFMName(), SETTINGS.getLastFMPassword(), SETTINGS.getToken());

        SETTINGS.setLastFMName(accountInfo.getLastFMName());
        SETTINGS.setLastFMPassword(accountInfo.getLastFMPassword());
        SETTINGS.setSessionKey(accountInfo.getLastFMSessionKey());
        SETTINGS.setToken(accountInfo.getDiscordToken());
        SETTINGS.saveSettings();

        if (SETTINGS.isRichPresenseEnabled()) {
            DISCORD_RICH_PRESENSE = new DiscordRichPresense();
        }

        if (SETTINGS.isDiscordConnectionEnabled()) {
            DISCORD = new Discord();
        }

        LAST_FM_MANAGER = new LastFMManager();
    }

    /**
     * The main {@link Stage} that displays song information and access to {@link PluginManager} and {@link Settings}
     * GUI components through the buttons. Also allows for bot to be enabled/disabled.
     *
     * @param stage The display window.
     * @throws Exception JavaFX's {@link Application#start(Stage)} throws all {@link Exception} by default.
     */
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;

        // Components
        Button btnSettings = new Button("Settings");
        btnSettings.setOnAction(e -> onSettingsClicked());

        Label lblBotEnabled = new Label("Bot is " + (BOT_ENABLED ? "ON" : "OFF"));
        lblBotEnabled.setFont(new Font("Calibri", 38));
        lblBotEnabled.setWrapText(true);

        String enabledComponentsText = "Discord " +
                (SETTINGS.isRichPresenseEnabled() ? "Rich " : "") +
                "Presence Update " +
                (SETTINGS.isDiscordConnectionEnabled() ? "enabled" : "disabled") + ".\n" +
                "Last.FM Scrobbling " + (SETTINGS.isScrobblingEnabled() ? "enabled" : "disabled") + ".";
        Label lblComponentsEnabled = new Label(enabledComponentsText);
        lblComponentsEnabled.setFont(new Font("Calibri", 20));
        lblComponentsEnabled.setWrapText(true);

        Button btnEnableBot = new Button("Enable Bot");
        btnEnableBot.setOnAction(e -> {
            BOT_ENABLED = true;

            lblBotEnabled.setText("Bot is ON");
        });

        Button btnDisableBot = new Button("Disable Bot");
        btnDisableBot.setOnAction(e -> {
            BOT_ENABLED = false;

            lblBotEnabled.setText("Bot is OFF");
        });

        Button btnViewPlugins = new Button("Plugins");
        btnViewPlugins.setOnAction(e -> PLUGIN_MANAGER.viewAndEditPlugins());

        Label lblSongInfo = new Label("Current song is: ");
        lblSongInfo.setFont(new Font("Calibri", 22));
        lblSongInfo.setTextAlignment(TextAlignment.CENTER);
        lblSongInfo.setWrapText(true);

        Label lblSong = new Label();
        lblSong.setFont(Font.font("Calibri", FontWeight.BOLD, 22));
        lblSong.setTextAlignment(TextAlignment.CENTER);
        lblSong.setWrapText(true);
        lblSong.textProperty().bind(LAST_FM_MANAGER.getSong());

        // Layouts
        VBox lytTopText = new VBox(10);
        lytTopText.setAlignment(Pos.CENTER);
        lytTopText.getChildren().addAll(lblBotEnabled, lblComponentsEnabled);

        VBox lytButtons = new VBox(6);
        lytButtons.setAlignment(Pos.CENTER);
        lytButtons.getChildren().addAll(btnEnableBot, btnDisableBot, btnViewPlugins, btnSettings);

        VBox lytSong = new VBox(4);
        lytSong.heightProperty().addListener((a, b, c) -> {
            if (stageHeight == 0 || songHeight == 0) {
                return;
            }

            stage.setHeight(stageHeight + (a.getValue().doubleValue() - songHeight));
        });
        lytSong.setAlignment(Pos.CENTER);
        lytSong.getChildren().addAll(lblSongInfo, lblSong);

        BorderPane lytAll = new BorderPane();
        lytAll.setTop(lytTopText);
        lytAll.setCenter(lytButtons);
        lytAll.setBottom(lytSong);

        stage.setTitle("Discord Last.FM Scrobbler");
        stage.setScene(new Scene(lytAll));
        stage.setOnCloseRequest(e -> shutdown());
        stage.centerOnScreen();

        stage.show();

        stageHeight = stage.getHeight();
        songHeight = lytSong.getHeight();

        if (SETTINGS.isScrobblingEnabled()) {
            PLUGIN_MANAGER = new PluginManager();
        }
    }

    /**
     * Creates a {@link Stage} which allows for editing of some components stored in {@link Settings}, specifically
     * sensitive data like Last.FM username/password and Discord token.
     */
    private void onSettingsClicked() {
        Stage settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.initOwner(stage);
        settingsStage.setTitle("Settings");

        Label lastFMNameLabel = new Label("LastFM Username");
        Label lastFMPasswordLabel = new Label("LastFM Password");
        Label tokenLabel = new Label("Discord Token");

        TextField lastFMNameField = new TextField(SETTINGS.getLastFMName());
        PasswordField lastFMPasswordField = new PasswordField();
        lastFMPasswordField.setText(SETTINGS.getLastFMPassword());
        TextField tokenField = new TextField(SETTINGS.getToken());

        CheckBox enableDiscordConnect = new CheckBox("Enable Regular Discord Connection");
        CheckBox enableRichPresence = new CheckBox("Enable Discord Rich Presence");

        enableDiscordConnect.setAllowIndeterminate(false);
        enableDiscordConnect.setOnMouseClicked(e -> {
            if (enableDiscordConnect.isSelected()) {
                enableRichPresence.setSelected(false);
                lastFMNameField.setDisable(false);
                lastFMPasswordField.setDisable(false);
                tokenField.setEditable(true);
            } else {
                enableRichPresence.setSelected(true);
                lastFMNameField.setDisable(true);
                lastFMPasswordField.setDisable(true);
                tokenField.setEditable(false);
            }
        });
        enableDiscordConnect.setSelected(SETTINGS.isDiscordConnectionEnabled());

        enableRichPresence.setAllowIndeterminate(false);
        enableRichPresence.setOnMouseClicked(e -> {
            if (enableRichPresence.isSelected()) {
                enableDiscordConnect.setSelected(false);
                lastFMNameField.setDisable(true);
                lastFMPasswordField.setDisable(true);
                tokenField.setEditable(false);
            } else {
                enableDiscordConnect.setSelected(true);
                lastFMNameField.setDisable(false);
                lastFMPasswordField.setDisable(false);
                tokenField.setEditable(true);
            }
        });
        enableRichPresence.setSelected(SETTINGS.isRichPresenseEnabled());
        enableRichPresence.getOnMouseClicked().handle(null);

        CheckBox enableScrobbling = new CheckBox("Enable Last.FM Scrobbling");
        enableScrobbling.setAllowIndeterminate(false);
        enableScrobbling.setSelected(SETTINGS.isScrobblingEnabled());

        Button btnSave = new Button("Save");
        btnSave.setOnAction(ex -> {
            SETTINGS.setDiscordConnectionEnabled(enableDiscordConnect.isSelected());
            SETTINGS.setRichPresenseEnabled(enableRichPresence.isSelected());
            SETTINGS.setScrobblingEnabled(enableScrobbling.isSelected());

            AccountInfo savedAccountInfo = verificationOfExistingFiles(lastFMNameField.getText(),
                    lastFMPasswordField.getText(), tokenField.getText());

            SETTINGS.setLastFMName(savedAccountInfo.getLastFMName());
            SETTINGS.setLastFMPassword(savedAccountInfo.getLastFMPassword());
            SETTINGS.setSessionKey(savedAccountInfo.getLastFMSessionKey());
            SETTINGS.setToken(savedAccountInfo.getDiscordToken());
            SETTINGS.saveSettings();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("Settings have been saved. Please restart the program for changes to take effect.");

            alert.showAndWait();

            shutdown();
        });

        Button btnReset = new Button("Reset");
        btnReset.setOnAction(ex -> {
            enableDiscordConnect.setSelected(SETTINGS.isDiscordConnectionEnabled());
            enableRichPresence.setSelected(SETTINGS.isRichPresenseEnabled());
            enableScrobbling.setSelected(SETTINGS.isScrobblingEnabled());
            lastFMNameField.setText(SETTINGS.getLastFMName());
            lastFMPasswordField.setText(SETTINGS.getLastFMPassword());
            tokenField.setText(SETTINGS.getToken());
        });

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(ex -> settingsStage.close());

        int spacingBetweenNodes = 3;
        int spacingBetweenLayouts = 6;

        VBox lastFMLayout = new VBox();
        lastFMLayout.setSpacing(spacingBetweenNodes);
        lastFMLayout.getChildren().addAll(lastFMNameLabel, lastFMNameField, lastFMPasswordLabel, lastFMPasswordField);

        VBox tokenLayout = new VBox();
        tokenLayout.setSpacing(spacingBetweenNodes);
        tokenLayout.getChildren().addAll(tokenLabel, tokenField);

        VBox allLayouts = new VBox();
        allLayouts.setSpacing(spacingBetweenLayouts);
        allLayouts.getChildren().addAll(lastFMLayout, tokenLayout, enableDiscordConnect, enableRichPresence,
                enableScrobbling);

        BorderPane buttonLayout = new BorderPane();
        buttonLayout.setLeft(btnCancel);
        buttonLayout.setCenter(btnSave);
        buttonLayout.setRight(btnReset);

        BorderPane finalLayout = new BorderPane();
        finalLayout.setCenter(allLayouts);
        finalLayout.setBottom(buttonLayout);

        settingsStage.setScene(new Scene(finalLayout, 500, 270));

        settingsStage.show();
    }

    /**
     * Configures program on first time run. Currently just displays dialog to user explaining that all sensitive data
     * is encrypted before being stored.
     */
    private void oneTimeConfiguration() {
        if (!SETTINGS.hasViewedEncryptionMessage()) {
            GUITools.showOnTopMessageDialog("When supplying information, please know that the Last.FM password, " +
                    "Last.FM session key, and Discord token are encrypted in a salted AES-128 encrypted string.\n" +
                    "The source can be viewed at '" + Constants.GITHUB + "'.");

            SETTINGS.setViewedEncryptionMessage();
            SETTINGS.saveSettings();
        }
    }

    /**
     * Verifies validity of LastFM credentials and Discord token. If verification fails, the program will endlessly loop
     * until valid credentials are inputted.
     *
     * @param lastFMName     Inputted LastFM username. Can be set to default value in {@link Settings#lastFMName}.
     * @param lastFMPassword The password for the aforementioned LastFM username.
     * @param token          The Discord user token.
     * @return Returns a final data class containing information about the LastFM account and also the Discord token.
     */
    private AccountInfo verificationOfExistingFiles(String lastFMName, String lastFMPassword, String token) {
        AccountInfo accountInfo = new AccountInfo();

        boolean accountExists = false;
        while (!accountExists) {
            try {
                String sessionKey = LastFM.getSessionKey(lastFMName, lastFMPassword);

                accountInfo.setLastFMName(lastFMName);
                accountInfo.setLastFMPassword(lastFMPassword);
                accountInfo.setLastFMSessionKey(sessionKey);

                accountExists = true;
            } catch (JSONException | IOException e) {
                lastFMName = GUITools.showOnTopInputDialog("Please enter a valid Last.FM username.");
                lastFMPassword = GUITools.showPasswordInputDialog("Last.FM Password", "Please enter a valid Last.FM password.");

                if (lastFMName.equals("") || lastFMPassword.equals("")) {
                    close();
                }
            }
        }

        if (SETTINGS.isDiscordConnectionEnabled()) {
            boolean loggedIn = false;
            while (!loggedIn) {
                if (token != null) {
                    token = token.replace(" ", "");

                    if (token.startsWith("\"")) {
                        token = token.substring(1);
                    }

                    if (token.endsWith("\"")) {
                        token = token.substring(0, token.length() - 1);
                    }
                }

                if (token == null || !token.equalsIgnoreCase("SKIP")) {
                    boolean success = false;

                    if (token != null) {
                        success = Discord.validDiscordToken(token);
                    }

                    if (!success) {
                        token = GUITools.showOnTopInputDialog("Your Discord token is not set!\n" +
                                "Go to your Discord, click CTRL + SHIFT + I and click the double arrow on the same " +
                                "row as 'Elements' and 'Console'.\n" +
                                "Click 'Application'. Under the 'Storage' section, click the right facing arrow " +
                                "pointing right beside 'Local Storage'.\n" +
                                "Click 'https://discordapp.com' and find the key 'token'.\n" +
                                "Double click corresponding value and copy it.\n" +
                                "Paste that into the dialog box below the quotations.\n\n" +
                                "Type 'SKIP' if you DO NOT want to set up the Discord connection at this time.\n" +
                                "You can change your decision later on.");
                    } else {
                        accountInfo.setDiscordToken(token);

                        loggedIn = true;
                    }
                } else if (token.equalsIgnoreCase("SKIP")) {
                    SETTINGS.setDiscordConnectionEnabled(false);
                    SETTINGS.setRichPresenseEnabled(false);
                    SETTINGS.saveSettings();

                    loggedIn = true;
                }
            }
        }

        return accountInfo;
    }

    /**
     * Notifies user that the program will close.
     */
    public static void close() {
        JOptionPane.showMessageDialog(null, "The program will now close.");
        System.exit(0);
    }

    /**
     * Moves all old folder data to the latest data folder defined by {@link Constants#SETTINGS_FILE}.
     */
    private void moveOldFolderToNew() {
        File newDir = new File(Constants.SETTINGS_FILE);
        newDir.getParentFile().mkdirs();

        File oldDir = new File(Constants.SETTINGS_DIR_OLD);
        if (oldDir.exists()) {
            try {
                Files.move(Paths.get(oldDir.toURI()), Paths.get(newDir.toURI()), StandardCopyOption.REPLACE_EXISTING);

                oldDir.getParentFile().delete();
                oldDir.getParentFile().getParentFile().delete();
            } catch (IOException e) {
                Logging.logError(new String[]{oldDir.getAbsolutePath(), newDir.getAbsolutePath()}, e);
            }
        }
    }

    /**
     * Shutdowns all components of the program upon stopping all {@link Timer} and disconnecting the websocket from
     * {@link DiscordSocket}.
     */
    public static void shutdown() {
        if (PLUGIN_MANAGER != null) {
            PLUGIN_MANAGER.getExecutorMusicUpdate().shutdownNow();
            PLUGIN_MANAGER.getExecutorProcessesUpdate().shutdownNow();
        }

        if (DISCORD_RICH_PRESENSE != null) {
            DISCORD_RICH_PRESENSE.shutdown();
        }

        if (DISCORD != null) {
            DISCORD.getDiscordSocket().closeWebsocket();
        }

        LAST_FM_MANAGER.getExecutorService().shutdown();

        Platform.exit();
    }

    /**
     * Uses {@link UIManager#setLookAndFeel(String)} to allow for {@link javax.swing} components to have a native
     * appearance.
     */
    private void matchSwingUIWithWindowsUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            Logging.logError(new String[]{"UIManager is not supported here... skipping."}, e);
        }
    }

    /**
     * Launches JavaFX Application.
     *
     * @param args Unused arguments.
     */
    public static void main(String[] args) throws Exception {
        launch(args);
    }
}
