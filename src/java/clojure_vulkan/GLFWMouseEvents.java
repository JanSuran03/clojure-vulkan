package clojure_vulkan;

import org.lwjgl.glfw.GLFW;

public class GLFWMouseEvents {
    public static boolean isLeftButtonPressed(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    public static boolean isMiddleButtonPressed(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
    }

    public static boolean isRightButtonPressed(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_RIGHT;
    }
}