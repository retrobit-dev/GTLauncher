package dev.retrobit.gtlauncher;
public final class GateMotionCheck {
 public static void main(String[] args) {
  for(String edge:new String[]{"N","E","S","W"}) {
   if(GateMotion.openness(0,0,edge,0,0)!=1 || GateMotion.openness(0,0,edge,10,10)!=0)throw new AssertionError(edge);
  }
  if(GateMotion.openness(0,0,"E",1.25,0)!=.5 || GateMotion.openness(0,0,"bad",0,0)!=0)throw new AssertionError("interpolation");
  GateMotion.State a=GateMotion.update(0,0,"E",0,0,0),b=GateMotion.update(0,0,"E",1,0,a.side);
  if(a.side!=-1 || b.side!=-1 || GateMotion.update(0,0,"E",4,0,b.side).side!=0 || GateMotion.update(0,0,"E",1,0,0).side!=1)throw new AssertionError("approach side");
  System.out.println("Android automatic gate opening geometry: OK");
 }
}
