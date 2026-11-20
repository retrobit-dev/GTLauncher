package dev.retrobit.gtlauncher;
/** Small, bounded projected shadow whose base remains at the fence endpoints. */
final class FenceShadow {
    private static float clamp(float v,float max){return Math.max(-max,Math.min(max,v));}
    static float[] offset(float ax,float ay,float bx,float by,float lightX,float lightY,float lift) {
        return offset(ax,ay,bx,by,lightX,lightY,lift,15);
    }
    static float[] offset(float ax,float ay,float bx,float by,float lightX,float lightY,float lift,float height) {
        float dx=clamp(((ax+bx)/2-lightX)*.08f,Math.abs(bx-ax)*.14f);
        float dy=clamp(((ay+by)/2-lightY-lift)*.08f,Math.max(.75f,Math.min(12,height)));
        if(Math.abs(dy)<.75f)dy=dy<0?-.75f:.75f;
        return new float[]{dx,dy};
    }
    private FenceShadow() {}
}
