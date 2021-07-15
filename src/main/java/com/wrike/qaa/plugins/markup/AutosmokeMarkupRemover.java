package com.wrike.qaa.plugins.markup;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.wrike.qaa.plugins.utils.AnnotationUtils;
import com.wrike.qaa.plugins.utils.ImportUtils;
import com.wrike.qaa.plugins.utils.UINotificationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static com.wrike.qaa.plugins.markup.Constants.OLD_TEST_MARKUP_VALUE;
import static com.wrike.qaa.plugins.markup.Constants.TEST_MARKUP;
import static com.wrike.qaa.plugins.utils.SearchUtils.searchAnnotations;
import static java.util.stream.Collectors.toList;

public class AutosmokeMarkupRemover {
    private static final PropertiesComponent properties = PropertiesComponent.getInstance();

    static void removeOldMarkup(Project project) {
        Collection<VirtualFile> virtualTestFiles = FilenameIndex.getAllFilesByExt(project, "java", GlobalSearchScope.projectScope(project));
        ProgressManager.getInstance().run(new Task.Modal(project, "Removing Old Markup", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    removeAutoSmokeAnnotations(project, virtualTestFiles, indicator);
                    UINotificationUtils.showBalloon(project, MessageType.INFO, "Removing markup finished");
                } catch (ProcessCanceledException t) {
                    UINotificationUtils.showBalloon(project, MessageType.WARNING, "Removing markup cancelled");
                }
            }
        });
    }

    private static void removeAutoSmokeAnnotations(Project project, Collection<VirtualFile> virtualTestFiles, ProgressIndicator indicator) {
        indicator.setText("Searching for old markup");
        List<PsiFile> psiTestFilesList = ApplicationManager.getApplication().runReadAction(
                (Computable<List<PsiFile>>) () -> virtualTestFiles.stream()
                                                            .map(item -> PsiManager.getInstance(project).findFile(item))
                                                            .collect(toList()));
        List<PsiAnnotation> typeAnnotationList = searchAnnotations(
                psiTestFilesList, properties.getValue(TEST_MARKUP), properties.getValue(OLD_TEST_MARKUP_VALUE));
        int i = 0;
        indicator.setText("Removing old markup");
        for (PsiAnnotation ann : typeAnnotationList) {
            ApplicationManager.getApplication().runReadAction(() -> indicator.setText2("Current file: " + ann.getContainingFile().getName()));
            indicator.setFraction((double) i++ / typeAnnotationList.size());
            AnnotationUtils.deleteAnnotationValue(project, ann, properties.getValue(OLD_TEST_MARKUP_VALUE));
            PsiFile annContainingFile = ApplicationManager.getApplication().runReadAction(
                    (Computable<PsiFile>) ann::getContainingFile);
            if (annContainingFile instanceof PsiJavaFile) {
                ImportUtils.deleteRedundantImports(project, (PsiJavaFile) annContainingFile);
            }
        }
    }

}
