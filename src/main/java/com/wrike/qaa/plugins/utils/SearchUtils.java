package com.wrike.qaa.plugins.utils;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.wrike.qaa.plugins.visitors.AnnotatedElementsCollectingElementVisitor;
import com.wrike.qaa.plugins.visitors.AnnotationCollectingElementVisitor;

import java.util.List;
import java.util.stream.Collectors;

public class SearchUtils {

    public static List<PsiModifierListOwner> searchElementsByAnnotation(List<PsiFile> fileList, String qualifiedAnnotationName, ProgressIndicator indicator) {
        if (indicator != null) {
            indicator.setText("Searching elements with annotation \"" + qualifiedAnnotationName + "\"");
        }
        AnnotatedElementsCollectingElementVisitor visitor = new AnnotatedElementsCollectingElementVisitor(qualifiedAnnotationName);
        int i = 0;
        for (PsiFile file : fileList) {
            if (indicator != null) {
                indicator.setFraction((double) i++ / fileList.size());
                indicator.setText2("Current file: " + file.getName());
            }
            visitor.visitElement(file);
        }
        if (indicator != null) {
            indicator.setFraction(0);
            indicator.setText("");
            indicator.setText2("");
        }
        return visitor.getAnnotateableElements();
    }

    public static List<PsiMethod> searchMethodsByAnnotation(List<PsiFile> fileList, String qualifiedAnnotationName) {
        return searchMethodsByAnnotation(fileList, qualifiedAnnotationName, null);
    }

    public static List<PsiMethod> searchMethodsByAnnotation(List<PsiFile> fileList, String qualifiedAnnotationName, ProgressIndicator indicator) {
        List<PsiModifierListOwner> list = searchElementsByAnnotation(fileList, qualifiedAnnotationName, indicator);
        return list.stream().filter(el -> el instanceof PsiMethod).map(el -> (PsiMethod) el).collect(Collectors.toList());
    }

    public static List<PsiAnnotation> searchAnnotations(List<PsiFile> filesList, String qualifiedAnnotationName, String containValue) {
        AnnotationCollectingElementVisitor visitor = new AnnotationCollectingElementVisitor();
        filesList.forEach(visitor::visitElement);
        return visitor.getAnnotations(qualifiedAnnotationName, containValue);
    }

}
