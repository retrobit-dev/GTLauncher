package dev.retrobit.gtlauncher;
import java.util.*;
public final class RoadPriorityCheck {
 public static void main(String[] args) {
  Set<String> roads=new HashSet<>();for(int c=2;c<=10;c++)roads.add(c+",3");
  PortalTravel.Terrain terrain=new PortalTravel.Terrain(){public boolean canStand(int c,int r){return true;}public boolean canStep(int a,int b,int c,int d){return true;}public int stepCost(int c,int r){return roads.contains(c+","+r)?3:4;}};
  PortalTravel.Tile start=new PortalTravel.Tile(2,2),end=new PortalTravel.Tile(10,2);List<PortalTravel.Tile> route=PortalTravel.path(start,end,terrain,20);
  if(route==null || route.size()!=11 || PortalTravel.routeCost(route,terrain)!=31 || route.stream().noneMatch(t->t.row==3))throw new AssertionError("road detour");
  route=PortalTravel.path(start,new PortalTravel.Tile(6,2),terrain,20);if(route.size()!=5 || route.stream().anyMatch(t->t.row!=2))throw new AssertionError("excessive detour");
  PortalTravel.Terrain fence=new PortalTravel.Terrain(){public boolean canStand(int c,int r){return true;}public int stepCost(int c,int r){return terrain.stepCost(c,r);}public boolean canStep(int a,int b,int c,int d){return !(b==3 && d==3 && Math.min(a,c)==5 && Math.max(a,c)==6);}};
  route=PortalTravel.path(start,end,fence,20);for(int i=1;i<route.size();i++){PortalTravel.Tile a=route.get(i-1),b=route.get(i);if(!fence.canStep(a.column,a.row,b.column,b.row))throw new AssertionError("fence");}
  PortalTravel.DryTerrain dry=new PortalTravel.DryTerrain(){public boolean canStand(int c,int r){return c==5&&r==5 || c==5&&r==6 || c==6&&r==5 || c==7&&r==5;}public boolean canStep(int a,int b,int c,int d){return true;}public int stepCost(int c,int r){return c==7?1:100;}public boolean isDry(int c,int r){return c==5&&r==6 || c==7&&r==5;}};
  route=PortalTravel.pathToNearestDry(new PortalTravel.Tile(5,5),dry,20);if(route.size()!=2 || route.get(1).row!=6)throw new AssertionError("dry recovery must be shortest");
  PortalTravel.Terrain open=new PortalTravel.Terrain(){public boolean canStand(int c,int r){return true;}public boolean canStep(int a,int b,int c,int d){return true;}};
  GroundRedirect.Result redirect=GroundRedirect.plan(new PortalTravel.Tile(4,1),new PortalTravel.Tile(5,1),.2f,new PortalTravel.Tile(2,1),open,50);
  if(redirect==null || !redirect.reversed || redirect.route.size()!=4)throw new AssertionError("redirect backwards");
  redirect=GroundRedirect.plan(new PortalTravel.Tile(4,1),new PortalTravel.Tile(5,1),.3f,new PortalTravel.Tile(7,1),open,50);
  if(redirect==null || redirect.reversed)throw new AssertionError("redirect forwards");
  System.out.println("Android road preference, bounded detours, obstacles and water recovery: OK");
 }
}
