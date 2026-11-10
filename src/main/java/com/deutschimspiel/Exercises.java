package com.deutschimspiel;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generator for A1 / A2 / B1 exercises. A task is built from random building blocks
 * (main clause kind x connector x clause x modifiers) so the number of variants is practically endless.
 * Each task can be shown in several exercise modes and modes can be chained.
 */
public final class Exercises {
    private Exercises() {}

    public enum Mode { TYPE, TILES, GAP, GAPTYPE, FIX, PICK }

    public record Stage(Mode mode, Task task) {}

    /** ru = extra Russian instruction lines; tokens = words of the main answer (punctuation attached). */
    public record Task(String level, List<String> ru, String primary, Set<String> accepted, List<String> tokens,
                       List<String> forms, List<String> smart, String adjStem) {}

    // ---------------------------------------------------------------- data

    record V(String inf, String ich, String part, String prefix, String loc) {
        String zuInf() {
            if (!prefix.isEmpty() && inf.startsWith(prefix)) return prefix + "zu" + inf.substring(prefix.length());
            return "zu " + inf;
        }
        String subFinal() { return prefix + ich; }
    }

    private static V v(String spec) {
        String[] p = spec.split("\\|", -1);
        return new V(p[0], p[1], p[2], p.length > 3 ? p[3] : "", p.length > 4 ? p[4] : "");
    }

    private static List<V> vl(String... specs) {
        List<V> l = new ArrayList<>();
        for (String s : specs) l.add(v(s));
        return l;
    }

    private static final Map<Action, List<V>> VERBS = new EnumMap<>(Action.class);

    static {
        VERBS.put(Action.EAT, vl("essen|esse|gegessen"));
        VERBS.put(Action.DRINK, vl("trinken|trinke|getrunken"));
        VERBS.put(Action.CRAFT, vl("herstellen|stelle|hergestellt|her", "bauen|baue|gebaut", "basteln|bastle|gebastelt", "machen|mache|gemacht"));
        VERBS.put(Action.CHOP, vl("fällen|fälle|gefällt", "hacken|hacke|gehackt", "hauen|haue|gehauen"));
        VERBS.put(Action.MINE, vl("abbauen|baue|abgebaut|ab", "schürfen|schürfe|geschürft", "ausgraben|grabe|ausgegraben|aus"));
        VERBS.put(Action.DIG, vl("graben|grabe|gegraben", "schaufeln|schaufle|geschaufelt", "ausheben|hebe|ausgehoben|aus"));
        VERBS.put(Action.ENCHANT, vl("verzaubern|verzaubere|verzaubert"));
        VERBS.put(Action.ANVIL, vl("reparieren|repariere|repariert", "bearbeiten|bearbeite|bearbeitet",
                "verbessern|verbessere|verbessert", "umbenennen|benenne|umbenannt|um"));
        VERBS.put(Action.BREW, vl("brauen|braue|gebraut", "machen|mache|gemacht"));
        VERBS.put(Action.SMITH, vl("schmieden|schmiede|geschmiedet", "verbessern|verbessere|verbessert", "herstellen|stelle|hergestellt|her"));
        VERBS.put(Action.STONECUT, vl("schneiden|schneide|geschnitten", "sägen|säge|gesägt", "herstellen|stelle|hergestellt|her",
                "zuhauen|haue|zugehauen|zu"));
        VERBS.put(Action.WEAVE, vl("weben|webe|gewebt", "gestalten|gestalte|gestaltet", "machen|mache|gemacht"));
        VERBS.put(Action.MAP, vl("zeichnen|zeichne|gezeichnet", "bearbeiten|bearbeite|bearbeitet", "kopieren|kopiere|kopiert",
                "vergrößern|vergrößere|vergrößert"));
        VERBS.put(Action.GRIND, vl("schleifen|schleife|geschliffen", "reparieren|repariere|repariert", "entzaubern|entzaubere|entzaubert"));
        VERBS.put(Action.TRADE, vl("kaufen|kaufe|gekauft", "bekommen|bekomme|bekommen", "eintauschen|tausche|eingetauscht|ein"));
        VERBS.put(Action.FISH, vl("angeln|angle|geangelt", "fangen|fange|gefangen"));
        VERBS.put(Action.FEED, vl("füttern|füttere|gefüttert"));
        VERBS.put(Action.PLANT, vl("pflanzen|pflanze|gepflanzt", "säen|säe|gesät"));
        VERBS.put(Action.HARVEST, vl("ernten|ernte|geerntet", "sammeln|sammle|gesammelt"));
        VERBS.put(Action.HIT, vl("schlagen|schlage|geschlagen", "angreifen|greife|angegriffen|an", "hauen|haue|gehauen",
                "bekämpfen|bekämpfe|bekämpft"));
        VERBS.put(Action.SHOOT, vl("spannen|spanne|gespannt", "benutzen|benutze|benutzt"));
        VERBS.put(Action.BLOCK, vl("heben|hebe|gehoben", "benutzen|benutze|benutzt", "hochhalten|halte|hochgehalten|hoch"));
        VERBS.put(Action.PLACE_FLOOR, vl("stellen|stelle|gestellt||auf den Boden", "setzen|setze|gesetzt||auf den Boden",
                "platzieren|platziere|platziert||auf den Boden", "platzieren|platziere|platziert||auf dem Boden"));
        VERBS.put(Action.PLACE_WALL, vl("hängen|hänge|gehängt||an die Wand", "befestigen|befestige|befestigt||an der Wand",
                "setzen|setze|gesetzt||an die Wand", "platzieren|platziere|platziert||an der Wand"));
    }

    private static List<V> verbs(Action a, boolean food) {
        if (a == Action.SMELT) {
            return food ? vl("braten|brate|gebraten", "kochen|koche|gekocht", "backen|backe|gebacken", "schmelzen|schmelze|geschmolzen")
                    : vl("schmelzen|schmelze|geschmolzen");
        }
        return VERBS.getOrDefault(a, List.of());
    }

    record Modal(String ich, String inf, String ru) {}

    private static final List<Modal> MODALS = List.of(
            new Modal("möchte", "möchten", "хотеть, вежливо"), new Modal("will", "wollen", "хотеть"),
            new Modal("kann", "können", "мочь"), new Modal("muss", "müssen", "быть должным"),
            new Modal("darf", "dürfen", "можно, разрешено"), new Modal("soll", "sollen", "следует"));

    /** ru = Russian clause (indicative), v2 = main-clause word order, sub = verb-final word order. */
    record Cl(String ru, String v2, String sub) {}

    private static final List<Cl> FACTS = List.of(
            new Cl("я голоден", "ich habe Hunger", "ich Hunger habe"),
            new Cl("идёт дождь", "es regnet", "es regnet"),
            new Cl("сейчас ночь", "es ist Nacht", "es Nacht ist"),
            new Cl("я устал", "ich bin müde", "ich müde bin"),
            new Cl("у меня есть время", "ich habe Zeit", "ich Zeit habe"),
            new Cl("мне страшно", "ich habe Angst", "ich Angst habe"),
            new Cl("холодно", "es ist kalt", "es kalt ist"),
            new Cl("я один", "ich bin allein", "ich allein bin"),
            new Cl("моя подруга помогает", "meine Freundin hilft", "meine Freundin hilft"),
            new Cl("приходит зомби", "ein Zombie kommt", "ein Zombie kommt"),
            new Cl("светит солнце", "die Sonne scheint", "die Sonne scheint"),
            new Cl("у меня хорошее настроение", "ich habe gute Laune", "ich gute Laune habe"),
            new Cl("у меня мало места", "ich habe wenig Platz", "ich wenig Platz habe"),
            new Cl("мне нужны ресурсы", "ich brauche Ressourcen", "ich Ressourcen brauche"));

    private static final List<Cl> GOALS = List.of(
            new Cl("я оставался сильным", "", "ich stark bleibe"),
            new Cl("я выжил", "", "ich überlebe"),
            new Cl("у меня было больше места", "", "ich mehr Platz habe"),
            new Cl("мы играли вместе", "", "wir zusammen spielen"),
            new Cl("я был быстрее", "", "ich schneller bin"),
            new Cl("я мог лучше строить", "", "ich besser bauen kann"),
            new Cl("я ничего не забыл", "", "ich nichts vergesse"));

    private static final List<Cl> WISHES = List.of(
            new Cl("у меня было время", "", "ich Zeit hätte"),
            new Cl("я был голоден", "", "ich Hunger hätte"),
            new Cl("у меня было больше места", "", "ich mehr Platz hätte"),
            new Cl("я был сильнее", "", "ich stärker wäre"),
            new Cl("не шёл дождь", "", "es nicht regnen würde"),
            new Cl("моя подруга была здесь", "", "meine Freundin hier wäre"),
            new Cl("у меня была кирка", "", "ich eine Spitzhacke hätte"));

    record Adj(String stem, String ru) {}

    private static final List<Adj> ADJ = List.of(
            new Adj("groß", "большой"), new Adj("klein", "маленький"), new Adj("neu", "новый"), new Adj("alt", "старый"),
            new Adj("schön", "красивый"), new Adj("gut", "хороший"), new Adj("frisch", "свежий"),
            new Adj("billig", "дешёвый"), new Adj("stark", "сильный"), new Adj("schnell", "быстрый"));

    record Place(List<String> de, String ru) {}

    private static Place pl(String ru, String... de) { return new Place(Arrays.asList(de), ru); }

    private static final List<Place> PLACES = List.of(
            pl("в лесу", "im Wald", "in dem Wald"), pl("у озера", "am See", "an dem See"),
            pl("в пещере", "in der Höhle"), pl("в доме", "im Haus", "in dem Haus"),
            pl("в деревне", "im Dorf", "in dem Dorf"), pl("на поле", "auf dem Feld"),
            pl("в шахте", "in der Mine"), pl("на пляже", "am Strand", "an dem Strand"),
            pl("на горе", "auf dem Berg"), pl("на кухне", "in der Küche"),
            pl("в подвале", "im Keller", "in dem Keller"), pl("в саду", "im Garten", "in dem Garten"),
            pl("на острове", "auf der Insel"), pl("в долине", "im Tal", "in dem Tal"),
            pl("у реки", "am Fluss", "an dem Fluss"), pl("в болоте", "im Sumpf", "in dem Sumpf"));

    private static final String[] ADV_PRES = {"Dann|потом", "Jetzt|сейчас", "Heute|сегодня", "Danach|после этого",
            "Zuerst|сначала", "Manchmal|иногда", "Gleich|сейчас же", "Morgen|завтра"};
    private static final String[] ADV_PERF = {"Gestern|вчера", "Vorhin|недавно", "Heute|сегодня", "Gerade eben|только что"};

    private static final String[] DASS_OPEN = {"Ich bin froh,|Я рад,|1", "Ich weiß,|Я знаю,|1", "Ich hoffe,|Я надеюсь,|0",
            "Meine Freundin sagt,|Моя подруга говорит,|1", "Es ist gut,|Хорошо,|0"};
    private static final String[] ZU_OPEN = {"Ich habe Lust,|мне хочется", "Ich habe keine Lust,|мне не хочется",
            "Es macht Spaß,|приятно", "Ich versuche,|я пытаюсь", "Es ist wichtig,|важно",
            "Ich habe keine Zeit,|у меня нет времени", "Ich habe vergessen,|я забыл"};
    private static final String[] UM_OPEN = {"Ich gehe nach draußen, um|Я выхожу наружу, чтобы", "Ich gehe zum Dorf, um|Я иду в деревню, чтобы",
            "Ich bin hier, um|Я здесь, чтобы", "Ich nehme die Spitzhacke, um|Я беру кирку, чтобы"};

    private static final String[][] GROUPS = {
            {"der", "die", "das", "den", "dem"},
            {"ein", "eine", "einen", "einem", "einer"},
            {"weil", "dass", "denn", "aber", "wenn", "obwohl", "während", "damit", "und", "oder", "ob"},
            {"habe", "hat", "bin", "ist", "wird", "werden", "würde", "hätte", "wäre"},
            {"möchte", "will", "kann", "muss", "darf", "soll"},
            {"ich", "du", "er", "wir", "sie"},
            {"im", "in", "am", "an", "auf", "unter", "aus"},
            {"dann", "jetzt", "heute", "danach", "zuerst", "manchmal", "gleich", "gestern", "vorhin"},
            {"zu", "um", "für", "mit"}};
    private static final String[] NOUNS = {"Apfel", "Schwert", "Baum", "Haus", "Brot", "Stein", "Pferd", "Wasser"};
    private static final String[] FILLERS = {"nicht", "auch", "sehr", "oder", "mit", "dann", "nur"};

    // ---------------------------------------------------------------- sentence building

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(p);
        }
        return sb.toString();
    }

    private static String cap(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    static final class Ctx {
        V v;
        GermanWords.Word w;
        boolean indef;
        Adj adj;
        String placeTxt;
        boolean placeFirst;

        String acc() {
            char g = w.objG();
            String art = indef ? GermanWords.indefArticle(g) : GermanWords.defArticle(g, true);
            String end = indef
                    ? switch (g) { case 'm' -> "en"; case 'f' -> "e"; case 'n' -> "es"; default -> "e"; }
                    : switch (g) { case 'm' -> "en"; case 'f', 'n' -> "e"; default -> "en"; };
            return join(art, adj == null ? "" : adj.stem() + end, w.objNoun());
        }

        String nom() {
            char g = w.objG();
            String end = g == 'p' ? "en" : "e";
            return join(GermanWords.defArticle(g, false), adj == null ? "" : adj.stem() + end, w.objNoun());
        }

        String extra() {
            if (!v.loc().isEmpty()) return v.loc();
            return placeTxt == null ? "" : placeTxt;
        }

        String mid() {
            String o = acc();
            if (!v.loc().isEmpty()) return join(o, v.loc());
            if (placeTxt == null) return o;
            return placeFirst ? join(placeTxt, o) : join(o, placeTxt);
        }

        boolean plural() { return w.objG() == 'p'; }
    }

    interface Frame { String render(Ctx c); }

    interface Main {
        String v2(Ctx c);
        String inv(Ctx c);
    }

    static final class Spec {
        Frame f;
        final List<String> ru = new ArrayList<>();
        final List<String> smart = new ArrayList<>();
        boolean forceMod;
        boolean allowIndef = true;
    }

    private static final Main PRES = new Main() {
        public String v2(Ctx c) { return join("ich", c.v.ich(), c.mid(), c.v.prefix()); }
        public String inv(Ctx c) { return join(c.v.ich(), "ich", c.mid(), c.v.prefix()); }
    };
    private static final Main PERF = new Main() {
        public String v2(Ctx c) { return join("ich", "habe", c.mid(), c.v.part()); }
        public String inv(Ctx c) { return join("habe", "ich", c.mid(), c.v.part()); }
    };

    private static Main modal(Modal m) {
        return new Main() {
            public String v2(Ctx c) { return join("ich", m.ich(), c.mid(), c.v.inf()); }
            public String inv(Ctx c) { return join(m.ich(), "ich", c.mid(), c.v.inf()); }
        };
    }

    private static Main passive() {
        return new Main() {
            String be(Ctx c) { return c.plural() ? "werden" : "wird"; }
            public String v2(Ctx c) { return join(c.nom(), be(c), c.extra(), c.v.part()); }
            public String inv(Ctx c) { return join(be(c), c.nom(), c.extra(), c.v.part()); }
        };
    }

    private static Main advFront(String adv, Main base) {
        return new Main() {
            public String v2(Ctx c) { return join(adv, base.inv(c)); }
            public String inv(Ctx c) { return null; }
        };
    }

    private static <T> T pick(List<T> l, Random r) { return l.get(r.nextInt(l.size())); }

    private static void addConjSmart(Spec sp, String used) {
        for (String s : new String[]{"weil", "dass", "denn", "aber", "wenn", "obwohl", "damit", "und"}) {
            if (!s.equals(used)) sp.smart.add(s);
        }
    }

    // ---------------------------------------------------------------- specs

    private static int weighted(Random r, int... w) {
        int t = 0;
        for (int x : w) t += x;
        int n = r.nextInt(t);
        for (int i = 0; i < w.length; i++) {
            n -= w[i];
            if (n < 0) return i;
        }
        return 0;
    }

    static Spec spec(Config.Level lvl, Random r) {
        Spec sp = new Spec();
        if (lvl == Config.Level.A1) {
            Main m = Config.TENSE.get() == Config.Tense.PERFECT ? PERF : PRES;
            sp.f = m::v2;
            return sp;
        }
        boolean b1 = lvl == Config.Level.B1;
        if (b1 && r.nextInt(100) < 55) {
            standalone(sp, r);
            return sp;
        }
        // main clause
        int k = b1 ? weighted(r, 15, 20, 20, 15, 30) : weighted(r, 25, 25, 25, 25, 0);
        Main m;
        boolean plainMain = false;
        switch (k) {
            case 1 -> {
                m = PERF;
                sp.ru.add("Скажи в Perfekt (прошедшее время).");
                sp.smart.addAll(List.of("hat", "bin", "ist"));
            }
            case 2 -> {
                Modal md = pick(MODALS, r);
                m = modal(md);
                sp.ru.add("С модальным глаголом «" + md.inf() + "» (" + md.ru() + ").");
                for (Modal o : MODALS) if (o != md) sp.smart.add(o.ich());
            }
            case 3 -> {
                boolean perf = r.nextBoolean();
                String[] e = pick(Arrays.asList(perf ? ADV_PERF : ADV_PRES), r).split("\\|");
                m = advFront(e[0].toLowerCase(Locale.ROOT), perf ? PERF : PRES);
                sp.ru.add("Начни предложение со слова «" + e[0] + "» (" + e[1] + ")." + (perf ? " Время: Perfekt." : ""));
                sp.smart.addAll(List.of("ich", "dann", "jetzt"));
            }
            case 4 -> {
                m = passive();
                sp.allowIndef = false;
                sp.ru.add("Скажи в пассиве (Passiv): предмет — подлежащее, «wird/werden».");
                sp.smart.addAll(List.of("ist", "wurde", "wirst", "ich"));
            }
            default -> {
                m = PRES;
                plainMain = true;
            }
        }
        // attachment
        int at = b1 ? weighted(r, 10, 45, 45) : weighted(r, 30, 35, 35);
        final Main mainF = m;
        boolean noInv = k == 3;
        if (at == 2 && noInv) at = 1;
        if (at == 0) {
            sp.f = mainF::v2;
            if (plainMain) sp.forceMod = true;
            return sp;
        }
        if (at == 1) {
            String[] conjs = b1 ? new String[]{"weil", "obwohl", "damit", "während", "denn", "aber"} : new String[]{"weil", "denn", "aber"};
            String conj = conjs[r.nextInt(conjs.length)];
            Cl cl = conj.equals("damit") ? pick(GOALS, r) : pick(FACTS, r);
            sp.ru.add(afterLine(conj, cl.ru()));
            addConjSmart(sp, conj);
            boolean v2cl = conj.equals("denn") || conj.equals("aber");
            sp.f = c -> mainF.v2(c) + ", " + conj + " " + (v2cl ? cl.v2() : cl.sub());
            return sp;
        }
        String[] conjs = b1 ? new String[]{"obwohl", "während", "wenn", "weil"} : new String[]{"wenn", "weil"};
        String conj = conjs[r.nextInt(conjs.length)];
        Cl cl = pick(FACTS, r);
        sp.ru.add("Начни с придаточного «" + cap(conj) + "» (" + conjRu(conj) + "): " + cl.ru() + ".");
        addConjSmart(sp, conj);
        sp.f = c -> cap(conj) + " " + cl.sub() + ", " + mainF.inv(c);
        return sp;
    }

    private static String conjRu(String c) {
        return switch (c) {
            case "weil" -> "потому что";
            case "denn" -> "потому что";
            case "aber" -> "но";
            case "obwohl" -> "хотя";
            case "während" -> "пока / в то время как";
            case "wenn" -> "если / когда";
            case "damit" -> "чтобы";
            default -> "";
        };
    }

    private static String afterLine(String conj, String ru) {
        return switch (conj) {
            case "damit" -> "Добавь цель с «damit» (чтобы): " + ru + ".";
            case "aber" -> "Добавь «aber» (но): " + ru + ".";
            case "denn" -> "Добавь причину с «denn» (потому что): " + ru + ".";
            case "weil" -> "Добавь причину с «weil» (потому что): " + ru + ".";
            default -> "Добавь придаточное с «" + conj + "» (" + conjRu(conj) + "): " + ru + ".";
        };
    }

    private static void standalone(Spec sp, Random r) {
        switch (r.nextInt(5)) {
            case 0 -> { // dass-clause
                String[] o = pick(Arrays.asList(DASS_OPEN), r).split("\\|");
                boolean perf = o[2].equals("1") && r.nextBoolean();
                sp.ru.add("Начни с «" + o[0] + " dass…» (" + o[1] + " что…)." + (perf ? " Время: Perfekt." : ""));
                sp.smart.addAll(List.of("weil", "damit", "ob", "wenn", "habe", "hat", "hätte"));
                sp.f = c -> join(o[0], "dass", "ich", c.mid(), perf ? c.v.part() + " habe" : c.v.subFinal());
            }
            case 1 -> { // relative clause
                boolean perf = r.nextBoolean();
                sp.allowIndef = false;
                sp.ru.add("Составь предложение «Это (тот/та/то) …, который я …» с относительным придаточным." + (perf ? " Время: Perfekt." : ""));
                sp.smart.addAll(List.of("der", "die", "das", "den", "dem", "habe", "hat"));
                sp.f = c -> {
                    String rel = switch (c.w.objG()) { case 'm' -> "den"; case 'n' -> "das"; default -> "die"; };
                    String inner = c.v.loc().isEmpty() ? (c.placeTxt == null ? "" : c.placeTxt) : c.v.loc();
                    return join(c.plural() ? "das sind" : "das ist", c.nom() + ",", rel, "ich", inner,
                            perf ? c.v.part() + " habe" : c.v.subFinal());
                };
            }
            case 2 -> { // zu-Infinitiv
                String[] o = pick(Arrays.asList(ZU_OPEN), r).split("\\|");
                sp.ru.add("Начни с «" + o[0].replace(",", "") + "» (" + o[1] + ") + infinitiv mit zu.");
                sp.smart.addAll(List.of("zu", "um", "dass", "weil"));
                sp.f = c -> join(o[0], c.mid(), c.v.zuInf());
            }
            case 3 -> { // um ... zu
                String[] o = pick(Arrays.asList(UM_OPEN), r).split("\\|");
                sp.ru.add(o[1] + " … (um … zu + infinitiv).");
                sp.smart.addAll(List.of("zu", "damit", "für", "weil"));
                sp.f = c -> join(o[0], c.mid(), c.v.zuInf());
            }
            default -> { // Konjunktiv II
                Cl cl = pick(WISHES, r);
                boolean first = r.nextBoolean();
                sp.ru.add("Konjunktiv II («würde» / «hätte» / «wäre»): «если бы» … " + cl.ru() + ".");
                sp.smart.addAll(List.of("wurde", "wird", "habe", "hatte", "weil", "dass"));
                sp.f = c -> first
                        ? join("wenn", cl.sub() + ",", "würde", "ich", c.mid(), c.v.inf())
                        : join("ich", "würde", c.mid(), c.v.inf()) + ", wenn " + cl.sub();
            }
        }
    }

    // ---------------------------------------------------------------- task creation

    public static Task make(Action a, Subject s, Config.Level lvl, Random r) {
        GermanWords.Word w = s.word();
        List<V> vs = verbs(a, s.food());
        if (w == null || vs.isEmpty()) return null;
        Spec sp = spec(lvl, r);
        boolean noLoc = vs.stream().allMatch(x -> x.loc().isEmpty());
        Adj adj = null;
        Place place = null;
        if (lvl != Config.Level.A1) {
            boolean wantAdj = r.nextInt(100) < (lvl == Config.Level.B1 ? 30 : 25);
            boolean wantPlace = noLoc && r.nextInt(100) < 30;
            if (sp.forceMod && !wantAdj && !wantPlace) {
                if (noLoc && r.nextBoolean()) wantPlace = true;
                else wantAdj = true;
            }
            if (wantAdj) adj = pick(ADJ, r);
            if (wantPlace) place = pick(PLACES, r);
        }
        List<String> ru = new ArrayList<>(sp.ru);
        if (adj != null) ru.add("Добавь к предмету прилагательное «" + adj.ru() + "» (" + adj.stem() + ").");
        if (place != null) ru.add("Добавь место: " + place.ru() + ".");

        Ctx c = new Ctx();
        c.w = w;
        c.adj = adj;
        Set<String> acc = new LinkedHashSet<>();
        String primary = null;
        for (V v : vs) {
            c.v = v;
            for (int ind = 0; ind < 2; ind++) {
                c.indef = ind == 1;
                if (c.indef && !sp.allowIndef) continue;
                List<String> places = place == null ? Collections.singletonList(null) : place.de();
                for (String pt : places) {
                    c.placeTxt = pt;
                    for (int pf = 0; pf < (pt == null ? 1 : 2); pf++) {
                        c.placeFirst = pf == 1;
                        String sentence = cap(sp.f.render(c));
                        acc.add(Sentences.norm(sentence));
                        if (primary == null) primary = sentence + ".";
                    }
                }
            }
        }
        V v0 = vs.get(0);
        List<String> forms = new ArrayList<>();
        for (String f : new String[]{v0.inf(), v0.part(), v0.ich(), v0.subFinal()}) {
            if (!f.isEmpty() && !forms.contains(f)) forms.add(f);
        }
        List<String> smart = new ArrayList<>(sp.smart);
        smart.addAll(forms);
        String stem = adj == null ? "" : adj.stem();
        if (adj != null) {
            for (String e : new String[]{"e", "en", "es", "er", "em"}) smart.add(stem + e);
        }
        for (String art : new String[]{"der", "die", "das", "den", "dem"}) smart.add(art);
        List<String> tokens = new ArrayList<>(Arrays.asList(primary.split(" ")));
        return new Task(lvl.name(), ru, primary, acc, tokens, forms, smart, stem);
    }

    // ---------------------------------------------------------------- helpers for the screen

    private static String core(String t) { return t.replaceAll("[.,!?;:]+$", ""); }

    private static String tail(String t) { return t.substring(core(t).length()); }

    public static String norm1(String t) { return Sentences.norm(t); }

    /** Tokens that look like confusable alternatives of the given token (same punctuation and capitalisation). */
    public static List<String> confuse(Task t, String token) {
        String co = core(token);
        String tl = tail(token);
        boolean cap = !co.isEmpty() && Character.isUpperCase(co.charAt(0));
        String low = co.toLowerCase(Locale.ROOT);
        Set<String> out = new LinkedHashSet<>();
        for (String[] g : GROUPS) {
            if (Arrays.asList(g).contains(low)) {
                for (String y : g) if (!y.equals(low)) out.add((cap ? cap(y) : y) + tl);
            }
        }
        String n = Sentences.norm(co);
        for (String f : t.forms()) {
            if (Sentences.norm(f).equals(n)) {
                for (String y : t.forms()) if (!Sentences.norm(y).equals(n)) out.add((cap ? cap(y) : y) + tl);
            }
        }
        if (!t.adjStem().isEmpty() && n.startsWith(Sentences.norm(t.adjStem()))) {
            for (String e : new String[]{"e", "en", "es", "er", "em"}) {
                String y = t.adjStem() + e;
                if (!Sentences.norm(y).equals(n)) out.add((cap ? cap(y) : y) + tl);
            }
        }
        if (cap && out.isEmpty() && !Arrays.asList(NOUNS).contains(co)) {
            // looks like a noun
            for (String nn : NOUNS) out.add(nn + tl);
        }
        return new ArrayList<>(out);
    }

    private static boolean grammatical(Task t, String token) {
        String n = Sentences.norm(core(token));
        for (String[] g : GROUPS) for (String x : g) if (Sentences.norm(x).equals(n)) return true;
        for (String f : t.forms()) if (Sentences.norm(f).equals(n)) return true;
        return !t.adjStem().isEmpty() && n.startsWith(Sentences.norm(t.adjStem()));
    }

    /** Indices (never 0) of tokens to blank out, grammar words are preferred. */
    public static List<Integer> pickBlanks(Task t, int k, Random r) {
        List<Integer> idx = new ArrayList<>();
        List<Integer> wts = new ArrayList<>();
        for (int i = 1; i < t.tokens().size(); i++) {
            idx.add(i);
            wts.add(grammatical(t, t.tokens().get(i)) ? 4 : 1);
        }
        List<Integer> out = new ArrayList<>();
        while (out.size() < k && !idx.isEmpty()) {
            int total = 0;
            for (int x : wts) total += x;
            int n = r.nextInt(total);
            int j = 0;
            for (; j < idx.size(); j++) {
                n -= wts.get(j);
                if (n < 0) break;
            }
            if (j >= idx.size()) j = idx.size() - 1;
            out.add(idx.remove(j));
            wts.remove(j);
        }
        Collections.sort(out);
        return out;
    }

    /** Wrong tokens for the word bank. */
    public static List<String> distractors(Task t, List<String> answers, int n, Random r) {
        Set<String> inSentence = new LinkedHashSet<>();
        for (String tok : t.tokens()) inSentence.add(Sentences.norm(tok));
        List<String> primary = new ArrayList<>();
        for (String a : answers) {
            List<String> cs = confuse(t, a);
            Collections.shuffle(cs, r);
            primary.addAll(cs.subList(0, Math.min(2, cs.size())));
        }
        Collections.shuffle(primary, r);
        List<String> rest = new ArrayList<>(t.smart());
        for (String f : FILLERS) rest.add(f);
        Collections.shuffle(rest, r);
        List<String> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (List<String> src : List.of(primary, rest)) {
            for (String d : src) {
                String nd = Sentences.norm(d);
                if (nd.isEmpty() || inSentence.contains(nd) || !seen.add(nd)) continue;
                out.add(d);
                if (out.size() >= n) return out;
            }
        }
        return out;
    }

    /** A grammatically wrong variant of the answer. */
    public static String corrupt(Task t, Random r) {
        List<String> tk = new ArrayList<>(t.tokens());
        int last = tk.size() - 1;
        if (last >= 0 && tk.get(last).endsWith(".")) tk.set(last, tk.get(last).substring(0, tk.get(last).length() - 1));
        return corruptTokens(t, tk, r) + ".";
    }

    private static String corruptTokens(Task t, List<String> tk, Random r) {
        for (int attempt = 0; attempt < 14; attempt++) {
            List<String> w = new ArrayList<>(tk);
            int kind = r.nextInt(3);
            if (kind == 0) {
                List<Integer> cand = new ArrayList<>();
                for (int i = 1; i < w.size(); i++) if (!confuse(t, w.get(i)).isEmpty() && grammatical(t, w.get(i))) cand.add(i);
                if (cand.isEmpty()) continue;
                int i = cand.get(r.nextInt(cand.size()));
                List<String> m = confuse(t, w.get(i));
                w.set(i, m.get(r.nextInt(m.size())));
            } else if (kind == 1) {
                if (w.size() < 3) continue;
                int i = 1 + r.nextInt(w.size() - 2);
                String x = w.get(i);
                w.set(i, w.get(i + 1));
                w.set(i + 1, x);
            } else {
                List<Integer> cand = new ArrayList<>();
                for (int i = 1; i < w.size(); i++) {
                    String n = Sentences.norm(core(w.get(i)));
                    if (n.equals("der") || n.equals("die") || n.equals("das") || n.equals("den") || n.equals("ein") || n.equals("eine")
                            || n.equals("einen") || n.equals("zu") || n.equals("ich")) cand.add(i);
                }
                if (cand.isEmpty()) continue;
                w.remove((int) cand.get(r.nextInt(cand.size())));
            }
            String s = String.join(" ", w);
            if (!t.accepted().contains(Sentences.norm(s)) && !Sentences.norm(s).equals(Sentences.norm(t.primary()))) return s;
        }
        List<String> w = new ArrayList<>(tk);
        if (w.size() >= 3) {
            String x = w.get(1);
            w.set(1, w.get(2));
            w.set(2, x);
        }
        return String.join(" ", w);
    }

    // ---------------------------------------------------------------- planning

    static Config.Level levelFor(Random r) {
        Config.Level l = Config.LEVEL.get();
        if (l == Config.Level.AUTO) {
            int n = Progress.get().correct;
            if (n < 30) return Config.Level.A1;
            if (n < 80) return r.nextInt(100) < 30 ? Config.Level.A1 : Config.Level.A2;
            if (n < 200) return r.nextInt(100) < 70 ? Config.Level.A2 : Config.Level.B1;
            return r.nextBoolean() ? Config.Level.A2 : Config.Level.B1;
        }
        if (l == Config.Level.MIX) {
            int x = r.nextInt(100);
            return x < 15 ? Config.Level.A1 : x < 55 ? Config.Level.A2 : Config.Level.B1;
        }
        return l;
    }

    static List<Mode> pickModes(Random r) {
        int[] w = {Config.W_TYPE.get(), Config.W_TILES.get(), Config.W_GAP.get(), Config.W_GAPTYPE.get(),
                Config.W_FIX.get(), Config.W_PICK.get()};
        Mode[] all = Mode.values();
        int n = r.nextInt(100) < Config.CHAIN_PERCENT.get() ? (r.nextInt(100) < 70 ? 2 : 3) : 1;
        List<Mode> out = new ArrayList<>();
        boolean[] used = new boolean[w.length];
        for (int i = 0; i < n; i++) {
            int total = 0;
            for (int j = 0; j < w.length; j++) if (!used[j]) total += w[j];
            if (total <= 0) break;
            int x = r.nextInt(total);
            for (int j = 0; j < w.length; j++) {
                if (used[j]) continue;
                x -= w[j];
                if (x < 0) {
                    used[j] = true;
                    out.add(all[j]);
                    break;
                }
            }
        }
        if (out.isEmpty()) out.add(Mode.TYPE);
        return out;
    }

    public static Screen open(Action a, Subject s) {
        Random r = new Random();
        List<Stage> stages = new ArrayList<>();
        String last = "";
        for (Mode m : pickModes(r)) {
            Task t = null;
            for (int tries = 0; tries < 6; tries++) {
                t = make(a, s, levelFor(r), r);
                if (t == null || !Sentences.norm(t.primary()).equals(last)) break;
            }
            if (t == null) break;
            last = Sentences.norm(t.primary());
            stages.add(new Stage(m, t));
        }
        if (stages.isEmpty()) return new SentenceScreen(a, s);
        if (stages.size() == 1 && stages.get(0).mode() == Mode.TYPE && stages.get(0).task().level().equals("A1")) {
            return new SentenceScreen(a, s);
        }
        return new ExerciseScreen(a, s, stages);
    }
}
