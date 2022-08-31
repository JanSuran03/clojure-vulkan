package clojure_vulkan.Vulkan;

import clojure.lang.IPersistentMap;
import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import org.lwjgl.vulkan.*;

import java.util.Vector;

public class VulkanGlobals {
    public static boolean VALIDATION_LAYERS_ENABLED = false;
    public static LogicalDevice LOGICAL_DEVICE = new LogicalDevice();
    public static PhysicalDevice PHYSICAL_DEVICE = new PhysicalDevice();
    public static QueueFamilies QUEUE_FAMILIES = new QueueFamilies();
    public static SwapChainExtent SWAP_CHAIN_EXTENT = new SwapChainExtent();
    public static UniformBuffers UNIFORM_BUFFERS = new UniformBuffers();

    public static CommandBuffers COMMAND_BUFFERS = new CommandBuffers();
    public static Queue GRAPHICS_QUEUE = new Queue();
    public static Queue PRESENT_QUEUE = new Queue();
    public static VulkanInstance VULKAN_INSTANCE = new VulkanInstance();

    public static VkPointer DEBUG_MESSENGER_POINTER = new VkPointer() {
        @Override
        public void free() {
            if (VK13.vkGetInstanceProcAddr(VULKAN_INSTANCE.get(), "vkDestroyDebugUtilsMessengerEXT") != VK13.VK_NULL_HANDLE) {
                EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT(VULKAN_INSTANCE.get(), this.get(), null);
                this.set(0L);
            }
        }
    };
    public static VkPointer WINDOW_SURFACE_POINTER = new VkPointer() {
        @Override
        public void free() {
            KHRSurface.vkDestroySurfaceKHR(VULKAN_INSTANCE.get(), this.get(), null);
            this.set(0L);
        }
    };

    public static VkPointerVector SWAP_CHAIN_IMAGE_POINTERS = new VkPointerVector() {
    };

    public static VkPointer SWAP_CHAIN_POINTER = new VkPointer() {
        @Override
        public void free() {
            KHRSwapchain.vkDestroySwapchainKHR(LOGICAL_DEVICE.get(), this.get(), null);
            this.set(0L);
        }
    };

    public static VkDevice getLogicalDevice() {
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

    public static class SwapChainExtent implements VkResource<VkExtent2D> {
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

    public static void enableValidationLayers() {
        VALIDATION_LAYERS_ENABLED = true;
    }

    public static void disableValidationLayers() {
        VALIDATION_LAYERS_ENABLED = false;
    }

    public static class UniformBuffers implements VkResource<Vector<Buffer>> {
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

    public static class CommandBuffers implements VkResource<Vector<VkCommandBuffer>> {
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

    private interface VkResource<T> {
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
