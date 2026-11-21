package dev.retrobit.gtlauncher;
import org.json.*;
public final class GateImportCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray(),fences=new JSONArray();for(int i=0;i<400;i++)tiles.put(1);
  for(int i=0;i<2;i++)fences.put(new JSONObject().put("column",5+i).put("row",5).put("edge","S").put("material",i==0?"gate-wood":"gate-stone"));
  JSONObject w=new JSONObject().put("id","gates").put("name","Gates").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("fences",fences).put("showCharacter",true).put("characterColumn",3).put("characterRow",3).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL);
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",32).put("world",w);
  WorldStore.World world=WorldStore.parseExport(root.toString());if(world.surfaces.stream().filter(o->o.material.startsWith("preview-gate-")).count()!=2)throw new AssertionError("gates lost");
  PreviewWorld20 rotated=WorldViewProjection.projectTerrain(world.previewData,1);WorldStore.World turned=WorldStore.parseExport(rotated.toJson());if(turned.surfaces.stream().filter(o->o.material.startsWith("preview-gate-")).count()!=2)throw new AssertionError("rotation");
  root.put("version",31);try{PreviewWorld20.parse(root.toString());throw new AssertionError("v31 accepted gate");}catch(JSONException expected){}
  for(String edge:new String[]{"N","E","S","W"})for(int side:new int[]{-1,1})if(GateMesh.faces(0,0,14,7,edge,false,1,side).size()<4)throw new AssertionError("mesh");
  System.out.println("Android gate import, rotation, mesh and version rejection: OK");
 }
}
