package dev.retrobit.gtlauncher;
import org.json.*;
public class BridgeResetCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray(),stacks=new JSONArray();
  for(int i=0;i<400;i++){JSONArray a=new JSONArray();if(i!=105)a.put(1);if(i==106){a.put(1);a.put(3);}if(i==107)a.put(1);stacks.put(a);tiles.put(a.length()==0?0:a.getInt(a.length()-1));}
  JSONObject b=new JSONObject().put("column",5).put("row",5).put("axis","E").put("material","wood").put("gap",true).put("free",true).put("deck",1).put("straight",true);
  JSONObject ramp=new JSONObject().put("column",6).put("row",5).put("direction","E").put("material",3);
  JSONObject w=new JSONObject().put("id","free").put("name","Free").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("blockStacks",stacks).put("showCharacter",true).put("characterColumn",5).put("characterRow",5).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL).put("bridges",new JSONArray().put(b)).put("ramps",new JSONArray().put(ramp));
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",28).put("world",w);
  JSONArray entries=new JSONArray().put(new JSONObject().put("id","free").put("previewExport",root.toString()));
  String original=entries.toString(),clean=BridgeReset.clean(original);
  JSONObject out=new JSONObject(new JSONArray(clean).getJSONObject(0).getString("previewExport")).getJSONObject("world");
  if(out.has("bridges") || out.getInt("characterColumn")==5 && out.getInt("characterRow")==5)throw new AssertionError("reset or safe relocation failed");
  if(out.getJSONArray("tiles").getInt(105)!=0 || out.getJSONArray("ramps").length()!=1)throw new AssertionError("other data changed");
  if(!BridgeReset.clean(clean).equals(clean))throw new AssertionError("reset not idempotent");
  boolean failed=false;try{BridgeReset.clean("[{\"id\":\"bad\",\"previewExport\":\"{}\"}]");}catch(JSONException e){failed=true;}if(!failed)throw new AssertionError("unsupported data rewritten");
  System.out.println("Android bridge reset preserves terrain and relocates unsupported player: OK");
 }
}
