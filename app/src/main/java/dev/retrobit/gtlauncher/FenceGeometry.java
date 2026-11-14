package dev.retrobit.gtlauncher;

import java.util.List;

/** Matches preview's inset fence edges and shared posts, including opposite edge ownership. */
final class FenceGeometry {
    static final class Edge {
        final int column, row; final String side;
        Edge(int column,int row,String side){this.column=column;this.row=row;this.side=side;}
    }
    static final class Bounds {
        final float x,y,width,height,firstX,firstY,secondX,secondY;final boolean rising;
        Bounds(float x,float y,float width,float height,float x1,float y1,float x2,float y2,boolean rising) {
            this.x=x;this.y=y;this.width=width;this.height=height;firstX=x1;firstY=y1;secondX=x2;secondY=y2;this.rising=rising;
        }
    }
    private static boolean exists(List<Edge> edges,int column,int row,String side) {
        for(Edge e:edges)if(e.column==column && e.row==row && e.side.equals(side))return true;
        return false;
    }
    private static int neighbor(Edge fence,List<Edge> edges,int delta) {
        boolean along=fence.side.equals("N")||fence.side.equals("S");
        int c=fence.column+(along?delta:0),r=fence.row+(along?0:-delta);
        if(exists(edges,c,r,fence.side))return 1;
        int dc=fence.side.equals("E")?1:fence.side.equals("W")?-1:0,dr=fence.side.equals("S")?1:fence.side.equals("N")?-1:0;
        String opposite=fence.side.equals("N")?"S":fence.side.equals("S")?"N":fence.side.equals("E")?"W":"E";
        return exists(edges,c+dc,r+dr,opposite)?2:0;
    }
    static Bounds bounds(Edge edge,List<Edge> edges,float cx,float cy,float tileWidth,float tileHeight) {
        float hw=tileWidth/2,hh=tileHeight/2,x1,y1,x2,y2;
        switch(edge.side){case "N":x1=cx;y1=cy-hh;x2=cx+hw;y2=cy;break;case "E":x1=cx;y1=cy+hh;x2=cx+hw;y2=cy;break;case "S":x1=cx-hw;y1=cy;x2=cx;y2=cy+hh;break;case "W":x1=cx-hw;y1=cy;x2=cx;y2=cy-hh;break;default:throw new IllegalArgumentException("Invalid edge");}
        float dx=x2-x1,dy=y2-y1, ax=cx+(x1-cx)*.8f,ay=cy+(y1-cy)*.8f,bx=cx+(x2-cx)*.8f,by=cy+(y2-cy)*.8f;
        int first=neighbor(edge,edges,-1),second=neighbor(edge,edges,1);
        if(first==2){ax=x1;ay=y1;}else if(first==1){ax-=dx*.1f;ay-=dy*.1f;}
        if(second==2){bx=x2;by=y2;}else if(second==1){bx+=dx*.1f;by+=dy*.1f;}
        boolean rising=edge.side.equals("E")||edge.side.equals("W");
        float sx=(bx-ax)/(34.65f-4.63f),sy=Math.abs(by-ay)/(39.63f-26.82f);
        float sourceX=rising?4.63f:40-34.65f,sourceY=rising?39.63f:26.82f;
        return new Bounds(ax-sourceX*sx,ay-sourceY*sy,40*sx,40*sy,ax,ay,bx,by,rising);
    }
    private FenceGeometry() {}
}
