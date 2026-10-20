package me.tzahi.TCPack.compress;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Compression adaptation by Trenly; original TCPack by Tzahi and TacoID, CC BY-SA 3.0. */
public final class Conversion {
    private final Material input;
    private final short inputData;
    private final Material output;
    private final short outputData;
    private final int ratio;

    Conversion(Material input, int inputData, Material output, int outputData, int ratio) {
        this.input = input;
        this.inputData = (short) inputData;
        this.output = output;
        this.outputData = (short) outputData;
        this.ratio = ratio;
    }

    boolean matches(ItemStack stack) {
        return matchesGenerated(stack) && !stack.hasItemMeta();
    }

    boolean matchesGenerated(ItemStack stack) {
        return stack != null && stack.getAmount() > 0 && stack.getType() == input
                && stack.getDurability() == inputData;
    }

    int ratio() {
        return ratio;
    }

    ItemStack output(int amount) {
        return new ItemStack(output, amount, outputData);
    }
}
