package com.gaarx.tvplayer.core.ytlivedashmanifestparser;

import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helpers {

    public static void setField(Object these, String fieldName, long value) {
        try {
            Field f1 = getDeclaredField(these.getClass(), fieldName);
            if (f1 != null) {
                // Change private modifier to public
                f1.setAccessible(true);
                // Remove final modifier (don't working!!!)
                //Field modifiersField = Field.class.getDeclaredField("modifiers");
                //modifiersField.setAccessible(true);
                //modifiersField.setInt(f1, f1.getModifiers() & ~Modifier.FINAL);
                // Set field (at last)
                f1.setLong(these, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setField(Object these, String fieldName, Object value) {
        try {
            Field f1 = getDeclaredField(these.getClass(), fieldName);
            if (f1 != null) {
                // Change private modifier to public
                f1.setAccessible(true);
                // Remove final modifier (don't working!!!)
                //Field modifiersField = Field.class.getDeclaredField("modifiers");
                //modifiersField.setAccessible(true);
                //modifiersField.setInt(f1, f1.getModifiers() & ~Modifier.FINAL);
                // Set field (at last)
                f1.set(these, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static @Nullable Object getField(Object these, String fieldName) {
        try {
            Field f1 = getDeclaredField(these.getClass(), fieldName);

            if (f1 != null) {
                f1.setAccessible(true);
                return f1.get(these);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static Field getDeclaredField(Class<?> aClass, String fieldName) {
        if (aClass == null) { // null if superclass is object
            return null;
        }

        Field f1 = null;

        try {
            f1 = aClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            f1 = getDeclaredField(aClass.getSuperclass(), fieldName);
        }

        return f1;
    }

    public static long parseLong(String numString) {
        if (!isInteger(numString)) {
            return -1;
        }

        return Long.parseLong(numString);
    }

    public static boolean isInteger(String s) {
        return s != null && s.matches("^[-+]?\\d+$");
    }

    public static boolean matchAll(String input, Pattern... patterns) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(input);
            if (!matcher.find()) {
                return false;
            }
        }

        return true;
    }

    public static boolean matchAll(String input, String... regex) {
        for (String reg : regex) {
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(input);
            if (!matcher.find()) {
                return false;
            }
        }

        return true;
    }

    private static final Pattern URL_PREFIX = Pattern.compile("^[a-z.]+://.+$");

    public static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        Matcher m = URL_PREFIX.matcher(url);
        return m.matches();
    }

    /*public static String toString(InputStream content) {
        return FileHelpers.toString(content);
    }*/

    public static String replace(String content, Pattern oldVal, String newVal) {
        if (content == null) {
            return null;
        }

        return oldVal.matcher(content).replaceFirst(newVal);
    }

}
