package dev.retrobit.gtlauncher;

/** Screen-space island scale and its inverse, for consistent drawing and hit tests. */
final class IslandProjection {
    static final float BASE_ZOOM = 4f / 7f;
    // 1× now corresponds to the former 5× visual scale; world coordinates stay unchanged.
    static float modeZoom(boolean gameplay) { return 1f; }
    static float renderZoom(boolean gameplay) { return modeZoom(gameplay) * 5f; }
    static float scale(float viewSize,float sceneSize) { return BASE_ZOOM * sceneSize * 2f / viewSize; }
    static float toScene(float screenValue,float viewSize,float sceneSize,float anchor) {
        return anchor+(screenValue*sceneSize/viewSize-anchor)/scale(viewSize,sceneSize);
    }
    static float inverse(float screenValue,float viewSize,float sceneSize,float source,float destination,float zoom) {
        return source + (screenValue*sceneSize/viewSize-destination)/(scale(viewSize,sceneSize)*zoom);
    }
    private IslandProjection() {}
}
