package dev.bonemealplus.listeners;

import dev.bonemealplus.BoneMealPlus;
import dev.bonemealplus.handler.PlantGrowthHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BoneMealListener implements Listener {

    private final BoneMealPlus plugin;
    private final PlantGrowthHandler growthHandler;

    public BoneMealListener(BoneMealPlus plugin) {
        this.plugin = plugin;
        this.growthHandler = new PlantGrowthHandler(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only handle right-click on blocks
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Only handle main hand to avoid double-firing
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        // Check permission
        if (!player.hasPermission("bonemealplus.use")) return;

        // Check item in hand
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.BONE_MEAL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        // Let vanilla handle plants it already supports natively
        // We only intercept plants that vanilla DOESN'T support
        if (isNativelySupported(block.getType())) return;

        // Attempt to grow the plant
        boolean handled = growthHandler.tryGrow(player, block, item);

        if (handled) {
            event.setCancelled(true);
        }
    }

    /**
     * Returns true for plants vanilla already handles with bone meal.
     * We skip these so we don't double-apply or conflict.
     */
    private boolean isNativelySupported(Material material) {
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS,
                 MELON_STEM, PUMPKIN_STEM,
                 OAK_SAPLING, SPRUCE_SAPLING, BIRCH_SAPLING,
                 JUNGLE_SAPLING, ACACIA_SAPLING, DARK_OAK_SAPLING,
                 MANGROVE_PROPAGULE, CHERRY_SAPLING,
                 GRASS_BLOCK, FERN, TALL_GRASS, LARGE_FERN,
                 DANDELION, POPPY, BLUE_ORCHID, ALLIUM,
                 AZURE_BLUET, RED_TULIP, ORANGE_TULIP, WHITE_TULIP, PINK_TULIP,
                 OXEYE_DAISY, CORNFLOWER, LILY_OF_THE_VALLEY, WITHER_ROSE,
                 SUNFLOWER, LILAC, ROSE_BUSH, PEONY,
                 PINK_PETALS, TORCHFLOWER, TORCHFLOWER_CROP,
                 PITCHER_CROP,
                 SEAGRASS, TALL_SEAGRASS,
                 COCOA -> true;
            default -> false;
        };
    }
}
