package me.tzahi.TCPack.compress;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

class CompressionTest {
    @Test
    void redstoneAndLegacyLapisCombineStacksAndPreserveOtherDyes() {
        Inventory inventory = new Inventory();
        inventory.items[0] = stack(Material.REDSTONE, 8, 0);
        inventory.items[1] = stack(Material.REDSTONE, 11, 0);
        inventory.items[2] = stack(Material.INK_SACK, 8, 4);
        inventory.items[3] = stack(Material.INK_SACK, 11, 4);
        inventory.items[4] = stack(Material.INK_SACK, 18, 15);
        inventory.items[5] = stack(Material.INK_SACK, 18, 0);
        Stack custom = stack(Material.REDSTONE, 9, 0);
        custom.customized = true;
        inventory.items[6] = custom;
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("ores")));
        assertEquals(2, inventory.count(Material.REDSTONE_BLOCK, 0));
        assertEquals(10, inventory.count(Material.REDSTONE, 0));
        assertEquals(2, inventory.count(Material.LAPIS_BLOCK, 0));
        assertEquals(1, inventory.count(Material.INK_SACK, 4));
        assertEquals(18, inventory.count(Material.INK_SACK, 15));
        assertEquals(18, inventory.count(Material.INK_SACK, 0));
        assertSame(custom, inventory.items[6]);
    }

    @Test
    void modernLapisUsesItsOwnMaterialRatherThanLegacyDye() {
        Map<String, Material> aliases = new LinkedHashMap<>();
        aliases.put("LAPIS_LAZULI", Material.DIAMOND);
        aliases.put("LAPIS_BLOCK", Material.LAPIS_BLOCK);
        aliases.put("INK_SACK", Material.INK_SACK);
        Inventory inventory = new Inventory();
        inventory.items[0] = stack(Material.DIAMOND, 19, 0);
        inventory.items[1] = stack(Material.INK_SACK, 18, 4);
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes(aliases).forGroup("ores")));
        assertEquals(2, inventory.count(Material.LAPIS_BLOCK, 0));
        assertEquals(1, inventory.count(Material.DIAMOND, 0));
        assertEquals(18, inventory.count(Material.INK_SACK, 4));
    }

    @Test
    void legacyWoodPreservesEverySpeciesAndRemainders() {
        for (int species = 0; species < 6; species++) {
            Inventory inventory = new Inventory();
            inventory.items[0] = stack(Material.WOOD, 3, species);
            inventory.items[1] = stack(Material.WOOD, 10, species);
            assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("wood")));
            assertEquals(1, inventory.count(Material.WOOD, species));
            assertEquals(3, inventory.count(species < 4 ? Material.LOG : Material.LOG_2,
                    species < 4 ? species : species - 4));
        }
    }

    @Test
    void legacyBonemealUsesBonesAndNeverOtherDyes() {
        Inventory inventory = new Inventory();
        inventory.items[0] = stack(Material.INK_SACK, 11, 15);
        inventory.items[1] = stack(Material.INK_SACK, 12, 0);
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("bonemeal")));
        assertEquals(3, inventory.count(Material.BONE, 0));
        assertEquals(2, inventory.count(Material.INK_SACK, 15));
        assertEquals(12, inventory.count(Material.INK_SACK, 0));
    }

    @Test
    void consumesAllSupportedOresAndNuggetsWithoutConsumingOreBlocks() {
        Inventory inventory = new Inventory();
        inventory.items[0] = stack(Material.IRON_INGOT, 19, 0);
        inventory.items[1] = stack(Material.GOLD_INGOT, 18, 0);
        inventory.items[2] = stack(Material.DIAMOND, 8, 0);
        inventory.items[3] = stack(Material.GOLD_NUGGET, 64, 0);
        inventory.items[4] = stack(Material.IRON_ORE, 64, 0);
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("ores")));
        assertEquals(2, inventory.count(Material.IRON_BLOCK, 0));
        assertEquals(1, inventory.count(Material.IRON_INGOT, 0));
        assertEquals(2, inventory.count(Material.GOLD_BLOCK, 0));
        assertEquals(8, inventory.count(Material.DIAMOND, 0));
        assertEquals(1, inventory.count(Material.GOLD_NUGGET, 0));
        assertEquals(7, inventory.count(Material.GOLD_INGOT, 0));
        assertEquals(64, inventory.count(Material.IRON_ORE, 0));
    }

    @Test
    void nuggetsCombineWithExistingIngotsAndTakePriority() {
        for (int ingots : new int[] {8, 9, 17}) {
            Inventory inventory = new Inventory();
            inventory.items[0] = stack(Material.GOLD_INGOT, ingots, 0);
            inventory.items[1] = stack(Material.GOLD_NUGGET, 10, 0);
            assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("ores")));
            assertEquals((ingots + 1) / 9, inventory.count(Material.GOLD_BLOCK, 0));
            assertEquals((ingots + 1) % 9, inventory.count(Material.GOLD_INGOT, 0));
            assertEquals(1, inventory.count(Material.GOLD_NUGGET, 0));
        }
    }

    @Test
    void ironNuggetsAreDiscoveredWhenAvailableAndCanProduceBlocksWithoutIntermediateSpace() {
        Map<String, Material> aliases = new LinkedHashMap<>();
        aliases.put("IRON_NUGGET", Material.GOLD_NUGGET);
        aliases.put("IRON_INGOT", Material.IRON_INGOT);
        aliases.put("IRON_BLOCK", Material.IRON_BLOCK);
        Inventory inventory = new Inventory();
        for (int slot = 0; slot < 36; slot++) {
            inventory.items[slot] = stack(Material.COBBLESTONE, 64, 0);
        }
        inventory.items[0] = stack(Material.IRON_INGOT, 8, 0);
        ItemStack originalIngots = inventory.items[0];
        inventory.items[1] = stack(Material.GOLD_NUGGET, 64, 0);
        inventory.items[2] = stack(Material.GOLD_NUGGET, 18, 0);
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes(aliases).forGroup("ores")));
        assertEquals(1, inventory.count(Material.IRON_BLOCK, 0));
        assertEquals(8, inventory.count(Material.IRON_INGOT, 0));
        assertSame(originalIngots, inventory.items[0]);
        assertEquals(1, inventory.count(Material.GOLD_NUGGET, 0));
    }

    @Test
    void finalBlockFitsEvenWhenIntermediateIngotsWouldNotFit() {
        Inventory inventory = new Inventory();
        for (int slot = 0; slot < 36; slot++) {
            inventory.items[slot] = stack(Material.COBBLESTONE, 64, 0);
        }
        inventory.items[0] = stack(Material.GOLD_INGOT, 8, 0);
        inventory.items[1] = stack(Material.GOLD_NUGGET, 10, 0);
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("ores")));
        assertEquals(1, inventory.count(Material.GOLD_BLOCK, 0));
        assertEquals(0, inventory.count(Material.GOLD_INGOT, 0));
        assertEquals(1, inventory.count(Material.GOLD_NUGGET, 0));
    }

    @Test
    void nuggetsAcrossStacksCompressAndPreserveCustomItems() {
        Inventory inventory = new Inventory();
        inventory.items[0] = stack(Material.GOLD_NUGGET, 64, 0);
        inventory.items[1] = stack(Material.GOLD_NUGGET, 18, 0);
        Stack named = stack(Material.GOLD_NUGGET, 9, 0);
        named.customized = true;
        inventory.items[2] = named;
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("ores")));
        assertEquals(1, inventory.count(Material.GOLD_BLOCK, 0));
        assertEquals(0, inventory.count(Material.GOLD_INGOT, 0));
        assertEquals(10, inventory.count(Material.GOLD_NUGGET, 0));
        assertSame(named, inventory.items[2]);
    }

    @Test
    void nuggetRemaindersAndNoSpaceRejectWithoutPartialConversion() {
        Inventory inventory = new Inventory();
        inventory.items[0] = stack(Material.GOLD_NUGGET, 8, 0);
        assertEquals(Compressor.Result.NOTHING_TO_CONVERT, inventory.run(new Recipes().forGroup("ores")));
        for (int slot = 1; slot < 36; slot++) {
            inventory.items[slot] = stack(Material.COBBLESTONE, 64, 0);
        }
        inventory.items[0] = stack(Material.GOLD_NUGGET, 10, 0);
        ItemStack[] before = inventory.items.clone();
        assertEquals(Compressor.Result.NO_SPACE, inventory.run(new Recipes().forGroup("ores")));
        assertArrayEquals(before, inventory.items);
        assertEquals(10, inventory.items[0].getAmount());
        assertEquals(0, inventory.writes);
    }

    @Test
    void noSpaceRejectsEveryConversionWithoutTouchingOriginalStacks() {
        Inventory inventory = new Inventory();
        for (int slot = 0; slot < 36; slot++) {
            inventory.items[slot] = stack(Material.COBBLESTONE, 64, 0);
        }
        inventory.items[0] = stack(Material.IRON_INGOT, 10, 0);
        inventory.items[1] = stack(Material.GOLD_INGOT, 10, 0);
        ItemStack[] before = inventory.items.clone();
        assertEquals(Compressor.Result.NO_SPACE, inventory.run(new Recipes().forGroup("ores")));
        assertArrayEquals(before, inventory.items);
        assertEquals(10, inventory.items[0].getAmount());
        assertEquals(0, inventory.writes);
    }

    @Test
    void usesSlotsFreedByConsumptionAndExistingOutputStacks() {
        Inventory inventory = new Inventory();
        for (int slot = 0; slot < 36; slot++) {
            inventory.items[slot] = stack(Material.COBBLESTONE, 64, 0);
        }
        inventory.items[0] = stack(Material.IRON_INGOT, 10, 0);
        inventory.items[1] = stack(Material.IRON_BLOCK, 63, 0);
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("ores")));
        assertEquals(64, inventory.items[1].getAmount());
        assertEquals(1, inventory.items[0].getAmount());

        inventory.items[0] = stack(Material.IRON_INGOT, 9, 0);
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("ores")));
        assertEquals(Material.IRON_BLOCK, inventory.items[0].getType());
        assertEquals(1, inventory.items[0].getAmount());
    }

    @Test
    void customItemsArmorAndOffhandAreNeverConsumedOrUsedAsOutputSpace() {
        Inventory inventory = new Inventory();
        Stack custom = stack(Material.WOOD, 64, 0);
        custom.customized = true;
        inventory.items[0] = custom;
        inventory.items[36] = stack(Material.WOOD, 64, 0);
        inventory.items[40] = stack(Material.WOOD, 64, 0);
        assertEquals(Compressor.Result.NOTHING_TO_CONVERT,
                inventory.run(new Recipes().forGroup("wood")));
        assertSame(custom, inventory.items[0]);
        assertEquals(64, inventory.items[36].getAmount());
        assertEquals(64, inventory.items[40].getAmount());
        assertEquals(0, inventory.writes);
    }

    @Test
    void outputsAreSplitAtInventoryStackLimit() {
        Inventory inventory = new Inventory();
        inventory.limit = 2;
        inventory.items[0] = stack(Material.IRON_INGOT, 63, 0);
        assertEquals(Compressor.Result.CONVERTED, inventory.run(new Recipes().forGroup("ores")));
        assertEquals(7, inventory.count(Material.IRON_BLOCK, 0));
        for (int slot = 0; slot < 36; slot++) {
            assertTrue(inventory.items[slot] == null || inventory.items[slot].getAmount() <= 2);
        }
    }

    @Test
    void modernNameDiscoveryChoosesBaseLogsAndStemsNeverStrippedWoodOrBamboo() {
        Map<String, Material> aliases = new LinkedHashMap<>();
        aliases.put("OAK_PLANKS", Material.WOOD);
        aliases.put("OAK_LOG", Material.LOG);
        aliases.put("PALE_OAK_PLANKS", Material.SAND);
        aliases.put("PALE_OAK_LOG", Material.STONE);
        aliases.put("CRIMSON_PLANKS", Material.REDSTONE);
        aliases.put("CRIMSON_STEM", Material.REDSTONE_BLOCK);
        aliases.put("BAMBOO_PLANKS", Material.STICK);
        aliases.put("BAMBOO_BLOCK", Material.BOOKSHELF);
        aliases.put("OAK_WOOD", Material.BOOKSHELF);
        aliases.put("STRIPPED_OAK_LOG", Material.BOOKSHELF);
        List<Conversion> recipes = new Recipes(aliases).forGroup("wood");
        assertEquals(3, recipes.size());
        assertEquals(Material.LOG, recipes.get(0).output(1).getType());
        assertEquals(Material.STONE, recipes.get(1).output(1).getType());
        assertEquals(Material.REDSTONE_BLOCK, recipes.get(2).output(1).getType());
        assertTrue(recipes.stream().allMatch(recipe -> recipe.ratio() == 4));
    }

    @Test
    void boneBlocksArePreferredEvenBeforeMaterialFlattening() {
        Map<String, Material> aliases = new LinkedHashMap<>();
        aliases.put("INK_SACK", Material.INK_SACK);
        aliases.put("BONE", Material.BONE);
        aliases.put("BONE_BLOCK", Material.STONE);
        Conversion recipe = new Recipes(aliases).forGroup("bonemeal").get(0);
        assertEquals(9, recipe.ratio());
        assertTrue(recipe.matches(stack(Material.INK_SACK, 9, 15)));
        assertFalse(recipe.matches(stack(Material.INK_SACK, 9, 0)));
        assertEquals(Material.STONE, recipe.output(1).getType());
    }

    @Test
    void modernOresIncludeAvailableIngotsAndRawItemsOnlyWhenTheirBlockExists() {
        Map<String, Material> aliases = new LinkedHashMap<>();
        aliases.put("COPPER_INGOT", Material.IRON_INGOT);
        aliases.put("COPPER_BLOCK", Material.IRON_BLOCK);
        aliases.put("RAW_COPPER", Material.GOLD_INGOT);
        aliases.put("RAW_COPPER_BLOCK", Material.GOLD_BLOCK);
        aliases.put("NETHERITE_INGOT", Material.DIAMOND);
        aliases.put("NETHERITE_BLOCK", Material.DIAMOND_BLOCK);
        aliases.put("RAW_UNSUPPORTED", Material.STICK);
        List<Conversion> recipes = new Recipes(aliases).forGroup("ores");
        assertEquals(3, recipes.size());
        assertTrue(recipes.stream().allMatch(recipe -> recipe.ratio() == 9));
        Inventory inventory = new Inventory();
        inventory.items[0] = stack(Material.GOLD_INGOT, 20, 0);
        assertEquals(Compressor.Result.CONVERTED, inventory.run(recipes));
        assertEquals(2, inventory.count(Material.GOLD_BLOCK, 0));
        assertEquals(2, inventory.count(Material.GOLD_INGOT, 0));
    }

    @Test
    void insufficientMaterialsAndEmptyInventoryDoNotMutate() {
        Inventory inventory = new Inventory();
        assertEquals(Compressor.Result.NOTHING_TO_CONVERT,
                inventory.run(new Recipes().forGroup("wood")));
        inventory.items[0] = stack(Material.WOOD, 3, 0);
        assertEquals(Compressor.Result.NOTHING_TO_CONVERT,
                inventory.run(new Recipes().forGroup("wood")));
        assertEquals(3, inventory.items[0].getAmount());
        assertEquals(0, inventory.writes);
        assertThrows(IllegalArgumentException.class, () -> new Recipes().forGroup("unknown"));
    }

    private static Stack stack(Material type, int amount, int data) {
        return new Stack(type, amount, data);
    }

    private static final class Stack extends ItemStack {
        private boolean customized;

        Stack(Material type, int amount, int data) {
            super(type, amount, (short) data);
        }

        @Override
        public boolean hasItemMeta() {
            return customized;
        }

        @Override
        public Stack clone() {
            Stack copy = new Stack(getType(), getAmount(), getDurability());
            copy.customized = customized;
            return copy;
        }

        @Override
        public boolean isSimilar(ItemStack other) {
            return other != null && getType() == other.getType()
                    && getDurability() == other.getDurability()
                    && customized == (other instanceof Stack && ((Stack) other).customized);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ItemStack && isSimilar((ItemStack) other)
                    && getAmount() == ((ItemStack) other).getAmount();
        }

    }

    private static final class Inventory {
        final ItemStack[] items = new ItemStack[41];
        int limit = 64;
        int writes;
        final PlayerInventory proxy = (PlayerInventory) Proxy.newProxyInstance(
                PlayerInventory.class.getClassLoader(), new Class<?>[] {PlayerInventory.class},
                (object, method, arguments) -> {
                    switch (method.getName()) {
                        case "getSize": return items.length;
                        case "getMaxStackSize": return limit;
                        case "getItem": return items[(Integer) arguments[0]];
                        case "setItem":
                            int slot = (Integer) arguments[0];
                            assertTrue(slot < 36);
                            items[slot] = (ItemStack) arguments[1];
                            writes++;
                            return null;
                        default: throw new AssertionError("Unexpected inventory call: " + method);
                    }
                });

        Compressor.Result run(List<Conversion> recipes) {
            return new Compressor().compress(proxy, recipes);
        }

        int count(Material type, int data) {
            return Arrays.stream(items).filter(stack -> stack != null && stack.getType() == type
                    && stack.getDurability() == data).mapToInt(ItemStack::getAmount).sum();
        }
    }
}
