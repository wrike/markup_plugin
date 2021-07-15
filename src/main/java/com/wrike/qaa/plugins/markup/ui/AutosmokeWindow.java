package com.wrike.qaa.plugins.markup.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.wrike.qaa.plugins.markup.AutosmokeAction;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static com.wrike.qaa.plugins.markup.Constants.*;
import static com.wrike.qaa.plugins.utils.UINotificationUtils.showBalloon;

public class AutosmokeWindow {
    private JPanel mainPanel;
    private JButton pickSmokeTestsButton;
    private JButton parseStructureButton;
    private JButton removeOldSmokeMarkupButton;
    private JButton markTestsInCodeButton;
    private JLabel coverageLabel;
    private JLabel stepsLabel;
    private JLabel pickingResultsLabel;
    private JCheckBox ignoreStepsWithUsagesCheckBox;
    private JSpinner ignoreStepsSpinner;
    private JTabbedPane tabbedPane;
    private JRadioButton jUnit4RButton;
    private JRadioButton jUnit5RButton;
    private JRadioButton customRButton;
    private JTextField testAnnTField;
    private JTextField beforeEachAnnTField;
    private JTextField markupAnnTField;
    private JTextField oldMarkupValueTField;
    private JTextField stepAnnTField;
    private JTextField newMarkupValueTField;

    private final AutosmokeAction autosmokeAction;
    private final PropertiesComponent properties = PropertiesComponent.getInstance();

    public AutosmokeWindow(Project project) {
        loadConfig();
        coverageLabel.setText("");
        stepsLabel.setText("");
        pickingResultsLabel.setText("");
        autosmokeAction = new AutosmokeAction(project);
        parseStructureButton.addActionListener(l -> {
            disable(pickSmokeTestsButton, markTestsInCodeButton);
            coverageLabel.setText("");
            stepsLabel.setText("");
            pickingResultsLabel.setText("");
            if (autosmokeAction.parseStepsAndTests()) {
                enable(pickSmokeTestsButton);
                float maxCoverage = autosmokeAction.getMaxCoverage();
                coverageLabel.setText("Max. possible coverage: " + String.format("%.2f", maxCoverage) + "%");
                stepsLabel.setText("Steps found: " + autosmokeAction.getStepsFound());
            }
        });
        pickSmokeTestsButton.addActionListener(l -> {
            disable(markTestsInCodeButton);
            pickingResultsLabel.setText("");
            int ignoredSteps = ignoreStepsWithUsagesCheckBox.isSelected() ? (Integer) ignoreStepsSpinner.getValue() : 0;
            if (autosmokeAction.pickSmokeTests(ignoredSteps)) {
                enable(markTestsInCodeButton);
                pickingResultsLabel.setText(
                        String.format("Tests picked: %d  |  Final coverage: %.2f%%",
                                autosmokeAction.getStepsPicked(),
                                autosmokeAction.getFinalCoverage()));
            } else {
                showBalloon(project, MessageType.ERROR, "Failed to pick smoke tests");
            }
        });
        removeOldSmokeMarkupButton.addActionListener(l -> {
            try {
                autosmokeAction.removeOldMarkup();
            } catch (Throwable t) {
                showBalloon(project, MessageType.ERROR, "Removing markup failed");
            }
        });
        // look if old markup removed AND tests picked
        markTestsInCodeButton.addActionListener(l -> {
            try {
                autosmokeAction.markNewSmoke();
            } catch (Throwable t) {
                showBalloon(project, MessageType.ERROR, "Adding markup failed");
            }
        });
        jUnit4RButton.addActionListener(l -> {
            disable(testAnnTField, beforeEachAnnTField, markupAnnTField);
            testAnnTField.setText("org.junit.Test");
            beforeEachAnnTField.setText("org.junit.Before");
            markupAnnTField.setText("org.junit.experimental.categories.Category");
            saveConfig();
        });
        jUnit5RButton.addActionListener(l -> {
            disable(testAnnTField, beforeEachAnnTField, markupAnnTField);
            testAnnTField.setText("org.junit.jupiter.api.Test");
            beforeEachAnnTField.setText("org.junit.jupiter.api.BeforeEach");
            markupAnnTField.setText("org.junit.jupiter.api.Tag");
            saveConfig();
        });
        customRButton.addActionListener(l -> enable(testAnnTField, beforeEachAnnTField, markupAnnTField));
        stepAnnTField.addKeyListener(getSaveKeyListener(STEP_ANN, stepAnnTField));
        testAnnTField.addKeyListener(getSaveKeyListener(TEST_ANN, testAnnTField));
        beforeEachAnnTField.addKeyListener(getSaveKeyListener(BEFORE_EACH, beforeEachAnnTField));
        markupAnnTField.addKeyListener(getSaveKeyListener(TEST_MARKUP, markupAnnTField));
        oldMarkupValueTField.addKeyListener(getSaveKeyListener(OLD_TEST_MARKUP_VALUE, oldMarkupValueTField));
        newMarkupValueTField.addKeyListener(getSaveKeyListener(NEW_TEST_MARKUP_VALUE, newMarkupValueTField));
    }

    private KeyListener getSaveKeyListener(String property, JTextField field) {
        return new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                properties.setValue(property, field.getText());
            }
        };
    }

    private void saveConfig() {
        PropertiesComponent properties = PropertiesComponent.getInstance();
        properties.setValue(STEP_ANN, stepAnnTField.getText());
        properties.setValue(TEST_ANN, testAnnTField.getText());
        properties.setValue(BEFORE_EACH, beforeEachAnnTField.getText());
        properties.setValue(TEST_MARKUP, markupAnnTField.getText());
        properties.setValue(OLD_TEST_MARKUP_VALUE, oldMarkupValueTField.getText());
        properties.setValue(NEW_TEST_MARKUP_VALUE, newMarkupValueTField.getText());
    }

    private void loadConfig() {
        stepAnnTField.setText(properties.getValue(STEP_ANN, "io.qameta.allure.Step"));
        testAnnTField.setText(properties.getValue(TEST_ANN, ""));
        beforeEachAnnTField.setText(properties.getValue(BEFORE_EACH, ""));
        markupAnnTField.setText(properties.getValue(TEST_MARKUP, ""));
        oldMarkupValueTField.setText(properties.getValue(OLD_TEST_MARKUP_VALUE, "Smoke"));
        newMarkupValueTField.setText(properties.getValue(NEW_TEST_MARKUP_VALUE, "Constants.Smoke"));
    }

    private void enable(JComponent ... component) {
        for (JComponent comp : component) {
            comp.setEnabled(true);
        }
    }

    private void disable(JComponent ... component) {
        for (JComponent comp : component) {
            comp.setEnabled(false);
        }
    }

    private void createUIComponents() {
        SpinnerNumberModel numberModel = new SpinnerNumberModel();
        numberModel.setMinimum(1);
        numberModel.setValue(2);
        ignoreStepsSpinner = new JSpinner(numberModel);
    }

    public JPanel getContent() {
        return mainPanel;
    }

}
