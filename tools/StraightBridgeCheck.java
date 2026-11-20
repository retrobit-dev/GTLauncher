package dev.retrobit.gtlauncher;
import org.json.*;
public class StraightBridgeCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray(),stacks=new JSONArray();
  for(int i=0;i<400;i++){JSONArray a=new JSONArray();if(i!=105)a.put(1);if(i==106){a.put(1);a.put(3);}if(i==107)a.put(1);stacks.put(a);tiles.put(a.length()==0?0:a.getInt(a.length()-1));}
  JSONObject b=new JSONObject().put("column",5).put("row",5).put("axis","E").put("material","wood").put("gap",true).put("free",true).put("deck",1).put("straight",true);
  JSONObject ramp=new JSONObject().put("column",6).put("row",5).put("direction","E").put("material",3);
  JSONObject w=new JSONObject().put("id","free").put("name","Free").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("blockStacks",stacks).put("showCharacter",true).put("characterColumn",5).put("characterRow",5).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL).put("bridges",new JSONArray().put(b)).put("ramps",new JSONArray().put(ramp));
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",28).put("world",w);
  PreviewWorld20 p=PreviewWorld20.parse(root.toString());if(!p.canStand(5,5) || !p.canStep(5,5,6,5) || !p.canStep(6,5,7,5) || p.footHeight(6,5)!=1)throw new AssertionError("bridge route");
  for(int q=0;q<4;q++){PreviewWorld20 r=WorldViewProjection.projectTerrain(p,q);BridgeGeometry.Bridge rb=r.bridges.get(0);if(!rb.free || !rb.straight || rb.deck!=1 || r.footHeight(rb.column,rb.row)!=1)throw new AssertionError("rotated deck");}
  root.put("version",27);boolean rejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException e){rejected=true;}if(!rejected)throw new AssertionError("older format accepted straight bridge");
  for(String axis:new String[]{"E","S"})for(int length:new int[]{3,4,5}) {
   BridgeGeometry.Bridge bridge=new BridgeGeometry.Bridge(5,5,axis,"wood",length,1,-1,true,false,true,1,false,false,true);
   for(float along:new float[]{0,.25f,.5f,1,length-1}) {
    float c=5+(axis.equals("E")?along:0),r=5+(axis.equals("S")?along:0);
    if(BridgeGeometry.riseAt(java.util.Collections.singletonList(bridge),c,r,4)!=0)throw new AssertionError("flat bridge rise");
   }
  }
  System.out.println("Android straight bridge parsing, ramp, routing, rotation and version guard: OK");
 }
}
