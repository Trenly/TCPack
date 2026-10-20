package me.tzahi.TCPack.compress;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Plan the entire conversion on clones before touching the player's inventory. */
public final class Compressor {
    public enum Result { CONVERTED, NOTHING_TO_CONVERT, NO_SPACE }

    public Result compress(PlayerInventory inventory, List<Conversion> recipes) {
        int slots = Math.min(36, inventory.getSize());
        ItemStack[] original = new ItemStack[slots];
        ItemStack[] planned = new ItemStack[slots];
        for (int slot = 0; slot < slots; slot++) {
            original[slot] = inventory.getItem(slot);
            planned[slot] = original[slot] == null ? null : original[slot].clone();
        }
        List<ItemStack> outputs = new ArrayList<>();
        for (Conversion recipe : recipes) {
            int total = 0;
            for (ItemStack output : outputs) {
                if (recipe.matchesGenerated(output)) {
                    total += output.getAmount();
                }
            }
            for (ItemStack stack : planned) {
                if (recipe.matches(stack)) {
                    total += stack.getAmount();
                }
            }
            int amount = total / recipe.ratio();
            if (amount == 0) {
                continue;
            }
            int remove = amount * recipe.ratio();
            // Consume virtual nugget-derived ingots before existing inventory ingots.
            for (Iterator<ItemStack> iterator = outputs.iterator(); iterator.hasNext() && remove > 0;) {
                ItemStack output = iterator.next();
                if (recipe.matchesGenerated(output)) {
                    int consumed = Math.min(remove, output.getAmount());
                    remove -= consumed;
                    if (consumed == output.getAmount()) {
                        iterator.remove();
                    } else {
                        output.setAmount(output.getAmount() - consumed);
                    }
                }
            }
            for (int slot = 0; slot < slots && remove > 0; slot++) {
                ItemStack stack = planned[slot];
                if (recipe.matches(stack)) {
                    int consumed = Math.min(remove, stack.getAmount());
                    remove -= consumed;
                    if (consumed == stack.getAmount()) {
                        planned[slot] = null;
                    } else {
                        stack.setAmount(stack.getAmount() - consumed);
                    }
                }
            }
            outputs.add(recipe.output(amount));
        }
        if (outputs.isEmpty()) {
            return Result.NOTHING_TO_CONVERT;
        }
        for (ItemStack output : outputs) {
            int remaining = output.getAmount();
            int limit = Math.min(output.getMaxStackSize(), inventory.getMaxStackSize());
            for (ItemStack stack : planned) {
                if (stack != null && stack.isSimilar(output) && stack.getAmount() < limit) {
                    int insert = Math.min(remaining, limit - stack.getAmount());
                    stack.setAmount(stack.getAmount() + insert);
                    remaining -= insert;
                }
            }
            for (int slot = 0; slot < slots && remaining > 0; slot++) {
                if (planned[slot] == null || planned[slot].getType() == Material.AIR
                        || planned[slot].getAmount() <= 0) {
                    ItemStack stack = output.clone();
                    stack.setAmount(Math.min(remaining, limit));
                    planned[slot] = stack;
                    remaining -= stack.getAmount();
                }
            }
            if (remaining > 0) {
                return Result.NO_SPACE;
            }
        }
        for (int slot = 0; slot < slots; slot++) {
            if (!Objects.equals(original[slot], planned[slot])) {
                inventory.setItem(slot, planned[slot]);
            }
        }
        return Result.CONVERTED;
    }
}
