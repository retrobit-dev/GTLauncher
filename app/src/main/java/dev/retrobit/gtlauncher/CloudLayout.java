package dev.retrobit.gtlauncher;

/** Decorative cloud density, maintaining the 9×9 weather thresholds per island area. */
final class CloudLayout {
    static int count(double cover, int gridSize) {
        double value = Math.max(0, Math.min(100, cover));
        int baseline = 1 + (value >= 32 ? 1 : 0) + (value >= 58 ? 1 : 0) + (value >= 82 ? 1 : 0);
        return (int)Math.ceil(baseline * gridSize * gridSize / 32.0);
    }
    static float cellSize(float tileWidth, float tileHeight, int columns, int rows) {
        if (tileWidth <= 0 || tileHeight <= 0 || columns <= 0 || rows <= 0) throw new IllegalArgumentException("Invalid cloud dimensions");
        return Math.min(tileWidth/columns,tileHeight/rows);
    }
    // Fade together so intersecting masks keep a single, uniform shadow opacity.
    static double shadowFade(double seconds, boolean animationEnabled) {
        return animationEnabled ? (1 + Math.cos(2 * Math.PI * seconds / 24.0)) / 2 : 1;
    }
    private CloudLayout() {}
}
