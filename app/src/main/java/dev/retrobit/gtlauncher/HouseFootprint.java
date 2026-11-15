package dev.retrobit.gtlauncher;

/** Grid footprint only. Format validation decides which designs are supported. */
final class HouseFootprint {
    final int columns, rows;
    private HouseFootprint(int columns,int rows){this.columns=columns;this.rows=rows;}
    static HouseFootprint of(String design) {
        if(design==null || design.equals("classic") || design.equals("3x3"))return new HouseFootprint(3,3);
        switch(design){
            case "wide": case "4x3":return new HouseFootprint(4,3);
            case "3x4":return new HouseFootprint(3,4);
            case "3x5":return new HouseFootprint(3,5);
            case "5x3":return new HouseFootprint(5,3);
            default:throw new IllegalArgumentException("Unknown house design");
        }
    }
    boolean contains(int column,int row,int c,int r){return c>=column && c<column+columns && r>=row && r<row+rows;}
    boolean overlaps(int column,int row,HouseFootprint other,int otherColumn,int otherRow){
        return column<otherColumn+other.columns && otherColumn<column+columns && row<otherRow+other.rows && otherRow<row+rows;
    }
    boolean fits(int column,int row){return column>=0 && row>=0 && column<=9-columns && row<=9-rows;}
}
