package com.cceckman.blocks;

import static java.lang.Byte.toUnsignedInt;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class Materials {

    private static ItemStack[] by_byte;
    private static Map<Material, Byte> by_material;

    static {
        by_byte = new ItemStack[256];
        by_byte[0] = null;
        by_material = new HashMap<Material, Byte>();

        int i = 1;

        for (var material : Material.values()) {
            if (by_material.size() == 255) {
                break;
            }

            if (material.isItem()) {
                by_byte[i] = new ItemStack(material);
                by_material.put(material, (byte) i);
                i++;
            }
        }
    }

    public static ItemStack value(byte b) {
        return by_byte[toUnsignedInt(b)];
    }
    public static byte value(ItemStack s) {
        if(s == null || !by_material.containsKey(s.getType())) {
            return 0;
        }
        return by_material.get(s.getType());
    }

    public static void Print(Logger l) {
        l.info(String.format("Byte %d: null", 0));
        for(byte i = 1; i < 256; i++) {
            l.info(String.format("Byte %d: %s", i, value(i).getType().toString()));
        }
    }
}