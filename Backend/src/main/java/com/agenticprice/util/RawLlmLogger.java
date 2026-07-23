package com.agenticprice.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Writes raw inputs that are about to be sent to the LLM into a per-run
 * folder on disk, so we can debug cases where the prompt is too
 * restrictive (e.g. "Extract the product price..." collapsing
 * "$10.00 /month" down to "$10.00").
 *
 * Layout:
 *   logs/raw-llm-<runId>/<seq>-<label>-<runId>.html
 *
 * The runId is generated once per JVM and stable across all calls, so
 * every file from a single app run lands in the same folder. A new
 * runId is picked on restart, giving us one folder per launch.
 *
 * Failures are intentionally swallowed: this logger must never crash a
 * scrape. If disk is full or permissions are bad we just log to stderr.
 */
public final class RawLlmLogger {

    private static final Path LOG_ROOT = Path.of("logs");
    private static final DateTimeFormatter RUN_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter FILE_TS_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS");

    private static final String RUN_ID = RUN_FMT.format(LocalDateTime.now());
    private static final Path RUN_DIR = LOG_ROOT.resolve("raw-llm-" + RUN_ID);
    private static final AtomicLong SEQ = new AtomicLong();

    static {
        try {
            Files.createDirectories(RUN_DIR);
        } catch (IOException e) {
            System.err.println("[RawLlmLogger] could not create " + RUN_DIR + ": " + e.getMessage());
        }
    }

    private RawLlmLogger() {
    }

    /**
     * Persist the raw string that is about to be sent to the LLM under a
     * given label (e.g. "extractPrice"). Returns the absolute path of the
     * written file, or null if writing failed.
     */
    public static synchronized String write(String label, String content) {
        if (content == null) return null;
        long seq = SEQ.incrementAndGet();
        String fileName = String.format("%02d-%s-%s.html",
                seq,
                sanitize(label),
                FILE_TS_FMT.format(LocalDateTime.now()));
        Path file = RUN_DIR.resolve(fileName);
        try {
            Files.writeString(
                    file,
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            return file.toAbsolutePath().toString();
        } catch (IOException e) {
            System.err.println("[RawLlmLogger] failed to write " + file + ": " + e.getMessage());
            return null;
        }
    }

    private static String sanitize(String label) {
        if (label == null || label.isBlank()) return "llm";
        return label.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
