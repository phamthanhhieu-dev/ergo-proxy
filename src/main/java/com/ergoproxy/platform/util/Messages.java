package com.ergoproxy.platform.util;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.exception.LogicException;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Vietnamese messages shared by HTTP handlers and background download tasks.
 */
public final class Messages {

    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("message.message", Locale.ROOT);

    private Messages() {
    }

    public static String get(String key, Object... args) {
        Object[] values = new Object[args.length];
        for (int index = 0; index < args.length; index++) {
            Object value = args[index];
            if (value != null) {
                values[index] = value.toString();
            }
        }
        String pattern = BUNDLE.getString(key);
        Locale locale = Locale.forLanguageTag(Const.Common.VI);
        MessageFormat formatter = new MessageFormat(pattern, locale);
        String response = formatter.format(values);
        return response;
    }
}
