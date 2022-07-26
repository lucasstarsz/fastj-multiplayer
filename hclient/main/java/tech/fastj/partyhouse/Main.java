package tech.fastj.partyhouse;

import com.formdev.flatlaf.FlatDarkLaf;
import tech.fastj.engine.FastJEngine;
import tech.fastj.engine.config.ExceptionAction;
import tech.fastj.graphics.dialog.DialogConfig;
import tech.fastj.logging.LogLevel;
import tech.fastj.partyhouse.util.Dialogs;
import tech.fastj.partyhouse.util.Fonts;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class Main {
    public static final String GameName = "Party House v0.0.1";

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            FlatDarkLaf.setup();

            FastJEngine.init(GameName, new GameManager());
            FastJEngine.setTargetUPS(1);
            FastJEngine.configureLogging(LogLevel.Debug);
            FastJEngine.configureExceptionAction(ExceptionAction.Throw);

            FastJEngine.run();
        } catch (Exception exception) {
            if (FastJEngine.isRunning()) {
                FastJEngine.closeGame();
            }

            displayException("Error while running FastJ", exception);
        } finally {
            System.exit(0);
        }
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
        textArea.setBackground(new Color(238, 238, 238));
        textArea.setEditable(false);
        textArea.setFont(Fonts.notoSansMono(Font.PLAIN, 13));

        Dialogs.message(DialogConfig.create().withParentComponent(null)
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
