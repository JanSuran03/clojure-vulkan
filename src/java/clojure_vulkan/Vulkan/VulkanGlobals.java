package clojure_vulkan.Vulkan;

import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDevice;

public class VulkanGlobals {
    public static LogicalDevice LOGICAL_DEVICE = new LogicalDevice();
    public static VkDevice getLogicalDevice(){
        return LOGICAL_DEVICE.get();
    }

    public static class LogicalDevice implements VkResource<VkDevice> {

        private VkDevice device;

        @Override
        public VkDevice get() {
            return device;
        }

        @Override
        public void set(VkDevice device) {
            this.device = device;
        }

        @Override
        public void free() {
            VK13.vkDestroyDevice(device, null);
        }
    }

    private interface VkResource<T> {
        public T get();

        public void set(T val);

        public void free();
    }
}
