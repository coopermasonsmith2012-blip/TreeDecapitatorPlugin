package me.yourname.treechop;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.ArrayList;
import java.util.List;

public class TreeListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // 1. Requirement: Must be sneaking and using an Axe
        if (!player.isSneaking() || !player.getInventory().getItemInMainHand().getType().name().contains("_AXE")) {
            return;
        }

        // 2. Requirement: Must be a Log
        if (!block.getType().name().contains("LOG")) return;

        // 3. Simple "Is it a tree?" check: Look for leaves within 5 blocks above
        boolean hasLeaves = false;
        for (int y = 1; y <= 5; y++) {
            if (block.getRelative(0, y, 0).getType().name().contains("LEAVES")) {
                hasLeaves = true;
                break;
            }
        }

        if (hasLeaves) {
            breakTree(block, player);
        }
    }

    private void breakTree(Block startBlock, Player player) {
        List<Block> toBreak = new ArrayList<>();
        findLogs(startBlock, toBreak);

        ItemStack axe = player.getInventory().getItemInMainHand();
        int logsBroken = 0;

        for (Block b : toBreak) {
            b.breakNaturally(axe); // Breaks and gives drops
            logsBroken++;
            
            // Also break nearby leaves (don't count for durability)
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        Block leaf = b.getRelative(x, y, z);
                        if (leaf.getType().name().contains("LEAVES")) {
                            leaf.breakNaturally();
                        }
                    }
                }
            }
        }

        // Apply durability damage
        if (axe.getItemMeta() instanceof Damageable meta) {
            meta.setDamage(meta.getDamage() + logsBroken);
            axe.setItemMeta(meta);
        }
    }

    private void findLogs(Block block, List<Block> found) {
        if (found.size() > 100 || !block.getType().name().contains("LOG") || found.contains(block)) return;
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
