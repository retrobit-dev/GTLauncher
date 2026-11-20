package dev.retrobit.gtlauncher;

/** Stable spatial variation for the decorative grass texture. Does not alter world data. */
final class GrassDetail {
    static int variant(int column, int row) {
        int hash = (column * 73856093) ^ (row * 19349663);
        hash ^= hash >>> 16;
        hash *= 0x7feb352d;
        hash ^= hash >>> 15;
        int slot = hash & 7;
        return slot < 4 ? 0 : 1 + (slot - 4) % 3;
    }
    private GrassDetail() {}
}
