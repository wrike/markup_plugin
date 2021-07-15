package com.wrike.qaa.plugins.markup.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class AutosmokeWindowFactory implements ToolWindowFactory {

    public void createToolWindowContent(@NotNull Project project, ToolWindow toolWindow) {
        ContentFactory contentFactory = toolWindow.getContentManager().getFactory();
        AutosmokeWindow autosmokeWindow = new AutosmokeWindow(project);
        Content optionsContent = contentFactory.createContent(autosmokeWindow.getContent(), "", false);
        toolWindow.getContentManager().addContent(optionsContent);
    }

}
