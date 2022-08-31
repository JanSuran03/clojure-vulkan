package clojure_vulkan.Vulkan;

import java.util.Vector;

public class VulkanGlobalsIntefaces {
    public interface VkResource<T> {
        public T get();

        public void set(T val);

        public void free();
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
