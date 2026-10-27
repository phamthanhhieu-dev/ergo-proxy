package com.ergoproxy.platform.util;

import com.ergoproxy.platform.constant.Const;
import com.ergoproxy.platform.exception.LogicException;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.UnknownContentTypeException;

import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class SubtitleDiagnostics {

    public static final String MDC_KEY = "opensubtitlesRequest";

    private SubtitleDiagnostics() {
    }

    public static String category(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof java.net.UnknownHostException) {
                String response = "DNS";
                return response;
            }
            if (cause instanceof javax.net.ssl.SSLException) {
                String response = "TLS";
                return response;
            }
            if (cause instanceof java.net.SocketTimeoutException || cause instanceof TimeoutException) {
                String response = "TIMEOUT";
                return response;
            }
            if (cause instanceof java.net.ConnectException) {
                String response = "CONNECT";
                return response;
            }
            if (cause instanceof RestClientResponseException) {
                String response = "HTTP";
                return response;
            }
            if (
                cause instanceof HttpMessageConversionException ||
                    cause instanceof tools.jackson.core.JacksonException ||
                    cause instanceof UnknownContentTypeException
            ) {
                String response = "JSON";
                return response;
            }
        }
        String response = "REQUEST";
        return response;
    }

    public static String sanitize(String text, String... secrets) {
        String safe = Objects.toString(text, Const.Common.EMPTY);
        for (String secret : secrets) {
            if (secret != null && !secret.isBlank()) {
                safe = safe.replace(secret, "[REDACTED]");
            }
        }
        safe = safe.replaceAll(Const.Regex.DIAGNOSTIC_URL, "[URL]");
        safe = safe.replaceAll(Const.Regex.DIAGNOSTIC_BEARER, "$1[REDACTED]");
        safe = safe.replaceAll(Const.Regex.DIAGNOSTIC_CREDENTIAL, "$1[REDACTED]");
        safe = safe.replaceAll(Const.Regex.DIAGNOSTIC_WHITESPACE, Const.Common.SPACE);
        String response = safe.substring(0, Math.min(safe.length(), 1500));
        return response;
    }

    public static <T> T observe(String operation, Supplier<T> action, Supplier<String[]> secrets) {
        String previous = MDC.get(MDC_KEY);
        try {
            T response = action.get();
            return response;
        } catch (Exception exception) {
            LogicException failure = failure(exception);
            throw failure;
        } finally {
            if (previous == null) {
                MDC.remove(MDC_KEY);
            } else {
                MDC.put(MDC_KEY, previous);
            }
        }
    }

    public static LogicException failure(Exception exception) {
        if (exception instanceof LogicException existing) {
            return existing;
        }
        String type = category(exception);
        LogicException response;
        switch (type) {
            case "DNS" -> response = new LogicException(exception, "FSD_00001", "subtitle.connection.dns.failed");
            case "TLS" -> response = new LogicException(exception, "FSD_00002", "subtitle.connection.tls.failed");
            case "TIMEOUT" -> response = new LogicException(exception, "FSD_00003", "subtitle.connection.timeout");
            case "CONNECT" -> response = new LogicException(exception, "FSD_00004", "subtitle.connection.failed");
            case "JSON" -> response = new LogicException(exception, "FSD_00005", "subtitle.response.json.invalid");
            default -> response = new LogicException(exception, "FSD_00006", "subtitle.request.failed");
        }
        return response;
    }
}
