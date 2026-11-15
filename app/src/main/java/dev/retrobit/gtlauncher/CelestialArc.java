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
    private CelestialArc() {}
}
