package tech.fastj.partyhouse.util;

import tech.fastj.engine.FastJEngine;
import tech.fastj.graphics.dialog.DialogConfig;
import tech.fastj.graphics.dialog.DialogOptions;
import tech.fastj.graphics.dialog.DialogUtil;
import tech.fastj.graphics.display.SimpleDisplay;

import javax.swing.*;

public class Dialogs {
    public static void confirmExit() {
        SwingUtilities.invokeLater(() -> {
            DialogConfig confirmExitConfig = DialogConfig.create().withTitle("Exit?")
                    .withPrompt("Are you sure you want to exit?")
                    .withParentComponent(FastJEngine.<SimpleDisplay>getDisplay().getWindow())
                    .build();

            if (DialogUtil.showConfirmationDialog(confirmExitConfig, DialogOptions.YesNoCancel)) {
                FastJEngine.closeGame();
            }
        });
    }

    public static String userInput(DialogConfig dialogConfig) {
        String input;

        do {
            input = DialogUtil.showInputDialog(dialogConfig);
            if (input == null) {
                return null;
            }
        } while (input.isBlank());

        return input;
    }

    public static void message(DialogConfig dialogConfig) {
        if (SwingUtilities.isEventDispatchThread()) {
            DialogUtil.showMessageDialog(dialogConfig);
        } else {
            SwingUtilities.invokeLater(() -> message(dialogConfig));
        }
    }
}
