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
    private CharacterShadow() {}
}
