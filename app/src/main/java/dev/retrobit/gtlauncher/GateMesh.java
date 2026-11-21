package dev.retrobit.gtlauncher;
import java.util.ArrayList;
import java.util.List;
/** Matching isometric gate posts and hinged wooden leaf. */
final class GateMesh {
 private static void polygon(List<FenceMesh.Face> f,int color,float... points){f.add(new FenceMesh.Face(color,points));}
 private static void post(List<FenceMesh.Face> f,float x,float y,boolean stone){float w=stone?2:1.6f,h=stone?13:15;polygon(f,stone?0xff909a88:0xffa67c56,x-w,y,x+w,y,x+w,y-h,x-w,y-h);polygon(f,stone?0xffc4c7b8:0xffd0ab79,x-w,y-h,x,y-h-1.4f,x+w,y-h,x,y-h+1.4f);}
 static List<FenceMesh.Face> faces(float ax,float ay,float bx,float by,String edge,boolean stone,double amount,int side) {
  List<FenceMesh.Face> out=new ArrayList<>();float dx=bx-ax,dy=by-ay,sign=side<0?-1:1;
  boolean vertical="E".equals(edge)||"W".equals(edge);float nx=(vertical?-sign:sign)*Math.abs(dx),ny=-sign*Math.abs(dy);
  double angle=Math.max(0,Math.min(1,amount))*Math.PI/2;float ex=(float)(dx*Math.cos(angle)+nx*Math.sin(angle)),ey=(float)(dy*Math.cos(angle)+ny*Math.sin(angle));
  post(out,ax,ay,stone);post(out,bx,by,stone);
  for(int i=0;i<5;i++){float u=.08f+i*.17f,v=u+.13f;polygon(out,i%2==1?0xffa6784f:0xffb78b60,ax+ex*u,ay+ey*u-1,ax+ex*v,ay+ey*v-1,ax+ex*v,ay+ey*v-10,ax+ex*u,ay+ey*u-10);}
  for(float z:new float[]{3,8})polygon(out,0xff80583b,ax+ex*.06f,ay+ey*.06f-z,ax+ex*.94f,ay+ey*.94f-z,ax+ex*.94f,ay+ey*.94f-z-1.5f,ax+ex*.06f,ay+ey*.06f-z-1.5f);
  return out;
 }
 private GateMesh(){}
}
