package clojure_vulkan;

import clojure.lang.*;

import java.util.*;

public class ShaderAnalyzer {
    enum Mode {
        IN,
        OUT,
        UNIFORM
    }

    public static class ShaderLayout {
        final static Keyword
                typeKW = Keyword.intern("type"),
                nameKW = Keyword.intern("name"),
                modeKW = Keyword.intern("mode"),
                inKW = Keyword.intern("in"),
                outKW = Keyword.intern("out"),
                uniformKW = Keyword.intern("uniform");
        private static final HashMap<Mode, Keyword> modeToKW = new HashMap<>(Map.of(
                Mode.IN, inKW,
                Mode.OUT, outKW,
                Mode.UNIFORM, uniformKW));
        private int location;
        private int binding;
        private String type;
        private String name;

        private Mode mode;


        public PersistentHashMap hashMap() {
            Keyword mode_kw = modeToKW.get(mode);
            PersistentHashMap ret = PersistentHashMap.create(
                    modeKW, mode_kw,
                    nameKW, name,
                    typeKW, Keyword.intern(type == null ? "unknown" : type));
            return (PersistentHashMap)
                    (mode_kw == uniformKW ?
                            ret.assoc(bindingKW, binding)
                            : ret.assoc(locationKW, location));
        }

        ShaderLayout() {
        }
    }

    final static Keyword
            locationKW = Keyword.intern("location"),
            bindingKW = Keyword.intern("binding");

    static final int
            locationLength = "(location".length(),
            bindingLength = "(binding".length();

    private static String dropSpaces(String s) {
        String ret = s;
        while (ret.charAt(0) == ' ') {
            ret = ret.substring(1);
        }
        return ret;
    }

    private static final HashSet<Character> terminatingWord = new HashSet<>(Set.of(' ', ',', ';'));

    private static String dropWord(String s) {
        for (int i = 0; i < s.length(); i++)
            if (terminatingWord.contains(s.charAt(i)))
                return s.substring(i);
        return "";
    }

    public static Vector<PersistentHashMap> analyze(String s) {
        String src = s;
        Vector<PersistentHashMap> analyzedLocations = new Vector<>();
        int locationIndexOf;
        int bindingIndexOf;
        char ch;
        while (src.contains("layout")) {
            ShaderLayout currentLayout = new ShaderLayout();
            locationIndexOf = src.indexOf("(location");
            if (locationIndexOf == -1)
                locationIndexOf = 10000;
            bindingIndexOf = src.indexOf("(binding");
            if (bindingIndexOf == -1)
                bindingIndexOf = 10000;
            Keyword winner = locationIndexOf < bindingIndexOf ? locationKW : bindingKW;
            src = src.substring(winner == locationKW ?
                    locationIndexOf + locationLength :
                    bindingIndexOf + bindingLength);
            src = dropSpaces(src);
            src = src.substring(1);
            src = dropSpaces(src);
            StringBuilder sb = new StringBuilder();
            while (Character.isDigit(ch = src.charAt(0))) {
                sb.append(ch);
                src = src.substring(1);
            }
            int locationOrBinding = Integer.parseInt(sb.toString());
            src = src.substring(1);
            src = dropSpaces(src);

            Mode mode = src.startsWith("in") ? Mode.IN
                    : (src.startsWith("out") ? Mode.OUT
                    : (src.startsWith("uniform") ? Mode.UNIFORM
                    : null));
            if (mode != null) {
                currentLayout.mode = mode;
                if (winner == locationKW) {
                    currentLayout.location = locationOrBinding;
                } else {
                    currentLayout.binding = locationOrBinding;
                }
                src = dropWord(src);
                src = dropSpaces(src);
                sb = new StringBuilder();
                while (src.charAt(0) != ' ') {
                    sb.append(src.charAt(0));
                    src = src.substring(1);
                }
                if (mode == Mode.IN || mode == Mode.OUT) {
                    currentLayout.type = sb.toString();
                    src = dropSpaces(src);
                    sb = new StringBuilder();
                    while (src.charAt(0) != ';') {
                        sb.append(src.charAt(0));
                        src = src.substring(1);
                    }
                    currentLayout.name = sb.toString();
                } else {
                    currentLayout.name = sb.toString();
                    src = dropSpaces(src);
                    sb = new StringBuilder();
                    while (!terminatingWord.contains(src.charAt(0))) {
                        sb.append(src.charAt(0));
                        src = src.substring(1);
                    }
                }
                analyzedLocations.add(currentLayout.hashMap());
            }
        }
        return analyzedLocations;
    }
}
