package dev.retrobit.gtlauncher;
import org.json.*;
public final class FloatingBlocksCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray(),stacks=new JSONArray();for(int i=0;i<400;i++){JSONArray col=new JSONArray().put(1);if(i==105)col.put(0).put(0).put(2);stacks.put(col);tiles.put(i==105?2:1);}
  JSONObject w=new JSONObject().put("id","test").put("name","Test").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("blockStacks",stacks).put("showCharacter",true).put("characterColumn",5).put("characterRow",5).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL);
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",29).put("world",w);
  PreviewWorld20 p=PreviewWorld20.parse(root.toString());if(p.height(5,5)!=4 || p.block(5,5,1)!=0 || p.block(5,5,3)!=2 || !p.hasFloatingBlocks())throw new AssertionError();WorldStore.parseExport(root.toString());
  root.put("version",28);boolean rejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException ex){rejected=true;}if(!rejected)throw new AssertionError("old formats must reject holes");
  System.out.println("Floating blocks roundtrip and old-format rejection: OK");
 }
}
