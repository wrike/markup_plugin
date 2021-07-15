package com.wrike.qaa.plugins.markup;

public class Constants {
    public static final String STEP_ANN = "com.markup_plugin.step_ann";
    public static final String TEST_ANN = "com.markup_plugin.test_ann";
    public static final String BEFORE_EACH = "com.markup_plugin.before_each";
    public static final String TEST_MARKUP = "com.markup_plugin.test_markup";
    public static final String OLD_TEST_MARKUP_VALUE = "com.markup_plugin.old_test_markup_value";
    public static final String NEW_TEST_MARKUP_VALUE = "com.markup_plugin.new_test_markup_value";

//    Limit usages chain length to avoid stack overflow in case of recursive calls
    static final int DEFAULT_MAX_DEPTH = 100;
}
