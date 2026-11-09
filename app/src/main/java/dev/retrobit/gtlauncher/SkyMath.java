package dev.retrobit.gtlauncher;

/** Low-precision NOAA/Meeus-style Sun, approximate Moon, and precessed bright-star positions. */
public final class SkyMath {
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static final double RAD_TO_DEG = 180.0 / Math.PI;

    public static final Star[] BRIGHT_STARS = {
            new Star(101.287, -16.716, 1.00), // Sirius
            new Star(95.988, -52.696, 0.78),  // Canopus
            new Star(213.915, 19.182, 0.72),  // Arcturus
            new Star(79.172, 45.998, 0.65),   // Capella
            new Star(78.634, -8.202, 0.61),   // Rigel
            new Star(88.793, 7.407, 0.57),    // Betelgeuse
            new Star(279.234, 38.783, 0.55),  // Vega
            new Star(201.298, -11.161, 0.48), // Spica
            new Star(247.351, -26.432, 0.45), // Antares
            new Star(297.695, 8.868, 0.42),   // Altair
            new Star(68.980, 16.509, 0.40),   // Aldebaran
            new Star(310.358, 45.280, 0.38),  // Deneb
            new Star(114.825, 5.225, 0.35),   // Procyon
            new Star(186.650, -63.099, 0.34), // Alpha Centauri
            new Star(24.429, -57.237, 0.30),  // Achernar
            new Star(154.993, 19.842, 0.29)   // Regulus
    };

    private SkyMath() { }

    public static Position sun(long timeMillis, double latitude, double longitude) {
        double julianDay = julianDay(timeMillis);
        double centuries = (julianDay - 2451545.0) / 36525.0;
        double meanLongitude = normalizeDegrees(
                280.46646 + centuries * (36000.76983 + centuries * 0.0003032));
        double meanAnomaly = normalizeDegrees(
                357.52911 + centuries * (35999.05029 - 0.0001537 * centuries));
        double eccentricity = 0.016708634
                - centuries * (0.000042037 + 0.0000001267 * centuries);
        double anomalyRadians = meanAnomaly * DEG_TO_RAD;
        double equationOfCenter = Math.sin(anomalyRadians)
                * (1.914602 - centuries * (0.004817 + 0.000014 * centuries))
                + Math.sin(2.0 * anomalyRadians) * (0.019993 - 0.000101 * centuries)
                + Math.sin(3.0 * anomalyRadians) * 0.000289;
        double omega = 125.04 - 1934.136 * centuries;
        double eclipticLongitude = meanLongitude + equationOfCenter
                - 0.00569 - 0.00478 * Math.sin(omega * DEG_TO_RAD);
        double meanObliquity = 23.0 + (26.0
                + (21.448 - centuries * (46.815 + centuries * (0.00059 - centuries * 0.001813)))
                / 60.0) / 60.0;
        double obliquity = meanObliquity + 0.00256 * Math.cos(omega * DEG_TO_RAD);
        double rightAscension = normalizeDegrees(Math.atan2(
                Math.cos(obliquity * DEG_TO_RAD) * Math.sin(eclipticLongitude * DEG_TO_RAD),
                Math.cos(eclipticLongitude * DEG_TO_RAD)) * RAD_TO_DEG);
        double declination = Math.asin(
                Math.sin(obliquity * DEG_TO_RAD) * Math.sin(eclipticLongitude * DEG_TO_RAD))
                * RAD_TO_DEG;
        return equatorialToHorizon(rightAscension, declination, julianDay, latitude, longitude);
    }

    /** Low-precision lunar orbital elements; this omits the full lunar perturbation series. */
    public static Position moon(long timeMillis, double latitude, double longitude) {
        double julianDay = julianDay(timeMillis);
        double days = julianDay - 2451543.5;
        double node = (125.1228 - 0.0529538083 * days) * DEG_TO_RAD;
        double inclination = 5.1454 * DEG_TO_RAD;
        double periapsis = (318.0634 + 0.1643573223 * days) * DEG_TO_RAD;
        double eccentricity = 0.0549;
        double meanAnomaly = normalizeDegrees(115.3654 + 13.0649929509 * days) * DEG_TO_RAD;
        double eccentricAnomaly = meanAnomaly + eccentricity * Math.sin(meanAnomaly)
                * (1.0 + eccentricity * Math.cos(meanAnomaly));
        for (int iteration = 0; iteration < 3; iteration++) {
            eccentricAnomaly -= (eccentricAnomaly - eccentricity * Math.sin(eccentricAnomaly)
                    - meanAnomaly) / (1.0 - eccentricity * Math.cos(eccentricAnomaly));
        }

        double x = Math.cos(eccentricAnomaly) - eccentricity;
        double y = Math.sqrt(1.0 - eccentricity * eccentricity) * Math.sin(eccentricAnomaly);
        double trueAnomaly = Math.atan2(y, x);
        double distance = Math.sqrt(x * x + y * y);
        double argument = trueAnomaly + periapsis;
        double eclipticX = distance * (Math.cos(node) * Math.cos(argument)
                - Math.sin(node) * Math.sin(argument) * Math.cos(inclination));
        double eclipticY = distance * (Math.sin(node) * Math.cos(argument)
                + Math.cos(node) * Math.sin(argument) * Math.cos(inclination));
        double eclipticZ = distance * Math.sin(argument) * Math.sin(inclination);
        double eclipticLongitude = Math.atan2(eclipticY, eclipticX);
        double obliquity = 23.4393 * DEG_TO_RAD;
        double equatorialY = eclipticY * Math.cos(obliquity) - eclipticZ * Math.sin(obliquity);
        double equatorialZ = eclipticY * Math.sin(obliquity) + eclipticZ * Math.cos(obliquity);
        double rightAscension = normalizeDegrees(Math.atan2(equatorialY, eclipticX) * RAD_TO_DEG);
        double declination = Math.atan2(equatorialZ,
                Math.sqrt(eclipticX * eclipticX + equatorialY * equatorialY)) * RAD_TO_DEG;
        double sunLongitude = solarEclipticLongitude(julianDay);
        double phase = normalizeDegrees(eclipticLongitude * RAD_TO_DEG - sunLongitude) / 360.0;
        double illumination = (1.0 - Math.cos(phase * 2.0 * Math.PI)) * 0.5;
        Position horizon = equatorialToHorizon(
                rightAscension, declination, julianDay, latitude, longitude);
        return new Position(horizon.azimuthDegrees, horizon.altitudeDegrees,
                illumination, horizon.hourAngleDegrees);
    }

    /** Star catalogue coordinates are J2000; apply Meeus' IAU 1976 precession before projection. */
    public static Position star(Star star, long timeMillis, double latitude, double longitude) {
        double jd = julianDay(timeMillis);
        double centuries = (jd - 2451545.0) / 36525.0;
        double t2 = centuries * centuries;
        double t3 = t2 * centuries;
        double zeta = (2306.2181 * centuries + 0.30188 * t2 + 0.017998 * t3)
                / 3600.0 * DEG_TO_RAD;
        double z = (2306.2181 * centuries + 1.09468 * t2 + 0.018203 * t3)
                / 3600.0 * DEG_TO_RAD;
        double theta = (2004.3109 * centuries - 0.42665 * t2 - 0.041833 * t3)
                / 3600.0 * DEG_TO_RAD;
        double alpha = star.rightAscensionDegrees * DEG_TO_RAD;
        double delta = star.declinationDegrees * DEG_TO_RAD;
        double a = Math.cos(delta) * Math.sin(alpha + zeta);
        double b = Math.cos(theta) * Math.cos(delta) * Math.cos(alpha + zeta)
                - Math.sin(theta) * Math.sin(delta);
        double c = Math.sin(theta) * Math.cos(delta) * Math.cos(alpha + zeta)
                + Math.cos(theta) * Math.sin(delta);
        double precessedRightAscension = normalizeDegrees(Math.atan2(a, b) * RAD_TO_DEG
                + z * RAD_TO_DEG);
        double precessedDeclination = Math.asin(Math.max(-1.0, Math.min(1.0, c))) * RAD_TO_DEG;
        return equatorialToHorizon(precessedRightAscension, precessedDeclination,
                jd, latitude, longitude);
    }

    public static double normalizeDegrees(double degrees) {
        return ((degrees % 360.0) + 360.0) % 360.0;
    }

    private static double solarEclipticLongitude(double julianDay) {
        double centuries = (julianDay - 2451545.0) / 36525.0;
        double meanLongitude = normalizeDegrees(
                280.46646 + centuries * (36000.76983 + centuries * 0.0003032));
        double meanAnomaly = normalizeDegrees(
                357.52911 + centuries * (35999.05029 - 0.0001537 * centuries));
        double radians = meanAnomaly * DEG_TO_RAD;
        double center = Math.sin(radians)
                * (1.914602 - centuries * (0.004817 + 0.000014 * centuries))
                + Math.sin(2.0 * radians) * (0.019993 - 0.000101 * centuries)
                + Math.sin(3.0 * radians) * 0.000289;
        return normalizeDegrees(meanLongitude + center);
    }

    private static Position equatorialToHorizon(
            double rightAscension,
            double declination,
            double julianDay,
            double latitude,
            double longitude) {
        double t = (julianDay - 2451545.0) / 36525.0;
        double greenwichSidereal = normalizeDegrees(280.46061837
                + 360.98564736629 * (julianDay - 2451545.0)
                + 0.000387933 * t * t - t * t * t / 38710000.0);
        double localSidereal = normalizeDegrees(greenwichSidereal + longitude);
        double hourAngleDegrees = normalizeSigned(localSidereal - rightAscension);
        double hourAngle = hourAngleDegrees * DEG_TO_RAD;
        double dec = declination * DEG_TO_RAD;
        double lat = latitude * DEG_TO_RAD;
        double altitude = Math.asin(Math.sin(lat) * Math.sin(dec)
                + Math.cos(lat) * Math.cos(dec) * Math.cos(hourAngle));
        double azimuth = Math.atan2(-Math.sin(hourAngle),
                Math.tan(dec) * Math.cos(lat) - Math.sin(lat) * Math.cos(hourAngle));
        return new Position(normalizeDegrees(azimuth * RAD_TO_DEG), altitude * RAD_TO_DEG,
                0.0, hourAngleDegrees);
    }

    private static double julianDay(long timeMillis) {
        return timeMillis / 86400000.0 + 2440587.5;
    }

    private static double normalizeSigned(double degrees) {
        double normalized = normalizeDegrees(degrees);
        return normalized > 180.0 ? normalized - 360.0 : normalized;
    }

    public static final class Position {
        public final double azimuthDegrees;
        public final double altitudeDegrees;
        public final double illumination;
        /** Local apparent hour angle: negative before solar noon, positive after it. */
        public final double hourAngleDegrees;

        Position(double azimuthDegrees, double altitudeDegrees, double illumination,
                 double hourAngleDegrees) {
            this.azimuthDegrees = azimuthDegrees;
            this.altitudeDegrees = altitudeDegrees;
            this.illumination = illumination;
            this.hourAngleDegrees = hourAngleDegrees;
        }
    }

    /** Fixed J2000 catalog coordinates; proper motion and annual aberration are omitted. */
    public static final class Star {
        public final double rightAscensionDegrees;
        public final double declinationDegrees;
        public final double brightness;

        Star(double rightAscensionDegrees, double declinationDegrees, double brightness) {
            this.rightAscensionDegrees = rightAscensionDegrees;
            this.declinationDegrees = declinationDegrees;
            this.brightness = brightness;
        }
    }
}
