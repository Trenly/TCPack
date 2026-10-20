package me.tzahi.TCPack.compress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;

/** Detect capabilities from material names, not Minecraft version strings. */
public final class Recipes {
    private final Map<String, List<Conversion>> groups = new LinkedHashMap<>();

    public Recipes() {
        this(runtimeMaterials());
    }

    private static Map<String, Material> runtimeMaterials() {
        Map<String, Material> materials = new LinkedHashMap<>();
        for (Material material : Material.values()) {
            if (!material.name().startsWith("LEGACY_")) {
                materials.put(material.name(), material);
            }
        }
        return materials;
    }

    Recipes(Map<String, Material> materials) {
        groups.put("wood", wood(materials));
        List<Conversion> bonemeal = new ArrayList<>();
        String meal = materials.containsKey("BONE_MEAL") ? "BONE_MEAL" : "INK_SACK";
        boolean blocks = materials.containsKey("BONE_BLOCK");
        add(bonemeal, materials, meal, "INK_SACK".equals(meal) ? 15 : 0,
                blocks ? "BONE_BLOCK" : "BONE", 0, blocks ? 9 : 3);
        groups.put("bonemeal", bonemeal);
        List<Conversion> ores = new ArrayList<>();
        for (String metal : new String[] {"IRON", "GOLD"}) {
            add(ores, materials, metal + "_NUGGET", 0, metal + "_INGOT", 0, 9);
        }
        for (String name : materials.keySet()) {
            if (name.endsWith("_INGOT")) {
                add(ores, materials, name, 0,
                        name.substring(0, name.length() - "_INGOT".length()) + "_BLOCK", 0, 9);
            } else if (name.startsWith("RAW_") && !name.endsWith("_BLOCK")) {
                add(ores, materials, name, 0, name + "_BLOCK", 0, 9);
            }
        }
        add(ores, materials, "DIAMOND", 0, "DIAMOND_BLOCK", 0, 9);
        add(ores, materials, "REDSTONE", 0, "REDSTONE_BLOCK", 0, 9);
        boolean modernLapis = materials.containsKey("LAPIS_LAZULI");
        add(ores, materials, modernLapis ? "LAPIS_LAZULI" : "INK_SACK",
                modernLapis ? 0 : 4, "LAPIS_BLOCK", 0, 9);
        groups.put("ores", ores);
    }

    private List<Conversion> wood(Map<String, Material> materials) {
        List<Conversion> result = new ArrayList<>();
        if (materials.containsKey("OAK_PLANKS")) {
            for (String name : materials.keySet()) {
                if (name.endsWith("_PLANKS")) {
                    String species = name.substring(0, name.length() - "_PLANKS".length());
                    String log = species + "_LOG";
                    if (!materials.containsKey(log)) {
                        log = species + "_STEM";
                    }
                    add(result, materials, name, 0, log, 0, 4);
                }
            }
        } else {
            for (int data = 0; data < 6; data++) {
                add(result, materials, "WOOD", data, data < 4 ? "LOG" : "LOG_2",
                        data < 4 ? data : data - 4, 4);
            }
        }
        return result;
    }

    private void add(List<Conversion> recipes, Map<String, Material> materials,
            String input, int inputData, String output, int outputData, int ratio) {
        Material source = materials.get(input);
        Material target = materials.get(output);
        if (source != null && target != null) {
            recipes.add(new Conversion(source, inputData, target, outputData, ratio));
        }
    }

    public List<Conversion> forGroup(String name) {
        List<Conversion> recipes = groups.get(name);
        if (recipes == null) {
            throw new IllegalArgumentException("Unknown compression group: " + name);
        }
        return Collections.unmodifiableList(recipes);
    }
}
