package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONException;

/** Immutable flat or stacked terrain; parsing this model alone does not enable world import. */
final class BlockTerrain {
    private final int size;
    private final byte[][] columns;
    private final byte[] flat;
    private BlockTerrain(int size,byte[][] columns,byte[] flat) { this.size=size;this.columns=columns;this.flat=flat; }
    private static int material(Object value,int min,int max) throws JSONException {
        if(!(value instanceof Number)) throw new JSONException("Invalid block material");
        double n=((Number)value).doubleValue();
        if(!Double.isFinite(n) || n!=Math.rint(n) || n<min || n>max) throw new JSONException("Invalid block material");
        return (int)n;
    }
    /** An absent stack layer means one block for each nonempty tile, as in old saves. */
    static BlockTerrain parse(int size,JSONArray tiles,JSONArray stacks) throws JSONException {
        if(size<1 || size>100 || tiles==null || tiles.length()!=size*size
                || (stacks!=null && stacks.length()!=size*size)) throw new JSONException("Invalid block terrain size");
        byte[][] columns=stacks==null?null:new byte[size*size][];
        byte[] flat=stacks==null?new byte[size*size]:null;
        for(int i=0;i<size*size;i++) {
            int top=material(tiles.get(i),0,10);
            if(stacks==null) {
                flat[i]=(byte)top;
            } else {
                Object value=stacks.get(i);
                if(!(value instanceof JSONArray)) throw new JSONException("Invalid block stack");
                JSONArray stack=(JSONArray)value;
                if(stack.length()>32) throw new JSONException("Block stack too tall");
                byte[] blocks=new byte[stack.length()];
                for(int n=0;n<blocks.length;n++) blocks[n]=(byte)material(stack.get(n),1,10);
                if((blocks.length==0?0:blocks[blocks.length-1])!=top) throw new JSONException("Block surface mismatch");
                columns[i]=blocks;
            }
        }
        return new BlockTerrain(size,columns,flat);
    }
    private int index(int column,int row) {
        if(column<0 || row<0 || column>=size || row>=size) throw new IndexOutOfBoundsException();
        return row*size+column;
    }
    int height(int column,int row) { int i=index(column,row);return flat!=null?(flat[i]==0?0:1):columns[i].length; }
    int top(int column,int row) {
        int i=index(column,row);if(flat!=null)return flat[i];
        byte[] blocks=columns[i];return blocks.length==0?0:blocks[blocks.length-1];
    }
    int block(int column,int row,int level) {
        int i=index(column,row);
        if(flat==null)return columns[i][level];
        if(level!=0 || flat[i]==0)throw new IndexOutOfBoundsException();
        return flat[i];
    }
    boolean canStand(int column,int row) {
        return column>=0 && row>=0 && column<size && row<size && height(column,row)>0;
    }
    boolean canStep(int fromColumn,int fromRow,int toColumn,int toRow) {
        return canStand(fromColumn,fromRow) && canStand(toColumn,toRow)
                && Math.abs(fromColumn-toColumn)+Math.abs(fromRow-toRow)==1
                && Math.abs(height(fromColumn,fromRow)-height(toColumn,toRow))<=1;
    }
}
