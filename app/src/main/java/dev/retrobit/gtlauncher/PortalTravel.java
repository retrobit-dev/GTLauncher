package dev.retrobit.gtlauncher;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure portal routing rules. This class does not alter or import any worlds. */
final class PortalTravel {
    static final int SIZE = 9;
    static final class Tile {
        final int column, row;
        Tile(int column, int row) { this.column = column; this.row = row; }
        @Override public boolean equals(Object value) {
            return value instanceof Tile && ((Tile) value).column == column && ((Tile) value).row == row;
        }
        @Override public int hashCode() { return 31 * row + column; }
    }
    static final class Portal {
        final String worldId, id, edge, destinationWorldId, destinationPortalId;
        final Tile tile;
        final int opacity;
        Portal(String worldId, String id, String edge, Tile tile, String destinationWorldId, String destinationPortalId) {
            this(worldId,id,edge,tile,destinationWorldId,destinationPortalId,-1);
        }
        Portal(String worldId, String id, String edge, Tile tile, String destinationWorldId, String destinationPortalId, int opacity) {
            this.worldId=worldId; this.id=id; this.edge=edge; this.tile=tile;
            this.destinationWorldId=destinationWorldId; this.destinationPortalId=destinationPortalId; this.opacity=opacity;
        }
    }
    interface Terrain {
        boolean canStand(int column, int row);
        boolean canStep(int fromColumn, int fromRow, int toColumn, int toRow);
    }
    private static boolean validTile(Tile tile) { return validTile(tile, SIZE); }
    private static boolean validTile(Tile tile, int size) {
        return size > 0 && size <= 100 && tile != null && tile.column >= 0 && tile.column < size && tile.row >= 0 && tile.row < size;
    }
    private static boolean isPortalOnEdge(Portal p) {
        if (p == null || p.worldId == null || p.id == null || !validTile(p.tile) || p.edge == null) return false;
        switch (p.edge) {
            case "N": return p.tile.row == 0;
            case "S": return p.tile.row == SIZE - 1;
            case "W": return p.tile.column == 0;
            case "E": return p.tile.column == SIZE - 1;
            default: return false;
        }
    }
    private static String opposite(String edge) {
        switch (edge) { case "N": return "S"; case "S": return "N"; case "E": return "W"; case "W": return "E"; default: return ""; }
    }
    static boolean arePaired(Portal a, Portal b) {
        return isPortalOnEdge(a) && isPortalOnEdge(b) && !a.worldId.equals(b.worldId)
                && opposite(a.edge).equals(b.edge)
                && b.worldId.equals(a.destinationWorldId) && b.id.equals(a.destinationPortalId)
                && a.worldId.equals(b.destinationWorldId) && a.id.equals(b.destinationPortalId);
    }
    /** Return a path including both endpoints, or null. Never choose a nearby substitute tile. */
    static List<Tile> pathToPortal(Tile start, Portal source, Terrain terrain) {
        if (!validTile(start) || !isPortalOnEdge(source) || terrain == null
                || !terrain.canStand(start.column, start.row) || !terrain.canStand(source.tile.column, source.tile.row)) return null;
        return path(start,source.tile,terrain);
    }
    /** Path to an exact tile, respecting every obstacle and boundary. */
    static List<Tile> path(Tile start,Tile destination,Terrain terrain) {
        return path(start, destination, terrain, SIZE);
    }
    /** Route within an explicit square grid; existing 9x9 callers retain their bounds. */
    static List<Tile> path(Tile start, Tile destination, Terrain terrain, int size) {
        if(!validTile(start, size) || !validTile(destination, size) || terrain==null || !terrain.canStand(start.column,start.row) || !terrain.canStand(destination.column,destination.row))return null;
        Tile[][] previous = new Tile[size][size]; boolean[][] visited = new boolean[size][size];
        ArrayDeque<Tile> queue = new ArrayDeque<>(); queue.add(start); visited[start.row][start.column] = true;
        final int[][] steps = {{0,-1},{1,0},{0,1},{-1,0}};
        while (!queue.isEmpty()) {
            Tile current = queue.remove();
            if (current.equals(destination)) {
                ArrayList<Tile> route = new ArrayList<>();
                for (Tile p=current; p!=null; p=previous[p.row][p.column]) route.add(p);
                Collections.reverse(route); return route;
            }
            for (int[] step : steps) {
                int c=current.column+step[0], r=current.row+step[1];
                if (c<0 || c>=size || r<0 || r>=size || visited[r][c] || !terrain.canStand(c,r)
                        || !terrain.canStep(current.column,current.row,c,r)) continue;
                Tile next=new Tile(c,r); previous[r][c]=current; visited[r][c]=true; queue.add(next);
            }
        }
        return null;
    }
    /** Shortest reachable tile sharing an edge with a rectangular object footprint. */
    static List<Tile> pathBeside(Tile start,int column,int row,int width,int height,Terrain terrain) {
        return pathBeside(start,column,row,width,height,terrain,SIZE);
    }
    static List<Tile> pathBeside(Tile start,int column,int row,int width,int height,Terrain terrain,int size) {
        if(!validTile(start,size) || terrain==null || width<1 || height<1 || column<0 || row<0 || column+width>size || row+height>size)return null;
        List<Tile> best=null;
        for(int r=Math.max(0,row-1);r<=Math.min(size-1,row+height);r++)for(int c=Math.max(0,column-1);c<=Math.min(size-1,column+width);c++) {
            int dx=Math.max(Math.max(column-c,0),c-(column+width-1)),dy=Math.max(Math.max(row-r,0),r-(row+height-1));
            if(dx+dy!=1)continue;
            List<Tile> candidate=path(start,new Tile(c,r),terrain,size);
            if(candidate!=null && (best==null || candidate.size()<best.size()))best=candidate;
        }
        return best;
    }
    /** Confirmation is required by the caller; this checks conditions again at confirmation time. */
    static boolean canTravel(Portal source, Portal target, Tile character, Terrain destination) {
        return arePaired(source,target) && source.tile.equals(character) && destination != null
                && destination.canStand(target.tile.column,target.tile.row);
    }
    private PortalTravel() {}
}
