package com.deutschimspiel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Builds and checks accepted German sentences. */
public final class Sentences {
    private Sentences() {}

    /** Lowercase, ä/ö/ü/ß -> ae/oe/ue/ss, everything except a-z becomes a single space. */
    public static String norm(String s) {
        if (s == null) return "";
        s = s.toLowerCase(Locale.ROOT)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss");
        return s.replaceAll("[^a-z]+", " ").trim();
    }

    public static String objectDef(GermanWords.Word w) {
        return GermanWords.defArticle(w.objG(), true) + " " + w.objNoun();
    }

    private static String objectIndef(GermanWords.Word w) {
        String art = GermanWords.indefArticle(w.objG());
        return art.isEmpty() ? w.objNoun() : art + " " + w.objNoun();
    }

    /** Sentences with a correct article (definite or indefinite). */
    public static Set<String> strict(Action a, GermanWords.Word w, Config.Tense t, boolean food) {
        Set<String> out = new LinkedHashSet<>();
        for (String tpl : a.templates(t, food)) {
            out.add(norm(String.format(tpl, objectDef(w))));
            out.add(norm(String.format(tpl, objectIndef(w))));
        }
        for (String ex : a.extras(t)) {
            out.add(norm(ex));
        }
        return out;
    }

    /** Sentences without any article (counted as "almost right"). */
    public static Set<String> bare(Action a, GermanWords.Word w, Config.Tense t, boolean food) {
        Set<String> out = new LinkedHashSet<>();
        for (String tpl : a.templates(t, food)) {
            out.add(norm(String.format(tpl, w.objNoun())));
        }
        return out;
    }

    public static String primary(Action a, GermanWords.Word w, Config.Tense t, boolean food) {
        List<String> tpls = a.templates(t, food);
        return String.format(tpls.get(0), objectDef(w)) + ".";
    }

    /** First template with the object replaced by "…", e.g. "Ich habe … gegessen". */
    public static String pattern(Action a, Config.Tense t, boolean food) {
        return a.templates(t, food).get(0).replace("%s", "…");
    }
}
