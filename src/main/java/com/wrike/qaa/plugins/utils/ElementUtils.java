package com.wrike.qaa.plugins.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;

import java.util.Collection;

public class ElementUtils {

    public static void deleteElement(Project project, PsiElement psiElement) {
        ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, psiElement::delete));
    }

    public static void deleteElement(Project project, Collection<? extends PsiElement> psiElementsList) {
        psiElementsList.forEach(element -> deleteElement(project, element));
    }

}
