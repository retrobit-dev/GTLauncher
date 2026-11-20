package dev.retrobit.gtlauncher;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.UUID;

/** Browser-exported world data stored locally on this Android device. */
final class WorldStore {
    private static final String WORLDS_KEY = "worlds_v1";
    private static final String ACTIVE_KEY = "active_world_v1";
    private static final String OBJECT_BAG_KEY = "object_bag_v1";
    static final String BASE_ID = "base";
    private final SharedPreferences prefs;
    private final InventoryStore inventory;

    static final class SurfaceObject {
        final String type, edge, material, raw;
        final int column, row, opacity;
        SurfaceObject(String type,int column,int row,String edge,String material,String raw) {
            this(type,column,row,edge,material,raw,-1);
        }
        SurfaceObject(String type,int column,int row,String edge,String material,String raw,int opacity) {
            this.type=type;this.column=column;this.row=row;this.edge=edge;this.material=material;this.raw=raw;this.opacity=opacity;
        }
    }
    static final class House {
        final int column,row,opacity;final double scale;final String facing,id,design;final boolean visible;
        House(int column,int row,double scale,String facing,boolean visible,String id,String design,int opacity) {
            this.column=column;this.row=row;this.scale=scale;this.facing=facing;this.visible=visible;this.id=id;this.design=design;this.opacity=opacity;
        }
        boolean covers(int c,int r){return visible && HouseFootprint.of(design).contains(column,row,c,r);}
    }
    private static boolean houseCovers(List<House> houses,int column,int row) {
        for(House house:houses)if(house.covers(column,row))return true;return false;
    }
    static final class World {
        final String id, name, description, color;
        final boolean showCharacter, showHouse, hasHouse;
        final int houseColumn, houseRow, houseOpacity;
        final double houseScale;
        final String houseFacing, houseId, houseDesign;
        final int formatVersion;
        PreviewWorld20 previewData;
        int gridSize() { return previewData == null ? 9 : previewData.size; }
        boolean tileExists(int c,int r) { return previewData == null ? c>=0 && r>=0 && c<9 && r<9 : previewData.canStand(c,r); }
        final int characterColumn, characterRow;
        final double characterScale;
        final String characterFacing;
        final List<PortalTravel.Portal> portals;
        final List<SurfaceObject> surfaces;
        final List<House> houses;
        World(String id, String name, String description, String color,
                boolean showCharacter, int column, int row, double scale, String facing,
                boolean hasHouse, boolean showHouse, int houseColumn, int houseRow, double houseScale, String houseFacing, String houseId, String houseDesign, int houseOpacity, int formatVersion, List<PortalTravel.Portal> portals, List<SurfaceObject> surfaces, List<House> houses) {
            this.id = id; this.name = name; this.description = description; this.color = color;
            this.showCharacter = showCharacter; this.characterColumn = column;
            this.characterRow = row; this.characterScale = scale; this.characterFacing = facing;
            this.hasHouse = hasHouse; this.showHouse = showHouse; this.houseColumn = houseColumn;
            this.houseRow = houseRow; this.houseScale = houseScale; this.houseFacing = houseFacing;
            this.houses=java.util.Collections.unmodifiableList(new ArrayList<>(houses));
            this.houseId = houseId; this.houseDesign=houseDesign; this.houseOpacity=houseOpacity; this.formatVersion = formatVersion; this.portals = java.util.Collections.unmodifiableList(new ArrayList<>(portals)); this.surfaces = java.util.Collections.unmodifiableList(new ArrayList<>(surfaces));
        }
    }

    WorldStore(SharedPreferences prefs) { this(prefs,null); }
    WorldStore(SharedPreferences prefs,InventoryStore inventory) {this.prefs=prefs;this.inventory=inventory;}

    private static String requiredString(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof String)) throw new JSONException("Invalid " + key);
        return (String) value;
    }

    private static World parseWorld(JSONObject object, String id, int formatVersion) throws JSONException {
        String name = requiredString(object, "name").trim();
        String description = requiredString(object, "description").trim();
        String color = requiredString(object, "color");
        if (name.isEmpty() || name.length() > 48 || description.length() > 160
                || !color.matches("#[0-9a-fA-F]{6}")) throw new JSONException("Invalid world fields");
        boolean visible = false, house = false, houseVisible = false;
        int houseColumn = 0, houseRow = 0, houseOpacity = -1;
        double houseScale = 1.0;
        String houseFacing = "S", houseId=null, houseDesign=null;
        int column = 4, row = 4;
        double scale = 1.0;
        String facing = "S";
        List<PortalTravel.Portal> portals = new ArrayList<>();
        List<SurfaceObject> surfaces = new ArrayList<>();
        List<House> houses = new ArrayList<>();
        java.util.Set<String> fenceIds=new java.util.HashSet<>(), fenceEdges=new java.util.HashSet<>(), roadTiles=new java.util.HashSet<>();
        if (object.has("objects")) {
            Object value = object.get("objects");
            if (!(value instanceof JSONArray)) throw new JSONException("Invalid objects");
            JSONArray objects = (JSONArray) value;
            if (objects.length() > (formatVersion >= 21 ? 687 : formatVersion >= 17 ? 606 : formatVersion >= 16 ? 525 : formatVersion >= 12 ? 445 : formatVersion >= 9 ? 364 : formatVersion >= 8 ? 283 : formatVersion >= 7 ? 202 : formatVersion >= 5 ? 2 : 1)) throw new JSONException("Unsupported objects");
            for (int i = 0; i < objects.length(); i++) {
                JSONObject character = objects.getJSONObject(i);
                String type = requiredString(character, "type");
                if (formatVersion >= 8 && "road".equals(type)) {
                    requireOnlyKeys(character,"type","column","row","material","facing");
                    if(formatVersion<19 && character.has("facing"))throw new JSONException("Unsupported road direction");
                    Object col=character.get("column"),rowObject=character.get("row");
                    if (!(col instanceof Number) || !(rowObject instanceof Number) || ((Number)col).doubleValue()!=Math.rint(((Number)col).doubleValue()) || ((Number)rowObject).doubleValue()!=Math.rint(((Number)rowObject).doubleValue())) throw new JSONException("Invalid road");
                    int c=((Number)col).intValue(),r=((Number)rowObject).intValue();String material=requiredString(character,"material");
                    if(c<0||c>8||r<0||r>8||!roadTiles.add(c+","+r)) throw new JSONException("Invalid road");
                    if(!java.util.Arrays.asList("dirt","stone","gravel").contains(material) && !(formatVersion>=11 && material.equals("water")) && !(formatVersion>=12 && java.util.Arrays.asList("paving","asphalt").contains(material))) throw new JSONException("Unsupported object");
                    String roadFacing=character.has("facing")?requiredString(character,"facing"):null;
                    if(roadFacing!=null && !java.util.Arrays.asList("N","E","S","W").contains(roadFacing))throw new JSONException("Invalid road direction");
                    surfaces.add(new SurfaceObject(type,c,r,roadFacing,material,character.toString()));continue;
                }
                if (formatVersion >= 7 && "fence".equals(type)) {
                    requireOpacityKeys(character,formatVersion,"type","id","column","row","edge","variant","destinationWorldId","destinationPortalId");
                    int opacity=readOpacity(character,formatVersion);
                    String variant=character.has("variant") ? requiredString(character,"variant") : "";
                    if (!variant.isEmpty() && !java.util.Arrays.asList("portal","slat","stone","iron").contains(variant)) throw new JSONException("Unsupported object");
                    if (character.has("variant") && (variant.isEmpty() || formatVersion<10)) throw new JSONException("Unsupported fence variant");
                    if (variant.equals("portal") && formatVersion<22) throw new JSONException("Unsupported object");
                    String portalId=requiredString(character,"id"), edge=requiredString(character,"edge");
                    Object c=character.get("column"), r=character.get("row");
                    if (!portalId.matches("[A-Za-z0-9_-]{1,128}") || !(c instanceof Number) || !(r instanceof Number)
                            || ((Number)c).doubleValue()!=Math.rint(((Number)c).doubleValue()) || ((Number)r).doubleValue()!=Math.rint(((Number)r).doubleValue())) throw new JSONException("Invalid portal");
                    int col=((Number)c).intValue(), portalRow=((Number)r).intValue();
                    if (col<0 || col>8 || portalRow<0 || portalRow>8 || !(edge.equals("N")||edge.equals("E")||edge.equals("S")||edge.equals("W"))) throw new JSONException("Invalid fence edge");
                    int nc=col+(edge.equals("E")?1:edge.equals("W")?-1:0), nr=portalRow+(edge.equals("S")?1:edge.equals("N")?-1:0);
                    int start=portalRow*9+col, end=nr*9+nc;
                    String edgeKey=(nc>=0 && nc<9 && nr>=0 && nr<9) ? Math.min(start,end)+":"+Math.max(start,end) : col+","+portalRow+","+edge;
                    if(!fenceIds.add(portalId) || !fenceEdges.add(edgeKey)) throw new JSONException("Duplicate fence");
                    if (!variant.equals("portal")) {
                        if(character.has("destinationWorldId") || character.has("destinationPortalId")) throw new JSONException("Invalid fence pair");
                        surfaces.add(new SurfaceObject(type,col,portalRow,edge,variant,character.toString(),opacity));continue;
                    }
                    if (!((edge.equals("N") && portalRow==0) || (edge.equals("S") && portalRow==8) || (edge.equals("W") && col==0) || (edge.equals("E") && col==8))) throw new JSONException("Invalid portal edge");
                    if (character.has("destinationWorldId") != character.has("destinationPortalId")) throw new JSONException("Incomplete portal pair");
                    String destWorld=null,destPortal=null;
                    if (character.has("destinationWorldId")) {
                        if(formatVersion<28) throw new JSONException("Unsupported object");
                        destWorld=requiredString(character,"destinationWorldId");destPortal=requiredString(character,"destinationPortalId");
                        if (!destWorld.matches("[A-Za-z0-9_-]{1,128}") || !destPortal.matches("[A-Za-z0-9_-]{1,128}")) throw new JSONException("Invalid portal pair");
                    }
                    for (PortalTravel.Portal previous : portals) if (previous.id.equals(portalId) || (previous.tile.column==col && previous.tile.row==portalRow && previous.edge.equals(edge))) throw new JSONException("Duplicate portal");
                    portals.add(new PortalTravel.Portal(id,portalId,edge,new PortalTravel.Tile(col,portalRow),destWorld,destPortal,opacity)); continue;
                }
                if(formatVersion>=20 && "flower".equals(type)) {
                    requireOnlyKeys(character,"type","column","row","species");
                    Object col=character.get("column"),rowObject=character.get("row");
                    if(!(col instanceof Number)||!(rowObject instanceof Number)||((Number)col).doubleValue()!=Math.rint(((Number)col).doubleValue())||((Number)rowObject).doubleValue()!=Math.rint(((Number)rowObject).doubleValue()))throw new JSONException("Invalid flower tile");
                    int c=((Number)col).intValue(),r=((Number)rowObject).intValue();String species=requiredString(character,"species");
                    if(c<0||c>8||r<0||r>8||!java.util.Arrays.asList("rose","tulip","lavender","daisy","marigold","bluebell","jasmine","ylang","frangipani","hibiscus","periwinkle").contains(species))throw new JSONException("Invalid flower");
                    if(formatVersion<21 && !java.util.Arrays.asList("rose","tulip","lavender","daisy","marigold","bluebell").contains(species))throw new JSONException("Unsupported flower");
                    for(SurfaceObject prior:surfaces)if(prior.type.equals("flower")&&prior.column==c&&prior.row==r)throw new JSONException("Duplicate flower");
                    surfaces.add(new SurfaceObject(type,c,r,null,species,character.toString()));continue;
                }
                if ((formatVersion>=9 && "bush".equals(type)) || (formatVersion>=14 && "tree".equals(type)) || (formatVersion>=18 && "sunflower".equals(type)) || (formatVersion>=17 && "mailbox".equals(type)) || (formatVersion>=26 && "storage-box".equals(type)) || (formatVersion>=30 && "selling-box".equals(type))) {
                    if(type.equals("bush")) requireOpacityKeys(character,formatVersion,"type","column","row","variant");
                    else if(type.equals("tree")) requireOpacityKeys(character,formatVersion,"type","column","row","species");
                    else if(type.equals("sunflower") && formatVersion>=19) requireOpacityKeys(character,formatVersion,"type","column","row","facing");
                    else if((type.equals("storage-box") || type.equals("selling-box")) && formatVersion>=27) requireOpacityKeys(character,formatVersion,"type","column","row","facing");
                    else requireOpacityKeys(character,formatVersion,"type","column","row");
                    int opacity=readOpacity(character,formatVersion);
                    Object co=character.get("column"),ro=character.get("row");
                    if(!(co instanceof Number) || !(ro instanceof Number) || ((Number)co).doubleValue()!=Math.rint(((Number)co).doubleValue()) || ((Number)ro).doubleValue()!=Math.rint(((Number)ro).doubleValue())) throw new JSONException("Invalid object tile");
                    int c=((Number)co).intValue(),r=((Number)ro).intValue();
                    if(c<0||c>8||r<0||r>8) throw new JSONException("Invalid object tile");
                    if(type.equals("bush") && formatVersion<10 && character.has("variant"))throw new JSONException("Unexpected bush variant");
                    String variant=type.equals("bush") ? (character.has("variant") ? requiredString(character,"variant") : "round") : type.equals("tree") ? requiredString(character,"species") : (type.equals("storage-box") || type.equals("selling-box") || type.equals("sunflower")) && character.has("facing") ? requiredString(character,"facing") : type.equals("sunflower") ? "S" : "";
                    if(type.equals("bush") && !((formatVersion>=9 && variant.equals("round")) || (formatVersion>=10 && java.util.Arrays.asList("flower","berry").contains(variant)) || (formatVersion>=11 && java.util.Arrays.asList("spiky","tiered").contains(variant)) || (formatVersion>=29 && variant.equals("wild-grass")))) throw new JSONException("Unsupported object");
                    if(type.equals("tree") && !java.util.Arrays.asList("apple","orange","mango","pear","guava","starfruit").contains(variant) && !(formatVersion>=25 && (variant.equals("banana") || variant.equals("palm")))) throw new JSONException("Unsupported object");
                    if(type.equals("tree") && !(variant.equals("banana") || variant.equals("palm")) && (c>7||r>7)) throw new JSONException("Invalid tree tile");
                    if(type.equals("sunflower") && !java.util.Arrays.asList("N","E","S","W").contains(variant)) throw new JSONException("Invalid direction");
                    if((type.equals("storage-box") || type.equals("selling-box")) && !variant.isEmpty() && !java.util.Arrays.asList("N","E","S","W").contains(variant)) throw new JSONException("Invalid direction");
                    for(SurfaceObject previous:surfaces) if(!previous.type.equals("road") && !previous.type.equals("fence") && !previous.type.equals("flower") && previous.column==c && previous.row==r) throw new JSONException("Object collision");
                    surfaces.add(new SurfaceObject(type,c,r,null,variant,character.toString(),opacity));continue;
                }
                if ("house".equals(type)) {
                    if (formatVersion < 5 || (house && formatVersion<16) || houses.size()>=81) throw new JSONException("Unsupported house");
                    houseColumn=0;houseRow=0;houseOpacity=-1;houseScale=1.0;houseFacing="S";houseId=null;houseDesign=null;houseVisible=false;
                    if (formatVersion >= 6) {
                        if(formatVersion>=16) {
                            requireOpacityKeys(character,formatVersion,"type","id","column","row","scale","facing","visible","design");
                            if(character.has("id")) {houseId=requiredString(character,"id"); if(!houseId.matches("[A-Za-z0-9_-]{1,128}")) throw new JSONException("Invalid house id");}
                        } else requireOpacityKeys(character,formatVersion, "type", "column", "row", "scale", "facing", "visible", "design");
                        houseOpacity=readOpacity(character,formatVersion);
                        if(character.has("design")){houseDesign=requiredString(character,"design");if(formatVersion<15 || !"wide".equals(houseDesign))throw new JSONException("Unsupported house design");}
                        Object hc = character.get("column"), hr = character.get("row"), hs = character.get("scale"), hv = character.get("visible");
                        if (!(hc instanceof Number) || !(hr instanceof Number) || !(hs instanceof Number) || !(hv instanceof Boolean))
                            throw new JSONException("Invalid house geometry");
                        double c = ((Number) hc).doubleValue(), r = ((Number) hr).doubleValue();
                        houseScale = ((Number) hs).doubleValue();
                        houseFacing = requiredString(character, "facing");
                        if (c != Math.rint(c) || r != Math.rint(r) || !HouseFootprint.of(houseDesign).fits((int)c,(int)r)
                                || !Double.isFinite(houseScale) || houseScale < 0.5 || houseScale > 1.5
                                || !(houseFacing.equals("N") || houseFacing.equals("E") || houseFacing.equals("S") || houseFacing.equals("W")))
                            throw new JSONException("Invalid house geometry");
                        houseColumn = (int) c; houseRow = (int) r; houseVisible = (Boolean) hv;
                    } else {
                        requireOnlyKeys(character, "type", "column", "row");
                        Object hc = character.get("column"), hr = character.get("row");
                        if (!(hc instanceof Number) || !(hr instanceof Number)
                                || ((Number) hc).doubleValue() != 0 || ((Number) hr).doubleValue() != 0)
                            throw new JSONException("Unsupported house location");
                        houseVisible = true;
                    }
                    House h=new House(houseColumn,houseRow,houseScale,houseFacing,houseVisible,houseId,houseDesign,houseOpacity);
                    for(House prior:houses) {
                        if(h.id==null || prior.id==null || h.id.equals(prior.id))throw new JSONException("Duplicate or missing house id");
                        if(h.visible && prior.visible && HouseFootprint.of(h.design).overlaps(h.column,h.row,HouseFootprint.of(prior.design),prior.column,prior.row))throw new JSONException("Overlapping houses");
                    }
                    houses.add(h);house = true; continue;
                }
                if (visible || !"character".equals(type)) throw new JSONException("Unsupported object");
                requireOnlyKeys(character, "type", "column", "row", "scale", "facing");
                if (formatVersion >= 4) {
                    facing = requiredString(character, "facing");
                    if (!facing.equals("N") && !facing.equals("E") && !facing.equals("S") && !facing.equals("W"))
                        throw new JSONException("Invalid character direction");
                } else if (character.has("facing")) throw new JSONException("Unexpected direction");
                if (!"character".equals(requiredString(character, "type"))) throw new JSONException("Unsupported object");
                Object col = character.get("column"), r = character.get("row"), size = character.get("scale");
                if (!(col instanceof Number) || !(r instanceof Number) || !(size instanceof Number))
                    throw new JSONException("Invalid character geometry");
                double colValue = ((Number) col).doubleValue(), rowValue = ((Number) r).doubleValue();
                scale = ((Number) size).doubleValue();
                if (colValue != Math.rint(colValue) || rowValue != Math.rint(rowValue)
                        || colValue < 0 || colValue > 8 || rowValue < 0 || rowValue > 8
                        || !Double.isFinite(scale) || scale < 0.5 || scale > 1.5)
                    throw new JSONException("Invalid character geometry");
                column = (int) colValue; row = (int) rowValue; visible = true;
            }
        }
        for(SurfaceObject flower:surfaces) if(flower.type.equals("flower"))
            for(SurfaceObject water:surfaces) if(water.type.equals("road") && "water".equals(water.material) && water.column==flower.column && water.row==flower.row)throw new JSONException("Flower on water");
        // Validate the complete tree footprint, not only its anchor tile.
        for(SurfaceObject tree:surfaces) if(tree.type.equals("tree")) {
            int size=(tree.material.equals("banana") || tree.material.equals("palm"))?1:2;
            for(int dc=0;dc<size;dc++)for(int dr=0;dr<size;dr++) {
                int c=tree.column+dc,r=tree.row+dr;
                if(visible && c==column && r==row || houseCovers(houses,c,r))throw new JSONException("Object collision");
                for(SurfaceObject other:surfaces) {
                    if(other==tree || other.type.equals("fence") || other.type.equals("flower") || other.type.equals("road") && !"water".equals(other.material))continue;
                    int otherSize=other.type.equals("tree") && !(other.material.equals("banana")||other.material.equals("palm"))?2:1;
                    if(c>=other.column && c<other.column+otherSize && r>=other.row && r<other.row+otherSize)throw new JSONException("Object collision");
                }
            }
        }
        // New one-tile plants must never hide under an existing blocker, even if the
        // objects arrive in the opposite order in the file.
        for (SurfaceObject plant : surfaces) {
            boolean tropical=plant.type.equals("tree") && (plant.material.equals("banana") || plant.material.equals("palm"));
            if (!tropical && !plant.type.equals("sunflower")) continue;
            int c=plant.column,r=plant.row;
            if (visible && c==column && r==row || houseCovers(houses,c,r)) throw new JSONException("Object collision");
            for (SurfaceObject other:surfaces) {
                if (other==plant || other.type.equals("fence") || other.type.equals("flower")) continue;
                if (other.type.equals("road") && !"water".equals(other.material)) continue;
                int size=other.type.equals("tree") && !(other.material.equals("banana") || other.material.equals("palm"))?2:1;
                if(c>=other.column && c<other.column+size && r>=other.row && r<other.row+size) throw new JSONException("Object collision");
            }
        }
        if(!houses.isEmpty()) {House first=houses.get(0);houseColumn=first.column;houseRow=first.row;houseScale=first.scale;houseFacing=first.facing;houseVisible=first.visible;houseId=first.id;houseDesign=first.design;houseOpacity=first.opacity;}
        return new World(id, name, description, color, visible, column, row, scale, facing,
                house, houseVisible, houseColumn, houseRow, houseScale, houseFacing, houseId, houseDesign, houseOpacity, formatVersion, portals, surfaces, houses);
    }

    private static boolean hasOverlappingFlowers(List<SurfaceObject> surfaces) {
        for(SurfaceObject f:surfaces)if(f.type.equals("flower"))for(SurfaceObject b:surfaces)
            if((b.type.equals("bush")||b.type.equals("sunflower"))&&f.column==b.column&&f.row==b.row)return true;
        return false;
    }
    private static int readOpacity(JSONObject object,int version) throws JSONException {
        if(!object.has("opacity"))return -1;
        if(version<31)throw new JSONException("Unsupported opacity");
        Object value=object.get("opacity");
        if(!(value instanceof Number))throw new JSONException("Invalid opacity");
        double opacity=((Number)value).doubleValue();
        if(!Double.isFinite(opacity) || opacity<0 || opacity>90 || opacity%5!=0)throw new JSONException("Invalid opacity");
        return (int)opacity;
    }
    private static void requireOpacityKeys(JSONObject object,int version,String... allowed) throws JSONException {
        readOpacity(object,version);
        String[] keys=java.util.Arrays.copyOf(allowed,allowed.length+1);keys[allowed.length]="opacity";
        requireOnlyKeys(object,keys);
    }

    private static void requireOnlyKeys(JSONObject object, String... allowed) throws JSONException {
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            boolean found = false;
            for (String item : allowed) if (item.equals(key)) found = true;
            if (!found) throw new JSONException("Unsupported field: " + key);
        }
    }

    private static World fromPreview(PreviewWorld20 data) throws JSONException {
        JSONObject w=new JSONObject(data.toJson()).getJSONObject("world");
        JSONObject h=w.isNull("house") ? null : w.getJSONObject("house");
        List<House> houses=new ArrayList<>();
        if(h!=null) houses.add(new House(h.getInt("column"),h.getInt("row"),h.optDouble("scale",1),h.optString("facing","S"),h.optBoolean("visible",true),null,null,h.has("opacity")?h.getInt("opacity"):-1));
        House first=houses.isEmpty()?null:houses.get(0);
        List<SurfaceObject> trees=new ArrayList<>();
        if(w.has("orangeTrees")) {
            JSONArray source=w.getJSONArray("orangeTrees");
            for(int i=0;i<source.length();i++) {
                JSONObject tree=source.getJSONObject(i);
                String kind=tree.optString("kind","");boolean grass="wild-grass".equals(kind), bush="bush".equals(kind);
                trees.add(new SurfaceObject(grass || bush?"bush":"tree",tree.getInt("column"),tree.getInt("row"),tree.optString("facing","S"),grass?"wild-grass":bush?"preview-bush"+(tree.has("variant")?"-"+tree.getString("variant"):"")+(tree.optInt("size")==2?"-large":""):kind.isEmpty()?"orange":tree.optInt("size",2)==1?"conifer-small":"conifer",tree.toString(),tree.has("opacity")?tree.getInt("opacity"):-1));
            }
        }
        if(w.has("fences")) {
            JSONArray fences=w.getJSONArray("fences");
            for(int i=0;i<fences.length();i++) {
                JSONObject f=fences.getJSONObject(i);
                trees.add(new SurfaceObject("fence",f.getInt("column"),f.getInt("row"),f.getString("edge"),"preview-"+f.getString("material"),f.toString(),f.has("opacity")?f.getInt("opacity"):-1));
            }
        }
        World result=new World(data.id,w.getString("name"),w.getString("description"),w.getString("color"),
                w.getBoolean("showCharacter"),w.getInt("characterColumn"),w.getInt("characterRow"),w.getDouble("characterScale"),w.getString("characterFacing"),
                first!=null,first!=null && first.visible,first==null?0:first.column,first==null?0:first.row,first==null?1:first.scale,
                first==null?"S":first.facing,null,null,first==null?-1:first.opacity,32,new ArrayList<>(),trees,houses);
        result.previewData=data; return result;
    }

    static World parseExport(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if ("gtlauncher-preview-world".equals(root.optString("format"))) return fromPreview(PreviewWorld20.parse(json));
        if (!"gtlauncher-world".equals(requiredString(root, "format"))
                || !(root.get("version") instanceof Number)
                || !(root.get("world") instanceof JSONObject)) {
            throw new JSONException("Unsupported world format");
        }
        double version = ((Number) root.get("version")).doubleValue();
        JSONObject object = root.getJSONObject("world");
        if (version == 1.0) {
            // Legacy exports had no stable identifier; reject unknown fields rather than discard them.
            requireOnlyKeys(root, "format", "version", "world");
            requireOnlyKeys(object, "name", "description", "color");
            return parseWorld(object, UUID.randomUUID().toString(), 1);
        }
        if (version != 2.0 && version != 3.0 && version != 4.0 && version != 5.0 && version != 6.0 && version != 7.0 && version != 8.0 && version != 9.0 && version != 10.0 && version != 11.0 && version != 12.0 && version != 14.0 && version != 15.0 && version != 16.0 && version != 17.0 && version != 18.0 && version != 19.0 && version != 20.0 && version != 21.0 && version != 22.0 && version != 25.0 && version != 26.0 && version != 27.0 && version != 28.0 && version != 29.0 && version != 30.0 && version != 31.0) throw new JSONException("Unsupported world version");
        requireOnlyKeys(root, "format", "version", "world");
        requireOnlyKeys(object, "id", "name", "description", "color", "objects");
        String id = requiredString(object, "id");
        if (!id.matches("[A-Za-z0-9_-]{1,128}") || BASE_ID.equals(id))
            throw new JSONException("Invalid world id");
        Object objects = object.get("objects");
        if (!(objects instanceof JSONArray) || (version == 2.0 && ((JSONArray) objects).length() != 0))
            throw new JSONException("Unsupported world objects");
        return parseWorld(object, id, (int) version);
    }

    private static int storedVersion(JSONObject item) throws JSONException {
        if (!item.has("objects")) return 3;
        Object value = item.get("objects");
        if (!(value instanceof JSONArray)) throw new JSONException("Invalid objects");
        JSONArray objects = (JSONArray) value;
        int version = 3;
        for (int i = 0; i < objects.length(); i++) {
            JSONObject obj = objects.getJSONObject(i);
            if(obj.has("opacity"))version=Math.max(version,31);
            if("flower".equals(obj.optString("type")))version=Math.max(version,21);
            if ("selling-box".equals(obj.optString("type"))) version = Math.max(version, 30);
            else if ("sunflower".equals(obj.optString("type")) || "tree".equals(obj.optString("type")) && ("banana".equals(obj.optString("species")) || "palm".equals(obj.optString("species"))) || "bush".equals(obj.optString("type")) && "wild-grass".equals(obj.optString("variant"))) version = Math.max(version, 29);
            else if ("fence".equals(obj.optString("type")) || "road".equals(obj.optString("type")) || "bush".equals(obj.optString("type")) || "tree".equals(obj.optString("type")) || "mailbox".equals(obj.optString("type")) || "storage-box".equals(obj.optString("type"))) version = Math.max(version, 28);
            else if ("house".equals(obj.optString("type"))) version = Math.max(version, obj.has("id") ? 28 : obj.has("design") ? 15 : obj.has("visible") || obj.has("scale") || obj.has("facing") ? 6 : 5);
            else if (obj.has("facing")) version = Math.max(version, 4);
        }
        return version;
    }

    List<World> list() throws JSONException {
        JSONArray array = new JSONArray(prefs.getString(WORLDS_KEY, "[]"));
        if (array.length() > 1000) throw new JSONException("Too many worlds");
        List<World> worlds = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            String id = requiredString(item, "id");
            if (id.isEmpty() || BASE_ID.equals(id)) throw new JSONException("Invalid world id");
            for (World previous : worlds) if (previous.id.equals(id)) throw new JSONException("Duplicate world id");
            if(item.has("previewExport")) {
                requireOnlyKeys(item,"id","previewExport");
                World world=fromPreview(PreviewWorld20.parse(requiredString(item,"previewExport")));
                if(!world.id.equals(id)) throw new JSONException("World id mismatch");
                worlds.add(world);
            } else worlds.add(parseWorld(item, id, storedVersion(item)));
        }
        return worlds;
    }

    String activeId() { return prefs.getString(ACTIVE_KEY, BASE_ID); }

    World activeWorld() throws JSONException {
        String id = activeId();
        for (World world : list()) if (world.id.equals(id)) return world;
        return null; // Base World (or the removed built-in Home) has the base appearance
    }

    boolean select(String id) throws JSONException {
        boolean found = BASE_ID.equals(id);
        for (World world : list()) if (world.id.equals(id)) found = true;
        return found && prefs.edit().putString(ACTIVE_KEY, id).commit();
    }

    boolean upsertAndSelect(World world) throws JSONException {
        List<World> existing = list(); // Never overwrite unreadable stored data.
        if(inventory!=null)for(World old:existing)if(old.id.equals(world.id))for(SurfaceObject box:old.surfaces)if(box.type.equals("storage-box")) {
            boolean retained=false;for(SurfaceObject replacement:world.surfaces)if(replacement.type.equals("storage-box") && replacement.column==box.column && replacement.row==box.row)retained=true;
            if(!retained && inventory.box(InventoryStore.boxKey(old.id,box.column,box.row)).length()>0)throw new JSONException("Kosongkan storage box sebelum mengganti world");
        }

        boolean found = false;
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).id.equals(world.id)) {
                World old=existing.get(i);
                // These are separate schemas: a preview file has no legacy objects/portals,
                // and a legacy file has no portable terrain. Neither can replace the other.
                if ((old.previewData==null)!=(world.previewData==null))
                    throw new JSONException("World format cannot replace different format");
                if(old.previewData!=null && world.previewData.version<old.previewData.version) {
                    if(world.previewData.version<27 && old.previewData.bridges.stream().anyMatch(b->b.inset))throw new JSONException("Older preview format cannot replace inset bridges");
                    if(world.previewData.version<26 && old.previewData.bridges.stream().anyMatch(b->b.centered))throw new JSONException("Older preview format cannot replace centered bridges");
                    if(world.previewData.version<25 && old.previewData.bridges.stream().anyMatch(b->b.free))throw new JSONException("Older preview format cannot replace free bridges");
                    if(world.previewData.version<24 && old.previewData.bridges.stream().anyMatch(b->b.overWater))throw new JSONException("Older preview format cannot replace water bridges");
                    if(world.previewData.version<23 && old.previewData.bridges.stream().anyMatch(b->b.gap))throw new JSONException("Older preview format cannot replace gap bridges");
                    if(world.previewData.version<22 && old.previewData.bridges.stream().anyMatch(b->b.width>1))throw new JSONException("Older preview format cannot replace wide bridges");
                    if(world.previewData.version<21 && old.previewData.hasRamps())throw new JSONException("Older preview format cannot replace ramps");
                    if(world.previewData.version<4 && old.previewData.hasStacks())throw new JSONException("Older preview format cannot replace stacked terrain");
                    if(world.previewData.version<20 && old.previewData.hasBlockOpacity())throw new JSONException("Older preview format cannot replace block opacity");
                    if(world.previewData.version<19 && (old.surfaces.stream().anyMatch(o->o.opacity>=0) || old.previewData.bridges.stream().anyMatch(b->b.opacity>=0)))throw new JSONException("Older preview format cannot replace object opacity");
                    if(world.previewData.version<18 && old.houses.stream().anyMatch(h->h.opacity>=0))throw new JSONException("Older preview format cannot replace house opacity");
                    if(world.previewData.version<17 && old.surfaces.stream().anyMatch(o->o.type.equals("fence") && o.material.equals("preview-stone-tall")))throw new JSONException("Older preview format cannot replace tall walls");
                    if(world.previewData.version<16 && old.previewData.bridges.stream().anyMatch(b->b.length!=3))throw new JSONException("Older preview format cannot replace bridge length");
                    if(!old.previewData.bridges.isEmpty() && world.previewData.version<15)throw new JSONException("Older preview format cannot replace bridges");
                    if(!old.surfaces.isEmpty()) throw new JSONException("Older preview format cannot replace trees");
                    int maxTile=world.previewData.version==1?1:world.previewData.version==2?5:world.previewData.version<14?6:10;
                    for(int row=0;row<old.previewData.size;row++) for(int col=0;col<old.previewData.size;col++) {
                        if(old.previewData.tile(col,row)>maxTile)throw new JSONException("Older preview format cannot replace newer terrain");
                        if(world.previewData.version<14)for(int level=0;level<old.previewData.height(col,row);level++)if(old.previewData.block(col,row,level)>maxTile)throw new JSONException("Older preview format cannot replace newer terrain");
                    }
                }
                if(world.formatVersion<21 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("flower") && !java.util.Arrays.asList("rose","tulip","lavender","daisy","marigold","bluebell").contains(o.material)))throw new JSONException("Legacy export cannot replace flower variant");
                if(world.formatVersion<21 && hasOverlappingFlowers(existing.get(i).surfaces))throw new JSONException("Legacy export cannot replace overlapping flowers");
                if(world.formatVersion<20 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("flower")))throw new JSONException("Legacy export cannot replace flowers");
                if(world.formatVersion<31 && (existing.get(i).houses.stream().anyMatch(h->h.opacity>=0) || existing.get(i).surfaces.stream().anyMatch(o->o.opacity>=0) || existing.get(i).portals.stream().anyMatch(p->p.opacity>=0)))throw new JSONException("Legacy export cannot replace opacity");
                if (world.formatVersion < 30 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("selling-box"))) throw new JSONException("Legacy export cannot replace selling box");
                if (world.formatVersion < 25 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("tree") && (o.material.equals("banana") || o.material.equals("palm")))) throw new JSONException("Legacy export cannot replace tropical trees");
                if (world.formatVersion < 18 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("sunflower")) || world.formatVersion < 19 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("sunflower") && !o.material.equals("S"))) throw new JSONException("Legacy export cannot replace sunflower");
                if (world.formatVersion < 29 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("bush") && o.material.equals("wild-grass"))) throw new JSONException("Legacy export cannot replace wild grass");
                if(world.formatVersion<15 && existing.get(i).houses.stream().anyMatch(h->h.design!=null))throw new JSONException("Legacy export cannot replace house design");
                if (world.formatVersion < 16 && (existing.get(i).houses.size()>1 || existing.get(i).houses.stream().anyMatch(h->h.id!=null))) throw new JSONException("Legacy export cannot replace house identity");
                if (world.formatVersion < 27 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("storage-box") && !o.material.isEmpty())) throw new JSONException("Legacy export cannot replace storage direction");
                if (world.formatVersion < 26 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("storage-box"))) throw new JSONException("Legacy export cannot replace storage");
                if (world.formatVersion < 17 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("mailbox"))) throw new JSONException("Legacy export cannot replace mailbox");
                if (world.formatVersion < 14 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("tree"))) throw new JSONException("Legacy export cannot replace tree");
                if(world.formatVersion<11 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("bush") && (o.material.equals("spiky") || o.material.equals("tiered"))))throw new JSONException("Legacy export cannot replace bush variant");
                if(world.formatVersion<10 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("bush") && (o.material.equals("flower") || o.material.equals("berry"))))throw new JSONException("Legacy export cannot replace bush variant");
                if (world.formatVersion < 9 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("bush"))) throw new JSONException("Legacy export cannot replace bush");
                if(world.formatVersion<19 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("road") && o.edge!=null))throw new JSONException("Legacy export cannot replace road direction");
                if(world.formatVersion<12 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("road") && ("paving".equals(o.material)||"asphalt".equals(o.material))))throw new JSONException("Legacy export cannot replace road material");
                if (world.formatVersion < 8 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("road"))) throw new JSONException("Legacy export cannot replace road data");
                if(world.formatVersion<10 && existing.get(i).surfaces.stream().anyMatch(o->o.type.equals("fence") && o.material!=null && !o.material.isEmpty()))throw new JSONException("Legacy export cannot replace fence variant");
                if (world.formatVersion < 7 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("fence"))) throw new JSONException("Legacy export cannot replace fence data");
                if (world.formatVersion < 22 && !existing.get(i).portals.isEmpty()) throw new JSONException("Legacy export cannot replace portal data");
                if (world.formatVersion < 11 && existing.get(i).surfaces.stream().anyMatch(o -> "water".equals(o.material))) throw new JSONException("Legacy export cannot replace water data");
                if (world.formatVersion < 28 && existing.get(i).portals.stream().anyMatch(p -> p.destinationWorldId != null))
                    throw new JSONException("Legacy export cannot replace portal data");
                // A legacy file cannot express characters: reject rather than silently erase one.
                if (world.formatVersion < 3 && existing.get(i).showCharacter)
                    throw new JSONException("Legacy export cannot replace a world with a character");
                if (world.formatVersion < 4 && existing.get(i).showCharacter
                        && !"S".equals(existing.get(i).characterFacing))
                    throw new JSONException("Legacy export cannot replace a rotated character");
                if (world.formatVersion < 5 && existing.get(i).hasHouse)
                    throw new JSONException("Legacy export cannot replace a world with a house");
                if (world.formatVersion == 5 && existing.get(i).hasHouse &&
                        (existing.get(i).houseColumn != 0 || existing.get(i).houseRow != 0 || existing.get(i).houseScale != 1.0
                        || !"S".equals(existing.get(i).houseFacing) || !existing.get(i).showHouse))
                    throw new JSONException("Legacy export cannot replace edited house");
                existing.set(i, world);
                found = true;
                break;
            }
        }
        if (!found) {
            if (existing.size() >= 1000) throw new JSONException("Too many worlds");
            existing.add(world);
        }
        return saveWorlds(existing, world.id);
    }

    private boolean saveWorlds(List<World> worlds, String activeId) throws JSONException {
        JSONArray array = new JSONArray();
        for (World item : worlds) {
            JSONObject object = new JSONObject();
            if(item.previewData != null) {
                object.put("id",item.id); object.put("previewExport",item.previewData.toJson()); array.put(object); continue;
            }
            object.put("id", item.id); object.put("name", item.name);
            object.put("description", item.description); object.put("color", item.color);
            JSONArray objects = new JSONArray();
            if (item.showCharacter) {
                JSONObject character = new JSONObject();
                character.put("type", "character"); character.put("column", item.characterColumn);
                character.put("row", item.characterRow); character.put("scale", item.characterScale);
                character.put("facing", item.characterFacing);
                objects.put(character);
            }
            for(House h:item.houses) {
                JSONObject house = new JSONObject();house.put("type","house");if(h.id!=null)house.put("id",h.id);
                house.put("column",h.column);house.put("row",h.row);house.put("scale",h.scale);house.put("facing",h.facing);house.put("visible",h.visible);
                if(h.opacity>=0)house.put("opacity",h.opacity);if(h.design!=null)house.put("design",h.design);objects.put(house);
            }
            for (SurfaceObject surface : item.surfaces) objects.put(new JSONObject(surface.raw));
            for (PortalTravel.Portal portal : item.portals) {
                JSONObject p=new JSONObject();p.put("type","fence");p.put("variant","portal");p.put("id",portal.id);
                p.put("column",portal.tile.column);p.put("row",portal.tile.row);p.put("edge",portal.edge);
                if(portal.destinationWorldId!=null){p.put("destinationWorldId",portal.destinationWorldId);p.put("destinationPortalId",portal.destinationPortalId);}
                if(portal.opacity>=0)p.put("opacity",portal.opacity);
                objects.put(p);
            }
            object.put("objects", objects);
            array.put(object);
        }
        return prefs.edit().putString(WORLDS_KEY, array.toString())
                .putString(ACTIVE_KEY, activeId).commit();
    }

    boolean moveCharacter(String worldId, int column, int row, String facing) throws JSONException {
        if (!worldId.equals(activeId()) || !java.util.Arrays.asList("N","E","S","W").contains(facing)) return false;
        List<World> worlds=list();
        for(int i=0;i<worlds.size();i++) { World w=worlds.get(i); if(w.id.equals(worldId) && w.showCharacter) {
            if(!w.tileExists(column,row) || (houseCovers(w.houses,column,row) && (column!=w.characterColumn || row!=w.characterRow))) return false;
            if(w.previewData!=null) for(SurfaceObject object:w.surfaces) {
                if(walkThrough(object) || object.type.equals("fence"))continue;
                int size=surfaceFootprint(object);
                if(column>=object.column && row>=object.row && column<object.column+size && row<object.row+size)return false;
            }
            if(w.previewData!=null) {
                JSONObject root=new JSONObject(w.previewData.toJson()), data=root.getJSONObject("world");
                data.put("characterColumn",column); data.put("characterRow",row); data.put("characterFacing",facing);
                worlds.set(i,fromPreview(PreviewWorld20.parse(root.toString())));
            } else worlds.set(i, new World(w.id,w.name,w.description,w.color,true,column,row,w.characterScale,facing,w.hasHouse,w.showHouse,w.houseColumn,w.houseRow,w.houseScale,w.houseFacing,w.houseId,w.houseDesign,w.houseOpacity,w.formatVersion,w.portals,w.surfaces,w.houses));
            return saveWorlds(worlds,worldId);
        }}
        return false;
    }
    /** Only objects without coordinate-keyed storage, harvest history or portal links are portable. */
    static int treeFootprint(SurfaceObject object) {
        return object.material.equals("banana") || object.material.equals("palm") || object.material.equals("conifer-small")?1:2;
    }
    static boolean walkThrough(SurfaceObject object) {return object!=null && object.type.equals("bush");}
    static int surfaceFootprint(SurfaceObject object) {
        if(object.type.equals("tree"))return treeFootprint(object);
        return isPreviewBush(object) && object.material.endsWith("-large")?2:1;
    }
    static boolean isPreviewGrass(SurfaceObject object) {
        return object!=null && object.type.equals("bush") && object.material.equals("wild-grass") && object.edge!=null;
    }
    static boolean isPreviewBush(SurfaceObject object) {
        return object!=null && object.type.equals("bush") && object.material.startsWith("preview-bush") && object.edge!=null;
    }
    static boolean portableSmall(SurfaceObject object) {
        return object!=null && !isPreviewGrass(object) && !isPreviewBush(object) && (object.type.equals("flower") || object.type.equals("bush") || object.type.equals("sunflower"));
    }
    JSONArray objectBag() throws JSONException {
        JSONArray bag=new JSONArray(prefs.getString(OBJECT_BAG_KEY,"[]"));
        if(bag.length()>200)throw new JSONException("Object bag full or invalid");
        for(int i=0;i<bag.length();i++) {
            JSONObject entry=bag.getJSONObject(i);requireOnlyKeys(entry,"worldId","object");
            String id=requiredString(entry,"worldId");if(!id.matches("[A-Za-z0-9_-]{1,128}") || id.equals(BASE_ID))throw new JSONException("Invalid object bag world");
            JSONObject object=entry.getJSONObject("object");
            JSONObject probe=new JSONObject().put("id",id).put("name","Bag validation").put("description","").put("color","#a8c795").put("objects",new JSONArray().put(object));
            World parsed=parseWorld(probe,id,31);
            if(parsed.surfaces.size()!=1 || !portableSmall(parsed.surfaces.get(0)))throw new JSONException("Unsupported object in bag");
        }
        return bag;
    }
    static boolean sameHouse(House a,House b) {
        return a!=null && b!=null && a.column==b.column && a.row==b.row && a.visible==b.visible && a.scale==b.scale
                && a.opacity==b.opacity && java.util.Objects.equals(a.id,b.id) && java.util.Objects.equals(a.design,b.design) && a.facing.equals(b.facing);
    }
    /** Move a house or portable small object without rebuilding unrelated stored records. */
    boolean moveSelectedObject(String worldId,House expectedHouse,SurfaceObject expectedSurface,int column,int row) throws JSONException {
        if(!worldId.equals(activeId()) || (expectedHouse==null)==(expectedSurface==null))return false;
        World world=activeWorld();if(world==null)return false;
        House actualHouse=null;SurfaceObject actualSurface=null;
        if(expectedHouse!=null) {for(House h:world.houses)if(sameHouse(h,expectedHouse)){actualHouse=h;break;}if(actualHouse==null || !actualHouse.visible)return false;}
        else {if(!portableSmall(expectedSurface) && !(world.previewData!=null && (isPreviewGrass(expectedSurface) || isPreviewBush(expectedSurface) || (expectedSurface.type.equals("tree") && expectedSurface.material.startsWith("conifer")) || (expectedSurface.type.equals("fence") && expectedSurface.material.startsWith("preview-")))))return false;for(SurfaceObject o:world.surfaces)if(o.raw.equals(expectedSurface.raw)){actualSurface=o;break;}if(actualSurface==null)return false;}
        if(actualSurface!=null && world.previewData!=null && actualSurface.type.equals("fence") && actualSurface.material.startsWith("preview-")) {
            if(!world.tileExists(column,row) || world.previewData.tile(column,row)==3 || houseCovers(world.houses,column,row))return false;
            JSONArray all=new JSONArray(prefs.getString(WORLDS_KEY,"[]"));JSONObject stored=null;
            for(int i=0;i<all.length();i++)if(worldId.equals(all.getJSONObject(i).optString("id"))){stored=all.getJSONObject(i);break;}
            if(stored==null)return false;
            JSONObject root=new JSONObject(world.previewData.toJson());JSONArray fences=root.getJSONObject("world").getJSONArray("fences");boolean changed=false;
            for(int i=0;i<fences.length();i++){JSONObject f=fences.getJSONObject(i);if(f.toString().equals(actualSurface.raw)){f.put("column",column);f.put("row",row);changed=true;break;}}
            if(!changed)return false;
            try{stored.put("previewExport",PreviewWorld20.parse(root.toString()).toJson());}catch(JSONException invalid){return false;}
            return prefs.edit().putString(WORLDS_KEY,all.toString()).commit();
        }
        int width=actualHouse==null?surfaceFootprint(actualSurface):HouseFootprint.of(actualHouse.design).columns,height=actualHouse==null?surfaceFootprint(actualSurface):HouseFootprint.of(actualHouse.design).rows;
        if(column<0 || row<0 || column+width>world.gridSize() || row+height>world.gridSize())return false;
        for(int y=row;y<row+height;y++)for(int x=column;x<column+width;x++) {
            if(!world.tileExists(x,y) || (world.previewData!=null && world.previewData.tile(x,y)==3)
                || (world.showCharacter && world.characterColumn==x && world.characterRow==y))return false;
            for(House h:world.houses)if(h!=actualHouse && h.covers(x,y))return false;
            for(SurfaceObject o:world.surfaces) {
                if(o==actualSurface || (actualHouse==null && o.type.equals("fence")))continue;
                int size=surfaceFootprint(o);
                if(x>=o.column && x<o.column+size && y>=o.row && y<o.row+size)return false;
            }
            for(PortalTravel.Portal portal:world.portals)if(portal.tile.column==x && portal.tile.row==y)return false;
        }
        JSONArray all=new JSONArray(prefs.getString(WORLDS_KEY,"[]"));JSONObject stored=null;
        for(int i=0;i<all.length();i++)if(worldId.equals(all.getJSONObject(i).optString("id"))){stored=all.getJSONObject(i);break;}
        if(stored==null)return false;
        if(world.previewData!=null) {
            JSONObject root=new JSONObject(world.previewData.toJson()), data=root.getJSONObject("world");
            if(actualHouse!=null) {
                JSONObject h=data.getJSONObject("house");h.put("column",column);h.put("row",row);
            } else if(isPreviewGrass(actualSurface) || isPreviewBush(actualSurface) || (actualSurface.type.equals("tree") && actualSurface.material.startsWith("conifer"))) {
                JSONArray objects=data.getJSONArray("orangeTrees");boolean changed=false;
                for(int i=0;i<objects.length();i++) {
                    JSONObject o=objects.getJSONObject(i);
                    if(o.toString().equals(actualSurface.raw)){o.put("column",column);o.put("row",row);changed=true;break;}
                }
                if(!changed)return false;
            } else return false;
            stored.put("previewExport",PreviewWorld20.parse(root.toString()).toJson());
        } else {
            JSONArray objects=stored.getJSONArray("objects");boolean changed=false;
            for(int i=0;i<objects.length();i++) {
                JSONObject o=objects.getJSONObject(i);
                if((actualHouse!=null && o.optString("type").equals("house") && o.optInt("column",-1)==actualHouse.column && o.optInt("row",-1)==actualHouse.row && java.util.Objects.equals(o.optString("id",null),actualHouse.id))
                   || (actualSurface!=null && o.toString().equals(actualSurface.raw))) {o.put("column",column);o.put("row",row);changed=true;break;}
            }
            if(!changed)return false;parseWorld(stored,worldId,storedVersion(stored));
        }
        return prefs.edit().putString(WORLDS_KEY,all.toString()).commit();
    }
    /** Atomic group edit for supported flat preview objects. No partial writes on invalid destinations. */
    boolean editPreviewObjectGroup(String worldId,List<SurfaceObject> selected,int dc,int dr,boolean delete) throws JSONException {
        if(worldId==null || !worldId.equals(activeId()) || selected==null || selected.isEmpty())return false;
        World world=activeWorld();if(world==null || world.previewData==null)return false;
        java.util.Set<String> wantedTrees=new java.util.HashSet<>(),wantedFences=new java.util.HashSet<>();
        for(SurfaceObject o:selected){if(o==null)return false;boolean found=false;for(SurfaceObject actual:world.surfaces)if(actual.raw.equals(o.raw) && actual.type.equals(o.type)){found=true;break;}if(!found)return false;
            if(o.type.equals("fence") && o.material.startsWith("preview-")){if(!wantedFences.add(o.raw))return false;}
            else if(isPreviewGrass(o) || isPreviewBush(o) || (o.type.equals("tree") && o.material.startsWith("conifer"))){if(!wantedTrees.add(o.raw))return false;}
            else return false;
        }
        JSONObject root=new JSONObject(world.previewData.toJson()),data=root.getJSONObject("world");
        String[] keys={"orangeTrees","fences"};java.util.List<java.util.Set<String>> sets=java.util.Arrays.asList(wantedTrees,wantedFences);
        for(int n=0;n<keys.length;n++){
            JSONArray source=data.optJSONArray(keys[n]);if(source==null)source=new JSONArray();JSONArray updated=new JSONArray();int found=0;
            for(int i=0;i<source.length();i++){JSONObject item=source.getJSONObject(i);if(sets.get(n).contains(item.toString())){found++;if(delete)continue;item.put("column",item.getInt("column")+dc);item.put("row",item.getInt("row")+dr);
                    if(n==0 && world.showCharacter){int size=item.optInt("size",2),c=item.getInt("column"),r=item.getInt("row");if(world.characterColumn>=c && world.characterColumn<c+size && world.characterRow>=r && world.characterRow<r+size)return false;}}
                updated.put(item);
            }
            if(found!=sets.get(n).size())return false;if(updated.length()==0)data.remove(keys[n]);else data.put(keys[n],updated);
        }
        try{PreviewWorld20.parse(root.toString());}catch(JSONException invalid){return false;}
        JSONArray all=new JSONArray(prefs.getString(WORLDS_KEY,"[]"));JSONObject stored=null;for(int i=0;i<all.length();i++)if(worldId.equals(all.getJSONObject(i).optString("id"))){stored=all.getJSONObject(i);break;}if(stored==null)return false;
        stored.put("previewExport",root.toString());return prefs.edit().putString(WORLDS_KEY,all.toString()).commit();
    }
    /** Delete only stateless plants; storage, harvest and portal records must never be discarded here. */
    boolean deleteSelectedPlant(String worldId,SurfaceObject expected) throws JSONException {
        if(!worldId.equals(activeId()) || expected==null)return false;
        World world=activeWorld();if(world==null)return false;
        boolean supported=world.previewData!=null
                ? isPreviewGrass(expected) || isPreviewBush(expected) || (expected.type.equals("tree") && expected.material.startsWith("conifer")) || (expected.type.equals("fence") && expected.material.startsWith("preview-"))
                : portableSmall(expected);
        if(!supported)return false;
        boolean exists=false;for(SurfaceObject o:world.surfaces)if(o.raw.equals(expected.raw)){exists=true;break;}
        if(!exists)return false;
        JSONArray all=new JSONArray(prefs.getString(WORLDS_KEY,"[]"));JSONObject stored=null;
        for(int i=0;i<all.length();i++)if(worldId.equals(all.getJSONObject(i).optString("id"))){stored=all.getJSONObject(i);break;}
        if(stored==null)return false;
        JSONObject root=world.previewData!=null?new JSONObject(world.previewData.toJson()):null;
        JSONArray objects=root!=null?root.getJSONObject("world").getJSONArray(expected.type.equals("fence")?"fences":"orangeTrees"):stored.getJSONArray("objects");
        boolean removed=false;
        for(int i=0;i<objects.length();i++)if(objects.getJSONObject(i).toString().equals(expected.raw)){objects.remove(i);removed=true;break;}
        if(!removed)return false;
        if(root!=null)stored.put("previewExport",PreviewWorld20.parse(root.toString()).toJson());
        else parseWorld(stored,worldId,storedVersion(stored));
        return prefs.edit().putString(WORLDS_KEY,all.toString()).commit();
    }
    /** World removal and bag addition share one preferences commit, so neither half can be lost. */
    boolean takeSelectedObject(String worldId,SurfaceObject expected) throws JSONException {
        if(!worldId.equals(activeId()) || !portableSmall(expected))return false;
        World world=activeWorld();if(world==null || world.previewData!=null)return false;
        boolean exists=false;for(SurfaceObject o:world.surfaces)if(o.raw.equals(expected.raw)){exists=true;break;}if(!exists)return false;
        JSONArray bag=objectBag();if(bag.length()>=200)throw new JSONException("Object bag full");
        JSONArray all=new JSONArray(prefs.getString(WORLDS_KEY,"[]"));JSONObject stored=null;
        for(int i=0;i<all.length();i++)if(worldId.equals(all.getJSONObject(i).optString("id"))){stored=all.getJSONObject(i);break;}
        if(stored==null)return false;JSONArray objects=stored.getJSONArray("objects");boolean removed=false;
        for(int i=0;i<objects.length();i++)if(objects.getJSONObject(i).toString().equals(expected.raw)){objects.remove(i);removed=true;break;}
        if(!removed)return false;parseWorld(stored,worldId,storedVersion(stored));
        bag.put(new JSONObject().put("worldId",worldId).put("object",new JSONObject(expected.raw)));
        return prefs.edit().putString(WORLDS_KEY,all.toString()).putString(OBJECT_BAG_KEY,bag.toString()).commit();
    }
    boolean tillTile(String worldId,int column,int row) throws JSONException {
        if(!worldId.equals(activeId())) return false;
        List<World> worlds=list();
        for(int i=0;i<worlds.size();i++) {
            World world=worlds.get(i); if(!world.id.equals(worldId))continue;
            if(world.previewData==null || !world.showCharacter || !world.tileExists(column,row)
                    || !world.previewData.canStep(world.characterColumn,world.characterRow,column,row)
                    || houseCovers(world.houses,column,row)) return false;
            for(SurfaceObject object:world.surfaces) {
                int size=surfaceFootprint(object);
                if(column>=object.column && row>=object.row && column<object.column+size && row<object.row+size)return false;
            }
            if(world.previewData.rampDirection(column,row)!=null)return false;
            int tile=world.previewData.tile(column,row);if(tile!=1 && tile!=2)return false;
            JSONObject root=new JSONObject(world.previewData.toJson());
            root.put("version",Math.max(3,world.previewData.version));JSONObject data=root.getJSONObject("world");int index=row*world.gridSize()+column;data.getJSONArray("tiles").put(index,6);
            if(data.has("blockStacks")){JSONArray stack=data.getJSONArray("blockStacks").getJSONArray(index);stack.put(stack.length()-1,6);}
            worlds.set(i,fromPreview(PreviewWorld20.parse(root.toString())));return saveWorlds(worlds,worldId);
        }
        return false;
    }
    /** Atomically delete or replace one supported preview object at its original anchor. */
    boolean editCatalogObject(String worldId,String sourceKey,int index,String expected,String targetKey,JSONObject target) throws JSONException {
        java.util.List<String> keys=java.util.Arrays.asList("orangeTrees","fences","bridges");
        if(worldId==null || !worldId.equals(activeId()) || !keys.contains(sourceKey) || (target!=null && !keys.contains(targetKey)))return false;
        World world=activeWorld();if(world==null || world.previewData==null)return false;
        JSONObject root=new JSONObject(world.previewData.toJson()),data=root.getJSONObject("world");JSONArray source=data.optJSONArray(sourceKey);
        if(source==null || index<0 || index>=source.length() || !source.getJSONObject(index).toString().equals(expected))return false;
        JSONObject old=source.getJSONObject(index);
        if(target!=null && (target.optInt("column",-1)!=old.getInt("column") || target.optInt("row",-1)!=old.getInt("row")))return false;
        source.remove(index);data.put(sourceKey,source);
        if(target!=null){
            if(targetKey.equals("orangeTrees") && world.showCharacter){int size=target.optInt("size",2),c=target.getInt("column"),r=target.getInt("row");if(world.characterColumn>=c && world.characterColumn<c+size && world.characterRow>=r && world.characterRow<r+size)return false;}
            JSONArray dest=data.optJSONArray(targetKey);if(dest==null)dest=new JSONArray();dest.put(new JSONObject(target.toString()));data.put(targetKey,dest);
            int needed=targetKey.equals("bridges")?(target.has("length")?16:15):targetKey.equals("fences")?("stone-tall".equals(target.optString("material"))?17:13):target.has("plantId")?12:"bush".equals(target.optString("kind"))?(target.optInt("size",1)==2?11:10):"wild-grass".equals(target.optString("kind"))?9:target.has("size")?8:7;
            root.put("version",Math.max(world.previewData.version,needed));
        }
        PreviewWorld20 parsed;try{parsed=PreviewWorld20.parse(root.toString());}catch(JSONException invalid){return false;}
        List<World> worlds=list();for(int i=0;i<worlds.size();i++)if(worldId.equals(worlds.get(i).id)){worlds.set(i,fromPreview(parsed));return saveWorlds(worlds,worldId);}return false;
    }
    // Commit a checked batch once, after validating every source and the complete final world.
    boolean editCatalogBatch(String worldId, JSONArray edits) throws JSONException {
        if(worldId==null || !worldId.equals(activeId()) || edits.length()==0)return false;
        World world=activeWorld();if(world==null || world.previewData==null)return false;
        JSONObject root=new JSONObject(world.previewData.toJson()),data=root.getJSONObject("world");
        java.util.Set<String> seen=new java.util.HashSet<>();
        java.util.List<JSONObject> entries=new java.util.ArrayList<>();
        for(int i=0;i<edits.length();i++){
            JSONObject e=edits.getJSONObject(i);String key=e.getString("key");int index=e.getInt("index");
            if(!java.util.Arrays.asList("orangeTrees","fences","bridges").contains(key)||!seen.add(key+":"+index))return false;
            JSONArray a=data.optJSONArray(key);if(a==null||index<0||index>=a.length()||!a.getJSONObject(index).toString().equals(e.getString("expected")))return false;
            if(e.has("target")){JSONObject old=a.getJSONObject(index),target=e.getJSONObject("target");String to=e.getString("targetKey");
                if(!java.util.Arrays.asList("orangeTrees","fences","bridges").contains(to)||target.optInt("column",-1)!=old.getInt("column")||target.optInt("row",-1)!=old.getInt("row"))return false;
                if(to.equals("orangeTrees") && world.showCharacter){int size=target.optInt("size",2),c=target.getInt("column"),r=target.getInt("row");if(world.characterColumn>=c&&world.characterColumn<c+size&&world.characterRow>=r&&world.characterRow<r+size)return false;}
            }entries.add(e);
        }
        java.util.Collections.sort(entries,(a,b)->Integer.compare(b.optInt("index"),a.optInt("index")));
        for(JSONObject e:entries){String key=e.getString("key");JSONArray a=data.getJSONArray(key);a.remove(e.getInt("index"));}
        int version=world.previewData.version;
        for(JSONObject e:entries)if(e.has("target")){String key=e.getString("targetKey");JSONObject target=e.getJSONObject("target");JSONArray a=data.optJSONArray(key);if(a==null)a=new JSONArray();a.put(target);data.put(key,a);
            int needed=key.equals("bridges")?(target.has("length")?16:15):key.equals("fences")?("stone-tall".equals(target.optString("material"))?17:13):target.has("plantId")?12:"bush".equals(target.optString("kind"))?(target.optInt("size",1)==2?11:10):"wild-grass".equals(target.optString("kind"))?9:target.has("size")?8:7;version=Math.max(version,needed);
        }
        root.put("version",version);
        PreviewWorld20 parsed;try{parsed=PreviewWorld20.parse(root.toString());}catch(JSONException invalid){return false;}
        List<World> worlds=list();for(int i=0;i<worlds.size();i++)if(worldId.equals(worlds.get(i).id)){worlds.set(i,fromPreview(parsed));return saveWorlds(worlds,worldId);}return false;
    }
    boolean editPreviewBridge(String worldId,BridgeGeometry.Bridge expected,int column,int row,boolean delete) throws JSONException {
        if(worldId==null || !worldId.equals(activeId()) || expected==null)return false;
        World world=activeWorld();if(world==null || world.previewData==null)return false;
        JSONObject root=new JSONObject(world.previewData.toJson()),data=root.getJSONObject("world");JSONArray before=data.optJSONArray("bridges");if(before==null)return false;
        JSONArray next=new JSONArray();boolean found=false;
        for(int i=0;i<before.length();i++) {JSONObject item=before.getJSONObject(i);
            if(item.getInt("column")==expected.column && item.getInt("row")==expected.row && item.getString("axis").equals(expected.axis) && item.getString("material").equals(expected.material) && item.optInt("length",3)==expected.length && item.optInt("opacity",-1)==expected.opacity){
                if(found)return false;found=true;if(delete)continue;item.put("column",column);item.put("row",row);
            }
            next.put(item);
        }
        if(!found)return false;if(next.length()==0)data.remove("bridges");else data.put("bridges",next);
        PreviewWorld20 parsed;try{parsed=PreviewWorld20.parse(root.toString());}catch(JSONException invalid){return false;}
        java.util.List<World> worlds=list();for(int i=0;i<worlds.size();i++)if(worldId.equals(worlds.get(i).id)){worlds.set(i,fromPreview(parsed));return saveWorlds(worlds,worldId);}return false;
    }
    boolean placePreviewBridge(String worldId,int column,int row,String axis,String material) throws JSONException {return placePreviewBridge(worldId,column,row,axis,material,3);}
    boolean placePreviewBridge(String worldId,int column,int row,String axis,String material,int length) throws JSONException {
        if(length<3 || length>5 || !worldId.equals(activeId()) || !java.util.Arrays.asList("E","S").contains(axis) || !java.util.Arrays.asList("wood","stone").contains(material))return false;
        World world=activeWorld();if(world==null || world.previewData==null)return false;
        JSONObject root=new JSONObject(world.previewData.toJson()),data=root.getJSONObject("world");JSONArray bridges=data.optJSONArray("bridges");if(bridges==null)bridges=new JSONArray();
        JSONObject bridge=new JSONObject().put("column",column).put("row",row).put("axis",axis).put("material",material);if(length!=3)bridge.put("length",length);bridges.put(bridge);data.put("bridges",bridges);root.put("version",Math.max(world.previewData.version,length==3?15:16));
        PreviewWorld20 parsed;try{parsed=PreviewWorld20.parse(root.toString());}catch(JSONException invalid){return false;}
        java.util.List<World> worlds=list();for(int i=0;i<worlds.size();i++)if(worldId.equals(worlds.get(i).id)){worlds.set(i,fromPreview(parsed));return saveWorlds(worlds,worldId);}return false;
    }
    boolean placePreviewFence(String worldId,int column,int row,String edge,String material) throws JSONException {
        if(!worldId.equals(activeId()) || !java.util.Arrays.asList("N","E","S","W").contains(edge) || !java.util.Arrays.asList("wood","stone","stone-tall").contains(material))return false;
        List<World> worlds=list();
        for(int i=0;i<worlds.size();i++) {
            World w=worlds.get(i);if(!worldId.equals(w.id))continue;
            if(w.previewData==null || !w.tileExists(column,row) || w.previewData.tile(column,row)==3 || houseCovers(w.houses,column,row))return false;
            JSONObject root=new JSONObject(w.previewData.toJson()),data=root.getJSONObject("world");
            JSONArray fences=data.has("fences")?data.getJSONArray("fences"):new JSONArray();
            int x=column+(edge.equals("E")?1:edge.equals("W")?-1:0),y=row+(edge.equals("S")?1:edge.equals("N")?-1:0);
            for(SurfaceObject f:w.surfaces)if(f.type.equals("fence") && FenceEdges.blocks(f.column,f.row,f.edge,column,row,x,y))return false;
            fences.put(new JSONObject().put("column",column).put("row",row).put("edge",edge).put("material",material));
            data.put("fences",fences);root.put("version",Math.max(material.equals("stone-tall")?17:13,w.previewData.version));
            worlds.set(i,fromPreview(PreviewWorld20.parse(root.toString())));return saveWorlds(worlds,worldId);
        }
        return false;
    }
    /** Place one tuft in a supported flat portable world, committing only after full revalidation. */
    boolean placeWildGrass(String worldId,int column,int row) throws JSONException {return placePreviewPlant(worldId,column,row,"wild-grass");}
    boolean placePreviewPlant(String worldId,int column,int row,String kind) throws JSONException {return placePreviewPlant(worldId,column,row,kind,1,null);}
    boolean placePreviewPlant(String worldId,int column,int row,String kind,int size,String variant) throws JSONException {
        if(!"wild-grass".equals(kind) && !"bush".equals(kind))return false;
        if(size<1 || size>2 || ("wild-grass".equals(kind) && (size!=1 || variant!=null)) || (variant!=null && !java.util.Arrays.asList("flower","berry").contains(variant)))return false;
        if(!worldId.equals(activeId()))return false;
        List<World> worlds=list();
        for(int i=0;i<worlds.size();i++) {
            World w=worlds.get(i);if(!worldId.equals(w.id))continue;
            if(w.previewData==null || column<0 || row<0 || column+size>w.gridSize() || row+size>w.gridSize())return false;
            for(int y=row;y<row+size;y++)for(int x=column;x<column+size;x++) {
                if(!w.tileExists(x,y) || w.previewData.tile(x,y)==3 || (w.showCharacter && w.characterColumn==x && w.characterRow==y) || houseCovers(w.houses,x,y))return false;
                for(SurfaceObject object:w.surfaces) {
                    if(object.type.equals("fence"))continue;
                    int footprint=surfaceFootprint(object);
                    if(x>=object.column && y>=object.row && x<object.column+footprint && y<object.row+footprint)return false;
                }
            }
            JSONObject root=new JSONObject(w.previewData.toJson()), data=root.getJSONObject("world");
            JSONArray objects=data.has("orangeTrees")?data.getJSONArray("orangeTrees"):new JSONArray();
            JSONObject plant=new JSONObject().put("column",column).put("row",row).put("kind",kind).put("size",size);
            if(variant!=null)plant.put("variant",variant).put("plantId",java.util.UUID.randomUUID().toString()).put("plantedAt",System.currentTimeMillis());objects.put(plant);
            int required="bush".equals(kind)?(variant!=null?12:size==2?11:10):9;
            data.put("orangeTrees",objects);root.put("version",Math.max(w.previewData.version,required));
            worlds.set(i,fromPreview(PreviewWorld20.parse(root.toString())));
            return saveWorlds(worlds,worldId);
        }
        return false;
    }
    boolean travel(String sourceWorldId, String portalId) throws JSONException {
        if (!sourceWorldId.equals(activeId())) return false;
        List<World> worlds=list(); World source=null,destination=null; PortalTravel.Portal from=null,to=null;
        for(World w:worlds) if(w.id.equals(sourceWorldId)) source=w;
        if(source==null || !source.showCharacter) return false;
        for(PortalTravel.Portal p:source.portals) if(p.id.equals(portalId)) from=p;
        if(from==null) return false;
        for(World w:worlds) if(w.id.equals(from.destinationWorldId)) destination=w;
        if(destination==null) return false;
        for(PortalTravel.Portal p:destination.portals) if(p.id.equals(from.destinationPortalId)) to=p;
        final World dest=destination;
        PortalTravel.Terrain terrain=new PortalTravel.Terrain() {
            public boolean canStand(int c,int r){if(houseCovers(dest.houses,c,r))return false;for(SurfaceObject o:dest.surfaces) {if(o.type.equals("tree") && c>=o.column && c<o.column+((o.material.equals("banana") || o.material.equals("palm"))?1:2) && r>=o.row && r<o.row+((o.material.equals("banana") || o.material.equals("palm"))?1:2)) return false;if((o.type.equals("sunflower")||o.type.equals("mailbox")||o.type.equals("storage-box")||o.type.equals("selling-box")||(o.type.equals("road") && "water".equals(o.material))) && o.column==c && o.row==r)return false;}return true;}
            public boolean canStep(int a,int b,int c,int d){return true;}
        };
        if(!PortalTravel.canTravel(from,to,new PortalTravel.Tile(source.characterColumn,source.characterRow),terrain)) return false;
        for(int i=0;i<worlds.size();i++) if(worlds.get(i).id.equals(destination.id)) {
            worlds.set(i,new World(dest.id,dest.name,dest.description,dest.color,true,to.tile.column,to.tile.row,source.characterScale,source.characterFacing,dest.hasHouse,dest.showHouse,dest.houseColumn,dest.houseRow,dest.houseScale,dest.houseFacing,dest.houseId,dest.houseDesign,dest.houseOpacity,dest.formatVersion,dest.portals,dest.surfaces,dest.houses)); break;
        }
        return saveWorlds(worlds,dest.id);
    }
}
