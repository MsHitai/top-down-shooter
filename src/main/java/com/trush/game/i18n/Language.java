package com.trush.game.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Localizes player-facing text without changing IDs, saved data or gameplay rules. */
public enum Language {
    ENGLISH("en", "English"),
    RUSSIAN("ru", "Русский");

    public static final String PREFERENCE = "language";
    public final String code;
    public final String label;

    Language(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Language fromCode(String code) {
        return RUSSIAN.code.equals(code) ? RUSSIAN : ENGLISH;
    }

    public Language toggle() {
        return this == ENGLISH ? RUSSIAN : ENGLISH;
    }

    public String text(String source) {
        if (this == ENGLISH || source == null) return source;
        return Russian.TEXT.getOrDefault(source, source);
    }

    public String upper(String source) {
        return text(source).toUpperCase(Locale.ROOT);
    }

    public String format(String source, Object... arguments) {
        return String.format(Locale.ROOT, text(source), arguments);
    }

    private static class Russian {
        private static final Map<String, String> TEXT = load();

        private static Map<String, String> load() {
            InputStream stream = Language.class.getResourceAsStream("/i18n/ru.tsv");
            if (stream == null) throw new IllegalStateException("Russian text catalog is missing");
            Map<String, String> entries = new HashMap<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                int number = 0;
                while ((line = reader.readLine()) != null) {
                    number++;
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] entry = line.split("\t", -1);
                    if (entry.length != 2 || entry[0].isBlank() || entry[1].isBlank()
                            || entries.putIfAbsent(entry[0], entry[1]) != null) {
                        throw new IllegalStateException("Invalid Russian text catalog entry at line " + number);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not load Russian text catalog", e);
            }
            return Map.copyOf(entries);
        }
    }
}
