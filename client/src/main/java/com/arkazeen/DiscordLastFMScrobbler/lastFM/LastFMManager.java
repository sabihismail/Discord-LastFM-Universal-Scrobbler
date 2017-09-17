package com.arkazeen.DiscordLastFMScrobbler.lastFM;

import com.arkazeen.DiscordLastFMScrobbler.connection.Main;
import com.arkazeen.DiscordLastFMScrobbler.tools.Logging;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is dedicated to managing information being sent to Last.FM using the {@link LastFM} class. This class also
 * constantly updates the {@link com.arkazeen.DiscordLastFMScrobbler.discord.Discord} Game status.
 *
 * @since 1.0
 */
public class LastFMManager {
    /**
     * Minimum amount of seconds that song must be played to Scrobble song.
     */
    private static final int MIN_SECONDS_TO_SCROBBLE = 30;

    /**
     * Time in seconds to update Discord Game status.
     */
    private static final int TIME_TO_UPDATE_DISCORD = 1;

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private String artist = "";
    private String title = "";
    private int length = 0;
    private int timeStart = 0;
    private boolean scrobbled;

    private StringProperty song = new SimpleStringProperty();
    private boolean songUpdatedAlready = false;

    public LastFMManager() {
        enableNowPlayingUpdate();
    }

    /**
     * Takes in information about the song that is currently being played. Primarily checks if song information does
     * not match stored song information and if so, will update LastFM's Now Playing to match that information.
     * <p>
     * If the stored information is the same as what is supplied, the bot will then Scrobble this information to
     * LastFM. If the song does not exist on Last.FM's database and the length is unknown, after
     * {@link #MIN_SECONDS_TO_SCROBBLE}, the song will automatically scrobble.
     *
     * @param artist The artist of the song.
     * @param title  The title of the song.
     * @param length The length of the song in seconds (0 if unknown).
     */
    public void processInformation(String artist, String title, int length) {
        if (songUpdatedAlready) {
            return;
        } else {
            songUpdatedAlready = true;
        }

        if (length == 0) {
            LastFM.Track track = LastFM.getTrackInformation(artist, title);

            if (track != null) {
                length = (int) TimeUnit.MILLISECONDS.toSeconds(track.getLength());
            }
        }

        if (this.artist.equals(artist) && this.title.equals(title) && this.length == length) {
            int timeEnd = (int) (System.currentTimeMillis() / 1000);

            int difference = timeEnd - timeStart;

            if (!scrobbled && difference > MIN_SECONDS_TO_SCROBBLE) {
                if (length > 0 && difference < length / 2) {
                    return;
                }

                try {
                    LastFM.scrobble(artist, title, timeStart, Main.SETTINGS.getSessionKey());

                    Logging.log(artist + " - " + title + " scrobbled.");
                } catch (IOException e) {
                    Logging.logError(new String[]{artist, title, Integer.toString(length)}, e);
                }

                scrobbled = true;
            }
        } else {
            this.artist = artist;
            this.title = title;
            this.length = length;

            scrobbled = false;

            timeStart = (int) (System.currentTimeMillis() / 1000);

            try {
                LastFM.updateNowPlaying(artist, title, Main.SETTINGS.getSessionKey());
            } catch (IOException e) {
                Logging.logError(new String[]{artist, title, Integer.toString(length)}, e);
            }
        }
    }

    /**
     * Queries latest song information from LastFM and sets Discord Game status to that song if
     * {@link com.arkazeen.DiscordLastFMScrobbler.tools.Settings#discordConnectionEnabled} is true.
     */
    private void enableNowPlayingUpdate() {
        executorService.scheduleAtFixedRate(() -> {
            if (Main.SETTINGS.isDiscordConnectionEnabled()) {
                String latestSong = LastFM.getFormattedTrack(Main.SETTINGS.getLastFMName());

                String currentSong = Main.DISCORD.getDiscordSocket().getGame();

                if (currentSong == null || !currentSong.equals(latestSong)) {
                    Main.DISCORD.getDiscordSocket().setGame(latestSong);

                    Platform.runLater(() -> song.set(Main.DISCORD.getDiscordSocket().getGame()));
                }
            }
        }, 0, TIME_TO_UPDATE_DISCORD, TimeUnit.SECONDS);
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public StringProperty getSong() {
        return song;
    }

    public void resetActiveProcess() {
        songUpdatedAlready = false;
    }
}
