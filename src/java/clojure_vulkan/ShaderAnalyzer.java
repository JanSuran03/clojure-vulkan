package clojure_vulkan;

import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;

import java.util.Vector;

public class ShaderAnalyzer {
    enum Mode {
        IN,
        OUT
    }

    public static class ShaderLayout {
        static Keyword
                locationKW = Keyword.intern("location"),
                typeKW = Keyword.intern("type"),
                nameKW = Keyword.intern("name"),
                modeKW = Keyword.intern("mode"),
                inKW = Keyword.intern("in"),
                outKW = Keyword.intern("out");
        private int location;
        private String type;
        private String name;

        private Mode mode;


        public PersistentHashMap asHashMap() {
            return PersistentHashMap.create(
                    modeKW, mode == Mode.IN ? inKW : outKW,
                    locationKW, location,
                    typeKW, Keyword.intern(type),
                    nameKW, name);
        }

        ShaderLayout() {
        }
    }

    static final int
            locationLength = "(location".length(),
            inLength = "in".length(),
            outLength = "out".length();

    private static String dropSpaces(String s) {
        String ret = s;
        while (ret.charAt(0) == ' ') {
            ret = ret.substring(1);
        }
        return ret;
    }

    public static Vector<ShaderLayout> analyze(String s) {
        String src = s;
        Vector<ShaderLayout> analyzedLocations = new Vector<>();
        int layoutIndexOf;
        int locationIndexOf;
        char ch;
        while ((layoutIndexOf = src.indexOf("layout")) != -1) {
            ShaderLayout currentLayout = new ShaderLayout();
            src = src.substring(layoutIndexOf);
            locationIndexOf = src.indexOf("(location");
            src = src.substring(locationIndexOf + locationLength);
            src = dropSpaces(src);
            src = src.substring(1);
            src = dropSpaces(src);
            StringBuilder sb = new StringBuilder();
            while (Character.isDigit(ch = src.charAt(0))) {
                sb.append(ch);
                src = src.substring(1);
            }
            int location = Integer.parseInt(sb.toString());
            src = src.substring(1);
            src = dropSpaces(src);

            Mode mode = src.startsWith("in") ? Mode.IN
                    : (src.startsWith("out") ? Mode.OUT
                    : null);
            if (mode != null) {
                currentLayout.mode = mode;
                currentLayout.location = location;
                src = src.substring(mode == Mode.IN ? inLength : outLength);
                src = dropSpaces(src);
                sb = new StringBuilder();
                while (src.charAt(0) != ' ') {
                    sb.append(src.charAt(0));
                    src = src.substring(1);
                }
                currentLayout.type = sb.toString();
                src = dropSpaces(src);
                sb = new StringBuilder();
                while (src.charAt(0) != ';') {
                    sb.append(src.charAt(0));
                    src = src.substring(1);
                }
                currentLayout.name = sb.toString();
                analyzedLocations.add(currentLayout);
            }
        }
        return analyzedLocations;
    }
}

