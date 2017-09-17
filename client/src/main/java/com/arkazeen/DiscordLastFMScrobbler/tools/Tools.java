package com.arkazeen.DiscordLastFMScrobbler.tools;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;

/**
 * General tools that are used by multiple classes.
 *
 * @since 1.0
 */
public class Tools {
    /**
     * The {@link UrlValidator} that is used by {@link #checkIfURLIsUp(String)}.
     */
    private static UrlValidator urlValidator = new UrlValidator();

    /**
     * Reads a url and responds with the text contained on that web-page.
     *
     * @param url The url that will be read from.
     * @return Returns the text contained on that url's web-page.
     */
    public static String readURL(String url) {
        HttpClient httpClient = HttpClients.createMinimal();

        String response = "";
        try {
            HttpResponse httpResponse = httpClient.execute(new HttpGet(url));

            response = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            EntityUtils.consume(httpResponse.getEntity());
        } catch (IOException e) {
            Logging.logError(new String[]{url}, e);
        }

        return response;
    }

    /**
     * Checks if a url is a valid web address.
     *
     * @param url The url that is to be checked.
     * @return True if the url is a valid web address.
     */
    public static boolean checkIfURLIsUp(String url) {
        return urlValidator.isValid(url);
    }

    /**
     * Replaces all characters that are not letters (a-z, A-Z) or numbers (0-9) with an underscore. This is done to
     * prevent any file writing errors, specifically the {@link File#createNewFile()} method.
     *
     * @param text The text that is to be formatted.
     * @return Returns the formatted {@link String} that should not cause any problems on {@link File#createNewFile()}.
     */
    public static String escapeFile(String text) {
        return text.replaceAll("[^a-zA-Z0-9]", "_");
    }

    /**
     * Encodes a {@link String} of characters to URL encoding practices. Please view
     * <html>https://www.w3schools.com/tags/ref_urlencode.asp</html> for more information on the escape method.
     *
     * @param text The {@link String} that will be encoded.
     * @return Returns an encoded {@link String} that formats the input to match URL encoding practices.
     */
    public static String escapeURL(String text) {
        String response = "";

        try {
            URL url = new URL(text);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            response = uri.toASCIIString();
        } catch (IOException | URISyntaxException e) {
            Logging.logError(new String[]{text}, e);
        }

        return response;
    }

    /**
     * Returns the current time formatted as '(hh:mm:ss am/pm)'.
     *
     * @return Returns the current time formatted.
     */
    public static String getFormattedTime() {
        Calendar date = Calendar.getInstance();

        String hour = Integer.toString(date.get(Calendar.HOUR));
        String minute = Integer.toString(date.get(Calendar.MINUTE));
        String second = Integer.toString(date.get(Calendar.SECOND));
        String AMPM = date.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";

        if (Integer.parseInt(hour) < 10)
            hour = "0" + hour;
        if (Integer.parseInt(minute) < 10)
            minute = "0" + minute;
        if (Integer.parseInt(second) < 10)
            second = "0" + second;

        return "(" + hour + ":" + minute + ":" + second + " " + AMPM + ") ";
    }
}
