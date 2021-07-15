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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.javadoc.PsiDocMethodOrFieldRef;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.wrike.qaa.plugins.utils.UINotificationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.wrike.qaa.plugins.markup.Constants.BEFORE_EACH;
import static com.wrike.qaa.plugins.markup.Constants.DEFAULT_MAX_DEPTH;
import static com.wrike.qaa.plugins.markup.Constants.STEP_ANN;
import static com.wrike.qaa.plugins.markup.Constants.TEST_ANN;
import static com.wrike.qaa.plugins.utils.SearchUtils.searchMethodsByAnnotation;
import static com.wrike.qaa.plugins.utils.UINotificationUtils.showBalloon;
import static java.util.stream.Collectors.toList;

public class AutosmokeParser {
    private static long maxDepth = 0;
    private static final PropertiesComponent properties = PropertiesComponent.getInstance();

    static Map<PsiMethod, Set<PsiMethod>> parse(Project project) {
        Map<PsiMethod, Set<PsiMethod>> usagesMap = new HashMap<>();
        ProgressManager.getInstance().run(new Task.Modal(project, "Parsing Project Structure", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    List<PsiMethod> psiStepsList = new ArrayList<>();
                    usagesMap.putAll(ApplicationManager.getApplication().runReadAction((Computable<Map<PsiMethod, Set<PsiMethod>>>) () -> {
                        indicator.setText("Collecting all java files");
                        indicator.setIndeterminate(true);
                        Collection<VirtualFile> virtualStepFiles = FilenameIndex.getAllFilesByExt(project, "java", GlobalSearchScope.projectScope(project));
                        List<PsiFile> psiStepFilesList = virtualStepFiles.stream()
                                                                         .map(item -> PsiManager.getInstance(project).findFile(item))
                                                                         .collect(toList());
                        indicator.setText("Filtering files");
                        List<PsiFile> psiStepWithoutTestsFileList = psiStepFilesList.stream()
                                                                                    .filter(AutosmokeParser::isFileWithoutTests)
                                                                                    .collect(toList());
                        psiStepsList.addAll(searchMethodsByAnnotation(psiStepWithoutTestsFileList, properties.getValue(STEP_ANN), indicator));
                        return buildUsagesMap(psiStepsList, GlobalSearchScope.projectScope(project), indicator);
                    }));
                    if (usagesMap.isEmpty()) {
                        showBalloon(project, MessageType.ERROR, "Project parsing failed");
                    } else {
                        UINotificationUtils.showBalloon(project, MessageType.INFO, "Project parsing finished");
                    }
                } catch (ProcessCanceledException t) {
                    UINotificationUtils.showBalloon(project, MessageType.WARNING, "Project parsing cancelled");
                }
            }
        });
        return usagesMap;
    }

    private static boolean isFileWithoutTests(PsiFile file) {
        return searchMethodsByAnnotation(Collections.singletonList(file), properties.getValue(TEST_ANN)).isEmpty();
    }

    private static Map<PsiMethod, Set<PsiMethod>> buildUsagesMap(List<PsiMethod> psiStepsList, GlobalSearchScope testsSearchScope, ProgressIndicator indicator) {
        indicator.setIndeterminate(false);
        indicator.setFraction(0);
        indicator.setText("Building steps usages map");
        maxDepth = 0;
        Map<PsiMethod, Set<PsiMethod>> usagesMap = new HashMap<>();
        int i = 1;
        Map<PsiMethod, Set<PsiMethod>> usageMap = new HashMap<>();
        for (PsiMethod step : psiStepsList) {
            indicator.setFraction((double) i++ / psiStepsList.size());
            indicator.setText2("Current method: " + step.getName() + " " + step.getParameterList());
            Set<PsiMethod> allUsages = findMethodUsagesWithMemory(step, testsSearchScope, usageMap, 1);
            usagesMap.put(step, allUsages);
        }
        return usagesMap;
    }

    private static Set<PsiMethod> findMethodUsagesWithMemory(PsiMethod psiMethod, GlobalSearchScope searchScope, Map<PsiMethod, Set<PsiMethod>> usageMap, int depth) {
        if (psiMethod == null) {
            return Collections.emptySet();
        }
        if (usageMap.containsKey(psiMethod)) {
            return usageMap.get(psiMethod);
        }
        maxDepth = depth > maxDepth ? depth : maxDepth;
        if (depth > DEFAULT_MAX_DEPTH) {
            return Collections.emptySet();
        }
        Set<PsiMethod> resultSet = new HashSet<>();
        // don't use MethodReferencesSearch, because it returns overriding methods as well
        Collection<PsiReference> psiReferences = ReferencesSearch.search(psiMethod,
                GlobalSearchScope.projectScope(psiMethod.getProject()))
                                                                 .findAll()
                                                                 .stream()
                                                                 .filter(i -> i.isReferenceTo(psiMethod))
                                                                 .collect(toList());
        for (PsiReference reference : psiReferences) {
            PsiElement psiElement = reference.getElement();
            if (psiElement instanceof PsiDocMethodOrFieldRef) {
                continue;
            }
            PsiMethod parentMethod = getParentMethod(psiElement);
            if (isMethodTest(parentMethod)) {
                if (isMethodInScope(parentMethod, searchScope)) {
                    resultSet.add(parentMethod);
                }
                continue;
            }
            if (isMethodBefore(parentMethod)) {
                if (isMethodInScope(parentMethod, searchScope)) {
                    resultSet.addAll(getTestMethodsFromClass(parentMethod.getContainingClass()));
                }
                continue;
            }
            resultSet.addAll(findMethodUsagesWithMemory(parentMethod, searchScope, usageMap, depth + 1));
        }
        usageMap.put(psiMethod, resultSet);
        return resultSet;
    }

    private static PsiMethod getParentMethod(PsiElement psiElement) {
        if (psiElement instanceof PsiClass || psiElement instanceof PsiFile) {
            return null;
        }
        PsiElement parent = psiElement.getParent();
        if (parent == null || parent instanceof PsiMethod) {
            return (PsiMethod) parent;
        } else {
            return getParentMethod(parent);
        }
    }

    private static List<PsiMethod> getTestMethodsFromClass(PsiClass psiClass) {
        return Arrays.stream(psiClass.getMethods()).filter(AutosmokeParser::isMethodTest).collect(toList());
    }

    private static boolean isMethodBefore(PsiMethod psiMethod) {
        return psiMethod != null && Arrays.stream(psiMethod.getAnnotations())
                                          .map(PsiAnnotation::getQualifiedName)
                                          .anyMatch(annName -> Objects.equals(annName, properties.getValue(BEFORE_EACH)));
    }

    private static boolean isMethodTest(PsiMethod psiMethod) {
        return psiMethod != null && Arrays.stream(psiMethod.getAnnotations())
                                          .anyMatch(annotation -> Objects.equals(annotation.getQualifiedName(), properties.getValue(TEST_ANN)));
    }

    private static boolean isMethodInScope(PsiMethod psiMethod, GlobalSearchScope searchScope) {
        return searchScope.contains(psiMethod.getContainingFile().getVirtualFile());
    }

}
