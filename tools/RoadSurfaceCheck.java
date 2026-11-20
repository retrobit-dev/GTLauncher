package dev.retrobit.gtlauncher;
import org.json.*;
public final class RoadSurfaceCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray(),stacks=new JSONArray(),roads=new JSONArray();String[] names={"dirt","stone","sand","paving","asphalt","water"};
  for(int i=0;i<400;i++){tiles.put(1);stacks.put(new JSONArray().put(2).put(2).put(2).put(2).put(1));}
  for(int i=0;i<names.length;i++)roads.put(new JSONObject().put("column",2+i).put("row",2).put("material",names[i]));
  JSONObject w=new JSONObject().put("id","roads").put("name","Roads").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("blockStacks",stacks).put("roads",roads).put("showCharacter",true).put("characterColumn",5).put("characterRow",5).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL);
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",30).put("world",w);PreviewWorld20 data=PreviewWorld20.parse(root.toString());
  if(data.surfaceMaterial(7,2)!=3 || data.surfaceMaterial(6,2)!=1 || !data.hasRoads())throw new AssertionError("river classification");
  WorldStore.World world=WorldStore.parseExport(root.toString());long count=world.surfaces.stream().filter(o->o.type.equals("road")).count();if(count!=6)throw new AssertionError("roads lost");if(!"S".equals(world.surfaces.stream().filter(o->o.type.equals("road") && o.material.equals("water")).findFirst().get().edge))throw new AssertionError("old river must default south");
  PreviewWorld20 rotated=WorldViewProjection.projectTerrain(data,1);WorldViewRotation.Point p=WorldViewRotation.point(7,2,20,1);if(rotated.surfaceMaterial((int)p.column,(int)p.row)!=3)throw new AssertionError("rotated river");
  roads.getJSONObject(5).put("direction","E");root.put("version",31);PreviewWorld20 directed=PreviewWorld20.parse(root.toString());if(!directed.hasRiverDirection())throw new AssertionError("direction not retained");
  JSONObject rotatedWorld=WorldViewProjection.project(w,1);if(!"S".equals(rotatedWorld.getJSONArray("roads").getJSONObject(5).getString("direction")))throw new AssertionError("direction rotation");
  WorldStore.World withDirection=WorldStore.parseExport(root.toString());WorldStore.SurfaceObject river=withDirection.surfaces.stream().filter(o->o.type.equals("road") && o.material.equals("water")).findFirst().get();if(!"E".equals(river.edge))throw new AssertionError("renderer direction");
  root.put("version",30);boolean oldRejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException e){oldRejected=true;}if(!oldRejected)throw new AssertionError("old version cannot accept direction");roads.getJSONObject(5).remove("direction");
  root.put("version",29);boolean rejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException e){rejected=true;}if(!rejected)throw new AssertionError("version gate");
  System.out.println("Android road import, six materials, rotated river and version gate: OK");
 }
}
