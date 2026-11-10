package com.deutschimspiel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loads words.json (+ optional user file) and looks up German names for items. */
public final class GermanWords {

    /** g = m / f / n / p (plural). actDe/actG = optional different noun used in sentences (e.g. milk bucket -> Milch). */
    public record Word(String de, char g, String actDe, char actG) {
        public String objNoun() { return actDe != null ? actDe : de; }
        public char objG() { return actDe != null ? actG : g; }

        public Component tooltipLine() {
            String art = defArticle(g, false);
            ChatFormatting color = switch (g) {
                case 'm' -> ChatFormatting.AQUA;
                case 'f' -> ChatFormatting.RED;
                case 'n' -> ChatFormatting.GREEN;
                default -> ChatFormatting.YELLOW;
            };
            MutableComponent line = Component.literal(art + " ").withStyle(color)
                    .append(Component.literal(de).withStyle(ChatFormatting.GRAY));
            if (g == 'p') line.append(Component.literal(" (Pl.)").withStyle(ChatFormatting.DARK_GRAY));
            return line;
        }
    }

    private static final Word NONE = new Word("", 'm', null, 'm');
    private static final Map<String, Word> exact = new HashMap<>();
    private static final List<Map.Entry<String, Word>> suffixes = new ArrayList<>();
    private static final Map<String, Word> cache = new HashMap<>();
    private static final Set<String> missingLogged = new HashSet<>();
    private static boolean loaded = false;

    private GermanWords() {}

    public static String defArticle(char g, boolean accusative) {
        return switch (g) {
            case 'm' -> accusative ? "den" : "der";
            case 'n' -> "das";
            default -> "die";
        };
    }

    /** Indefinite article, accusative. Plural has none. */
    public static String indefArticle(char g) {
        return switch (g) {
            case 'm' -> "einen";
            case 'f' -> "eine";
            case 'n' -> "ein";
            default -> "";
        };
    }

    public static Word lookup(ItemStack stack) {
        ensureLoaded();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String full = id.toString();
        Word cached = cache.get(full);
        if (cached != null) return cached == NONE ? null : cached;

        Word w = exact.get(full);
        if (w == null) w = exact.get(id.getPath());
        if (w == null) {
            String path = id.getPath();
            for (Map.Entry<String, Word> e : suffixes) {
                if (path.endsWith(e.getKey())) { w = e.getValue(); break; }
            }
        }
        cache.put(full, w == null ? NONE : w);
        if (w == null && !"minecraft:air".equals(full)) logMissing(full);
        return w;
    }

    /** True if a German word exists for this item id (no logging). */
    public static boolean known(ResourceLocation id) {
        ensureLoaded();
        if (exact.containsKey(id.toString()) || exact.containsKey(id.getPath())) return true;
        String path = id.getPath();
        for (Map.Entry<String, Word> e : suffixes) {
            if (path.endsWith(e.getKey())) return true;
        }
        return false;
    }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(DeutschImSpiel.MODID, "words.json");
            Resource res = Minecraft.getInstance().getResourceManager().getResourceOrThrow(rl);
            try (Reader r = res.openAsReader()) {
                parse(GsonHelper.parse(r));
            }
        } catch (Exception ex) {
            System.err.println("[DeutschImSpiel] Could not load words.json: " + ex);
        }
        try {
            Path user = FMLPaths.CONFIGDIR.get().resolve("deutschimspiel-words.json");
            if (Files.exists(user)) {
                try (Reader r = Files.newBufferedReader(user, StandardCharsets.UTF_8)) {
                    parse(GsonHelper.parse(r));
                }
            }
        } catch (Exception ex) {
            System.err.println("[DeutschImSpiel] Could not load deutschimspiel-words.json: " + ex);
        }
        suffixes.sort(Comparator.comparingInt((Map.Entry<String, Word> e) -> e.getKey().length()).reversed());
    }

    private static void parse(JsonObject root) {
        if (root.has("words")) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("words").entrySet()) {
                Word w = toWord(e.getValue());
                if (w != null) exact.put(e.getKey(), w);
            }
        }
        if (root.has("suffixes")) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("suffixes").entrySet()) {
                Word w = toWord(e.getValue());
                if (w == null) continue;
                suffixes.removeIf(x -> x.getKey().equals(e.getKey()));
                suffixes.add(Map.entry(e.getKey(), w));
            }
        }
        cache.clear();
    }

    private static Word toWord(JsonElement el) {
        if (el == null || !el.isJsonArray()) return null;
        JsonArray a = el.getAsJsonArray();
        if (a.size() < 2) return null;
        String de = a.get(0).getAsString();
        char g = a.get(1).getAsString().charAt(0);
        if (a.size() >= 4) {
            return new Word(de, g, a.get(2).getAsString(), a.get(3).getAsString().charAt(0));
        }
        return new Word(de, g, null, g);
    }

    private static void logMissing(String id) {
        if (!missingLogged.add(id)) return;
        try {
            Path f = FMLPaths.CONFIGDIR.get().resolve("deutschimspiel-missing.txt");
            Files.writeString(f, id + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {
            // not critical
        }
    }
}
