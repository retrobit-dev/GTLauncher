package dev.retrobit.gtlauncher;

/** Per-tile timing and gait for a route. The first and last tiles are always walked. */
final class CharacterGait {
    static boolean running(int step, int totalSteps) {
        return totalSteps > 3 && step > 1 && step < totalSteps;
    }
    static int durationMillis(int step, int totalSteps) {
        return running(step,totalSteps) ? 180 : 300;
    }
    static float progress(long elapsed, int duration) {
        return Math.max(0f, Math.min(1f, (float)elapsed / duration));
    }
    static float runBlend(int step, int totalSteps, float progress) {
        if (!running(step,totalSteps)) return 0f;
        float in = step == 2 ? progress * 4 : 1f;
        float out = step == totalSteps - 1 ? (1f-progress) * 4 : 1f;
        return Math.max(0f,Math.min(1f,Math.min(in,out)));
    }
    static float elbowRadians(float phase, float runBlend, boolean left) {
        return runBlend * (1.05f + (left ? .18f : -.18f) * phase);
    }
    static double phase(int step, float progress) {
        return (step - 1 + progress) * Math.PI * 2;
    }
    private CharacterGait() {}
}
