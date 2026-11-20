package dev.retrobit.gtlauncher;
import org.json.*;
public class WaterBridgeCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray();for(int i=0;i<400;i++)tiles.put(1);tiles.put(106,3);
  JSONObject b=new JSONObject().put("column",5).put("row",5).put("axis","E").put("material","wood").put("gap",true).put("overWater",true);
  JSONObject w=new JSONObject().put("id","water").put("name","Water").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("showCharacter",true).put("characterColumn",6).put("characterRow",5).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL).put("bridges",new JSONArray().put(b));
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",24).put("world",w);
  PreviewWorld20 p=PreviewWorld20.parse(root.toString());if(!p.canStand(6,5) || !p.onBridge(6,5) || !p.canStep(5,5,6,5) || !p.canStep(6,5,7,5))throw new AssertionError("water bridge walkability");
  if(!WorldViewProjection.projectTerrain(p,1).onBridge(14,6))throw new AssertionError("rotated water bridge");
  root.put("version",23);boolean rejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException expected){rejected=true;}if(!rejected)throw new AssertionError("older format accepted water bridge");
  root.put("version",24);tiles.put(106,1);rejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException expected){rejected=true;}if(!rejected)throw new AssertionError("dry span accepted");
  System.out.println("Android v24 water bridge, rotation, downgrade guard: OK");
 }
}
