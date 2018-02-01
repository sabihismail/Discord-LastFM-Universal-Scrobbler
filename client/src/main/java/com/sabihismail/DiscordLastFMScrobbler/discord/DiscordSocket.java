package com.sabihismail.DiscordLastFMScrobbler.discord;

import com.sabihismail.DiscordLastFMScrobbler.tools.Constants;
import com.sabihismail.DiscordLastFMScrobbler.tools.Logging;
import com.sabihismail.DiscordLastFMScrobbler.lastFM.LastFM;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class is dedicated to establishing and managing the WebSocket Gateway connection to Discord's servers.
 * <p>
 * This will allow for messages to be received from Discord's server which will then be parsed using the
 * {@link #onMessage(String)} method.
 * <p>
 * Information can also be sent to Discord's servers. In this case, active music updates are sent to Discord's servers
 * which allow for Discord to update the user's active Game status. This will display a status under the user's name
 * similar to 'Playing {{Active Music}}'. The format of music updates is currently set to
 * {@link LastFM#TEXT_FORMAT}.
 *
 * @since 1.0
 */
public class DiscordSocket extends WebSocketClient {
    /**
     * Time in seconds to wait after client sends heartbeat to timeout if heartbeat acknowledgement has not been
     * received.
     */
    private static final int SESSION_TIMEOUT_SECONDS = 5;

    /**
     * Max length of Game status update
     */
    private static final int MAX_STATUS_LENGTH = 128;

    private String token;

    private Timer heartbeat = new Timer();
    private int heartbeatInterval = 0;
    private boolean heartbeatSent = false;
    private long heartbeatSentTime = 0;
    private boolean readyReceived = false;

    private boolean sentIdentify = false;
    private boolean sRecieved = false;
    private int lastS = 0;

    private String game;
    private int lastRequestTime = 0;
    private int requestCount = 0;

    /**
     * Establishes connection to Discord's servers.
     *
     * @param serverUri Discord websocket gateway uri.
     * @param token     User's Discord token.
     */
    public DiscordSocket(URI serverUri, String token) {
        super(serverUri);
        this.token = token;
        this.setConnectionLostTimeout(10000);

        try {
            setSocket(SSLSocketFactory.getDefault().createSocket(serverUri.getHost(), 443));
        } catch (IOException e) {
            Logging.logError(new String[]{serverUri.getHost()}, e);
        }
    }

    /**
     * Data sent by server on connect. HTTP Status Code should be 101 if connection was successful.
     *
     * @param handshakedata Data supplied by server.
     */
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        if (handshakedata.getHttpStatus() != 101) {
            Logging.logError(new String[]{this.getURI().getHost(), Short.toString(handshakedata.getHttpStatus()), handshakedata.getHttpStatusMessage()});
        } else {
            Logging.log("Websocket Connection to Discord Established");
        }
    }

    /**
     * Messages sent by server. Should be in JSON format.
     *
     * @param message Message received from server.
     */
    @Override
    public void onMessage(String message) {
        parseMessage(message);
    }

    /**
     * States code and reason websocket closed.
     *
     * @param code   HTTP close code.
     * @param reason Reason supplied by server.
     * @param remote True if remote host closed connection
     */
    @Override
    public void onClose(int code, String reason, boolean remote) {
        Logging.log("close: (Code = " + code + ") " + reason);
        heartbeat.cancel();
    }

    /**
     * Websocket errors are printed here.
     *
     * @param ex {@link Exception} supplied by websocket.
     */
    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    /**
     * Parse the message sent from the server. Each message contains an op code. Each op code is defined below.
     * <p>
     * op 0:    Other events pertaining to Discord's features (channel creation, etc). Only READY event is parsed.
     * The rest are thrown as they do not affect the bot.
     * op 10:   Hello event from discord supplying heartbeat interval. Interval is stored and timer begins to send
     * KeepAlive events at that interval.
     * op 11:   Heartbeat Accepted event sent from server when heartbeat from bot was received. Also sends client info
     * if that info was not sent before.
     * <p>
     * All other op codes are ignored as they are not relevant to the bot's features.
     * <p>
     * The bot also observes whether the connection has timed out. If the heartbeat was not received, then it will
     * disconnect and attempt to reconnect again.
     *
     * @param message The {@link JSONObject} sent by the client.
     */
    private void parseMessage(String message) {
        JSONObject obj = new JSONObject(message);

        int op = obj.getInt("op");

        if (op == 0) {
            int s = obj.getInt("s");
            String t = obj.getString("t");

            switch (t) {
                case "READY":
                    handleReady(obj);
                    break;
                default:
                    break;
            }

            sRecieved = true;
            lastS = s;
        } else if (op == 10) {
            JSONObject d = obj.getJSONObject("d");

            heartbeatInterval = d.getInt("heartbeat_interval");

            startHeartbeat();
        } else if (op == 11) {
            heartbeatSent = false;

            if (!sentIdentify) {
                sendClientInfo();

                sentIdentify = true;
            }
        }

        if (heartbeatSent) {
            int dif = (int) ((System.currentTimeMillis() - heartbeatSentTime) / 1000);

            if (dif > SESSION_TIMEOUT_SECONDS) {
                closeConnection(CloseFrame.ABNORMAL_CLOSE, "Discord did not respond with heartbeat!");
            }
        }
    }

    /**
     * Handle READY event sent by server.
     *
     * @param obj The {@link JSONObject} sent by the client.
     */
    private void handleReady(JSONObject obj) {
        JSONObject user = obj.getJSONObject("d").getJSONObject("user");

        Logging.log("READY event received. Welcome " +
                user.getString("username") + " (" + user.getString("id") + ")");

        readyReceived = true;
    }

    /**
     * Update user's game status when song changes. Also sends 'idle_since' variable set to {@link JSONObject#NULL}
     * which lets Discord know that the client was never idle.
     *
     * @param name Song title and name retrieved from {@link LastFM};
     */
    public void setGame(String name) {
        JSONObject game = new JSONObject();
        game.put("name", name.length() > MAX_STATUS_LENGTH ? name.substring(0, MAX_STATUS_LENGTH - 1) : name);
        game.put("type", 1);

        JSONObject d = new JSONObject();
        d.put("game", game);
        d.put("since", 0);
        d.put("status", "online");
        d.put("afk", false);

        JSONObject obj = new JSONObject();
        obj.put("op", 3);
        obj.put("d", d);

        int currentRequestTime = ((int) (System.currentTimeMillis() / (1000 * 60)));
        if (currentRequestTime > lastRequestTime) {
            requestCount = 0;

            lastRequestTime = currentRequestTime;
        }

        if (requestCount < 5) {
            requestCount++;

            send(obj.toString());

            this.game = name;
        }
    }

    /**
     * Begin sending KeepAlive payloads to ensure the connection remains established.
     */
    private void startHeartbeat() {
        heartbeat.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                JSONObject obj = new JSONObject();
                obj.put("op", 1);
                obj.put("d", sRecieved ? lastS : JSONObject.NULL);

                send(obj.toString());

                heartbeatSent = true;
                heartbeatSentTime = System.currentTimeMillis();
            }
        }, 0, heartbeatInterval);
    }

    /**
     * Send information about the bot such as the user's discord token. Also contains some metadata.
     */
    private void sendClientInfo() {
        JSONObject properties = new JSONObject();
        properties.put("$os", "windows");
        properties.put("$browser", Constants.NAME);
        properties.put("$device", Constants.NAME);
        properties.put("$referrer", "");
        properties.put("$referring_domain", "");

        JSONObject d = new JSONObject();
        d.put("token", token);
        d.put("properties", properties);
        d.put("compress", false);
        d.put("large_threshold", 50);

        JSONObject obj = new JSONObject();
        obj.put("op", 2);
        obj.put("d", d);

        send(obj.toString());
    }

    /**
     * Blocks thread until websocket connection is established and Discord returns READY event which ensures the user
     * connection has been established as well.
     *
     * @return Returns true when READY event was received.
     * @throws InterruptedException Thrown when the thread get interrupted.
     */
    public boolean connectBlocking() throws InterruptedException {
        super.connectBlocking();

        while (!readyReceived) {
            Thread.sleep(200);
        }

        return readyReceived;
    }

    /**
     * Close websocket normally when executed by user.
     */
    public void closeWebsocket() {
        closeConnection(1000, "Close request executed by user.");
    }

    public String getGame() {
        return game;
    }
}
