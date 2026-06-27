// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.paper;

import io.github.ganyuke.cea.core.util.BlockKey;
import io.github.ganyuke.cea.core.util.BlockType;
import io.github.ganyuke.cea.core.util.HandKind;
import io.github.ganyuke.cea.core.portal.PortalController;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class PaperPortalListener implements Listener {
    private final PortalController controller;

    public PaperPortalListener(PortalController controller) {
        this.controller = controller;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.END_PORTAL_FRAME) return;
        if (!(block.getBlockData() instanceof EndPortalFrame frame)) return;

        Player player = event.getPlayer();
        ItemStack eventItem = event.getItem();
        EquipmentSlot equipmentSlot = event.getHand();
        HandKind hand = equipmentSlot == EquipmentSlot.OFF_HAND ? HandKind.OFF_HAND : HandKind.MAIN_HAND;
        boolean holdingNothing = eventItem == null;
        boolean holdingEye = !holdingNothing && eventItem.getType() == Material.ENDER_EYE;
        boolean offHandHasEye = player.getInventory().getItemInOffHand().getType() == Material.ENDER_EYE;

        PortalController.PortalInteraction interaction = new PortalController.PortalInteraction(
                player.getUniqueId(),
                player.getName(),
                new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()),
                BlockType.END_PORTAL_FRAME,
                frame.hasEye(),
                holdingEye,
                holdingNothing,
                offHandHasEye,
                player.getGameMode() == GameMode.CREATIVE,
                hand
        );

        PortalController.InteractionDecision decision = controller.interact(interaction);
        if (decision.cancelled()) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.END_PORTAL_FRAME) return;
        controller.breakBlock(new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) controller.dragonDeath();
    }
}
