package com.deutschimspiel;

/** Colour palette for the prompt window. */
public record Theme(
        Config.ThemeId id,
        int bg, int border, int border2, int accent, int accentText,
        int title, int text, int hint,
        int btn, int btnHover, int btnText,
        int field, int fieldBorder, int slot,
        int ok, int warn, int bad,
        boolean sparkles, boolean pulse) {

    public static Theme of(Config.ThemeId id) {
        switch (id) {
            case PERGAMENT:
                return new Theme(id,
                        0xFFE8D5A8, 0xFF6B4423, 0xFFA67C45, 0xFF8B5A2B, 0xFFFFF3D6,
                        0xFF5A2E0E, 0xFF3B2412, 0xFF7A5A3A,
                        0xFFB98A52, 0xFFD6A867, 0xFF2E1A0A,
                        0xFFF6EBCB, 0xFF8B5A2B, 0xFFD8C08A,
                        0xFF2E7D32, 0xFF9A5B00, 0xFFB3261E,
                        false, false);
            case MODERN:
                return new Theme(id,
                        0xFF121417, 0xFF2A2F36, 0xFF38BDF8, 0xFF38BDF8, 0xFF04202E,
                        0xFFFFFFFF, 0xFFE5E7EB, 0xFF94A3B8,
                        0xFF1F2937, 0xFF334155, 0xFFFFFFFF,
                        0xFF0B0D10, 0xFF334155, 0xFF1B1F24,
                        0xFF4ADE80, 0xFFFACC15, 0xFFF87171,
                        false, false);
            case MAGIE:
            default:
                return new Theme(id,
                        0xFF1A1033, 0xFF7B4DFF, 0xFFC77DFF, 0xFF7B4DFF, 0xFFFFFFFF,
                        0xFFE9D5FF, 0xFFEDE7FF, 0xFF9C8CD0,
                        0xFF3B2A78, 0xFF5B3FB8, 0xFFFFFFFF,
                        0xFF0E0820, 0xFF7B4DFF, 0xFF2A1A55,
                        0xFF6EE7B7, 0xFFFCD34D, 0xFFFF6B8B,
                        true, true);
        }
    }
}
