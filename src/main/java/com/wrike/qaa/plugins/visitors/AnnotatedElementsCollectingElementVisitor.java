package com.wrike.qaa.plugins.visitors;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class AnnotatedElementsCollectingElementVisitor extends PsiRecursiveElementWalkingVisitor {

    private final String qualifiedAnnotationName;
    private final List<PsiModifierListOwner> annotateableElements = new ArrayList<>();

    public AnnotatedElementsCollectingElementVisitor(String qualifiedAnnotationName) {
        this.qualifiedAnnotationName = qualifiedAnnotationName;
    }

    @Override
    public void visitElement(PsiElement element) {
        ApplicationManager.getApplication().runReadAction(() -> {
            if (element instanceof PsiModifierListOwner) {
                PsiModifierListOwner psiModifierListOwner = (PsiModifierListOwner) element;
                if (isAnnotated(psiModifierListOwner)) {
                    annotateableElements.add(psiModifierListOwner);
                }
            }
            super.visitElement(element);
        });
    }

    private boolean isAnnotated(PsiModifierListOwner psiModifierListOwner) {
        return Arrays.stream(psiModifierListOwner.getAnnotations())
                .anyMatch(annotation -> Objects.equals(annotation.getQualifiedName(), qualifiedAnnotationName));
    }

    public List<PsiModifierListOwner> getAnnotateableElements() {
        return annotateableElements;
    }

}
