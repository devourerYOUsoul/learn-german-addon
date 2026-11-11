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
        public int sinceReward;
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

    /** Counts one clean correct answer. @return true when the reward wheel is due. */
    public static synchronized boolean countReward(int every) {
        Data d = get();
        d.sinceReward++;
        boolean hit = d.sinceReward >= every;
        if (hit) d.sinceReward = 0;
        try {
            Files.writeString(file(), GSON.toJson(d), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // not critical
        }
        return hit;
    }

    private static void save(Data d) {
        try {
            Files.writeString(file(), GSON.toJson(d), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // not critical
        }
    }

    public static synchronized void resetErrors() {
        Data d = get();
        d.wrong = 0;
        save(d);
    }

    /** A wrong submission (or a shown answer): one more mistake, streak back to 0. */
    public static synchronized void recordWrong(Action a, String subjectKey) {
        Data d = get();
        d.wrong++;
        d.streak.put(key(a, subjectKey), 0);
        save(d);
    }

    /** The prompt was solved. A clean solve (no mistake, no hint) also extends the streak. */
    public static synchronized void recordSolved(Action a, String subjectKey, boolean clean) {
        Data d = get();
        d.correct++;
        if (clean) d.streak.merge(key(a, subjectKey), 1, Integer::sum);
        save(d);
    }
}
