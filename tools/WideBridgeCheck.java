package dev.retrobit.gtlauncher;
import org.json.*;
public class WideBridgeCheck {
 public static void main(String[] args) throws Exception {
  JSONArray tiles=new JSONArray();for(int i=0;i<400;i++)tiles.put(1);
  JSONArray bridges=new JSONArray().put(new JSONObject().put("column",5).put("row",5).put("axis","E").put("material","wood").put("width",2));
  JSONObject w=new JSONObject().put("id","wide").put("name","Wide").put("description","").put("color","#a8c795").put("gridSize",20).put("tiles",tiles).put("showCharacter",true).put("characterColumn",1).put("characterRow",1).put("characterScale",1).put("characterFacing","S").put("house",JSONObject.NULL).put("bridges",bridges);
  JSONObject root=new JSONObject().put("format","gtlauncher-preview-world").put("version",22).put("world",w);
  PreviewWorld20 p=PreviewWorld20.parse(root.toString());if(p.bridges.get(0).width!=2 || !p.onBridge(6,6) || !p.canStep(6,5,6,6))throw new AssertionError("wide traversal");
  root.put("version",21);boolean rejected=false;try{PreviewWorld20.parse(root.toString());}catch(JSONException expected){rejected=true;}if(!rejected)throw new AssertionError("old version accepted width");
  WorldViewRotation.Bridge b=WorldViewRotation.bridge(5,5,"E",3,2,20,1);if(b==null || b.column!=13 || b.row!=5 || !b.axis.equals("S"))throw new AssertionError("rotation");
  System.out.println("Android v22 wide bridge parse, traversal, old version guard and rotation: OK");
 }
}
