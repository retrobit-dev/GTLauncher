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
    static double phase(int step, float progress) {
        return (step - 1 + progress) * Math.PI * 2;
    }
    private CharacterGait() {}
}
