package clojure_vulkan;

public class Matrix4f {
    public static float[] identity() {
        return new float[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        };
    }

    public static float[] multiply(float[] leftM, float[] rightM) {
        float[] ret = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                float temp = 0;
                for (int i = 0; i < 4; i++) {
                    temp += leftM[row * 4 + i] * rightM[col + 4 * i];
                }
                ret[4 * row + col] = temp;
            }
        }
        return ret;
    }
}
