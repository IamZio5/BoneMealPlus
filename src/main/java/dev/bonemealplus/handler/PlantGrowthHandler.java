package dev.bonemealplus.handler;

import dev.bonemealplus.BoneMealPlus;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class PlantGrowthHandler {

    private final BoneMealPlus plugin;
    private final Random random = new Random();

    public PlantGrowthHandler(BoneMealPlus plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempts to grow the plant at the given block.
     * Returns true if we handled it (regardless of success/fail roll), false if unrecognized.
     */
    public boolean tryGrow(Player player, Block block, ItemStack boneMeal) {
        Material type = block.getType();
        boolean handled = false;

        switch (type) {
            case CACTUS      -> { handled = growStackable(block, Material.CACTUS, 3); }
            case SUGAR_CANE  -> { handled = growStackable(block, Material.SUGAR_CANE, 3); }
            case BAMBOO      -> { handled = growBamboo(block); }
            case CHORUS_FLOWER -> { handled = growChorus(block); }
            case NETHER_WART -> { handled = growAged(block, 3); }
            case SWEET_BERRY_BUSH -> { handled = growAged(block, 3); }
            case KELP        -> { handled = growKelp(block); }
            case SEA_PICKLE  -> { handled = growSeaPickle(block); }
            case VINE        -> { handled = growVine(block); }
            case TWISTING_VINES -> { handled = growTwistingVines(block); }
            case WEEPING_VINES -> { handled = growWeepingVines(block); }
            case CAVE_VINES, CAVE_VINES_PLANT -> { handled = growCaveVines(block); }
            case CRIMSON_FUNGUS, WARPED_FUNGUS -> { handled = growFungus(block); }
            case BROWN_MUSHROOM, RED_MUSHROOM -> { handled = growMushroom(block); }
            case LILY_PAD    -> { handled = false; } // already vanilla? skip
            default          -> { handled = false; }
        }

        if (handled && rollChance(type)) {
            if (plugin.getConfig().getBoolean("consume-bone-meal", true)) {
                consumeItem(player, boneMeal);
            }
            if (plugin.getConfig().getBoolean("show-particles", true)) {
                spawnBoneMealParticles(block);
            }
            return true;
        } else if (handled) {
            // Recognised but chance failed – still cancel event, consume meal
            if (plugin.getConfig().getBoolean("consume-bone-meal", true)) {
                consumeItem(player, boneMeal);
            }
            if (plugin.getConfig().getBoolean("show-particles", true)) {
                spawnBoneMealParticles(block);
            }
            return true;
        }

        return false;
    }

    // -----------------------------------------------------------------------
    // Growth implementations
    // -----------------------------------------------------------------------

    /** Cactus / Sugar Cane – grow by adding a block on top up to maxHeight */
    private boolean growStackable(Block base, Material mat, int maxHeight) {
        // Find the top of the column
        Block top = base;
        while (top.getRelative(BlockFace.UP).getType() == mat) {
            top = top.getRelative(BlockFace.UP);
        }

        // Count total column height
        Block bottom = top;
        int height = 1;
        while (bottom.getRelative(BlockFace.DOWN).getType() == mat) {
            bottom = bottom.getRelative(BlockFace.DOWN);
            height++;
        }

        if (height >= maxHeight) return true; // already at max, handled but no grow

        if (!rollChance(mat)) return true;

        Block above = top.getRelative(BlockFace.UP);
        if (above.getType() == Material.AIR) {
            above.setType(mat);
        }
        return true;
    }

    /** Bamboo – grows up (max 16 blocks) */
    private boolean growBamboo(Block block) {
        Block top = block;
        int height = 1;
        while (top.getRelative(BlockFace.UP).getType() == Material.BAMBOO) {
            top = top.getRelative(BlockFace.UP);
            height++;
        }

        if (height >= 16) return true;
        if (!rollChance(Material.BAMBOO)) return true;

        Block above = top.getRelative(BlockFace.UP);
        if (above.getType() == Material.AIR) {
            above.setType(Material.BAMBOO);
            // Set the new bamboo to SMALL_LEAVES stage
            if (above.getBlockData() instanceof Bamboo bd) {
                bd.setLeaves(Bamboo.Leaves.SMALL);
                above.setBlockData(bd);
            }
        }
        return true;
    }

    /** Chorus Flower – advances age (0-4 growing, 5 dead) */
    private boolean growChorus(Block block) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return false;
        int max = ageable.getMaximumAge();
        int age = ageable.getAge();

        if (age >= max - 1) return true; // already at bloomed/dead stage, skip
        if (!rollChance(Material.CHORUS_FLOWER)) return true;

        ageable.setAge(Math.min(age + 1, max - 1));
        block.setBlockData(ageable);
        return true;
    }

    /** Generic Ageable plant (Nether Wart, Sweet Berry Bush, etc.) */
    private boolean growAged(Block block, int cap) {
        if (!(block.getBlockData() instanceof Ageable ageable)) return false;
        int age = ageable.getAge();
        int max = ageable.getMaximumAge();
        if (age >= max) return true;

        if (!rollChance(block.getType())) return true;

        int stages = plugin.getConfig().getInt("growth-stages", 1);
        ageable.setAge(Math.min(age + stages, max));
        block.setBlockData(ageable);
        return true;
    }

    /** Kelp – grows upward */
    private boolean growKelp(Block block) {
        Block top = block;
        while (top.getRelative(BlockFace.UP).getType() == Material.KELP
                || top.getRelative(BlockFace.UP).getType() == Material.KELP_PLANT) {
            top = top.getRelative(BlockFace.UP);
        }

        // Kelp has age 0-25; age 25 = cannot grow
        if (top.getBlockData() instanceof Ageable kelpData) {
            if (kelpData.getAge() >= kelpData.getMaximumAge()) return true;
        }

        if (!rollChance(Material.KELP)) return true;

        Block above = top.getRelative(BlockFace.UP);
        if (above.getType() == Material.WATER) {
            // Current top becomes KELP_PLANT
            top.setType(Material.KELP_PLANT);
            above.setType(Material.KELP);
        }
        return true;
    }

    /** Sea Pickle – increases pickles in the cluster (1-4) */
    private boolean growSeaPickle(Block block) {
        if (!(block.getBlockData() instanceof SeaPickle sp)) return false;
        if (sp.getPickles() >= sp.getMaximumPickles()) return true;

        if (!rollChance(Material.SEA_PICKLE)) return true;

        sp.setPickles(sp.getPickles() + 1);
        block.setBlockData(sp);
        return true;
    }

    /** Vine – extends downward (or in any direction it already has) */
    private boolean growVine(Block block) {
        if (!rollChance(Material.VINE)) return true;

        Block below = block.getRelative(BlockFace.DOWN);
        if (below.getType() == Material.AIR) {
            // Copy vine faces to the block below
            below.setBlockData(block.getBlockData());
        }
        return true;
    }

    /** Twisting Vines (grow upward) */
    private boolean growTwistingVines(Block block) {
        Block top = block;
        while (top.getRelative(BlockFace.UP).getType() == Material.TWISTING_VINES
                || top.getRelative(BlockFace.UP).getType() == Material.TWISTING_VINES_PLANT) {
            top = top.getRelative(BlockFace.UP);
        }
        if (!rollChance(Material.TWISTING_VINES)) return true;
        Block above = top.getRelative(BlockFace.UP);
        if (above.getType() == Material.AIR) {
            top.setType(Material.TWISTING_VINES_PLANT);
            above.setType(Material.TWISTING_VINES);
        }
        return true;
    }

    /** Weeping Vines (grow downward) */
    private boolean growWeepingVines(Block block) {
        Block bottom = block;
        while (bottom.getRelative(BlockFace.DOWN).getType() == Material.WEEPING_VINES
                || bottom.getRelative(BlockFace.DOWN).getType() == Material.WEEPING_VINES_PLANT) {
            bottom = bottom.getRelative(BlockFace.DOWN);
        }
        if (!rollChance(Material.WEEPING_VINES)) return true;
        Block below = bottom.getRelative(BlockFace.DOWN);
        if (below.getType() == Material.AIR) {
            bottom.setType(Material.WEEPING_VINES_PLANT);
            below.setType(Material.WEEPING_VINES);
        }
        return true;
    }

    /** Cave Vines – advances berry growth */
    private boolean growCaveVines(Block block) {
        if (!(block.getBlockData() instanceof CaveVines cv)) return false;
        if (!rollChance(Material.CAVE_VINES)) return true;
        cv.setBerries(!cv.isBerries()); // toggle berries on (simple approach)
        if (!cv.isBerries()) cv.setBerries(true);
        block.setBlockData(cv);
        return true;
    }

    /** Fungus (Crimson/Warped) – tries to grow into a giant fungus */
    private boolean growFungus(Block block) {
        Material type = block.getType();
        if (!rollChance(type)) return true;
        // Simulate bonemeal: use Bukkit's built-in bonemeal effect via block growth
        block.applyBoneMeal(BlockFace.UP);
        return true;
    }

    /** Mushroom – grows into a huge mushroom if space allows */
    private boolean growMushroom(Block block) {
        if (!rollChance(block.getType())) return true;
        // Use applyBoneMeal which attempts huge mushroom generation
        block.applyBoneMeal(BlockFace.UP);
        return true;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private boolean rollChance(Material material) {
        String key = "chances." + material.name();
        double chance = plugin.getConfig().contains(key)
                ? plugin.getConfig().getDouble(key)
                : plugin.getConfig().getDouble("chances.DEFAULT", 0.45);
        boolean success = random.nextDouble() < chance;
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] " + material.name() + " chance=" + chance + " success=" + success);
        }
        return success;
    }

    private void consumeItem(Player player, ItemStack item) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (item.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(item.getAmount() - 1);
        }
    }

    private void spawnBoneMealParticles(Block block) {
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        block.getWorld().spawnParticle(
                Particle.HAPPY_VILLAGER,
                center,
                6,
                0.3, 0.3, 0.3,
                0
        );
    }
}
