package dev.retrobit.gtlauncher;

/** Clock HUD phase follows the local Sun, not the rainfall-season icon or demo sky. */
final class DayNight {
    static boolean isNight(long timeMillis, double latitude, double longitude) {
        return SkyMath.sun(timeMillis, latitude, longitude).altitudeDegrees <= 0.0;
    }
    private DayNight() {}
}
