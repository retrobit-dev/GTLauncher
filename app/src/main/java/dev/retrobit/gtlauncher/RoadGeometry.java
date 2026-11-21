package dev.retrobit.gtlauncher;

import java.util.ArrayList;
import java.util.List;

/** Tile-local road geometry; rotation changes texture only, never topology. */
final class RoadGeometry {
    static final float INSET=.12f;
    interface Occupied { boolean has(int column,int row,boolean water); }
    static float depth(String material,float blockHeight) {
        return blockHeight*("water".equals(material)?.5f:.25f);
    }
    static float bedDepth(String material,float blockHeight) {return "water".equals(material)?blockHeight*.625f:depth(material,blockHeight);}
    static boolean sharedEdge(float[] a,float[] b,boolean n,boolean e,boolean s,boolean w) {
        return (a[0]==0 && b[0]==0 && w) || (a[0]==1 && b[0]==1 && e) || (a[1]==0 && b[1]==0 && n) || (a[1]==1 && b[1]==1 && s);
    }
    static float[] sharedSpan(float[][] a,float[][] b,boolean east) {
        int axis=east?0:1,other=east?1:0;float low=Float.POSITIVE_INFINITY,high=Float.NEGATIVE_INFINITY;
        for(int i=0;i<a.length;i++){float[] p=a[i],q=a[(i+1)%a.length];if(p[axis]!=1||q[axis]!=1)continue;
            for(int j=0;j<b.length;j++){float[] r=b[j],s=b[(j+1)%b.length];if(r[axis]!=0||s[axis]!=0)continue;
                float lo=Math.max(Math.min(p[other],q[other]),Math.min(r[other],s[other]));float hi=Math.min(Math.max(p[other],q[other]),Math.max(r[other],s[other]));if(hi>lo){low=Math.min(low,lo);high=Math.max(high,hi);}
            }
        }
        return high>low?new float[]{low,high}:null;
    }
    static float[] rotate(float u,float v,String facing) {
        switch(facing){case "W":return new float[]{1-v,u};case "N":return new float[]{1-u,1-v};case "E":return new float[]{v,1-u};default:return new float[]{u,v};}
    }
    static float[][] outline(int column,int row,boolean water,Occupied occupied) {
        return outline(column,row,water,occupied,9);
    }
    static float[][] outline(int column,int row,boolean water,Occupied occupied,int size) {
        boolean c0=occupied.has(column-1,row,water),c1=occupied.has(column+1,row,water),r0=occupied.has(column,row-1,water),r1=occupied.has(column,row+1,water);
        float u0=c0||column==0?0:INSET,u1=c1||column==size-1?1:1-INSET,v0=r0||row==0?0:INSET,v1=r1||row==size-1?1:1-INSET;
        boolean cut00=c0&&r0&&!occupied.has(column-1,row-1,water),cut10=c1&&r0&&!occupied.has(column+1,row-1,water),cut11=c1&&r1&&!occupied.has(column+1,row+1,water),cut01=c0&&r1&&!occupied.has(column-1,row+1,water);
        List<float[]> p=new ArrayList<>();
        if(cut00){p.add(new float[]{u0,v0+INSET});p.add(new float[]{u0+INSET,v0+INSET});p.add(new float[]{u0+INSET,v0});}else p.add(new float[]{u0,v0});
        if(cut10){p.add(new float[]{u1-INSET,v0});p.add(new float[]{u1-INSET,v0+INSET});p.add(new float[]{u1,v0+INSET});}else p.add(new float[]{u1,v0});
        if(cut11){p.add(new float[]{u1,v1-INSET});p.add(new float[]{u1-INSET,v1-INSET});p.add(new float[]{u1-INSET,v1});}else p.add(new float[]{u1,v1});
        if(cut01){p.add(new float[]{u0+INSET,v1});p.add(new float[]{u0+INSET,v1-INSET});p.add(new float[]{u0,v1-INSET});}else p.add(new float[]{u0,v1});
        return p.toArray(new float[0][]);
    }
    private RoadGeometry() {}
}
