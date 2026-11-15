package dev.retrobit.gtlauncher;

/** Decorative shadow layout, adjusted to the weather category. */
final class CloudLayout {
    static int count(double cover, int weatherCode) {
        double value = Math.max(0, Math.min(100, cover));
        int count = value >= 82 ? 8 : value >= 58 ? 6 : value >= 32 ? 4 : 3;
        if (weatherCode == 0 || weatherCode == 1) return Math.min(count, 4);
        if (weatherCode == 3 || weatherCode == 45 || weatherCode == 48 || weatherCode >= 51)
            count = Math.max(count, 5);
        return count;
    }
    static float cellSize(float tileWidth, float tileHeight, int columns, int rows) {
        if (tileWidth <= 0 || tileHeight <= 0 || columns <= 0 || rows <= 0) throw new IllegalArgumentException("Invalid cloud dimensions");
        return 4f * Math.min(tileWidth/columns,tileHeight/rows);
    }
    // Fade together so intersecting masks keep a single, uniform shadow opacity.
    static double shadowFade(double seconds, boolean animationEnabled) {
        return animationEnabled ? (1 + Math.cos(2 * Math.PI * seconds / 24.0)) / 2 : 1;
    }
    private CloudLayout() {}
}
