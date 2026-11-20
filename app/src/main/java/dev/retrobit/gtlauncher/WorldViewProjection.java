package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Camera-space copy of a preview world. Never use this copy for storage. */
final class WorldViewProjection {
    static JSONObject project(JSONObject world,int turns) throws JSONException {
        if(world==null)throw new JSONException("Missing world");
        JSONObject out=new JSONObject(world.toString());int size=integer(out,"gridSize");
        if(size<1 || size>100)throw new JSONException("Invalid grid size");
        rotateGrid(out,"tiles",size,turns,true);rotateGrid(out,"blockStacks",size,turns,false);
        if(out.has("characterColumn") || out.has("characterRow")) {
            int c=integer(out,"characterColumn"),r=integer(out,"characterRow");if(c<0 || r<0 || c>=size || r>=size)throw new JSONException("Invalid character");
            WorldViewRotation.Point p=WorldViewRotation.point(c,r,size,turns);
            out.put("characterColumn",(int)p.column);out.put("characterRow",(int)p.row);
        }
        if(out.has("characterFacing"))out.put("characterFacing",rotatedDirection(out.getString("characterFacing"),turns));
        if(out.has("house") && !out.isNull("house")) {
            JSONObject house=out.getJSONObject("house");rotateRectangle(house,3,size,turns);rotateFacing(house,turns);
        }
        JSONArray trees=optionalArray(out,"orangeTrees");if(trees!=null)for(int i=0;i<trees.length();i++){
            JSONObject tree=trees.getJSONObject(i);rotateRectangle(tree,tree.has("size")?integer(tree,"size"):2,size,turns);rotateFacing(tree,turns);
        }
        JSONArray fences=optionalArray(out,"fences");if(fences!=null)for(int i=0;i<fences.length();i++){
            JSONObject fence=fences.getJSONObject(i);WorldViewRotation.Edge edge=WorldViewRotation.fence(integer(fence,"column"),integer(fence,"row"),fence.getString("edge"),size,turns);
            if(edge==null)throw new JSONException("Invalid fence");fence.put("column",edge.column);fence.put("row",edge.row);fence.put("edge",edge.edge);
        }
        JSONArray bridges=optionalArray(out,"bridges");if(bridges!=null)for(int i=0;i<bridges.length();i++){
            JSONObject bridge=bridges.getJSONObject(i);WorldViewRotation.Bridge b=WorldViewRotation.bridge(integer(bridge,"column"),integer(bridge,"row"),bridge.getString("axis"),bridge.has("length")?integer(bridge,"length"):3,bridge.has("width")?integer(bridge,"width"):1,size,turns);
            if(b==null)throw new JSONException("Invalid bridge");bridge.put("column",b.column);bridge.put("row",b.row);bridge.put("axis",b.axis);
        }
        for(String name:new String[]{"blockOpacity","ramps"}) {
            JSONArray records=optionalArray(out,name);if(records==null)continue;
            for(int i=0;i<records.length();i++){
                JSONObject item=records.getJSONObject(i);int c=integer(item,"column"),r=integer(item,"row");if(c<0 || r<0 || c>=size || r>=size)throw new JSONException("Invalid tile record");
                WorldViewRotation.Point p=WorldViewRotation.point(c,r,size,turns);item.put("column",(int)p.column);item.put("row",(int)p.row);
                if(name.equals("ramps"))item.put("direction",rotatedDirection(item.getString("direction"),turns));
            }
        }
        return out;
    }
    /** Preview plants and fence geometry in camera space; raw identity stays world-space. */
    static WorldStore.SurfaceObject projectPreviewSurface(WorldStore.SurfaceObject object,int size,int turns) {
        if(object==null || !(object.type.equals("tree") || object.type.equals("bush") || object.type.equals("fence")))return null;
        if(object.type.equals("fence") && !object.material.startsWith("preview-"))return null;
        int footprint=WorldStore.surfaceFootprint(object);
        WorldViewRotation.Rect p=WorldViewRotation.rectangle(object.column,object.row,footprint,footprint,size,turns);
        String edge=object.edge==null?null:WorldViewRotation.direction(object.edge,turns);
        if(p==null || (object.edge!=null && edge==null))return null;
        if(Math.floorMod(turns,4)==0)return object;
        return new WorldStore.SurfaceObject(object.type,p.column,p.row,edge,object.material,object.raw,object.opacity);
    }
    /** Camera-space 3x3 house; interaction must still use the original house. */
    static WorldStore.House projectSquareHouse(WorldStore.House house,int size,int turns) {
        if(house==null)return null;
        HouseFootprint footprint=HouseFootprint.of(house.design);
        if(footprint.columns!=3 || footprint.rows!=3)return null;
        WorldViewRotation.Rect p=WorldViewRotation.rectangle(house.column,house.row,3,3,size,turns);
        String facing=WorldViewRotation.direction(house.facing,turns);
        if(p==null || facing==null)return null;
        if(Math.floorMod(turns,4)==0)return house;
        return new WorldStore.House(p.column,p.row,house.scale,facing,house.visible,house.id,house.design,house.opacity);
    }
    /** Parse an isolated camera-space copy without changing the persisted source. */
    static PreviewWorld20 projectTerrain(PreviewWorld20 source,int turns) throws JSONException {
        if(source==null)return null;
        if(Math.floorMod(turns,4)==0)return source;
        JSONObject root=new JSONObject(source.toJson());
        root.put("world",project(root.getJSONObject("world"),turns));
        return PreviewWorld20.parse(root.toString());
    }
    /** Pick using projected terrain depth while returning original world coordinates. */
    static PortalTravel.Tile pickWorld(PreviewWorld20 world,int turns,float x,float y,float originX,float originY,float tileWidth,float tileHeight,float blockHeight,float baseDepth) {
        if(world==null)return null;
        ElevatedTerrainGeometry.Heights heights=(c,r)->{WorldViewRotation.Point p=WorldViewRotation.point(c,r,world.size,-turns);return world.footHeight((int)p.column,(int)p.row);};
        ElevatedTerrainGeometry.Ramps ramps=(c,r)->{WorldViewRotation.Point p=WorldViewRotation.point(c,r,world.size,-turns);String d=world.onBridge((int)p.column,(int)p.row)?null:world.rampDirection((int)p.column,(int)p.row);return d==null?null:WorldViewRotation.direction(d,turns);};
        ElevatedTerrainGeometry.SurfaceOffset offsets=(c,r)->{WorldViewRotation.Point p=WorldViewRotation.point(c,r,world.size,-turns);int x0=(int)p.column,y0=(int)p.row;return 0;};
        PortalTravel.Tile hit=ElevatedTerrainGeometry.tileAt(x,y,world.size,heights,ramps,offsets,originX,originY,tileWidth,tileHeight,blockHeight,baseDepth);
        if(hit==null)return null;WorldViewRotation.Point p=WorldViewRotation.point(hit.column,hit.row,world.size,-turns);return new PortalTravel.Tile((int)p.column,(int)p.row);
    }
    private static JSONArray optionalArray(JSONObject obj,String key) throws JSONException {return obj.has(key)?obj.getJSONArray(key):null;}
    private static void rotateGrid(JSONObject out,String key,int size,int turns,boolean required) throws JSONException {
        if(!out.has(key) && !required)return;JSONArray input=out.getJSONArray(key);if(input.length()!=size*size)throw new JSONException("Invalid grid");
        JSONArray result=new JSONArray();for(int i=0;i<size*size;i++)result.put(JSONObject.NULL);
        for(int r=0;r<size;r++)for(int c=0;c<size;c++) {WorldViewRotation.Point p=WorldViewRotation.point(c,r,size,turns);result.put((int)p.row*size+(int)p.column,input.get(r*size+c));}
        out.put(key,result);
    }
    private static void rotateRectangle(JSONObject obj,int n,int size,int turns) throws JSONException {
        WorldViewRotation.Rect p=WorldViewRotation.rectangle(integer(obj,"column"),integer(obj,"row"),n,n,size,turns);if(p==null)throw new JSONException("Invalid footprint");
        obj.put("column",p.column);obj.put("row",p.row);
    }
    private static void rotateFacing(JSONObject obj,int turns) throws JSONException {if(obj.has("facing"))obj.put("facing",rotatedDirection(obj.getString("facing"),turns));}
    private static String rotatedDirection(String s,int turns) throws JSONException {String d=WorldViewRotation.direction(s,turns);if(d==null)throw new JSONException("Invalid facing");return d;}
    private static int integer(JSONObject obj,String key) throws JSONException {
        Object v=obj.get(key);if(!(v instanceof Number))throw new JSONException("Invalid coordinate");double n=((Number)v).doubleValue();if(!Double.isFinite(n) || n!=Math.rint(n) || n<Integer.MIN_VALUE || n>Integer.MAX_VALUE)throw new JSONException("Invalid coordinate");return (int)n;
    }
    private WorldViewProjection(){}
}
