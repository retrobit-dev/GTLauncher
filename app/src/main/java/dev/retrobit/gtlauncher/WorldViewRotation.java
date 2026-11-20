package dev.retrobit.gtlauncher;

/** Pure camera-coordinate transforms. Never mutate persisted world objects. */
final class WorldViewRotation {
    static final class Point {
        final double column,row;
        Point(double column,double row){this.column=column;this.row=row;}
    }
    static final class Rect {
        final int column,row,width,height;
        Rect(int c,int r,int w,int h){column=c;row=r;width=w;height=h;}
    }
    static final class Edge {
        final int column,row;final String edge;
        Edge(int c,int r,String e){column=c;row=r;edge=e;}
    }
    static final class Bridge {
        final int column,row,length;final String axis;
        Bridge(int c,int r,String a,int n){column=c;row=r;axis=a;length=n;}
    }
    static Point point(double column,double row,int size,int turns) {
        if(size<1 || !Double.isFinite(column) || !Double.isFinite(row))return null;
        double c=column,r=row;for(int i=0;i<Math.floorMod(turns,4);i++){double next=size-1-r;r=c;c=next;}return new Point(c,r);
    }
    static String direction(String direction,int turns) {
        String[] d={"N","E","S","W"};for(int i=0;i<4;i++)if(d[i].equals(direction))return d[Math.floorMod(i+Math.floorMod(turns,4),4)];return null;
    }
    static Rect rectangle(int column,int row,int width,int height,int size,int turns) {
        if(size<1 || width<1 || height<1 || column<0 || row<0 || width>size-column || height>size-row)return null;
        int c=column,r=row,w=width,h=height;for(int i=0;i<Math.floorMod(turns,4);i++){int nc=size-r-h,nr=c,t=w;c=nc;r=nr;w=h;h=t;}return new Rect(c,r,w,h);
    }
    static Edge fence(int column,int row,String edge,int size,int turns) {
        if(column<0 || row<0 || column>=size || row>=size)return null;
        Point p=point(column,row,size,turns);String d=direction(edge,turns);return p==null||d==null?null:new Edge((int)p.column,(int)p.row,d);
    }
    static Bridge bridge(int column,int row,String axis,int length,int size,int turns) {return bridge(column,row,axis,length,1,size,turns);}
    static Bridge bridge(int column,int row,String axis,int length,int width,int size,int turns) {
        if(width<1 || !("E".equals(axis)||"S".equals(axis)))return null;
        Rect r=rectangle(column,row,axis.equals("E")?length:width,axis.equals("S")?length:width,size,turns);
        if(r==null)return null;
        return new Bridge(r.column,r.row,r.width==length?"E":"S",length);
    }
    private WorldViewRotation(){}
}
