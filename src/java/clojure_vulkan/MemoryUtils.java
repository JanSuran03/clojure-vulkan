package clojure_vulkan;

import org.joml.Matrix4f;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MemoryUtils {
    public static void memcpyFloats(ByteBuffer buf, float[] data) {
        for (float f : data)
            buf.putFloat(f);
    }

    public static void memcpyShorts(ByteBuffer buf, short[] data) {
        for (short s : data)
            buf.putShort(s);
    }

    public static void memcpyIntegers(ByteBuffer buf, int[] data) {
        for (int i : data)
            buf.putInt(i);
    }

    public static void memcpyBytes(ByteBuffer buf, byte[] data) {
        for (byte b : data)
            buf.put(b);
    }

    public static void memcpyLongs(ByteBuffer buf, long[] data) {
        for (long l : data)
            buf.putLong(l);
    }

    public static void memcpyUBO(ByteBuffer buf, UniformBufferObject ubo) {
        ubo.copyInfoByteBuffer(buf);
    }

    public static void memCpyByteBuffer(ByteBuffer dest, ByteBuffer source, long size) {
        source.limit((int) size);
        dest.put(source);
        source.limit(source.capacity()).rewind();
    }

    private static final HashMap<Class<?>, Integer> SIZEOF_CACHE = new HashMap<>(Map.of(
            Matrix4f.class, 16 * Float.BYTES
    ));

    public static int sizeof(Object o) {
        if (o == null)
            throw new NullPointerException("Cannot get the size of null.");
        Integer ret = SIZEOF_CACHE.get(o.getClass());
        if (ret == null)
            throw new RuntimeException("Cannot determine the size of " + o + " with class: " + o.getClass());
        return ret;
    }

    public static int alignAs(int offset, int alignment) {
        return offset % alignment == 0 ? offset : ((offset - 1) | (alignment - 1)) + 1;
    }
}
