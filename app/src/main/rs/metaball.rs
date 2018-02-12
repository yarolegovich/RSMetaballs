#pragma version(1)
#pragma rs java_package_name(com.yarolegovich.rsmetaball)

typedef struct BallData {
  float x, y;
  float radiusSquared;
} BallData_t;

BallData_t *ballDataArray;
int ballCount;
float3 color;

static float calculateWeight(uint32_t x, uint32_t y) {
  float weight = 0;
  uint32_t coordIndex = 0;
  for (uint32_t ballIndex = 0; ballIndex < ballCount; ballIndex++) {
    const BallData_t ballData = ballDataArray[ballIndex];
    const float dx = ballData.x - x;
    const float dy = ballData.y - y;
    const float dist = dx * dx + dy * dy;
    weight += ballData.radiusSquared / dist;
  }
  return weight;
}

uchar4 RS_KERNEL drawMetaball(uchar4 inIgnored, uint32_t x, uint32_t y) {
  float weight = calculateWeight(x, y);
  const float colorModifier = floor(clamp(weight, 0.0f, 1.0f));
  return rsPackColorTo8888(color * colorModifier);
}

uchar4 RS_KERNEL drawGlowingMetaball(uchar4 inIgnored, uint32_t x, uint32_t y) {
  float weight = calculateWeight(x, y);
  return rsPackColorTo8888(color * weight);
}
