package dev.retrobit.gtlauncher;
/** Bounded ground projection of house artwork, pointing away from the active light. */
final class HouseShadow {
    static float[] projection(float width,float height,double azimuth,double altitude) {
        float[] ray=CharacterShadow.offset(azimuth,altitude,height);
        if(Math.abs(ray[1])<height*.12f)ray[1]=Math.copySign(height*.12f,ray[1]);
        return new float[]{ray[0],ray[1],CharacterShadow.fitAroundAnchor(width,height,ray[0],ray[1])};
    }
    private HouseShadow(){}
}
