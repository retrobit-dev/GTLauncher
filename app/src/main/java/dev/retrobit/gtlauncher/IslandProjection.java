package dev.retrobit.gtlauncher;

/** Screen-space island scale and its inverse, for consistent drawing and hit tests. */
final class IslandProjection {
    static final float BASE_ZOOM = 4f / 7f;
    static float toScene(float screenValue,float viewSize,float sceneSize,float anchor) {
        return anchor+(screenValue*sceneSize/viewSize-anchor)/BASE_ZOOM;
    }
    private IslandProjection() {}
}
