package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class WorldViewRotationTest {
 @Test public void inverseQuarterTurnsPreserveTilesDirectionsAndFootprints() {
  for(int size:new int[]{20,25,50,100})for(int q:new int[]{-5,-1,0,1,2,3,4,7}) {
   for(double[] v:new double[][]{{0,0},{size-1,size-1},{3,7},{3.25,7.5}}){WorldViewRotation.Point p=WorldViewRotation.point(v[0],v[1],size,q),a=WorldViewRotation.point(p.column,p.row,size,-q);assertEquals(v[0],a.column,1e-9);assertEquals(v[1],a.row,1e-9);}
   for(String d:new String[]{"N","E","S","W"})assertEquals(d,WorldViewRotation.direction(WorldViewRotation.direction(d,q),-q));
   for(int[] v:new int[][]{{1,3,3,4},{0,0,3,3}}){WorldViewRotation.Rect p=WorldViewRotation.rectangle(v[0],v[1],v[2],v[3],size,q),a=WorldViewRotation.rectangle(p.column,p.row,p.width,p.height,size,-q);assertEquals(v[0],a.column);assertEquals(v[1],a.row);assertEquals(v[2],a.width);assertEquals(v[3],a.height);}
   for(String d:new String[]{"N","E","S","W"}){WorldViewRotation.Edge p=WorldViewRotation.fence(2,4,d,size,q),a=WorldViewRotation.fence(p.column,p.row,p.edge,size,-q);assertEquals(2,a.column);assertEquals(4,a.row);assertEquals(d,a.edge);}
   for(String axis:new String[]{"E","S"})for(int n:new int[]{3,4,5}){WorldViewRotation.Bridge p=WorldViewRotation.bridge(3,4,axis,n,size,q),a=WorldViewRotation.bridge(p.column,p.row,p.axis,p.length,size,-q);assertEquals(3,a.column);assertEquals(4,a.row);assertEquals(axis,a.axis);assertEquals(n,a.length);}
  }
  WorldViewRotation.Point p=WorldViewRotation.point(3,7,20,1);assertEquals(12,p.column,1e-9);assertEquals(3,p.row,1e-9);assertEquals("E",WorldViewRotation.direction("N",1));
  assertNull(WorldViewRotation.point(0,0,0,1));assertNull(WorldViewRotation.direction("X",1));assertNull(WorldViewRotation.rectangle(19,19,3,3,20,1));assertNull(WorldViewRotation.bridge(19,0,"E",3,20,1));
 }
}
