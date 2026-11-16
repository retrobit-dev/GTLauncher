package dev.retrobit.gtlauncher;

/** Coordinate rules shared by facing and the front tool target. */
final class GameplayTarget {
    private GameplayTarget() {}
    static PortalTravel.Tile front(int column,int row,String facing) {
        switch(facing) {
            case "N": return new PortalTravel.Tile(column,row-1);
            case "E": return new PortalTravel.Tile(column+1,row);
            case "S": return new PortalTravel.Tile(column,row+1);
            case "W": return new PortalTravel.Tile(column-1,row);
            default: return null;
        }
    }
    static String direction(PortalTravel.Tile from,PortalTravel.Tile to) {
        if(to.column>from.column)return "E";
        if(to.column<from.column)return "W";
        if(to.row>from.row)return "S";
        if(to.row<from.row)return "N";
        return null;
    }
}
