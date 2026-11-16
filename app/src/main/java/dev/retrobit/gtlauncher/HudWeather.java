package dev.retrobit.gtlauncher;

/** Shared weather-code categories for the clock icon; negative means unavailable. */
final class HudWeather {
    static String kind(int code) {
        if (code == 0 || code == 1) return "clear";
        if (code == 2 || code == 3 || code == 45 || code == 48) return "cloud";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "rain";
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return "snow";
        if (code >= 95 && code <= 99) return "storm";
        return "unknown";
    }
    private HudWeather() {}
}
