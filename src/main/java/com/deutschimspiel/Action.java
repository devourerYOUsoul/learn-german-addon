package com.deutschimspiel;

import java.util.List;

public enum Action {
    EAT("eat"),
    DRINK("drink"),
    CRAFT("craft"),
    SMELT("smelt"),
    CHOP("chop"),
    MINE("mine"),
    DIG("dig");

    public final String key;

    Action(String key) {
        this.key = key;
    }

    /** Accepted verb templates; %s is replaced by the object phrase. The first one is the "main" answer. */
    public List<String> templates(Config.Tense tense, boolean food) {
        boolean present = tense == Config.Tense.PRESENT;
        switch (this) {
            case EAT:
                return present ? List.of("Ich esse %s") : List.of("Ich habe %s gegessen");
            case DRINK:
                return present ? List.of("Ich trinke %s") : List.of("Ich habe %s getrunken");
            case CRAFT:
                return present
                        ? List.of("Ich stelle %s her", "Ich baue %s", "Ich bastle %s", "Ich mache %s")
                        : List.of("Ich habe %s hergestellt", "Ich habe %s gebaut", "Ich habe %s gebastelt", "Ich habe %s gemacht");
            case CHOP:
                return present
                        ? List.of("Ich fälle %s", "Ich hacke %s", "Ich haue %s")
                        : List.of("Ich habe %s gefällt", "Ich habe %s gehackt", "Ich habe %s gehauen");
            case MINE:
                return present
                        ? List.of("Ich baue %s ab", "Ich schürfe %s", "Ich grabe %s aus")
                        : List.of("Ich habe %s abgebaut", "Ich habe %s geschürft", "Ich habe %s ausgegraben");
            case DIG:
                return present
                        ? List.of("Ich grabe %s", "Ich schaufle %s", "Ich hebe %s aus")
                        : List.of("Ich habe %s gegraben", "Ich habe %s geschaufelt", "Ich habe %s ausgehoben");
            case SMELT:
            default:
                if (present) {
                    return food
                            ? List.of("Ich brate %s", "Ich koche %s", "Ich backe %s", "Ich schmelze %s")
                            : List.of("Ich schmelze %s", "Ich brate %s", "Ich koche %s", "Ich backe %s");
                }
                return food
                        ? List.of("Ich habe %s gebraten", "Ich habe %s gekocht", "Ich habe %s gebacken", "Ich habe %s geschmolzen")
                        : List.of("Ich habe %s geschmolzen", "Ich habe %s gebraten", "Ich habe %s gekocht", "Ich habe %s gebacken");
        }
    }

    /** Extra complete sentences that are always accepted (no object placeholder). */
    public List<String> extras(Config.Tense tense) {
        if (this == CHOP) {
            return tense == Config.Tense.PRESENT
                    ? List.of("Ich fälle den Baum", "Ich fälle einen Baum", "Ich fälle Bäume", "Ich hacke Holz", "Ich sammle Holz")
                    : List.of("Ich habe den Baum gefällt", "Ich habe einen Baum gefällt", "Ich habe Holz gehackt", "Ich habe Holz gesammelt");
        }
        return List.of();
    }
}
