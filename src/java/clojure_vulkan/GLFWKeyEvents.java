package clojure_vulkan;

import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class GLFWKeyEvents {
    public static void processKeyEvent(int keyCode, int scancode, int action, int modifiers) {
        if (keyCode < 0 || keyCode > Keycodes.PRESSED_ARRAY.length)
            Keycodes.throwOutOfBounds(keyCode);
        switch (action) {
            case GLFW.GLFW_PRESS:
                Keycodes.setPressed(keyCode);
                break;
            case GLFW.GLFW_RELEASE:
                Keycodes.setReleased(keyCode);
                break;
            default:
                break;
        }
        Modifiers.setCtrlPressed(Modifiers.isCtrlPressed(modifiers));
        Modifiers.setAltPressed(Modifiers.isAltPressed(modifiers));
        Modifiers.setShiftPressed(Modifiers.isShiftPressed(modifiers));
    }

    public static final class Keycodes {
        private static void throwOutOfBounds(int keyCode) {
            throw new IndexOutOfBoundsException("Index for " + keyCode + "GLFW keycode is out of bounds.");
        }

        public static boolean[] PRESSED_ARRAY;

        public static boolean isPressed(int keyCode) {
            return PRESSED_ARRAY[keyCode];
        }

        public static void setPressed(int keyCode) {
            PRESSED_ARRAY[keyCode] = true;
        }

        public static void setReleased(int keyCode) {
            PRESSED_ARRAY[keyCode] = false;
        }

        static {
            Field[] fields = GLFW.class.getFields();
            int max = 0;
            for (Field field : fields)
                if (Modifier.isStatic(field.getModifiers()) && field.getName().startsWith("GLFW_KEY")) {
                    try {
                        max = Math.max(max, (int) field.get(null));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            PRESSED_ARRAY = (boolean[]) Array.newInstance(boolean.class, max);
        }
    }

    public static final class Modifiers {
        private static boolean CTRL_PRESSED = false;
        private static boolean ALT_PRESSED = false;
        private static boolean SHIFT_PRESSED = false;

        public static void setCtrlPressed(boolean isPressed) {
            CTRL_PRESSED = isPressed;
        }

        public static void setAltPressed(boolean isPressed) {
            ALT_PRESSED = isPressed;
        }

        public static void setShiftPressed(boolean isPressed) {
            SHIFT_PRESSED = isPressed;
        }

        public static boolean isCtrlPressed(int modifiers) {
            return (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        }

        public static boolean isAltPressed(int modifiers) {
            return (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        }

        public static boolean isShiftPressed(int modifiers) {
            return (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        }
    }
}