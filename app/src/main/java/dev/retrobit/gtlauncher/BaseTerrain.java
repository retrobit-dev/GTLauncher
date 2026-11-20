package dev.retrobit.gtlauncher;

/** Built-in Base World only: four earth blocks under one grass block. */
final class BaseTerrain {
    static final int SIZE=50, HEIGHT=5;
    static int block(int column,int row,int level) {
        if(column<0 || row<0 || column>=SIZE || row>=SIZE || level<0 || level>=HEIGHT)throw new IndexOutOfBoundsException();
        return level==HEIGHT-1?1:2;
    }
    private BaseTerrain() {}
}
