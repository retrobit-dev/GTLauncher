package dev.retrobit.gtlauncher;
import java.util.*;
public final class StoneRoadGeometryCheck {
 private static boolean side(int c){return c==0xff737e6d||c==0xff7b8675||c==0xff707d6d;}
 public static void main(String[] args) {
  for(float slope:new float[]{7,-7}) {
   List<FenceMesh.Face> stone=FenceMesh.faces(0,0,14,slope,true);
   if(stone.size()!=23 || stone.stream().filter(f->side(f.color)).count()!=2)throw new AssertionError("internal stone end faces");
   FenceJoins.Join j=new FenceJoins.Join();if(slope>0)j.linkB=true;else j.linkA=true;
   if(FenceMesh.faces(0,0,14,slope,true,j).stream().anyMatch(f->side(f.color)))throw new AssertionError("linked end face");
   if(FenceMesh.faces(0,0,14,slope,false).size()!=14)throw new AssertionError("wood geometry changed");
  }
  if(RoadGeometry.depth("dirt",4)!=2f || RoadGeometry.depth("water",4)!=3f || RoadGeometry.bedDepth("water",4)!=3.5f)throw new AssertionError("road depth");
  if(!RoadGeometry.sharedEdge(new float[]{0,.1f},new float[]{0,.9f},false,false,false,true))throw new AssertionError("connected seam");
  if(RoadGeometry.sharedEdge(new float[]{0,.1f},new float[]{0,.9f},false,false,false,false))throw new AssertionError("open bank");
  float[][] east=RoadGeometry.outline(10,10,false,(x,y,w)->x==11&&y==10,20),west=RoadGeometry.outline(11,10,false,(x,y,w)->x==10&&y==10,20);
  float[] span=RoadGeometry.sharedSpan(east,west,true);if(span==null || Math.abs(span[0]-.12f)>1e-5 || Math.abs(span[1]-.88f)>1e-5)throw new AssertionError("seam span");
  int[][] directions={{0,-1},{1,0},{0,1},{-1,0},{1,-1},{1,1},{-1,1},{-1,-1}};
  for(int mask=0;mask<256;mask++){
   final int flags=mask;float[][] polygon=RoadGeometry.outline(10,10,false,(x,y,w)->{for(int i=0;i<directions.length;i++)if(x==10+directions[i][0]&&y==10+directions[i][1])return (flags&(1<<i))!=0;return false;},20);
   for(int i=0;i<polygon.length;i++){float[] a=polygon[i],b=polygon[(i+1)%polygon.length];if(Math.abs(a[0]-b[0])>1e-6f && Math.abs(a[1]-b[1])>1e-6f)throw new AssertionError("non-orthogonal junction "+mask);}
  }
  System.out.println("Android stone end faces and orthogonal road junctions: OK");
 }
}
