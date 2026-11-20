package dev.retrobit.gtlauncher;

/** Pure full-frame gait timing. Not connected to rendering until every frame exists. */
final class CharacterFrameCycle {
    static final int FRAME_COUNT = 8;
    private static final double TAU = 2 * Math.PI;
    static int index(double phase) {
        if (!Double.isFinite(phase)) return 0;
        double wrapped = ((phase % TAU) + TAU) % TAU;
        return ((int)Math.floor(wrapped / TAU * FRAME_COUNT + .5)) % FRAME_COUNT;
    }
    /** Return -1 rather than rendering an incomplete sequence. */
    static int availableIndex(Object[] frames, double phase) {
        if (frames == null || frames.length != FRAME_COUNT) return -1;
        for (Object frame : frames) if (frame == null) return -1;
        return index(phase);
    }
    private CharacterFrameCycle() {}
}
