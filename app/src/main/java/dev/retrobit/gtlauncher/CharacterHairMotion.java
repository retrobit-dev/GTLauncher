package dev.retrobit.gtlauncher;

/** Small gait-linked hair offset, fading to zero above the face. */
final class CharacterHairMotion {
    static final float HAIR_END = .16f;
    static float offset(float y, float gait, float runBlend, float scale, boolean enabled) {
        if (!enabled || !Float.isFinite(y) || !Float.isFinite(gait) || !Float.isFinite(runBlend)
                || !Float.isFinite(scale) || scale <= 0 || y >= HAIR_END) return 0;
        float falloff = Math.max(0, Math.min(1, 1-Math.max(0,y)/HAIR_END));
        float running = Math.max(0, Math.min(1, runBlend));
        return Math.max(-1, Math.min(1,gait)) * (.2f + .12f*running) * scale * falloff * falloff;
    }
    private CharacterHairMotion() {}
}
