package com.fih.companion.diagnostics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ConsoleLog {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private ConsoleLog() {
    }

    /** One informational trace line: {@code [timestamp] [TAG] message}. */
    public static void log(String tag, String message) {
        System.out.println("[" + now() + "] [" + tag + "] " + message);
    }

    /** A failure line on {@code System.err}, followed by the FULL stack trace. */
    public static void error(String tag, String message, Throwable t) {
        System.err.println("[" + now() + "] [" + tag + "] " + message);
        if (t != null) {
            t.printStackTrace(System.err);
        }
    }


    public static String mask(String s) {
        if (s == null || s.isEmpty()) {
            return "<empty>";
        }
        return s.substring(0, Math.min(3, s.length())) + "\u2026(len=" + s.length() + ")";
    }

    private static String now() {
        return LocalDateTime.now().format(TS);
    }
}
