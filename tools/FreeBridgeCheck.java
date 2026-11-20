package dev.retrobit.gtlauncher;
import org.json.*;
public class FreeBridgeCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray(),stacks=new JSONArray();
  for(int i=0;i<400;i++){JSONArray a=new JSONArray();if(i!=105)a.put(1);if(i==106){a.put(1);a.put(3);}if(i==107)a.put(1);stacks.put(a);tiles.put(a.length()==0?0:a.getInt(a.length()-1));}
  JSONObject b=new JSONObject().put("column",5).put("row",5).put("axis","E").put("material","wood").put("gap",true).put("free",true).put("deck",1);
  JSONObject ramp=new JSONObject().put("column",6).put("row",5).put("direction","E").put("material",3);
  JSONObject w=new JSONObject().put("id","free").put("name","Free").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("blockStacks",stacks).put("showCharacter",true).put("characterColumn",5).put("characterRow",5).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL).put("bridges",new JSONArray().put(b)).put("ramps",new JSONArray().put(ramp));
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",25).put("world",w);
  PreviewWorld20 p=PreviewWorld20.parse(root.toString());if(!p.canStand(5,5) || !p.canStep(5,5,6,5) || !p.canStep(6,5,7,5) || p.footHeight(6,5)!=1)throw new AssertionError("bridge route");
  for(int q=0;q<4;q++){PreviewWorld20 r=WorldViewProjection.projectTerrain(p,q);BridgeGeometry.Bridge rb=r.bridges.get(0);if(!rb.free || rb.deck!=1 || r.footHeight(rb.column,rb.row)!=1)throw new AssertionError("rotated deck");}
  root.put("version",24);boolean rejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException e){rejected=true;}if(!rejected)throw new AssertionError("older format accepted free bridge");
  System.out.println("Android free bridge parsing, ramp, routing, rotation and version guard: OK");
 }
}
