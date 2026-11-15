package dev.retrobit.gtlauncher;

/** Decorative cloud density, maintaining the 9×9 weather thresholds per island area. */
final class CloudLayout {
    static int count(double cover, int gridSize) {
        double value = Math.max(0, Math.min(100, cover));
        int baseline = 1 + (value >= 32 ? 1 : 0) + (value >= 58 ? 1 : 0) + (value >= 82 ? 1 : 0);
        return (int)Math.ceil(baseline * gridSize * gridSize / 81.0 * (gridSize == 20 ? 5 : 1));
    }
    private CloudLayout() {}
}
