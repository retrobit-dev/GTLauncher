package dev.retrobit.gtlauncher;
/** Stylized orthographic projection of a vertical sprite onto an isometric ground plane. */
final class CharacterShadow {
    static float[] offset(double azimuth,double altitude,float height) {
        double a=Math.toRadians(azimuth), elevation=Math.toRadians(Math.max(8,Math.min(90,altitude)));
        double length=Math.min(3*height,height/Math.tan(elevation));
        // N is upper-right and E is lower-right on the projected ground.
        return new float[]{(float)(-length*(Math.sin(a)+Math.cos(a))/Math.sqrt(2)),
                (float)(-length*(Math.sin(a)-Math.cos(a))*.30)};
    }
    // The transformed sprite spans width + |dx| horizontally and |dy| vertically.
    static float fitScale(float width, float height, float dx, float dy) {
        return Math.min(1f, Math.min(width / (width + Math.abs(dx)),
                Math.abs(dy) > 0 ? height / Math.abs(dy) : 1f));
    }
    private CharacterShadow() {}
}
