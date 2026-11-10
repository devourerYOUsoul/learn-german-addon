package com.deutschimspiel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Tiny progress file: streaks per action+subject, total correct/wrong. */
public final class Progress {
    public static class Data {
        public Map<String, Integer> streak = new HashMap<>();
        public int correct;
        public int wrong;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data;

    private Progress() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("deutschimspiel-progress.json");
    }

    public static synchronized Data get() {
        if (data == null) {
            data = new Data();
            try {
                Path f = file();
                if (Files.exists(f)) {
                    try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                        Data d = GSON.fromJson(r, Data.class);
                        if (d != null) {
                            if (d.streak == null) d.streak = new HashMap<>();
                            data = d;
                        }
                    }
                }
            } catch (Exception ignored) {
                // start fresh
            }
        }
        return data;
    }

    public static String key(Action a, String subjectKey) {
        return a.name() + ":" + subjectKey;
    }

    public static synchronized void record(Action a, String subjectKey, boolean success) {
        Data d = get();
        String k = key(a, subjectKey);
        if (success) {
            d.correct++;
            d.streak.merge(k, 1, Integer::sum);
        } else {
            d.wrong++;
            d.streak.put(k, 0);
        }
        try {
            Files.writeString(file(), GSON.toJson(d), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // not critical
        }
    }
}
