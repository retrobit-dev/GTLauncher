package dev.retrobit.gtlauncher;

/** Geometry foundation only; no bridge persistence or gameplay integration yet. */
final class BridgeGeometry {
    static final int LENGTH=3;
    static final class Bridge {
        final int column,row;final String axis,material;
        Bridge(int column,int row,String axis,String material){this.column=column;this.row=row;this.axis=axis;this.material=material;}
    }
    static int indexAt(Bridge b,int c,int r) {
        if(b==null || !("E".equals(b.axis)||"S".equals(b.axis)) || !("wood".equals(b.material)||"stone".equals(b.material)))return -1;
        if("E".equals(b.axis) && r==b.row && c>=b.column && c<b.column+LENGTH)return c-b.column;
        if("S".equals(b.axis) && c==b.column && r>=b.row && r<b.row+LENGTH)return r-b.row;
        return -1;
    }
    static boolean permitsStep(java.util.List<Bridge> bridges,int fromC,int fromR,int toC,int toR) {
        if(Math.abs(fromC-toC)+Math.abs(fromR-toR)!=1)return false;
        for(Bridge b:bridges){int a=indexAt(b,fromC,fromR),z=indexAt(b,toC,toR);if(a<0 && z<0)continue;
            if(a>=0 && z>=0){if(Math.abs(a-z)!=1)return false;continue;}
            int i=a>=0?a:z,otherC=a>=0?toC:fromC,otherR=a>=0?toR:fromR;
            if("E".equals(b.axis)) {if(otherR!=b.row || !((i==0 && otherC==b.column-1)||(i==LENGTH-1 && otherC==b.column+LENGTH)))return false;}
            else if(otherC!=b.column || !((i==0 && otherR==b.row-1)||(i==LENGTH-1 && otherR==b.row+LENGTH)))return false;
        }
        return true;
    }
    private BridgeGeometry() {}
}
