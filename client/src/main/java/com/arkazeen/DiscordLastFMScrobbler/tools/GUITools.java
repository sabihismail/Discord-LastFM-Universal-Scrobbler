package com.arkazeen.DiscordLastFMScrobbler.tools;

import javax.swing.*;
import java.awt.*;

/**
 * This class allows for custom {@link JOptionPane} implementations to be easily instantiated.
 *
 * @since 1.0
 */
public class GUITools {
    /**
     * This method allows for a custom password input dialog to be displayed with any given text.
     *
     * @param title The title of the dialog popup.
     * @param text  The text that will be displayed on the label.
     * @return Returns the password that was inputted by the user.
     */
    public static String showPasswordInputDialog(String title, String text) {
        JPasswordField jPasswordField = new JPasswordField(24);
        JLabel jLabel = new JLabel(text);
        Box box = Box.createVerticalBox();
        box.add(jLabel);
        box.add(jPasswordField);
        int dialogSelection = JOptionPane.showConfirmDialog(null, box, title, JOptionPane.OK_CANCEL_OPTION);

        if (dialogSelection == JOptionPane.OK_OPTION) {
            return new String(jPasswordField.getPassword());
        }

        return null;
    }

    /**
     * Displays a {@link JOptionPane#showMessageDialog(Component, Object)} that shows in the taskbar.
     *
     * @param text The text that will be displayed on the dialog.
     */
    public static void showOnTopMessageDialog(String text) {
        JDialog dialog = new JDialog();
        dialog.setAlwaysOnTop(true);

        JOptionPane.showMessageDialog(dialog, text);
    }

    /**
     * Displays a {@link JOptionPane#showInputDialog(Component, Object)} that shows in the taskbar.
     *
     * @param text The text that will be displayed on the dialog.
     */
    public static String showOnTopInputDialog(String text) {
        JDialog dialog = new JDialog();
        dialog.setAlwaysOnTop(true);

        return JOptionPane.showInputDialog(dialog, text);
    }

    /**
     * Displays a {@link JOptionPane#showConfirmDialog(Component, Object)} that shows in the taskbar.
     *
     * @param text  The text that will be displayed on the dialog.
     * @param title The title of the dialog.
     */
    public static int showOnTopConfirmDialog(String text, String title) {
        JDialog dialog = new JDialog();
        dialog.setAlwaysOnTop(true);

        return JOptionPane.showConfirmDialog(dialog, text, title, JOptionPane.YES_NO_OPTION);
    }
}
