package com.sabihismail.DiscordLastFMScrobbler.discord;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import com.sabihismail.DiscordLastFMScrobbler.tools.Logging;

import java.time.OffsetDateTime;

/**
 * This class creates a Rich Presence to allow for more in-depth details about the song to be displayed.
 *
 * This is optional as opposed to the regular Discord presence.
 * 
 * Requires an application to be made on the Discord website.
 *
 * @since 1.1
 */
// TODO: rename class, presence is spelt wrong
public class DiscordRichPresense {
	private static final long APPLICATION_CLIENT_ID = 0L;
	
    private IPCClient client = null;

    private String game = "";

    /**
     * Creates the Rich Presence client using library (will be replaced with custom client soon).
     */
    public DiscordRichPresense() {
        client = new IPCClient(APPLICATION_CLIENT_ID);

        try {
            client.connect();
        } catch (NoDiscordClientException e) {
            Logging.logError(null, e);
            e.printStackTrace();
        }
    }

    /**
     * Sets the Rich Presence game based on the current LastFM information.
     *
     * @param text Full detail string already using the template at
     * {@link com.sabihismail.DiscordLastFMScrobbler.lastFM.LastFM#TEXT_FORMAT}.
     */
    public void setGame(String text) {
        RichPresence presence = new RichPresence.Builder()
                .setDetails(text)
                .setStartTimestamp(OffsetDateTime.now())
                .build();

        client.sendRichPresence(presence);

        game = presence.toJson().getString("details");
    }

    /**
     * Returns the current game string.
     *
     * @return Returns the current game string.
     */
    public String getGame() {
        return game;
    }

    public void shutdown() {
        client.close();
    }
}
