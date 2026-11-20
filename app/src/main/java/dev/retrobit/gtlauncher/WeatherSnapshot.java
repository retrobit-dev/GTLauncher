package dev.retrobit.gtlauncher;

/** A small, cached set of current weather values used by the sky renderer. */
public final class WeatherSnapshot {
    public final int weatherCode;
    public final double cloudCoverPercent;
    public final double precipitationMm;
    public final double rainMm;
    public final double windSpeedKmh;
    public final double windDirectionDegrees;
    public final double latitude;
    public final double longitude;
    public final long fetchedAtMillis;
    public final boolean fallbackLocation;

    public WeatherSnapshot(
            int weatherCode,
            double cloudCoverPercent,
            double precipitationMm,
            double rainMm,
            double windSpeedKmh,
            double windDirectionDegrees,
            double latitude,
            double longitude,
            long fetchedAtMillis,
            boolean fallbackLocation) {
        this.weatherCode = weatherCode;
        this.cloudCoverPercent = clamp(cloudCoverPercent, 0.0, 100.0);
        this.precipitationMm = Math.max(0.0, precipitationMm);
        this.rainMm = Math.max(0.0, rainMm);
        this.windSpeedKmh = Math.max(0.0, windSpeedKmh);
        this.windDirectionDegrees = normalize(windDirectionDegrees);
        this.latitude = latitude;
        this.longitude = longitude;
        this.fetchedAtMillis = fetchedAtMillis;
        this.fallbackLocation = fallbackLocation;
    }

    public boolean hasDrizzle() {
        return weatherCode >= 51 && weatherCode <= 57;
    }

    public boolean hasRain() {
        if (isSolidPrecipitationCode()) return false;
        return (weatherCode >= 61 && weatherCode <= 67)
                || (weatherCode >= 80 && weatherCode <= 82)
                || rainMm > 0.05
                || precipitationMm > 0.1;
    }

    private boolean isSolidPrecipitationCode() {
        return (weatherCode >= 71 && weatherCode <= 77)
                || (weatherCode >= 85 && weatherCode <= 86);
    }

    public boolean hasStorm() {
        return weatherCode >= 95 && weatherCode <= 99;
    }

    private static double normalize(double degrees) {
        return ((degrees % 360.0) + 360.0) % 360.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
