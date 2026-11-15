package dev.retrobit.gtlauncher;

/** Pixel-tested visual overlap and a per-frame stack in actual paint order. */
final class FenceOcclusion {
    interface PixelHit { boolean hit(float x,float y); }
    static final class Bounds {
        final float left,top,right,bottom;
        Bounds(float left,float top,float right,float bottom){this.left=left;this.top=top;this.right=right;this.bottom=bottom;}
    }
    static boolean overlaps(Bounds fence,PixelHit fenceHit,Bounds target,PixelHit targetHit){
        float left=Math.max(fence.left,target.left),right=Math.min(fence.right,target.right),top=Math.max(fence.top,target.top),bottom=Math.min(fence.bottom,target.bottom);
        if(left>=right || top>=bottom)return false;
        for(float y=top+.5f;y<bottom;y+=1f)for(float x=left+.5f;x<right;x+=1f)if(fenceHit.hit(x,y) && targetHit.hit(x,y))return true;
        return false;
    }
    static final class Stack {
        private static final class Layer {
            final Object owner; final Bounds bounds; final PixelHit hit;
            Layer(Object owner,Bounds bounds,PixelHit hit){this.owner=owner;this.bounds=bounds;this.hit=hit;}
        }
        private final java.util.List<Layer> layers=new java.util.ArrayList<>();
        void clear(){layers.clear();}
        boolean register(Object owner,Bounds bounds,PixelHit hit,boolean visible){
            boolean fades=false;
            for(Layer earlier:layers) if(earlier.owner!=owner && overlaps(bounds,hit,earlier.bounds,earlier.hit)){fades=true;break;}
            if(visible)layers.add(new Layer(owner,bounds,hit));
            return fades;
        }
    }
    private FenceOcclusion(){}
}
