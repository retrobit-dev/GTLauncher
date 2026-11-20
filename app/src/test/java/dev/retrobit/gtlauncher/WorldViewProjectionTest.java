package dev.retrobit.gtlauncher;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorldViewProjectionTest {
 @Test public void cameraWorldRoundTripsWithoutChangingSavedData() throws Exception {
  JSONArray tiles=new JSONArray(),stacks=new JSONArray();for(int i=0;i<400;i++){tiles.put(i==105?4:1);stacks.put(new JSONArray().put(i==105?4:1));}
  JSONObject w=new JSONObject().put("id","asymmetric").put("gridSize",20).put("tiles",tiles).put("blockStacks",stacks).put("characterColumn",4).put("characterRow",5).put("characterFacing","E")
   .put("house",new JSONObject().put("column",2).put("row",9).put("facing","W").put("opacity",35))
   .put("orangeTrees",new JSONArray().put(new JSONObject().put("column",8).put("row",11).put("kind","conifer").put("size",1).put("facing","E")).put(new JSONObject().put("column",10).put("row",3).put("facing","N")))
   .put("fences",new JSONArray().put(new JSONObject().put("column",1).put("row",15).put("edge","S").put("material","wood")))
   .put("bridges",new JSONArray().put(new JSONObject().put("column",3).put("row",16).put("axis","E").put("length",4).put("material","stone")).put(new JSONObject().put("column",9).put("row",11).put("axis","S").put("material","wood")))
   .put("ramps",new JSONArray().put(new JSONObject().put("column",5).put("row",5).put("direction","E").put("material",4)))
   .put("blockOpacity",new JSONArray().put(new JSONObject().put("column",6).put("row",5).put("level",2).put("opacity",45)));
  String original=w.toString();
  for(int q:new int[]{0,1,2,3,-1,5}){
   JSONObject projected=WorldViewProjection.project(w,q),restored=WorldViewProjection.project(projected,-q);
   assertEquals(original,w.toString());assertEquals(original,restored.toString());
   projected.getJSONArray("ramps").getJSONObject(0).put("material",1);projected.getJSONArray("tiles").put(0,0);assertEquals(original,w.toString());
  }
  JSONObject bad=new JSONObject(original);bad.getJSONArray("ramps").getJSONObject(0).put("direction","X");
  try{WorldViewProjection.project(bad,1);fail("bad direction must be rejected");}catch(JSONException expected){}
 }
 @Test public void rotatedPickingReturnsOriginalRampTile() throws Exception {
  int size=20;JSONArray tiles=new JSONArray(),stacks=new JSONArray();for(int i=0;i<size*size;i++){boolean ramp=i==8*size+6;tiles.put(ramp?4:0);JSONArray stack=new JSONArray();if(ramp)stack.put(1).put(2).put(4);stacks.put(stack);}
  JSONObject world=new JSONObject().put("id","asymmetric").put("name","Rotated").put("description","").put("color","#a8c795").put("gridSize",size).put("tiles",tiles).put("blockStacks",stacks).put("showCharacter",true).put("characterColumn",6).put("characterRow",8).put("characterFacing","E").put("characterScale",1).put("house",JSONObject.NULL).put("ramps",new JSONArray().put(new JSONObject().put("column",6).put("row",8).put("direction","E").put("material",5)));
  PreviewWorld20 terrain=PreviewWorld20.parse(new JSONObject().put("format","gtlauncher-preview-world").put("version",21).put("world",world).toString());String before=terrain.toJson();
  for(int q:new int[]{0,1,2,3,-1}){
   WorldViewRotation.Point p=WorldViewRotation.point(6,8,size,q);float cx=(float)(200+(p.column-p.row)*14),cy=(float)(100+(p.column+p.row)*7-8);String d=WorldViewRotation.direction("E",q);RampGeometry.Shape shape=RampGeometry.create(d,28,14,4);
   java.util.List<RampGeometry.Vertex[]> faces=new java.util.ArrayList<>();faces.add(shape.top);for(RampGeometry.Vertex[] face:shape.sides){boolean u=true,v=true;for(RampGeometry.Vertex point:face){u&=point.u==1;v&=point.v==1;}if(u||v)faces.add(face);}if(d.equals("E")||d.equals("S"))faces.add(shape.highFace);
   for(RampGeometry.Vertex[] face:faces){float x=cx,y=cy;for(RampGeometry.Vertex vertex:face){x+=(float)vertex.x/face.length;y+=(float)vertex.y/face.length;}assertEquals(new PortalTravel.Tile(6,8),WorldViewProjection.pickWorld(terrain,q,x,y,200,100,28,14,4,20));}
   assertNull(WorldViewProjection.pickWorld(terrain,q,-999,-999,200,100,28,14,4,20));
  }
  assertEquals(before,terrain.toJson());
 }

}
