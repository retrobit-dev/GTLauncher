package dev.retrobit.gtlauncher;

/** Geometry only. No terrain or movement changes are made by this class. */
final class RampGeometry {
    static final class Vertex {
        final double u,v,z,x,y;
        Vertex(double u,double v,double z,double width,double depth,double height) {
            this.u=u;this.v=v;this.z=z;this.x=(u-v)*width/2;this.y=(u+v-1)*depth/2-z*height;
        }
    }
    static final class Shape {
        final Vertex[] top,highFace;final Vertex[][] sides;
        Shape(Vertex[] top,Vertex[][] sides,Vertex[] highFace){this.top=top;this.sides=sides;this.highFace=highFace;}
    }
    static double fraction(String direction,double u,double v) {
        if(direction==null || !Double.isFinite(u) || !Double.isFinite(v) || u<0 || u>1 || v<0 || v>1)return Double.NaN;
        switch(direction){case "N":return 1-v;case "E":return u;case "S":return v;case "W":return 1-u;default:return Double.NaN;}
    }
    static Shape create(String direction,double width,double depth,double height) {
        if(!Double.isFinite(width)||!Double.isFinite(depth)||!Double.isFinite(height)||width<=0||depth<=0||height<=0||Double.isNaN(fraction(direction,0,0)))return null;
        int[][] corners={{0,0},{1,0},{1,1},{0,1}};
        Vertex[] top=new Vertex[4];for(int i=0;i<4;i++){int u=corners[i][0],v=corners[i][1];top[i]=new Vertex(u,v,fraction(direction,u,v),width,depth,height);}
        int[][] ends;
        switch(direction){case "E":ends=new int[][]{{0,0,1,0},{0,1,1,1}};break;case "W":ends=new int[][]{{1,0,0,0},{1,1,0,1}};break;case "S":ends=new int[][]{{0,0,0,1},{1,0,1,1}};break;default:ends=new int[][]{{0,1,0,0},{1,1,1,0}};}
        Vertex[][] sides=new Vertex[2][3];for(int i=0;i<2;i++){int[] e=ends[i];sides[i]=new Vertex[]{new Vertex(e[0],e[1],0,width,depth,height),new Vertex(e[2],e[3],0,width,depth,height),new Vertex(e[2],e[3],1,width,depth,height)};}
        int[] a=ends[0],b=ends[1];
        Vertex[] highFace={new Vertex(a[2],a[3],0,width,depth,height),new Vertex(b[2],b[3],0,width,depth,height),new Vertex(b[2],b[3],1,width,depth,height),new Vertex(a[2],a[3],1,width,depth,height)};
        return new Shape(top,sides,highFace);
    }
    // Null direction means flat; a ramp rises from height to height + 1.
    // Its triangular sides do not connect to paths.
    static boolean canConnect(int heightA,String directionA,int heightB,String directionB,String edge) {
        if(heightA<1 || heightB<1 || !isDirection(edge) || directionA!=null && !isDirection(directionA) || directionB!=null && !isDirection(directionB))return false;
        if(directionA==null && directionB==null)return Math.abs((long)heightA-heightB)<=1;
        long a=endpoint(heightA,directionA,edge),b=endpoint(heightB,directionB,opposite(edge));
        return a>=0 && a==b;
    }
    private static boolean isDirection(String d) {return "N".equals(d)||"E".equals(d)||"S".equals(d)||"W".equals(d);}
    private static String opposite(String d) {switch(d){case "N":return "S";case "E":return "W";case "S":return "N";default:return "E";}}
    private static long endpoint(int height,String direction,String edge) {
        if(direction==null)return height;
        if(direction.equals(edge))return (long)height+1;
        return opposite(direction).equals(edge)?height:-1;
    }
    private RampGeometry(){}
}
