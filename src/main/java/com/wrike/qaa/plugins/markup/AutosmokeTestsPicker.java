package com.wrike.qaa.plugins.markup;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.psi.PsiMethod;
import com.wrike.qaa.plugins.utils.UINotificationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

public class AutosmokeTestsPicker {

    static Set<PsiMethod> filterAndPickTests(Project project, Map<PsiMethod, Set<PsiMethod>> usagesMap, int ignoredSteps) {
        Map<PsiMethod, Set<PsiMethod>> filteredByUsagesMap = filterStepsWithUsagesLessThan(usagesMap, ignoredSteps);
        Set<PsiMethod> smokeTests = new HashSet<>();
        ProgressManager.getInstance().run(new Task.Modal(project, "Picking Smoke Tests", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Picking smoke tests");
                    smokeTests.addAll(pickSmokeTests(new HashMap<>(filteredByUsagesMap)));
                    UINotificationUtils.showBalloon(project, MessageType.INFO, "Picking Smoke Tests finished");
                } catch (ProcessCanceledException t) {
                    UINotificationUtils.showBalloon(project, MessageType.WARNING, "Picking Smoke Tests cancelled");
                }
            }
        });
        return smokeTests;
    }

    /**
     * Pick smoke tests pack.
     * Warning! Original collection is destroyed here. Always pass a copy.
     */
    private static Set<PsiMethod> pickSmokeTests(Map<PsiMethod, Set<PsiMethod>> usagesMap) {
        if (usagesMap.isEmpty()) {
            return Collections.emptySet();
        }
        Map.Entry<PsiMethod, Set<PsiMethod>> leastUsedStep = usagesMap.entrySet()
                                                              .parallelStream()
                                                              .min(Comparator.comparingInt(e -> e.getValue().size()))
                                                              .get();
        usagesMap.remove(leastUsedStep.getKey());
        Optional<PsiMethod> testMaxSteps = leastUsedStep.getValue()
                                                              .stream()
                                                              .max(Comparator.comparingInt(testWithRate ->
                                                                      (int) usagesMap.entrySet().stream().filter(e ->
                                                                              e.getValue().contains(testWithRate)).count()));
        Set<PsiMethod> resultSet = new HashSet<>();
        if (testMaxSteps.isPresent()) {
            usagesMap.entrySet()
                     .removeAll(usagesMap.entrySet()
                                         .parallelStream()
                                         .filter(e -> e.getValue().contains(testMaxSteps.get()))
                                         .collect(toSet()));
            resultSet.add(testMaxSteps.get());
        }
        resultSet.addAll(pickSmokeTests(usagesMap));
        return resultSet;
    }

    private static Map<PsiMethod, Set<PsiMethod>> filterStepsWithUsagesLessThan(Map<PsiMethod, Set<PsiMethod>> usageMap, int ignoredSteps) {
        return usageMap.entrySet().stream().filter(e -> e.getValue().size() >= ignoredSteps).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

}
