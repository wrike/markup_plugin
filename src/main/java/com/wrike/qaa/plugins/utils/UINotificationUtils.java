package com.wrike.qaa.plugins.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;

public class UINotificationUtils {
    public static void showBalloon(Project project, MessageType messageType, String message) {
        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        JBPopupFactory.getInstance()
                      .createHtmlTextBalloonBuilder(message, messageType, null)
                      .setFadeoutTime(7500)
                      .createBalloon()
                      .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                              Balloon.Position.atRight);
    }
}
