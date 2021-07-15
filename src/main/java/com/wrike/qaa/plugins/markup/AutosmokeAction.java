package com.wrike.qaa.plugins.markup;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AutosmokeAction {
    private final Project project;
    private Map<PsiMethod, Set<PsiMethod>> usagesMap;
    private Set<PsiMethod> smokeTests = new HashSet<>();

    public AutosmokeAction(Project project) {
        this.project = project;
    }

    public boolean parseStepsAndTests() {
        usagesMap = null;
        usagesMap = AutosmokeParser.parse(project);
        return !usagesMap.isEmpty();
    }

    public int getStepsFound() {
        return usagesMap.size();
    }

    public int getStepsPicked() {
        return smokeTests.size();
    }

    public double getFinalCoverage() {
        long stepsCovered = usagesMap.entrySet().stream().filter(e -> e.getValue().stream().anyMatch(t -> smokeTests.contains(t))).count();
        return 100 * stepsCovered / ((float) usagesMap.size());
    }

    public float getMaxCoverage() {
        long stepsUsed = usagesMap.entrySet().stream().filter(e -> !e.getValue().isEmpty()).count();
        return 100 * stepsUsed / ((float) usagesMap.size());
    }

    public boolean pickSmokeTests(int ignoredSteps) {
        smokeTests.clear();
        smokeTests = AutosmokeTestsPicker.filterAndPickTests(project, usagesMap, ignoredSteps);
        return !smokeTests.isEmpty();
    }

    public void markNewSmoke() {
        AutosmokeMarkupWriter.markupSmokeTests(project, smokeTests);
    }

    public void removeOldMarkup() {
        AutosmokeMarkupRemover.removeOldMarkup(project);
    }
}
