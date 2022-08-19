package clojure_vulkan;

import java.nio.ByteBuffer;

public class MemoryUtils {
    public static void memcpy(ByteBuffer buf, float[] data) {
        for (float f : data)
            buf.putFloat(f);
    }
}
