package dev.retrobit.gtlauncher;
public final class GateMotionCheck {
 public static void main(String[] args) {
  for(String edge:new String[]{"N","E","S","W"}) {
   if(GateMotion.openness(0,0,edge,0,0)!=1 || GateMotion.openness(0,0,edge,10,10)!=0)throw new AssertionError(edge);
  }
  if(GateMotion.openness(0,0,"E",1.25,0)!=.5 || GateMotion.openness(0,0,"bad",0,0)!=0)throw new AssertionError("interpolation");
  System.out.println("Android automatic gate opening geometry: OK");
 }
}
