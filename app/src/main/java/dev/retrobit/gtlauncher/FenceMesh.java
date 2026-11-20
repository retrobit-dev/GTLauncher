package dev.retrobit.gtlauncher;

import java.util.ArrayList;
import java.util.List;

/** Isometric solids for portable fences; no change to their stored tile-edge identity. */
final class FenceMesh {
    static final class Face {
        final float[] points; final int color; final boolean detail;
        Face(int color,float... points){this(false,color,points);}
        Face(boolean detail,int color,float... points){this.detail=detail;this.color=color;this.points=points;}
    }
    static float[] endpoints(float cx,float cy,float tileWidth,float tileHeight,String edge) {
        float hw=tileWidth/2,hh=tileHeight/2;
        switch(edge) {
            case "N":return new float[]{cx,cy-hh,cx+hw,cy};
            case "E":return new float[]{cx,cy+hh,cx+hw,cy};
            case "S":return new float[]{cx-hw,cy,cx,cy+hh};
            case "W":return new float[]{cx-hw,cy,cx,cy-hh};
            default:throw new IllegalArgumentException("Invalid fence edge");
        }
    }
    private final float ax,ay,bx,by,nx,ny;
    private final List<Face> faces=new ArrayList<>();
    private FenceMesh(float ax,float ay,float bx,float by,boolean stone) {
        this.ax=ax;this.ay=ay;this.bx=bx;this.by=by;
        float slope=(by-ay)/(bx-ax),depth=stone?2.6f:1.8f;
        nx=slope>=0?-depth:depth;ny=Math.abs(slope)*depth;
    }
    private float x(float u,float v){return ax+(bx-ax)*u+nx*v;}
    private float y(float u,float v,float z){return ay+(by-ay)*u+ny*v-z;}
    private void box(float u0,float u1,float v0,float v1,float z0,float z1,int top,int front,int side) {
        box(u0,u1,v0,v1,z0,z1,top,front,side,true);
    }
    private void box(float u0,float u1,float v0,float v1,float z0,float z1,int top,int front,int side,boolean showEnd) {
        float near=by>=ay?u1:u0;
        if(showEnd)faces.add(new Face(side,x(near,v0),y(near,v0,z0),x(near,v1),y(near,v1,z0),x(near,v1),y(near,v1,z1),x(near,v0),y(near,v0,z1)));
        faces.add(new Face(front,x(u0,v1),y(u0,v1,z0),x(u1,v1),y(u1,v1,z0),x(u1,v1),y(u1,v1,z1),x(u0,v1),y(u0,v1,z1)));
        faces.add(new Face(top,x(u0,v0),y(u0,v0,z1),x(u1,v0),y(u1,v0,z1),x(u1,v1),y(u1,v1,z1),x(u0,v1),y(u0,v1,z1)));
    }
    private void mark(float u0,float u1,float v,float z0,float z1,int color) {
        faces.add(new Face(true,color,x(u0,v),y(u0,v,z0),x(u1,v),y(u1,v,z0),x(u1,v),y(u1,v,z1),x(u0,v),y(u0,v,z1)));
    }
    static List<Face> faces(float ax,float ay,float bx,float by,boolean stone) {
        return faces(ax,ay,bx,by,stone,null);
    }
    static List<Face> faces(float ax,float ay,float bx,float by,boolean stone,FenceJoins.Join joins) {
        return faces(ax,ay,bx,by,stone,false,joins);
    }
    static List<Face> faces(float ax,float ay,float bx,float by,boolean stone,boolean tall,FenceJoins.Join joins) {
        FenceMesh m=new FenceMesh(ax,ay,bx,by,stone);
        if(stone) {
            int[] tops={0xffbbc2af,0xffc4c7b8,0xffb6c0ad},fronts={0xff909a88,0xff9da595,0xff899583},sides={0xff737e6d,0xff7b8675,0xff707d6d};
            float[][] boundaries={{0,.30f,.67f,1},{0,.19f,.51f,.79f,1}};
            int courses=tall?26:2;float courseHeight=tall?127.5f/26f:4.5f;
            for(int course=0;course<courses;course++)for(int i=0;i<boundaries[course%2].length-1;i++) {
                float gap=.006f,u0=boundaries[course%2][i]+(i==0 && joins!=null && joins.linkA?0:gap),u1=boundaries[course%2][i+1]-(i==boundaries[course%2].length-2 && joins!=null && joins.linkB?0:gap),z0=course*courseHeight+.10f,z1=(course+1)*courseHeight-.10f-(course!=0 && i%2!=0?.12f:0);int k=(i+course)%3;
                boolean showEnd=by>=ay ? (i==boundaries[course%2].length-2 && (joins==null || !joins.linkB)) : (i==0 && (joins==null || !joins.linkA));
                m.box(u0,u1,-.5f,.5f,z0,z1,tops[k],fronts[k],sides[k],showEnd);
                float chip=u0+(u1-u0)*(.26f+(i%2)*.18f);m.mark(chip,chip+(u1-u0)*.13f,.505f,z0+1.1f,z0+1.45f,0xffaab3a0);
            }
            float wallHeight=tall?127.4f:8.9f;
            if(joins!=null && joins.joinA)m.box(-.09f,.09f,-.5f,.5f,0,wallHeight,0xffb8c1b1,0xff929c8c,0xff73806e);
            if(joins!=null && joins.joinB)m.box(.91f,1.09f,-.5f,.5f,0,wallHeight,0xffb8c1b1,0xff929c8c,0xff73806e);
        } else {
            m.box(joins!=null && joins.linkA?0:.04f,joins!=null && joins.linkB?1:.96f,-.30f,.30f,5.2f,7.3f,0xffc69c68,0xffa6784f,0xff80583b);
            m.box(joins!=null && joins.linkA?0:.04f,joins!=null && joins.linkB?1:.96f,-.30f,.30f,10.1f,12.2f,0xffc69c68,0xffa6784f,0xff80583b);
            if(joins==null || joins.postA)m.box(-.065f,.065f,-.65f,.65f,0,15+4*(joins==null?0:joins.postExtraA),0xffd0ab79,0xffa67c56,0xff79543d);
            if(joins==null || joins.postB)m.box(.935f,1.065f,-.65f,.65f,0,15+4*(joins==null?0:joins.postExtraB),0xffd0ab79,0xffa67c56,0xff79543d);
            m.mark(.18f,.43f,.305f,6.1f,6.3f,0xff8e694a);m.mark(.50f,.77f,.305f,11,11.2f,0xff8e694a);
        }
        return m.faces;
    }
    static boolean contains(List<Face> faces,float x,float y) {
        for(Face face:faces) {
            float[] p=face.points;boolean inside=false;int count=p.length/2;
            for(int i=0,j=count-1;i<count;j=i++) {
                float ax=p[2*i],ay=p[2*i+1],bx=p[2*j],by=p[2*j+1];
                if((ay>y)!=(by>y) && x<(bx-ax)*(y-ay)/(by-ay)+ax)inside=!inside;
            }
            if(inside)return true;
        }
        return false;
    }
    private FenceMesh(){throw new AssertionError();}
}
