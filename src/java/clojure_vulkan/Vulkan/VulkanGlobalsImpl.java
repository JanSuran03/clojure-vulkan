package clojure_vulkan.Vulkan;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import org.lwjgl.vulkan.*;

import static clojure_vulkan.Vulkan.VulkanGlobalsInterfaces.VkResource;
import static clojure_vulkan.Vulkan.VulkanGlobalsInterfaces.VkStructVector;

import java.util.Vector;

import static clojure_vulkan.Vulkan.VulkanGlobals.VALIDATION_LAYERS_ENABLED;

public class VulkanGlobalsImpl {
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

    public static class PhysicalDevice implements VkResource<VkPhysicalDevice> {
        private VkPhysicalDevice device;

        @Override
        public VkPhysicalDevice get() {
            return device;
        }

        @Override
        public void set(VkPhysicalDevice device) {
            this.device = device;
        }

        @Override
        public void free() {
            if (VALIDATION_LAYERS_ENABLED)
                System.out.println("No need to destroy physical device, destroyed together with logical device.");
        }
    }

    public static class QueueFamilies implements VkResource<IPersistentMap> {
        private IPersistentMap queueFamilies = PersistentHashMap.EMPTY;
        private static final Keyword
                GRAPHICS_FAMILY_KEYWORD = Keyword.intern("graphics-family"),
                PRESENT_FAMILY_KEYWORD = Keyword.intern("present-family");

        @Override
        public IPersistentMap get() {
            return queueFamilies;
        }

        @Override
        public void set(IPersistentMap val) {
            queueFamilies = val;
        }

        @Override
        public void free() {
            queueFamilies = PersistentHashMap.EMPTY;
        }

        public int graphicsFamily() {
            return (int) queueFamilies.valAt(GRAPHICS_FAMILY_KEYWORD);
        }

        public QueueFamilies graphicsFamily(int graphicsFamily) {
            this.queueFamilies = queueFamilies.assoc(GRAPHICS_FAMILY_KEYWORD, graphicsFamily);
            return this;
        }

        public int presentFamily() {
            return (int) queueFamilies.valAt(PRESENT_FAMILY_KEYWORD);
        }

        public QueueFamilies presentFamily(int presentFamily) {
            this.queueFamilies = queueFamilies.assoc(PRESENT_FAMILY_KEYWORD, presentFamily);
            return this;
        }
    }

    public static class SwapChainExtent implements VulkanGlobalsInterfaces.VkResource<VkExtent2D> {
        private VkExtent2D extent;

        @Override
        public VkExtent2D get() {
            return extent;
        }

        @Override
        public void set(VkExtent2D extent) {
            this.extent = extent;
        }

        @Override
        public void free() {
            extent = null;
        }
    }

    public static class UniformBuffers implements VkStructVector<Buffer> {
        private Vector<Buffer> uniformBuffers;

        @Override
        public Vector<Buffer> get() {
            return uniformBuffers;
        }

        @Override
        public void set(Vector<Buffer> uniformBuffers) {
            this.uniformBuffers = uniformBuffers;
        }

        @Override
        public void free() {
            for (Buffer buffer : uniformBuffers)
                buffer.free();
            uniformBuffers = null;
        }
    }

    public static class CommandBuffers implements VkStructVector<VkCommandBuffer> {
        private Vector<VkCommandBuffer> commandBuffers;

        @Override
        public Vector<VkCommandBuffer> get() {
            return commandBuffers;
        }

        @Override
        public void set(Vector<VkCommandBuffer> commandBuffers) {
            this.commandBuffers = commandBuffers;
        }

        @Override
        public void free() {
            commandBuffers = null;
        }
    }

    public static class Queue implements VkResource<VkQueue> {
        private VkQueue queue;

        @Override
        public VkQueue get() {
            return queue;
        }

        @Override
        public void set(VkQueue queue) {
            this.queue = queue;
        }

        @Override
        public void free() {
            queue = null;
        }
    }

    public static class VulkanInstance implements VkResource<VkInstance> {
        private VkInstance instance;

        @Override
        public VkInstance get() {
            return instance;
        }

        @Override
        public void set(VkInstance instance) {
            this.instance = instance;
        }

        @Override
        public void free() {
            VK13.vkDestroyInstance(instance, null);
            instance = null;
        }
    }

}
