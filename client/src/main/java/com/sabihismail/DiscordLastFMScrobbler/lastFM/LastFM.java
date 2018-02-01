package com.sabihismail.DiscordLastFMScrobbler.lastFM;

import com.sabihismail.DiscordLastFMScrobbler.tools.Logging;
import com.sabihismail.DiscordLastFMScrobbler.tools.Tools;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is dedicated to operations that allow for communication with the Last.FM server.
 * <p>
 * This includes retrieving information, such as the user's latest played tracks, and sending information, such as what
 * track they are currently playing or what song they have completed listening.
 * <p>
 * Additional Information:
 * Scrobble - Last.FM calls a scrobble any song that has been listened to for more than 30 seconds or for more than half
 * of its duration. Scrobbling this data means that the track information and the time when it was played will be stored
 * on their servers.
 * Now Playing - This is considered the active song that is currently being played.
 *
 * @since 1.0
 */
public class LastFM {
    /**
     * Strings required to formulate api url to retrieve information about a specific user's Last.FM data and send
     * data to update the user's data.
     */
    private static final String API_ENDPOINT = "https://ws.audioscrobbler.com/2.0/";
    private static final String GET_TRACK_RECENT = API_ENDPOINT + "?method=user.getrecenttracks";
    private static final String GET_TRACK_INFO = API_ENDPOINT + "?method=track.getInfo";
    private static final String POST_AUTH_MOBILE_SESSION = API_ENDPOINT + "?method=auth.getMobileSession";
    private static final String POST_AUTH_UPDATE_NOW_PLAYING = API_ENDPOINT + "?method=track.updateNowPlaying";
    private static final String POST_AUTH_SCROBBLE = API_ENDPOINT + "?method=track.scrobble";

    private static final String API_FORMAT = "&format=json";
    private static final String API_KEY_PREFIX = "&api_key=";
    private static final String API_USER_PREFIX = "&user=";
    private static final String API_ARTIST_PREFIX = "&artist=";
    private static final String API_TRACK_PREFIX = "&track=";

    /**
     * Last.FM API key and secret.
     */
    private static final String LAST_FM_API_KEY = "e931d11abce57e671e321a8c532744d5";
    private static final String LAST_FM_API_SECRET = "ec947e5c589c18e8cdb1a40d2892e1af";

    /**
     * The desired format of the {@link Track} data when it is converted into a {@link String}.
     * <p>
     * Please view {@link #getFormattedTrack(Track)} for more information.
     */
    private static final String TEXT_FORMAT = "{artist} - {title}";

    /**
     * Queries the Last.FM server for data about a particular song based on the artist name and the title of the track.
     *
     * @param artist The artist of the track.
     * @param title  The title of the track.
     * @return Returns data about a track. View the {@link Track} information for more information about the data
     * contained in this class.
     */
    public static Track getTrackInformation(String artist, String title) {
        String fullURL = GET_TRACK_INFO + API_ARTIST_PREFIX + Tools.escapeURL(artist) + API_TRACK_PREFIX +
                Tools.escapeURL(title) + API_KEY_PREFIX + LAST_FM_API_KEY + API_FORMAT;

        boolean reachable = Tools.checkIfURLIsUp(fullURL);
        if (!reachable) {
            return null;
        }

        String json = Tools.readURL(fullURL);
        if (json.equals("")) {
            return null;
        }

        JSONObject obj = new JSONObject(json).getJSONObject("track");

        artist = obj.getJSONObject("artist").getString("name");
        title = obj.getString("name");
        int length = obj.getInt("duration");

        String album = !obj.isNull("album") ? obj.getJSONObject("album").getString("title") : "";
        List<String> tags = new ArrayList<>();

        JSONArray tagsJSON = obj.getJSONObject("toptags").getJSONArray("tag");
        for (int i = 0; i < tagsJSON.length(); i++) {
            tags.add(tagsJSON.getJSONObject(i).getString("name"));
        }

        return new Track(artist, title, length, album, tags);
    }

    /**
     * Retrieve information about a specific user's current listening information.
     *
     * @param username Last.FM username
     * @return {@link Track} with data pertaining to the latest song being played on Last.FM.
     */
    public static Track getLatestTrackInformation(String username) throws JSONException {
        String fullURL = GET_TRACK_RECENT + API_USER_PREFIX + username + API_KEY_PREFIX + LAST_FM_API_KEY + API_FORMAT;

        boolean reachable = Tools.checkIfURLIsUp(fullURL);
        if (!reachable) {
            return null;
        }

        String json = Tools.readURL(fullURL);

        JSONArray tracks = new JSONObject(json).getJSONObject("recenttracks").getJSONArray("track");
        JSONObject track = tracks.getJSONObject(0);

        String artist = track.getJSONObject("artist").getString("#text");
        String title = track.getString("name");

        return getTrackInformation(artist, title);
    }

    /**
     * Formats a {@link Track} to match {@link #TEXT_FORMAT}.
     * <p>
     * Acceptable placeholder values:
     * {artist} - The artist of the track
     * {title} - The title of the track
     * {length} - The length of the track converted using {@link #getFormattedTime(int)}
     * {album} - The album of the track
     * {tag} - The first available tag. If no tag exists, no tag will be displayed.
     * {tags} - All tags comma separated. If no tags exist, no tag will be displayed.
     *
     * @param track The {@link Track} that is to be formatted.
     * @return The formatted {@link String} that matches the format {@link #TEXT_FORMAT}.
     */
    public static String getFormattedTrack(Track track) {
        if (track == null) {
            return null;
        }

        Pattern p = Pattern.compile("\\{(.*?)}");
        Matcher m = p.matcher(TEXT_FORMAT);

        Map<String, String> map = new HashMap<>();
        map.put("artist", track.getArtist());
        map.put("title", track.getTitle());
        map.put("length", getFormattedTime(track.getLength() / 1000));
        map.put("album", track.getAlbum());
        map.put("tag", track.getTags().size() > 0 ? track.getTags().get(0) : "");
        map.put("tags", String.join(", ", track.getTags()));

        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String replacement = Matcher.quoteReplacement(map.get(m.group(1)));

            m.appendReplacement(sb, replacement);
        }
        m.appendTail(sb);

        System.out.println(sb.toString());

        return sb.toString();
    }

    /**
     * Returns the latest track being played based on the username inputted. The {@link String} format follows the
     * {@link #TEXT_FORMAT}. Please view {@link #getFormattedTrack(Track)} for more information.
     *
     * @param username The user's Last.FM username.
     * @return Returns the formatted {@link String} of the last track that is being played on Last.FM.
     */
    public static String getFormattedTrack(String username) {
        return getFormattedTrack(getLatestTrackInformation(username));
    }

    /**
     * Converts time in seconds to time formatted as 'hh:mm:ss' or 'mm:ss' if the number of hours is equal to 0.
     *
     * @param seconds The seconds to format.
     * @return Returns a {@link String} in format 'hh:mm:ss' or 'mm:ss'.
     */
    private static String getFormattedTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Sends a request to Last.FM to confirm that a song has been listened to for more than 30 seconds or more than
     * half of the duration of that song, as expected by the API rules.
     * <p>
     * The data will then be stored on Last.FM's servers and will be visible on the user's page.
     *
     * @param artist     The artist of the track.
     * @param title      The title of the track.
     * @param album      The album of the track. May be null if the {@link Track} was null.
     * @param timeStamp  The time since Unix epoch in seconds when the song started.
     * @param sessionKey The session key of the specific user.   @return Returns true if the request was successful and if the song has been scrobbled successfully.
     * @throws JSONException Throws any errors in JSON parsing.
     * @throws IOException   Throws any errors relating to connecting or reading the URL.
     */
    public static boolean scrobble(String artist, String title, String album, int timeStamp, String sessionKey)
            throws JSONException, IOException {
        Map<String, String> params = new HashMap<>();
        params.put("artist", artist);
        params.put("track", title);
        params.put("album", album);
        params.put("timestamp", Integer.toString(timeStamp));
        params.put("api_key", LAST_FM_API_KEY);
        params.put("sk", sessionKey);
        params.put("method", "track.scrobble");

        String signature = generateAPISignature(params);
        String md5Signature = md5(signature);

        HttpClient httpClient = HttpClients.createMinimal();
        HttpPost httpPost = new HttpPost(POST_AUTH_SCROBBLE);

        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("artist", artist));
        nameValuePairs.add(new BasicNameValuePair("track", title));
        nameValuePairs.add(new BasicNameValuePair("album", album));
        nameValuePairs.add(new BasicNameValuePair("timestamp", Integer.toString(timeStamp)));
        nameValuePairs.add(new BasicNameValuePair("api_key", LAST_FM_API_KEY));
        nameValuePairs.add(new BasicNameValuePair("api_sig", md5Signature));
        nameValuePairs.add(new BasicNameValuePair("sk", sessionKey));

        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        HttpResponse httpResponse = httpClient.execute(httpPost);

        String responseXML = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
        EntityUtils.consume(httpPost.getEntity());

        JSONObject xmlToJSON = XML.toJSONObject(responseXML);

        String status = xmlToJSON.getJSONObject("lfm").getString("status");

        return status.equals("ok");
    }

    /**
     * Sends a request to the Last.FM server to update active song for the user with the specified session key.
     * <p>
     * Note: A Now Playing request is different from a scrobble.
     *
     * @param artist     The artist of the track.
     * @param title      The title of the track.
     * @param sessionKey The session key of that specific user.
     * @return Returns true if the operation was successful.
     * @throws JSONException Throws any errors in JSON parsing.
     */
    public static boolean updateNowPlaying(String artist, String title, String sessionKey) throws JSONException {
        Map<String, String> params = new HashMap<>();
        params.put("artist", artist);
        params.put("track", title);
        params.put("api_key", LAST_FM_API_KEY);
        params.put("sk", sessionKey);
        params.put("method", "track.updateNowPlaying");

        String signature = generateAPISignature(params);
        String md5Signature = md5(signature);

        HttpClient httpClient = HttpClients.createMinimal();
        HttpPost httpPost = new HttpPost(POST_AUTH_UPDATE_NOW_PLAYING);

        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("artist", artist));
        nameValuePairs.add(new BasicNameValuePair("track", title));
        nameValuePairs.add(new BasicNameValuePair("api_key", LAST_FM_API_KEY));
        nameValuePairs.add(new BasicNameValuePair("api_sig", md5Signature));
        nameValuePairs.add(new BasicNameValuePair("sk", sessionKey));

        String responseXML;
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            HttpResponse httpResponse = httpClient.execute(httpPost);

            responseXML = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            EntityUtils.consume(httpPost.getEntity());
        } catch (IOException e) {
            return false;
        }

        JSONObject xmlToJSON = XML.toJSONObject(responseXML);

        String status = xmlToJSON.getJSONObject("lfm").getString("status");

        return status.equals("ok");
    }

    /**
     * Creates a Last.FM session key allowing for repeat queries to Last.FM servers. This is only necessary for API
     * requests requiring authentication. The session key is created one time and never expires so it is saved in
     * the settings file.
     *
     * @param lastFMName     The user's Last.FM username.
     * @param lastFMPassword The user's Last.FM password.
     * @return Returns the session key corresponding to the
     * @throws JSONException Throws any errors in JSON parsing.
     * @throws IOException   Throws any errors relating to connecting or reading the URL.
     */
    public static String getSessionKey(String lastFMName, String lastFMPassword) throws JSONException, IOException {
        Map<String, String> params = new HashMap<>();
        params.put("api_key", LAST_FM_API_KEY);
        params.put("username", lastFMName);
        params.put("password", lastFMPassword);
        params.put("method", "auth.getMobileSession");

        String signature = generateAPISignature(params);
        String md5Signature = md5(signature);

        HttpClient httpClient = HttpClients.createMinimal();
        HttpPost httpPost = new HttpPost(POST_AUTH_MOBILE_SESSION);

        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("username", lastFMName));
        nameValuePairs.add(new BasicNameValuePair("password", lastFMPassword));
        nameValuePairs.add(new BasicNameValuePair("api_key", LAST_FM_API_KEY));
        nameValuePairs.add(new BasicNameValuePair("api_sig", md5Signature));

        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        HttpResponse httpResponse = httpClient.execute(httpPost);

        String responseXML = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
        EntityUtils.consume(httpPost.getEntity());

        JSONObject xmlToJSON = XML.toJSONObject(responseXML);

        return xmlToJSON.getJSONObject("lfm").getJSONObject("session").getString("key");
    }

    /**
     * Generates a Last.FM API Signature exclusive to the parameters required. This is required for the usage of the
     * Authentication API.
     *
     * @param params The parameters that will be converted into the signature {@link String}. These parameters are first
     *               sorted in ascending order based on their key.
     * @return Returns a combined {@link String} of all sorted key/value pairs.
     */
    private static String generateAPISignature(Map<String, String> params) {
        List<String> parameterNames = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            parameterNames.add(entry.getKey());
            values.add(entry.getValue());
        }

        for (int i = 0; i < parameterNames.size(); i++) {
            for (int j = 1; j < (parameterNames.size() - i); j++) {
                if (parameterNames.get(j - 1).compareTo(parameterNames.get(j)) > 0) {
                    String tempParameterName = parameterNames.get(j - 1);
                    parameterNames.set(j - 1, parameterNames.get(j));
                    parameterNames.set(j, tempParameterName);

                    String tempValue = values.get(j - 1);
                    values.set(j - 1, values.get(j));
                    values.set(j, tempValue);
                }
            }
        }

        StringBuilder preMD5Hash = new StringBuilder();
        for (int i = 0; i < parameterNames.size(); i++) {
            preMD5Hash.append(parameterNames.get(i)).append(values.get(i));
        }
        preMD5Hash.append(LAST_FM_API_SECRET);

        return preMD5Hash.toString();
    }

    /**
     * Hashes the inputted {@link String} in the MD5 algorithm.
     *
     * @param text The {@link String} that will be converted into MD5.
     * @return Returns the MD5 hashed {@link String}.
     */
    private static String md5(String text) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Logging.logError(new String[]{text}, e);
        }

        byte[] messageDigestOutput = messageDigest.digest(text.getBytes());
        BigInteger number = new BigInteger(1, messageDigestOutput);

        StringBuilder md5Hash = new StringBuilder(number.toString(16));
        while (md5Hash.length() < 32) {
            md5Hash.insert(0, "0");
        }

        return md5Hash.toString();
    }

    /**
     * Contains information about the song that was requested from Last.FM's servers.
     * <p>
     * An instance of this class is produced from {@link #getLatestTrackInformation(String)}.
     */
    public static class Track {
        private String artist;
        private String title;
        private int length;
        private String album;
        private List<String> tags;

        public Track(String artist, String title, int length, String album, List<String> tags) {
            this.artist = artist;
            this.title = title;
            this.length = length;
            this.album = album;
            this.tags = tags;
        }

        public String getArtist() {
            return artist;
        }

        public String getTitle() {
            return title;
        }

        public int getLength() {
            return length;
        }

        public String getAlbum() {
            return album;
        }

        public List<String> getTags() {
            return tags;
        }

        @Override
        public String toString() {
            return "Track{" +
                    "artist='" + artist + '\'' +
                    ", title='" + title + '\'' +
                    ", length=" + length +
                    ", album='" + album + '\'' +
                    ", tags=" + tags +
                    '}';
        }
    }
}
