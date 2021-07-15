package com.wrike.qaa.plugins.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

public class AnnotationUtils {

    public static PsiAnnotation createNewAnnotation(Project project, String annotationFQN, String value) {
        return JavaPsiFacade.getElementFactory(project)
                            .createAnnotationFromText(format("@%s(%s)", annotationFQN, value), null);
    }

    public static void addAnnotationValue(Project project, PsiModifierListOwner psiElement, String annotationFQN, String value) {
        PsiAnnotation psiAnnotation = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiAnnotation>) () -> AnnotationUtil.findAnnotation(psiElement, annotationFQN));
        if (psiAnnotation != null) {
            String oldValue = ApplicationManager.getApplication().runReadAction(
                    (Computable<String>) () -> psiAnnotation.findAttributeValue("value").getText());
            String newValue;
            if (oldValue.contains("}")) {
                int index = oldValue.indexOf("}");
                newValue = oldValue.substring(0, index) + ", " + value + oldValue.substring(index);
            } else {
                newValue = "{" + oldValue + ", " + value + "}";
            }
            ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
                psiAnnotation.replace(AnnotationUtils.createNewAnnotation(
                        project,
                        psiAnnotation.getQualifiedName(),
                        newValue));
            }));
        } else {
            String preparedAnnotation = annotationFQN + "(" + value + ")";
            ApplicationManager.getApplication().runReadAction(() -> {
                        if (ImportUtils.getPsiImportStatementContaining((PsiJavaFile) psiElement./*!!!*/getContainingFile(), annotationFQN).isEmpty()) {
                            PsiClass annotationClass = JavaPsiFacade.getInstance(project).findClass(annotationFQN, GlobalSearchScope.everythingScope(project));
                            ImportUtils.addImportStatement(project, (PsiJavaFile) psiElement.getContainingFile(), annotationClass);
                        }
                    });
            ApplicationManager.getApplication().invokeLater(() -> WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiAnnotation linkAnnotation = psiElement.getModifierList().addAnnotation(preparedAnnotation);
                JavaCodeStyleManager.getInstance(project).shortenClassReferences(linkAnnotation);
            }));
        }
    }

    public static void deleteAnnotationValue(Project project, PsiAnnotation psiAnnotation, String value) {
        PsiAnnotationMemberValue annValue = ApplicationManager.getApplication().runReadAction(
                (Computable<PsiAnnotationMemberValue>) () -> psiAnnotation.findAttributeValue("value"));
        if (annValue == null) return;
        String annValueText = ApplicationManager.getApplication().runReadAction((Computable<String>) annValue::getText);
        if (value.equals(annValueText)) {
            ElementUtils.deleteElement(project, psiAnnotation);
        } else {
            List<PsiElement> valuesList = ApplicationManager.getApplication().runReadAction(
                    (Computable<List<PsiElement>>) () -> Arrays.stream(annValue.getChildren())
                                                               .filter(child -> child instanceof PsiAnnotationMemberValue)
                                                               .filter(child -> value.equals(child.getText()))
                                                               .collect(toList()));
            ElementUtils.deleteElement(project, valuesList);
        }
    }

}
