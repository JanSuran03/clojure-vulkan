package clojure_vulkan.Vulkan;

import org.lwjgl.system.NativeResource;

import java.util.Vector;

public class VulkanGlobalsInterfaces {
    public interface VkResource<T> extends NativeResource {
        T get();

        void set(T val);
    }

    public interface VkStructVector<T> extends VkResource<Vector<T>> {
        default T get(int index) {
            return this.get().elementAt(index);
        }
    }

    public static abstract class VkPointer implements VkResource<Long> {
        private Long pointer = 0L;

        public Long get() {
            return pointer;
        }

        public void set(Long pointer) {
            this.pointer = pointer;
        }

        public void free() {
            pointer = 0L;
        }
    }

    public static abstract class VkIntegerPointer implements VkResource<Integer> {
        private Integer pointer = 0;

        @Override
        public Integer get() {
            return pointer;
        }

        @Override
        public void set(Integer pointer) {
            this.pointer = pointer;
        }

        @Override
        public void free() {
            pointer = 0;
        }
    }

    public static abstract class VkPointerVector implements VkResource<Vector<VkPointer>> {
        public static Vector<VkPointer> asVkPointerVector(Vector<Long> pointerVector) {
            Vector<VkPointer> ret = new Vector<>();
            for (Long pointer : pointerVector) {
                VkPointer ptr = new VkPointer() {
                };
                ptr.set(pointer);
                ret.add(ptr);
            }
            return ret;
        }

        private Vector<VkPointer> pointerVector;

        @Override
        public Vector<VkPointer> get() {
            return pointerVector;
        }

        public Long get(int index) {
            return this.get().elementAt(index).get();
        }

        @Override
        public void set(Vector<VkPointer> pointerVector) {
            this.pointerVector = pointerVector;
        }

        @Override
        public void free() {
            pointerVector.forEach(VkPointer::free);
        }
    }
}
