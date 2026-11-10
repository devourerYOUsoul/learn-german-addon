package com.deutschimspiel;

import java.util.List;

public enum Action {
    EAT("eat", false, List.of("Ich esse %s"), List.of("Ich habe %s gegessen"), List.of(), List.of()),
    DRINK("drink", false, List.of("Ich trinke %s"), List.of("Ich habe %s getrunken"), List.of(), List.of()),
    CRAFT("craft", false,
            List.of("Ich stelle %s her", "Ich baue %s", "Ich bastle %s", "Ich mache %s"),
            List.of("Ich habe %s hergestellt", "Ich habe %s gebaut", "Ich habe %s gebastelt", "Ich habe %s gemacht"),
            List.of(), List.of()),
    /** Templates depend on food / non-food, see templates(). */
    SMELT("smelt", false, List.of("Ich schmelze %s"), List.of("Ich habe %s geschmolzen"), List.of(), List.of()),
    CHOP("chop", false,
            List.of("Ich fälle %s", "Ich hacke %s", "Ich haue %s"),
            List.of("Ich habe %s gefällt", "Ich habe %s gehackt", "Ich habe %s gehauen"),
            List.of("Ich fälle den Baum", "Ich fälle einen Baum", "Ich fälle Bäume", "Ich hacke Holz", "Ich sammle Holz"),
            List.of("Ich habe den Baum gefällt", "Ich habe einen Baum gefällt", "Ich habe Holz gehackt", "Ich habe Holz gesammelt")),
    MINE("mine", false,
            List.of("Ich baue %s ab", "Ich schürfe %s", "Ich grabe %s aus"),
            List.of("Ich habe %s abgebaut", "Ich habe %s geschürft", "Ich habe %s ausgegraben"),
            List.of(), List.of()),
    DIG("dig", false,
            List.of("Ich grabe %s", "Ich schaufle %s", "Ich hebe %s aus"),
            List.of("Ich habe %s gegraben", "Ich habe %s geschaufelt", "Ich habe %s ausgehoben"),
            List.of(), List.of()),
    ENCHANT("enchant", false, List.of("Ich verzaubere %s"), List.of("Ich habe %s verzaubert"), List.of(), List.of()),
    ANVIL("anvil", false,
            List.of("Ich repariere %s", "Ich bearbeite %s", "Ich verbessere %s", "Ich benenne %s um"),
            List.of("Ich habe %s repariert", "Ich habe %s bearbeitet", "Ich habe %s verbessert", "Ich habe %s umbenannt"),
            List.of(), List.of()),
    BREW("brew", false,
            List.of("Ich braue %s", "Ich mache %s"),
            List.of("Ich habe %s gebraut", "Ich habe %s gemacht"),
            List.of(), List.of()),
    SMITH("smith", false,
            List.of("Ich schmiede %s", "Ich verbessere %s", "Ich stelle %s her"),
            List.of("Ich habe %s geschmiedet", "Ich habe %s verbessert", "Ich habe %s hergestellt"),
            List.of(), List.of()),
    STONECUT("stonecut", false,
            List.of("Ich schneide %s", "Ich säge %s", "Ich stelle %s her", "Ich haue %s zu"),
            List.of("Ich habe %s geschnitten", "Ich habe %s gesägt", "Ich habe %s hergestellt", "Ich habe %s zugehauen"),
            List.of(), List.of()),
    WEAVE("weave", false,
            List.of("Ich webe %s", "Ich gestalte %s", "Ich mache %s"),
            List.of("Ich habe %s gewebt", "Ich habe %s gestaltet", "Ich habe %s gemacht"),
            List.of(), List.of()),
    MAP("map", false,
            List.of("Ich zeichne %s", "Ich bearbeite %s", "Ich kopiere %s", "Ich vergrößere %s"),
            List.of("Ich habe %s gezeichnet", "Ich habe %s bearbeitet", "Ich habe %s kopiert", "Ich habe %s vergrößert"),
            List.of(), List.of()),
    GRIND("grind", false,
            List.of("Ich schleife %s", "Ich repariere %s", "Ich entzaubere %s"),
            List.of("Ich habe %s geschliffen", "Ich habe %s repariert", "Ich habe %s entzaubert"),
            List.of(), List.of()),
    TRADE("trade", false,
            List.of("Ich kaufe %s", "Ich bekomme %s", "Ich tausche %s ein"),
            List.of("Ich habe %s gekauft", "Ich habe %s bekommen", "Ich habe %s eingetauscht"),
            List.of(), List.of()),
    FISH("fish", false,
            List.of("Ich angle %s", "Ich fange %s"),
            List.of("Ich habe %s geangelt", "Ich habe %s gefangen"),
            List.of(), List.of()),
    FEED("feed", false, List.of("Ich füttere %s"), List.of("Ich habe %s gefüttert"), List.of(), List.of()),
    PLANT("plant", false,
            List.of("Ich pflanze %s", "Ich säe %s"),
            List.of("Ich habe %s gepflanzt", "Ich habe %s gesät"),
            List.of(), List.of()),
    HARVEST("harvest", false,
            List.of("Ich ernte %s", "Ich sammle %s"),
            List.of("Ich habe %s geerntet", "Ich habe %s gesammelt"),
            List.of(), List.of()),
    HIT("hit", true,
            List.of("Ich schlage %s", "Ich greife %s an", "Ich haue %s", "Ich bekämpfe %s", "Ich kämpfe gegen %s"),
            List.of("Ich habe %s geschlagen", "Ich habe %s angegriffen", "Ich habe %s gehauen", "Ich habe %s bekämpft", "Ich habe gegen %s gekämpft"),
            List.of(), List.of()),
    SHOOT("shoot", true,
            List.of("Ich spanne %s", "Ich benutze %s"),
            List.of("Ich habe %s gespannt", "Ich habe %s benutzt"),
            List.of("Ich schieße mit dem Bogen", "Ich schieße einen Pfeil", "Ich schieße einen Pfeil ab", "Ich schieße mit Pfeil und Bogen"),
            List.of("Ich habe mit dem Bogen geschossen", "Ich habe einen Pfeil geschossen", "Ich habe einen Pfeil abgeschossen", "Ich habe mit Pfeil und Bogen geschossen")),
    BLOCK("block", true,
            List.of("Ich hebe %s", "Ich benutze %s", "Ich halte %s hoch"),
            List.of("Ich habe %s gehoben", "Ich habe %s benutzt", "Ich habe %s hochgehalten"),
            List.of("Ich blocke den Angriff", "Ich wehre den Angriff ab", "Ich verteidige mich", "Ich schütze mich", "Ich blocke mit dem Schild"),
            List.of("Ich habe den Angriff geblockt", "Ich habe den Angriff abgewehrt", "Ich habe mich verteidigt", "Ich habe mich geschützt", "Ich habe mit dem Schild geblockt")),
    PLACE_FLOOR("place_floor", false,
            List.of("Ich stelle %s auf den Boden", "Ich setze %s auf den Boden", "Ich platziere %s auf den Boden", "Ich platziere %s auf dem Boden"),
            List.of("Ich habe %s auf den Boden gestellt", "Ich habe %s auf den Boden gesetzt", "Ich habe %s auf den Boden platziert", "Ich habe %s auf dem Boden platziert"),
            List.of(), List.of()),
    PLACE_WALL("place_wall", false,
            List.of("Ich hänge %s an die Wand", "Ich befestige %s an der Wand", "Ich setze %s an die Wand", "Ich platziere %s an der Wand"),
            List.of("Ich habe %s an die Wand gehängt", "Ich habe %s an der Wand befestigt", "Ich habe %s an die Wand gesetzt", "Ich habe %s an der Wand platziert"),
            List.of(), List.of());

    public final String key;
    /** Combat-like actions get an extra, longer cooldown. */
    public final boolean combat;
    private final List<String> present;
    private final List<String> perfect;
    private final List<String> extraPresent;
    private final List<String> extraPerfect;

    Action(String key, boolean combat, List<String> present, List<String> perfect,
           List<String> extraPresent, List<String> extraPerfect) {
        this.key = key;
        this.combat = combat;
        this.present = present;
        this.perfect = perfect;
        this.extraPresent = extraPresent;
        this.extraPerfect = extraPerfect;
    }

    /** Accepted verb templates; %s is replaced by the object phrase. The first one is the "main" answer. */
    public List<String> templates(Config.Tense tense, boolean food) {
        boolean pres = tense == Config.Tense.PRESENT;
        if (this == SMELT) {
            if (pres) {
                return food
                        ? List.of("Ich brate %s", "Ich koche %s", "Ich backe %s", "Ich schmelze %s")
                        : List.of("Ich schmelze %s", "Ich brate %s", "Ich koche %s", "Ich backe %s");
            }
            return food
                    ? List.of("Ich habe %s gebraten", "Ich habe %s gekocht", "Ich habe %s gebacken", "Ich habe %s geschmolzen")
                    : List.of("Ich habe %s geschmolzen", "Ich habe %s gebraten", "Ich habe %s gekocht", "Ich habe %s gebacken");
        }
        return pres ? present : perfect;
    }

    /** Extra complete sentences that are always accepted (no object placeholder). */
    public List<String> extras(Config.Tense tense) {
        return tense == Config.Tense.PRESENT ? extraPresent : extraPerfect;
    }
}
