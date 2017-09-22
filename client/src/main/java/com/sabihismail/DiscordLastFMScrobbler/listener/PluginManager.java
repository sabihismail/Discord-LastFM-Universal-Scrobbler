package com.sabihismail.DiscordLastFMScrobbler.listener;

import com.sabihismail.DiscordLastFMScrobbler.connection.Main;
import com.sabihismail.DiscordLastFMScrobbler.tools.Constants;
import com.sabihismail.DiscordLastFMScrobbler.tools.GUITools;
import com.sabihismail.DiscordLastFMScrobbler.tools.Logging;
import com.sabihismail.DiscordLastFMScrobbler.tools.Tools;
import com.sabihismail.DiscordLastFMScrobbler.tools.Settings;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages all installed {@link Plugin}s whether they are enabled or disabled.
 * <p>
 * Also constantly checks active processes using the method {@link #processCheck()} and identifies if any music players
 * are active based on the current list of {@link #enabledPlugins}.
 * <p>
 * The main GUI for this class allows for the addition/removal/editing/installing of plugins saved as an instance of
 * {@link Plugin}.
 *
 * @since 1.0
 */
public class PluginManager {
    /**
     * Time in seconds to wait after completion of previous Last.FM music update.
     */
    private static final int TIME_UPDATE_PROCESS = 1;

    /**
     * Time in seconds to wait after completion of previous active process check to identify active music applications
     * and whether music is currently being played on that application.
     */
    private static final int TIME_MUSIC_UPDATE = 1;

    private List<Plugin> enabledPlugins = new ArrayList<>();
    private List<Plugin> disabledPlugins = new ArrayList<>();
    private List<CMDProcess> cmdProcesses = new ArrayList<>();

    private TableView<Plugin> tblEnabledPlugins;
    private TableView<Plugin> tblDisabledPlugins;

    private ScheduledExecutorService executorMusicUpdate = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService executorProcessesUpdate = Executors.newSingleThreadScheduledExecutor();

    /**
     * Begins the timer to repeatedly check for active music applications and retrieve current song information from
     * these applications. Also initiates Last.FM music update request based on the information retrieved from previous
     * timer.
     */
    public PluginManager() {
        executorMusicUpdate.scheduleWithFixedDelay(this::processCheck, 0, TIME_MUSIC_UPDATE, TimeUnit.SECONDS);
        executorProcessesUpdate.scheduleWithFixedDelay(() ->
                cmdProcesses = getProcessList(), 0, TIME_UPDATE_PROCESS, TimeUnit.SECONDS);

        retrieveAllPlugins();
    }

    /**
     * Loads all enabled plugins from the folder designated by {@link Constants#PLUGIN_DIR}.
     * <p>
     * If no plugins exists, the user can decide whether to add a new {@link Plugin} or to disable Last.FM scrobbling
     * functionality.
     * <p>
     * If plugins exist, they will automatically be loaded into {@link #disabledPlugins} and all active plugins denoted
     * by {@link Settings#enabledPlugins} will be added to
     * {@link #enabledPlugins} (in the same order as that list) and removed from {@link #disabledPlugins}.
     */
    private void retrieveAllPlugins() {
        if (enabledPlugins.size() > 0) {
            enabledPlugins.clear();
        }
        if (disabledPlugins.size() > 0) {
            disabledPlugins.clear();
        }

        File pluginDir = new File(Constants.PLUGIN_DIR);
        pluginDir.mkdirs();

        File[] files = pluginDir.listFiles((dir, name) -> name.endsWith(Constants.FILE_EXTENSION));

        if (files == null || files.length == 0) {
            int result = GUITools.showOnTopConfirmDialog("No plugins detected in '" + Constants.PLUGIN_DIR + "'." +
                            "\nWould you like to add one now? " +
                            "\n(Clicking 'No' will disable Last.FM Scrobbling until you add a plugin AND re-enable " +
                            "scrobbling in settings).\nYou can also download plugins from '" + Constants.GITHUB + "'.",
                    "No Plugins Detected");

            if (result == JOptionPane.YES_OPTION) {
                viewAndEditPlugins();
            } else {
                Main.SETTINGS.setScrobblingEnabled(false);
                Main.SETTINGS.saveSettings();

                GUITools.showOnTopMessageDialog("Settings have been saved. Please restart the program for " +
                        "changes to take effect.");

                Main.shutdown();
            }
        }

        Arrays.stream(files).forEach(file -> {
            try {
                disabledPlugins.add(new Plugin(file));
            } catch (IOException e) {
                Logging.logError(new String[]{file.getAbsolutePath()}, e);
            }
        });

        enabledPlugins = Main.SETTINGS.getEnabledPlugins(disabledPlugins);
        disabledPlugins.removeAll(enabledPlugins);
    }

    /**
     * Returns a list of {@link CMDProcess} with information about all current active processes with a window title.
     * This is done through the 'tasklist' command using the command prompt.
     *
     * @return Returns list of current active processes.
     */
    private List<CMDProcess> getProcessList() {
        String[] commands = {System.getenv("windir").replace("\\", "/") + "/system32/" + "tasklist.exe",
                "/v", "/fo", "CSV", "/fi", "\"SESSIONNAME eq Console\"", "/fi", "\"STATUS eq RUNNING\""};

        List<CMDProcess> processes = new ArrayList<>();

        try {
            Process process = Runtime.getRuntime().exec(commands);
            BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), "Cp850"));

            String line;
            while ((line = r.readLine()) != null) {
                line = new String(line.getBytes(), "UTF-8");

                if (!line.endsWith("\"N/A\"") && !line.endsWith("\"Window Title\"")) {
                    CMDProcess cmdProcess = new CMDProcess(line);

                    Predicate<CMDProcess> predicate = predicateProcess ->
                            predicateProcess.getProcessName().equalsIgnoreCase(cmdProcess.getProcessName());

                    if (processes.stream().anyMatch(predicate)) {
                        continue;
                    }

                    processes.add(cmdProcess);
                }
            }
            r.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return processes;
    }

    /**
     * Checks if {@link #cmdProcesses} contains any active music applications that are saved in {@link #enabledPlugins}.
     * <p>
     * This method only updates Now Playing or only sends a Scrobble request for the first active music player in the
     * order of the list {@link #enabledPlugins} to prevent Last.FM API abuse.
     * <p>
     * All {@link Plugin}s in {@link #enabledPlugins} have their {@link Plugin#title} and {@link Plugin#artist} updated
     * automatically regardless of whether their data was sent to Last.FM servers.
     */
    private void processCheck() {
        Main.LAST_FM_MANAGER.resetActiveProcess();

        for (Plugin plugin : enabledPlugins) {
            Optional<CMDProcess> cmdProcess = cmdProcesses.stream()
                    .filter(process -> process.getProcessName().equals(plugin.getProcessName()))
                    .findFirst();

            if (cmdProcess.isPresent()) {
                Pattern pattern = Pattern.compile(plugin.getRegex());
                Matcher matcher = pattern.matcher(cmdProcess.get().getWindowTitle());

                if (matcher.find()) {
                    plugin.setActive(true);

                    String artist = matcher.group(plugin.getArtistGroup());
                    String title = matcher.group(plugin.getTitleGroup());
                    int length = 0;

                    Main.LAST_FM_MANAGER.processInformation(artist, title, length);

                    plugin.setArtist(artist);
                    plugin.setTitle(title);
                } else {
                    plugin.setActive(false);

                    plugin.setTitle("");
                    plugin.setArtist("");
                }
            } else {
                plugin.setActive(false);

                plugin.setTitle("");
                plugin.setArtist("");
            }
        }
    }

    /**
     * Creates a {@link Stage} which displays information about the current active and inactive plugins installed.
     * <p>
     * This {@link Stage} allows for the addition of custom new plugins through regex, the editing of an existing
     * plugin also through regex, or the installation of a specific plugin with no regex.
     * <p>
     * Plugins can also be removed, disabled, and enabled from this menu.
     */
    public void viewAndEditPlugins() {
        Stage stage = new Stage();
        stage.setTitle("Plugin Setup");
        stage.initModality(Modality.APPLICATION_MODAL);

        createEnabledTable();
        createDisabledTable();

        Button btnExit = new Button("Exit");
        btnExit.setOnAction(e -> stage.close());

        Button btnAdd = new Button("Add");
        btnAdd.setOnAction(e -> {
            ButtonType btnInstallPlugin = new ButtonType("Install Plugin");
            ButtonType btnCreatePlugin = new ButtonType("Create Plugin");
            Alert alertAddType = new Alert(Alert.AlertType.CONFIRMATION,
                    "Would you like to install a plugin or create your own?",
                    btnInstallPlugin, btnCreatePlugin);

            Optional<ButtonType> addSelection = alertAddType.showAndWait();
            if (addSelection.isPresent()) {
                ButtonType selected = addSelection.get();

                if (selected == btnInstallPlugin) {
                    showPluginInstaller(stage);
                } else {
                    showPluginCreator(null);
                }
            }
        });

        Button btnEdit = new Button("Edit");
        btnEdit.setOnAction(e -> {
            Plugin selectedPlugin = tblEnabledPlugins.getSelectionModel().getSelectedItem();

            if (selectedPlugin != null) {
                if (selectedPlugin.isActive()) {
                    showPluginCreator(tblEnabledPlugins.getSelectionModel().getSelectedItem());
                } else {
                    GUITools.showOnTopMessageDialog("The process \"" + selectedPlugin.getProcessName() + "\" is not " +
                            "currently on!");
                }
            }
        });

        Button btnRemove = new Button("Remove");
        btnRemove.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

            int index = tblEnabledPlugins.getSelectionModel().getSelectedIndex();
            if (index == -1) {
                index = tblDisabledPlugins.getSelectionModel().getSelectedIndex();

                if (index == -1) {
                    return;
                } else {
                    Plugin selectedPlugin = disabledPlugins.get(index);

                    alert.setContentText("Are you sure you want to delete '" + selectedPlugin.getProcessName() + "'?");

                    Optional<ButtonType> result = alert.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        selectedPlugin.getPluginFile().delete();

                        disabledPlugins.remove(index);

                        tblDisabledPlugins.setItems(FXCollections.observableList(enabledPlugins));
                    }
                }
            }

            Plugin selectedPlugin = enabledPlugins.get(index);

            alert.setContentText("Are you sure you want to delete '" + selectedPlugin.getProcessName() + "'?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                selectedPlugin.getPluginFile().delete();

                enabledPlugins.remove(index);

                tblEnabledPlugins.setItems(FXCollections.observableList(enabledPlugins));
            }
        });

        Button btnEnablePlugin = new Button("↑");
        btnEnablePlugin.setOnAction(e -> {
            Plugin selectedPlugin = tblDisabledPlugins.getSelectionModel().getSelectedItem();
            if (selectedPlugin == null) {
                return;
            }

            disabledPlugins.remove(selectedPlugin);
            enabledPlugins.add(selectedPlugin);

            tblEnabledPlugins.setItems(FXCollections.observableList(enabledPlugins));
            tblDisabledPlugins.setItems(FXCollections.observableList(disabledPlugins));

            Main.SETTINGS.addPlugin(selectedPlugin);
            Main.SETTINGS.saveSettings();
        });

        Button btnDisablePlugin = new Button("↓");
        btnDisablePlugin.setOnAction(e -> {
            Plugin selectedPlugin = tblEnabledPlugins.getSelectionModel().getSelectedItem();
            if (selectedPlugin == null) {
                return;
            }

            enabledPlugins.remove(selectedPlugin);
            disabledPlugins.add(selectedPlugin);

            tblEnabledPlugins.setItems(FXCollections.observableList(enabledPlugins));
            tblDisabledPlugins.setItems(FXCollections.observableList(disabledPlugins));

            Main.SETTINGS.removePlugin(selectedPlugin);
            Main.SETTINGS.saveSettings();
        });

        HBox buttonsCenter = new HBox();
        buttonsCenter.setSpacing(80);
        buttonsCenter.setAlignment(Pos.CENTER);
        buttonsCenter.getChildren().addAll(btnEnablePlugin, btnDisablePlugin);

        HBox buttonsBottomCenter = new HBox();
        buttonsBottomCenter.setSpacing(80);
        buttonsBottomCenter.setAlignment(Pos.CENTER);
        buttonsBottomCenter.getChildren().addAll(btnEdit, btnRemove);

        BorderPane buttonsBottom = new BorderPane();
        buttonsBottom.setLeft(btnExit);
        buttonsBottom.setCenter(buttonsBottomCenter);
        buttonsBottom.setRight(btnAdd);

        BorderPane tableButtonLayout = new BorderPane();
        tableButtonLayout.setTop(tblEnabledPlugins);
        tableButtonLayout.setCenter(buttonsCenter);
        tableButtonLayout.setBottom(tblDisabledPlugins);

        BorderPane all = new BorderPane();
        all.setCenter(tableButtonLayout);
        all.setBottom(buttonsBottom);

        stage.setScene(new Scene(all, 700, -1));

        stage.showAndWait();
    }

    /**
     * Table for enabled plugins. Contains 4 columns, the first which is a checkbox which is enabled when the music
     * application is active, the second for the music application's process name, the third for the title of the
     * current track, and the fourth for the artist of the current track.
     * <p>
     * Horizontal scroll bar is slightly visible when using 0.1, 0.3, 0.3, and 0.3 so to prevent this, the 0.3
     * values have been replaced with 0.299 to allow for no visible horizontal bar.
     */
    private void createEnabledTable() {
        TableColumn<Plugin, Boolean> enabledColumnActive = new TableColumn<>("Active");
        enabledColumnActive.setResizable(false);
        enabledColumnActive.setCellValueFactory(e -> e.getValue().activeProperty());
        enabledColumnActive.setCellFactory(e -> new CheckBoxTableCell<>());

        TableColumn<Plugin, String> enabledColumnProcess = new TableColumn<>("Process Name");
        enabledColumnProcess.setResizable(false);
        enabledColumnProcess.setCellValueFactory(e -> e.getValue().processNameProperty());

        TableColumn<Plugin, String> enabledColumnTitle = new TableColumn<>("Title");
        enabledColumnTitle.setResizable(false);
        enabledColumnTitle.setCellValueFactory(e -> e.getValue().titleProperty());

        TableColumn<Plugin, String> enabledColumnArtist = new TableColumn<>("Artist");
        enabledColumnArtist.setResizable(false);
        enabledColumnArtist.setCellValueFactory(e -> e.getValue().artistProperty());

        tblEnabledPlugins = new TableView<>(FXCollections.observableArrayList(enabledPlugins));
        tblEnabledPlugins.getColumns().addAll(enabledColumnActive, enabledColumnProcess,
                enabledColumnTitle, enabledColumnArtist);

        enabledColumnActive.prefWidthProperty().bind(tblEnabledPlugins.widthProperty().multiply(0.1));
        enabledColumnProcess.prefWidthProperty().bind(tblEnabledPlugins.widthProperty().multiply(0.299));
        enabledColumnTitle.prefWidthProperty().bind(tblEnabledPlugins.widthProperty().multiply(0.299));
        enabledColumnArtist.prefWidthProperty().bind(tblEnabledPlugins.widthProperty().multiply(0.299));
    }

    /**
     * Designated table for disabled plugins. This table only contains the process name column.
     */
    private void createDisabledTable() {
        TableColumn<Plugin, String> disabledColumnProcess = new TableColumn<>("Disabled Plugins");
        disabledColumnProcess.setMinWidth(40);
        disabledColumnProcess.setCellValueFactory(e -> e.getValue().processNameProperty());

        tblDisabledPlugins = new TableView<>(FXCollections.observableArrayList(disabledPlugins));
        tblDisabledPlugins.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblDisabledPlugins.getColumns().addAll(disabledColumnProcess);
    }

    /**
     * Displays the {@link Plugin} creation/editing GUI.
     * <p>
     * All active processes with their window titles are displayed. Regex can be added and tested from this menu.
     *
     * @param selectedPlugin The plugin that is being edited. Is set to 'null' if there is no plugin being edited.
     */
    private void showPluginCreator(Plugin selectedPlugin) {
        Stage stageAdd = new Stage();
        stageAdd.setTitle("Add Plugin");
        stageAdd.initModality(Modality.APPLICATION_MODAL);

        TextField processName = new TextField();
        processName.setPromptText("Process Name");
        processName.setEditable(false);
        TextField regex = new TextField();
        regex.setPromptText("Regular Expression");

        VBox textFields = new VBox();
        textFields.setSpacing(4);
        textFields.getChildren().addAll(processName, regex);

        ComboBox<CMDProcess> comboBox = new ComboBox<>(FXCollections.observableArrayList(cmdProcesses));
        comboBox.setCellFactory(param -> new ListCell<CMDProcess>() {
            @Override
            protected void updateItem(CMDProcess item, boolean empty) {
                super.updateItem(item, empty);

                setText(item == null ? "" : item.getWindowTitle() + " - " + item.getProcessName());
            }
        });
        comboBox.setButtonCell(new ListCell<CMDProcess>() {
            @Override
            protected void updateItem(CMDProcess item, boolean empty) {
                super.updateItem(item, empty);

                setText(item == null ? "" : item.getWindowTitle());
            }
        });
        comboBox.setPromptText("Choose Process");

        comboBox.valueProperty().addListener((observableValue, oldValue, newValue) -> {
            if (observableValue == null || observableValue.getValue() == null) {
                return;
            }

            processName.setText(observableValue.getValue().getProcessName());
        });

        Button btnAddPlugin = new Button("Add Plugin");
        btnAddPlugin.setOnAction(ex ->
                showPluginSaver(stageAdd, comboBox.getButtonCell().getText(), processName.getText(), regex.getText()));

        Button btnCancelPlugin = new Button("Cancel Plugin Add");
        btnCancelPlugin.setOnAction(ex -> stageAdd.close());

        Button btnRefreshProcesses = new Button("Refresh Processes");
        btnRefreshProcesses.setOnAction(ex -> {
            if (comboBox.getSelectionModel().getSelectedItem() == null) {
                return;
            }

            String currentSelectedProcess = comboBox.getSelectionModel().getSelectedItem().getProcessName();

            comboBox.setItems(FXCollections.observableArrayList(cmdProcesses));
            Optional<CMDProcess> updatedCMDProcess = cmdProcesses.stream()
                    .filter(updatedProcess -> updatedProcess.getProcessName().equals(currentSelectedProcess))
                    .findFirst();

            updatedCMDProcess.ifPresent(cmdProcess -> comboBox.getSelectionModel().select(cmdProcess));
        });

        BorderPane buttons = new BorderPane();
        buttons.setRight(btnAddPlugin);
        buttons.setCenter(btnRefreshProcesses);
        buttons.setLeft(btnCancelPlugin);

        BorderPane all = new BorderPane();
        all.setTop(comboBox);
        all.setCenter(textFields);
        all.setBottom(buttons);

        stageAdd.setScene(new Scene(all));
        if (selectedPlugin != null) {
            stageAdd.setOnShown(e -> {
                Optional<CMDProcess> process = cmdProcesses.stream()
                        .filter(ex -> ex.getProcessName().equals(selectedPlugin.getProcessName()))
                        .findFirst();

                process.ifPresent(cmdProcess -> {
                    comboBox.getSelectionModel().select(cmdProcess);

                    regex.setText(selectedPlugin.getRegex());
                });
            });
        }
        stageAdd.showAndWait();
    }

    /**
     * Allows for user to select any plugin file with extension {@link Constants#FILE_EXTENSION} to install. This is the
     * Discord LastFM Scrobbler file extension and is what all files from this program, including plugins, are saved as.
     * <p>
     * If the selected plugin is valid, it will be renamed and copied to the plugin folder, which is
     * {@link Constants#PLUGIN_DIR}.
     *
     * @param stage The {@link Stage} for which the {@link FileChooser} requires to display.
     */
    private void showPluginInstaller(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open " + Constants.FILE_EXTENSION + " file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Discord LastFM Scrobbler files",
                "*" + Constants.FILE_EXTENSION));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null && file.exists()) {
            Plugin plugin;
            try {
                plugin = new Plugin(file);

                File pluginFile = new File(Constants.PLUGIN_DIR + Tools.escapeFile(plugin.getProcessName()) +
                        Constants.FILE_EXTENSION);

                Files.move(file.toPath(), pluginFile.toPath());
            } catch (IOException e) {
                Logging.throwUserError("The file '" + file.getAbsolutePath() + "' is not a valid " +
                        Constants.FILE_EXTENSION + "file as an error occured when reading the file. " +
                        "Please fix the file or recreate the plugin using the plugin creator.");
                return;
            }

            disabledPlugins.add(plugin);
            tblDisabledPlugins.setItems(FXCollections.observableList(disabledPlugins));
        }
    }

    /**
     * Verifies whether the regex matches the process window title.
     * <p>
     * If it does not match or the {@link Matcher#groupCount()} does not equal 2, the method will exit with an error.
     * <p>
     * If it does match, a verification of which group is the artist and title will appear. Upon confirmation, the
     * data will be saved as a {@link Plugin} and it will be added to {@link #disabledPlugins}.
     *
     * @param stage              The {@link Stage} that will be hidden upon successful {@link Plugin} creation.
     * @param processWindowTitle The window title of the selected process.
     * @param processName        The name of the process.
     * @param regex              Regex to test against the processWindowTitle.
     */
    private void showPluginSaver(Stage stage, String processWindowTitle, String processName, String regex) {
        if (processName.equals("") || regex.equals("")) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("You must include regex to capture the artist and the title of the process " +
                    "window title!");

            alert.showAndWait();
        } else {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(processWindowTitle);

            int artistGroup = 0;
            int titleGroup = 0;

            if (matcher.find() && matcher.groupCount() == 2) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setContentText("Is the artist: '" + matcher.group(1) + "' and the title: '" +
                        matcher.group(2) + "'?");

                ButtonType btnYes = new ButtonType("Yes");
                ButtonType btnSwitched = new ButtonType("Other way around");
                ButtonType btnNo = new ButtonType("No");

                alert.getButtonTypes().setAll(btnYes, btnSwitched, btnNo);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent()) {
                    if (result.get() == btnYes) {
                        artistGroup = 1;
                        titleGroup = 2;
                    } else if (result.get() == btnSwitched) {
                        artistGroup = 2;
                        titleGroup = 1;
                    }
                }
            }

            if (artistGroup != 0) {
                try {
                    Plugin.create(processName, regex, artistGroup, titleGroup);
                } catch (IOException e) {
                    Logging.logError(new String[]{processName, regex, Integer.toString(artistGroup),
                            Integer.toString(titleGroup)}, e);
                }

                retrieveAllPlugins();
                tblEnabledPlugins.setItems(FXCollections.observableArrayList(enabledPlugins));
                tblDisabledPlugins.setItems(FXCollections.observableArrayList(disabledPlugins));

                stage.hide();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Unfortunately, your regex is invalid. Please use http://regexr.com/ to " +
                        "test your regex.");

                alert.showAndWait();
            }
        }
    }

    public ScheduledExecutorService getExecutorMusicUpdate() {
        return executorMusicUpdate;
    }

    public ScheduledExecutorService getExecutorProcessesUpdate() {
        return executorProcessesUpdate;
    }
}
