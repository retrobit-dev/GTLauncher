package dev.retrobit.gtlauncher;

/** Supported bridge footprints and end-only traversal. */
final class BridgeGeometry {
    static final int LENGTH=3;
    static final class Bridge {
        final int column,row,length,width,opacity;final String axis,material;
        Bridge(int column,int row,String axis,String material){this(column,row,axis,material,3);}
        Bridge(int column,int row,String axis,String material,int length){this(column,row,axis,material,length,-1);}
        Bridge(int column,int row,String axis,String material,int length,int opacity){this(column,row,axis,material,length,1,opacity);}
        Bridge(int column,int row,String axis,String material,int length,int width,int opacity){this.column=column;this.row=row;this.axis=axis;this.material=material;this.length=length;this.width=width;this.opacity=opacity;}
    }
    static int indexAt(Bridge b,int c,int r) {
        if(b==null || b.length<3 || b.length>5 || !(b.width==1 || b.width==(b.length==5?3:2)) || !("E".equals(b.axis)||"S".equals(b.axis)) || !("wood".equals(b.material)||"stone".equals(b.material)))return -1;
        if("E".equals(b.axis) && r>=b.row && r<b.row+b.width && c>=b.column && c<b.column+b.length)return c-b.column;
        if("S".equals(b.axis) && c>=b.column && c<b.column+b.width && r>=b.row && r<b.row+b.length)return r-b.row;
        return -1;
    }
    interface Terrain {
        int size();
        int tile(int column,int row);
        int height(int column,int row);
        boolean obstructed(int column,int row);
    }
    static boolean valid(java.util.List<Bridge> bridges,Terrain terrain) {
        if(bridges==null || terrain==null || terrain.size()<1 || terrain.size()>100)return false;
        java.util.Set<Integer> seen=new java.util.HashSet<>();
        for(Bridge b:bridges){
            if(b==null || indexAt(b,b.column,b.row)!=0)return false;
            int level=-1;
            for(int i=0;i<b.length;i++)for(int j=0;j<b.width;j++){
                int c=b.column+("E".equals(b.axis)?i:j),r=b.row+("S".equals(b.axis)?i:j);
                if(c<0 || r<0 || c>=terrain.size() || r>=terrain.size())return false;
                int tile=terrain.tile(c,r),height=terrain.height(c,r);
                if(tile==0 || height<=0 || ((i==0 || i==b.length-1) && tile==3) || (level>=0 && level!=height) || terrain.obstructed(c,r) || !seen.add(r*terrain.size()+c))return false;
                level=height;
            }
        }
        return true;
    }
    static boolean permitsStep(java.util.List<Bridge> bridges,int fromC,int fromR,int toC,int toR) {
        if(Math.abs(fromC-toC)+Math.abs(fromR-toR)!=1)return false;
        for(Bridge b:bridges){int a=indexAt(b,fromC,fromR),z=indexAt(b,toC,toR);if(a<0 && z<0)continue;
            if(a>=0 && z>=0)continue;
            int i=a>=0?a:z,otherC=a>=0?toC:fromC,otherR=a>=0?toR:fromR;
            int insideC=a>=0?fromC:toC,insideR=a>=0?fromR:toR;
            if("E".equals(b.axis)) {if(otherR!=insideR || !((i==0 && otherC==b.column-1)||(i==b.length-1 && otherC==b.column+b.length)))return false;}
            else if(otherC!=insideC || !((i==0 && otherR==b.row-1)||(i==b.length-1 && otherR==b.row+b.length)))return false;
        }
        return true;
    }
    private BridgeGeometry() {}
}
