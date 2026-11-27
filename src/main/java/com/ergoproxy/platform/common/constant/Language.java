package com.ergoproxy.platform.common.constant;

import com.ibm.icu.util.ULocale;

import java.util.Arrays;

public class Language {
    public static final String VIETNAM = "vi";
    public static final String ENGLISH = "en";

    public static boolean isValid(String language) {
        return Arrays.asList(ULocale.getISOLanguages()).contains(language);
    }
    private Language() {
    }
}
