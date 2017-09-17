package com.arkazeen.DiscordLastFMScrobbler.discord;

import com.arkazeen.DiscordLastFMScrobbler.connection.Main;
import com.arkazeen.DiscordLastFMScrobbler.tools.Constants;
import com.arkazeen.DiscordLastFMScrobbler.tools.Logging;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class manages the connection to Discord servers and verifies if the user's inputted token is valid.
 *
 * The Discord API connection is done through the WebSocket {@link DiscordSocket}.
 *
 * @since 1.0
 */
public class Discord {
    /**
     * Discord API endpoint to send HTTP requests.
     */
    private static final String API_ENDPOINT = "https://discordapp.com/api/";

    /**
     * Discord API endpoint to retrieve gateway url for websocket connection and token verification information.
     */
    private static final String GET_GATEWAY = API_ENDPOINT + "gateway";
    private static final String GET_TOKEN_VERIFICATION = "users/@me";

    /**
     * Discord API gateway version
     */
    private static final String GATEWAY_VERSION = "6";

    private DiscordSocket discordSocket;

    /**
     * Creates a {@link DiscordSocket} connection using the server gateway endpoint retrieved from
     * {@link #getGateway()}.
     *
     * This method is blocking.
     */
    public Discord() {
        try {
            discordSocket = new DiscordSocket(new URI(getGateway()), Main.SETTINGS.getToken());

            discordSocket.connectBlocking();
        } catch (URISyntaxException | InterruptedException e) {
            Logging.logError(new String[]{getGateway(), String.valueOf(validDiscordToken(Main.SETTINGS.getToken()))});
        }
    }

    /**
     * Retrieve {@link URI} to latest websocket gateway to Discord.
     *
     * @return {@link String} gateway endpoint.
     */
    public static String getGateway() {
        HttpClient httpClient = HttpClients.createMinimal();

        HttpGet httpRequest = new HttpGet(GET_GATEWAY);
        httpRequest.addHeader("User-Agent", Constants.USER_AGENT);

        String response = "";
        try {
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            EntityUtils.consume(httpResponse.getEntity());
        } catch (IOException e) {
            Logging.logError(new String[]{GET_GATEWAY, Constants.USER_AGENT}, e);
        }

        String server = "";

        try {
            server = new JSONObject(response).getString("url");
        } catch (JSONException e) {
            Logging.logError(new String[]{GET_GATEWAY, response}, e);
        }

        server += "?v=" + GATEWAY_VERSION + "&encoding=json";

        return server;
    }

    /**
     * Query Discord and verify if token inputted by user is valid. Discord should return a 200 if token is valid and
     * an error code 4XX if the token does not exist.
     *
     * @param token User token inputted on request;
     * @return Whether the user token is a valid token.
     */
    public static boolean validDiscordToken(String token) {
        String url = API_ENDPOINT + GET_TOKEN_VERIFICATION;

        HttpClient httpClient = HttpClients.createMinimal();

        HttpGet httpRequest = new HttpGet(url);
        httpRequest.setHeader("Authorization", token);
        httpRequest.setHeader("User-Agent", Constants.USER_AGENT);

        try {
            HttpResponse httpResponse = httpClient.execute(httpRequest);

            int status = httpResponse.getStatusLine().getStatusCode();

            if (status == 200) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }

        return false;
    }

    public DiscordSocket getDiscordSocket() {
        return discordSocket;
    }
}
