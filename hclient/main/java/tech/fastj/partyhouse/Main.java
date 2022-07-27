package tech.fastj.partyhouse;

import com.formdev.flatlaf.FlatDarkLaf;
import tech.fastj.engine.FastJEngine;
import tech.fastj.engine.config.ExceptionAction;
import tech.fastj.graphics.dialog.DialogConfig;
import tech.fastj.logging.LogLevel;

import tech.fastj.network.rpc.Client;

import tech.fastj.partyhouse.user.User;
import tech.fastj.partyhouse.util.Dialogs;
import tech.fastj.partyhouse.util.Fonts;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class Main {
    public static final String GameName = "Party House v0.0.1";

    public static void main(String[] args) {
        try {
            System.setProperty("sun.java2d.uiScale", "1");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            FlatDarkLaf.setup();

            FastJEngine.init(GameName, new GameManager());
            FastJEngine.setTargetUPS(1);
            FastJEngine.configureLogging(LogLevel.Debug);
            FastJEngine.configureExceptionAction(ExceptionAction.Throw);

            FastJEngine.run();

            Client client = User.getInstance().getClient();
            if (client != null) {
                client.disconnect();
            }
        } catch (Exception exception) {
            gameCrashed("Error while running FastJ.", exception);
        }
    }

    public static void gameCrashed(String message, Exception exception) {
        if (FastJEngine.isRunning()) {
            FastJEngine.closeGame();
        }

        if (User.getInstance().getClient() != null) {
            User.getInstance().getClient().disconnect();
        }

        displayException(message, exception);
    }

    public static void displayException(String message, Exception exception) {
        StringBuilder formattedException = new StringBuilder(exception.getClass().getName() + ": " + exception.getMessage());
        Throwable currentException = exception;
        do {
            formattedException.append(System.lineSeparator())
                    .append("Caused by: ")
                    .append(currentException.getClass().getName())
                    .append(": ")
                    .append(currentException.getMessage())
                    .append(System.lineSeparator())
                    .append(formatStackTrace(currentException));
        } while ((currentException = currentException.getCause()) != null);

        JTextArea textArea = new JTextArea(formattedException.toString());
        textArea.setEditable(false);
        textArea.setFont(Fonts.notoSansMono(Font.PLAIN, 13));

        Dialogs.errorQuitMessage(DialogConfig.create().withParentComponent(null)
                .withTitle(exception.getClass().getName() + (message != null ? (": " + message) : ""))
                .withPrompt(textArea)
                .build()
        );
    }

    private static String formatStackTrace(Throwable exception) {
        return Arrays.stream(exception.getStackTrace())
                .map(stackTraceElement -> "at " + stackTraceElement.toString() + "\n")
                .toList()
                .toString()
                .replaceFirst("\\[", "")
                .replaceAll("](.*)\\[", "")
                .replaceAll("(, )?at ", "    at ")
                .replace("]", "")
                .trim();
    }
}
