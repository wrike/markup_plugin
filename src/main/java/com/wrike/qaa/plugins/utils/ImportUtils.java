package com.wrike.qaa.plugins.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class ImportUtils {

    public static void addImportStatement(Project project, PsiJavaFile psiJavaFile, PsiClass aClass) {
        CommandProcessor.getInstance().executeCommand(project,
                () -> ApplicationManager.getApplication().runWriteAction(() -> {
                    final PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
                    final PsiImportList importList = psiJavaFile.getImportList();
                    importList.add(elementFactory.createImportStatement(aClass));
                }), null, null);
    }

    public static Optional<PsiImportStatementBase> getPsiImportStatementContaining(PsiJavaFile file, String partQualifiedImport) {
        return Arrays.stream(requireNonNull(file.getImportList()).getAllImportStatements())
                     .filter(psiImportStatement ->
                             psiImportStatement.getImportReference().getQualifiedName().contains(partQualifiedImport)).findAny();
    }

    public static void deleteRedundantImports(Project project, PsiJavaFile file) {
        Collection<PsiImportStatementBase> redundantImports = ApplicationManager.getApplication().runReadAction(
                (Computable<Collection<PsiImportStatementBase>>) () ->
                        JavaCodeStyleManager.getInstance(project).findRedundantImports(file));
        redundantImports.stream().filter(Objects::nonNull).forEach(importStatement -> ElementUtils.deleteElement(project, importStatement));
    }

}
