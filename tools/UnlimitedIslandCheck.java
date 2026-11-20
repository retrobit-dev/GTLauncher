package dev.retrobit.gtlauncher;
import org.json.*;
public class UnlimitedIslandCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray(),stacks=new JSONArray();for(int i=0;i<400;i++){JSONArray a=new JSONArray();for(int j=0;j<(i==105?150:1);j++)a.put(1);stacks.put(a);tiles.put(1);}
  JSONObject w=new JSONObject().put("id","test").put("name","Test").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("blockStacks",stacks).put("blockOpacity",new JSONArray().put(new JSONObject().put("column",5).put("row",5).put("level",149).put("opacity",40))).put("showCharacter",true).put("characterColumn",5).put("characterRow",5).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL);
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",20).put("world",w);
  PreviewWorld20 parsed=PreviewWorld20.parse(root.toString());if(parsed.height(5,5)!=150 || parsed.blockOpacity(5,5,149)!=40)throw new AssertionError("height/opacity");WorldStore.parseExport(root.toString());
  System.out.println("Android 150-block world and high opacity parse/import: OK");
 }
}
