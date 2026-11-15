package dev.retrobit.gtlauncher;

/** Screen-space visual arc; rise/set timing follows hour angle, latitude and declination. */
final class CelestialArc {
    static float[] position(double hourAngle, double declination, double latitude,
                            float width, float horizonY, float margin) {
        double cosH=-Math.tan(Math.toRadians(latitude))*Math.tan(Math.toRadians(declination));
        double horizonAngle=Double.isFinite(cosH) && Math.abs(cosH)<1 ? Math.toDegrees(Math.acos(cosH)) : 180;
        double progress=Math.max(0,Math.min(1,.5+hourAngle/(2*horizonAngle)));
        return new float[]{(float)(width-margin-progress*(width-2*margin)),
                (float)(horizonY-(horizonY-margin)*Math.sin(progress*Math.PI))};
    }
    /** Half circle in island scene units, centred on the island. */
    static float[] islandOffset(double hourAngle, double declination, double latitude, float radius) {
        return islandOffset(hourAngle,declination,latitude,radius,radius);
    }
    static float[] islandOffset(double hourAngle, double declination, double latitude, float radiusX, float radiusY) {
        double cosH=-Math.tan(Math.toRadians(latitude))*Math.tan(Math.toRadians(declination));
        double horizonAngle=Double.isFinite(cosH) && Math.abs(cosH)<1 ? Math.toDegrees(Math.acos(cosH)) : 180;
        double progress=Math.max(0,Math.min(1,.5+hourAngle/(2*horizonAngle)));
        double angle=progress*Math.PI;
        return new float[]{(float)(radiusX*Math.cos(angle)),(float)(-radiusY*Math.sin(angle))};
    }
    static double islandShadowScale(double hourAngle, double declination, double latitude, double depth) {
        float[] offset = islandOffset(hourAngle, declination, latitude, 300, 175);
        double distance = Math.hypot(offset[0], offset[1]);
        return distance / (distance - depth);
    }
    static float moonRadius(float sunRadius) { return sunRadius * 0.5f; }
    private CelestialArc() {}
}
