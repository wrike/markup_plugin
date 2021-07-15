package com.wrike.qaa.plugins.markup;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.wrike.qaa.plugins.utils.AnnotationUtils;
import com.wrike.qaa.plugins.utils.ImportUtils;
import com.wrike.qaa.plugins.utils.UINotificationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import static com.wrike.qaa.plugins.markup.Constants.NEW_TEST_MARKUP_VALUE;
import static com.wrike.qaa.plugins.markup.Constants.TEST_MARKUP;

public class AutosmokeMarkupWriter {
    private static final PropertiesComponent properties = PropertiesComponent.getInstance();

    public static void markupSmokeTests(Project project, Set<PsiMethod> smokeTests) {
        ProgressManager.getInstance().run(new Task.Modal(project, "Adding New Markup", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setText("Adding new markup");
                    addMarkupToTests(project, smokeTests, indicator);
                    indicator.setText("Adding missing static imports");
                    addMissingImports(project, smokeTests, properties.getValue(NEW_TEST_MARKUP_VALUE));
                    UINotificationUtils.showBalloon(project, MessageType.INFO, "Adding markup finished");
                } catch (ProcessCanceledException t) {
                    UINotificationUtils.showBalloon(project, MessageType.WARNING, "Adding markup cancelled");
                } catch (PsiInvalidElementAccessException t) {
                    UINotificationUtils.showBalloon(project, MessageType.WARNING, "Files aren't valid any more. Please parse project again.");
                }
            }
        });
    }

    private static void addMissingImports(Project project, Set<PsiMethod> testsList, String aClass) {
        Set<PsiJavaFile> uniqueValues = new HashSet<>();
        for (PsiMethod test : testsList) {
            ApplicationManager.getApplication().runReadAction(() -> {
                PsiJavaFile containingFile = (PsiJavaFile) test.getContainingFile();
                if (uniqueValues.add(containingFile)) {
                    PsiClass importClass = JavaPsiFacade.getInstance(project).findClass(aClass, GlobalSearchScope.everythingScope(project));
                    if (importClass != null) {
                        ImportUtils.addImportStatement(project, containingFile, importClass);
                    }
                }
            });
        }
    }

    private static void addMarkupToTests(Project project, Set<PsiMethod> testsList, ProgressIndicator indicator) {
        int i = 0;
        for (PsiMethod testMethod : testsList) {
            indicator.setFraction((double) i++ / testsList.size());
            AnnotationUtils.addAnnotationValue(project, testMethod, properties.getValue(TEST_MARKUP), properties.getValue(NEW_TEST_MARKUP_VALUE));
        }
    }

}
