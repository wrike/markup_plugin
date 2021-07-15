package com.wrike.qaa.plugins.visitors;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AnnotationCollectingElementVisitor extends PsiRecursiveElementWalkingVisitor {

    protected List<PsiAnnotation> annotations = new ArrayList<>();

    @Override
    public void visitElement(PsiElement element) {
        ApplicationManager.getApplication().runReadAction(() -> {
            if (element instanceof PsiClass || element instanceof PsiMethod) {
                PsiModifierListOwner psiModifierListOwner = (PsiModifierListOwner) element;
                annotations.addAll(Arrays.asList(psiModifierListOwner.getAnnotations()));
            }
            super.visitElement(element);
        });
    }

    public List<PsiAnnotation> getAnnotations(String qualifiedAnnotationName, String value) {
        List<PsiAnnotation> psiAnnotations = new ArrayList<>();
        ApplicationManager.getApplication().runReadAction(() -> {
            psiAnnotations.addAll(annotations.stream()
                                             .filter(annotation -> Objects.equals(annotation.getQualifiedName(), qualifiedAnnotationName))
                                             .filter(annotation -> Objects.requireNonNull(annotation.findAttributeValue("value")).toString().contains(value))
                                             .collect(Collectors.toList()));
        });
        return psiAnnotations;
    }

}
