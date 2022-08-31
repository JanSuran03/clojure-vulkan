package clojure_vulkan.Vulkan;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.*;

import static clojure_vulkan.Vulkan.VulkanGlobalsIntefaces.*;
import static clojure_vulkan.Vulkan.VulkanGlobalsImpl.*;

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
                super.free();
            }
        }
    };
    public static VkPointer WINDOW_SURFACE_POINTER = new VkPointer() {
        @Override
        public void free() {
            KHRSurface.vkDestroySurfaceKHR(VULKAN_INSTANCE.get(), this.get(), null);
            super.free();
        }
    };

    public static VkPointerVector SWAP_CHAIN_IMAGE_POINTERS = new VkPointerVector() {
    };

    public static VkPointer SWAP_CHAIN_POINTER = new VkPointer() {
        @Override
        public void free() {
            KHRSwapchain.vkDestroySwapchainKHR(LOGICAL_DEVICE.get(), this.get(), null);
            super.free();
        }
    };

    public static VkPointer WINDOW_POINTER = new VkPointer() {
        @Override
        public void free() {
            GLFW.glfwDestroyWindow(this.get());
            super.free();
        }
    };

    public static VkPointerVector SWAP_CHAIN_IMAGE_VIEWS_POINTERS = new VkPointerVector() {
        @Override
        public void free() {
            this.get().forEach(imageViewsPointer -> VK13.vkDestroyImageView(LOGICAL_DEVICE.get(), imageViewsPointer.get(), null));
            super.free();
        }
    };

    public static VkPointer PIPELINE_LAYOUT_POINTER = new VkPointer() {
        @Override
        public void free() {
            VK13.vkDestroyPipelineLayout(LOGICAL_DEVICE.get(), this.get(), null);
            super.free();
        }
    };

    public static VkPointerVector SWAP_CHAIN_FRAME_BUFFER_POINTERS = new VkPointerVector() {
        @Override
        public void free() {
            this.get().forEach(frameBufferPointer -> VK13.vkDestroyFramebuffer(LOGICAL_DEVICE.get(), frameBufferPointer.get(), null));
            super.free();
        }
    };

    public static VkPointer RENDER_PASS_POINTER = new VkPointer() {
        @Override
        public void free() {
            VK13.vkDestroyRenderPass(LOGICAL_DEVICE.get(), this.get(), null);
            super.free();
        }
    };

    public static VkIntegerPointer SWAP_CHAIN_IMAGE_FORMAT = new VkIntegerPointer() {
        @Override
        public void free() {
            super.free();
        }
    };
    public static VkPointer GRAPHICS_PIPELINE_POINTER = new VkPointer() {
    };

    public static VkPointer COMMAND_POOL = new VkPointer() {
        @Override
        public void free() {
            VK13.vkDestroyCommandPool(LOGICAL_DEVICE.get(), this.get(), null);
            super.free();
            COMMAND_BUFFERS.free();
        }
    };

    public static VkDevice getLogicalDevice() {
        return LOGICAL_DEVICE.get();
    }

    public static void enableValidationLayers() {
        VALIDATION_LAYERS_ENABLED = true;
    }

    public static void disableValidationLayers() {
        VALIDATION_LAYERS_ENABLED = false;
    }


}
