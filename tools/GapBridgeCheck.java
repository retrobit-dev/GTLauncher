package dev.retrobit.gtlauncher;
import org.json.*;
public class GapBridgeCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray();for(int i=0;i<400;i++)tiles.put(1);tiles.put(5*20+6,0);
  JSONArray bridges=new JSONArray().put(new JSONObject().put("column",5).put("row",5).put("axis","E").put("material","wood").put("gap",true));
  JSONObject w=new JSONObject().put("id","gap").put("name","Gap").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("showCharacter",true).put("characterColumn",6).put("characterRow",5).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL).put("bridges",bridges);
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",23).put("world",w);
  PreviewWorld20 p=PreviewWorld20.parse(root.toString());if(!p.canStand(6,5) || !p.canStep(5,5,6,5) || !p.canStep(6,5,7,5) || p.canStep(6,4,6,5) || p.footHeight(6,5)!=1)throw new AssertionError("gap bridge walkability");
  PreviewWorld20 rotated=WorldViewProjection.projectTerrain(p,1);if(!rotated.canStand(14,6))throw new AssertionError("rotated gap bridge");
  root.put("version",22);boolean rejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException expected){rejected=true;}if(!rejected)throw new AssertionError("old version accepted gap");
  root.put("version",23);tiles.put(5*20+6,1);rejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException expected){rejected=true;}if(!rejected)throw new AssertionError("flat ground accepted");
  System.out.println("Android v23 gap bridge validation, traversal, rotation and old-version guard: OK");
 }
}
