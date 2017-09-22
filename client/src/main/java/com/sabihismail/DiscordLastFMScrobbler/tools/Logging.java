package com.sabihismail.DiscordLastFMScrobbler.tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * This class is dedicated to the general logging of the class, whether that be visible to a user, a general log file,
 * or a log containing errors with information relating to that error.
 *
 * @since 1.0
 */
public class Logging {
    /**
     * The save file for the log.
     */
    public static final String LOGGING_FILE_NAME = "log.txt";

    /**
     * Catches any error and prints a logfile that contains the specified printStackTrace().
     * Tells user where to locate logfile and what email address/Discord username to send this file to.
     *
     * @param relevantInformation The information relevant to when the error occurred.
     * @param e                   Any given exception to be converted to log file.
     */
    public static void logError(String[] relevantInformation, Exception e) {
        try {
            File f = new File(LOGGING_FILE_NAME);
            if (!f.exists())
                f.createNewFile();

            if (e != null) {
                PrintStream ps = new PrintStream(f);
                e.printStackTrace(ps);
                ps.close();
                e.printStackTrace();
            }

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\n").append(Tools.getFormattedTime()).append("\n");

            stringBuilder.append("New Information");
            for (String info : relevantInformation) {
                stringBuilder.append(info).append("\n");
            }

            Files.write(Paths.get(f.getAbsolutePath()), stringBuilder.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Logs any information as an error without an {@link Exception}.
     *
     * @param relevantInformation The information relevant to when the error occurred.
     */
    public static void logError(String[] relevantInformation) {
        logError(relevantInformation, null);
    }

    /**
     * Logs a message to the console and also prints the text into a log file {@link #LOGGING_FILE_NAME}.
     *
     * @param msg The text that is to be logged.
     */
    public static void log(String msg) {
        msg = Tools.getFormattedTime() + msg;

        try {
            File f = new File(LOGGING_FILE_NAME);
            if (!f.exists())
                f.createNewFile();

            if (!msg.endsWith("\n"))
                msg += "\n";

            System.out.print(msg);
            Files.write(Paths.get(f.getAbsolutePath()), msg.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Throw error that should display pertinent information about the error to the user.
     *
     * @param error The error message.
     */
    public static void throwUserError(String error) {
        GUITools.showOnTopMessageDialog(error);

        System.exit(0);
    }
}
