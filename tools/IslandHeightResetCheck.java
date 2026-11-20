package dev.retrobit.gtlauncher;
import org.json.*;
public class IslandHeightResetCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray(),stacks=new JSONArray();
  for(int i=0;i<400;i++){JSONArray stack=new JSONArray();int n=i==105?7:5;for(int j=0;j<n;j++)stack.put(j==n-1?1:2);stacks.put(stack);tiles.put(1);}
  JSONArray opacity=new JSONArray().put(new JSONObject().put("column",5).put("row",5).put("level",0).put("opacity",20)).put(new JSONObject().put("column",5).put("row",5).put("level",6).put("opacity",40));
  JSONObject w=new JSONObject().put("id","test").put("name","Test").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("blockStacks",stacks).put("blockOpacity",opacity).put("showCharacter",true).put("characterColumn",5).put("characterRow",5).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL);
  w.put("ramps",new JSONArray().put(new JSONObject().put("column",6).put("row",5).put("direction","E").put("material",1)));
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",21).put("world",w);
  String old=new JSONArray().put(new JSONObject().put("id","test").put("previewExport",root.toString())).toString();
  boolean rejected=false;try{WorldStore.parseExport(root.toString());}catch(JSONException e){rejected=true;}if(!rejected)throw new AssertionError("oversized import must be rejected");
  String result=IslandHeightReset.clean(old);JSONObject out=new JSONObject(new JSONArray(result).getJSONObject(0).getString("previewExport")).getJSONObject("world");
  if(out.getJSONArray("blockStacks").getJSONArray(105).length()!=5 || out.getJSONArray("tiles").getInt(105)!=1 || out.getJSONArray("blockStacks").getJSONArray(106).length()!=4 || out.getJSONArray("blockOpacity").length()!=1 || out.getJSONArray("blockOpacity").getJSONObject(0).getInt("level")!=4)throw new AssertionError("height or opacity");
  if(!IslandHeightReset.clean(result).equals(result))throw new AssertionError("reset must be idempotent");
  System.out.println("Android five-block terrain migration retains surfaces, opacity and validates resulting world: OK");
 }
}
