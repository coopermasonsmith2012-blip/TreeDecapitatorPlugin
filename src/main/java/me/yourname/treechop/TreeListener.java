package me.yourname.treechop;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TreeListener implements Listener {

    private final Random random = new Random();
    private final NamespacedKey timberKey = new NamespacedKey("treechop", "timber");

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        ItemStack tool = player.getInventory().getItemInMainHand();

        // 1. Check for Timber Enchantment (No shifting required now)
        if (!hasTimberEnchantment(tool)) return;

        // 2. Check if the tool is an Axe and the block is a Log
        if (!tool.getType().name().contains("_AXE") || !isLog(block.getType())) return;

        // 3. Safety Check: Verify it's a natural tree
        if (!isNearLeaves(block)) return;

        // 4. Map the tree structure
        Set<Block> logs = new HashSet<>();
        findLogs(block, logs);

        // 5. Tool Safety: Ensure axe survives the logs (ignores unbreaking for safety buffer)
        if (!hasEnoughDurability(tool, logs.size())) {
            player.sendMessage("§cThis tree is too large for your axe's remaining durability!");
            event.setCancelled(true);
            return;
        }

        // 6. Execute Tree Chop
        event.setCancelled(true);
        processTree(logs, player, tool);
        
        player.playSound(player.getLocation(), Sound.BLOCK_WOOD_BREAK, 1.0f, 0.8f);
    }

    private boolean hasTimberEnchantment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        // Checks if the item has the custom enchantment "treechop:timber"
        return item.getEnchantments().keySet().stream()
                .anyMatch(ench -> ench.getKey().equals(timberKey));
    }

    private boolean isLog(Material mat) {
        String name = mat.name();
        return name.contains("_LOG") || name.contains("_WOOD") || name.contains("MANGROVE_ROOTS");
    }

    private boolean isNearLeaves(Block log) {
        for (int x = -3; x <= 3; x++) {
            for (int y = -2; y <= 8; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (log.getRelative(x, y, z).getType().name().contains("LEAVES")) return true;
                }
            }
        }
        return false;
    }

    private boolean hasEnoughDurability(ItemStack tool, int logCount) {
        if (!(tool.getItemMeta() instanceof Damageable meta)) return true;
        int remaining = tool.getType().getMaxDurability() - meta.getDamage();
        return remaining > logCount; 
    }

    private void processTree(Set<Block> logs, Player player, ItemStack tool) {
        int unbreakingLevel = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        int totalDamageToTake = 0;

        for (Block log : logs) {
            log.breakNaturally(tool); // Drops log items

            // Durability logic (Vanilla Unbreaking formula)
            if (shouldTakeDamage(unbreakingLevel)) {
                totalDamageToTake++;
            }

            // Break surrounding leaves (Normal drops, 0 durability cost)
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block potentialLeaf = log.getRelative(x, y, z);
                        if (potentialLeaf.getType().name().contains("LEAVES")) {
                            potentialLeaf.breakNaturally(); 
                        }
                    }
                }
            }
        }

        // Apply final calculated damage to the tool
        Damageable meta = (Damageable) tool.getItemMeta();
        meta.setDamage(meta.getDamage() + totalDamageToTake);
        tool.setItemMeta(meta);
    }

    private boolean shouldTakeDamage(int level) {
        // Chance is 100% / (level + 1)
        return random.nextDouble() < (1.0 / (level + 1.0));
    }

    private void findLogs(Block block, Set<Block> found) {
        if (found.size() > 400 || !isLog(block.getType()) || found.contains(block)) return;
        found.add(block);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    findLogs(block.getRelative(x, y, z), found);
                }
            }
        }
    }
}
