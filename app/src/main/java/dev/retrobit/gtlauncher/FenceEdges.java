package dev.retrobit.gtlauncher;

/** A tile boundary can be owned by either adjacent tile, but is only one edge. */
final class FenceEdges {
    static boolean blocks(int column,int row,String edge,int a,int b,int c,int d) {
        if(Math.abs(a-c)+Math.abs(b-d)!=1 || edge==null)return false;
        int x=column,y=row;
        switch(edge) {
            case "E": x++;break;
            case "W": x--;break;
            case "S": y++;break;
            case "N": y--;break;
            default:return false;
        }
        return (column==a && row==b && x==c && y==d)
                || (column==c && row==d && x==a && y==b);
    }
    private FenceEdges() {}
}
