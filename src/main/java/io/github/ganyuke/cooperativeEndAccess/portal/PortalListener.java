package io.github.ganyuke.cooperativeEndAccess.portal;

import io.github.ganyuke.cooperativeEndAccess.util.BlockKey;
import io.github.ganyuke.cooperativeEndAccess.config.Config;
import io.github.ganyuke.cooperativeEndAccess.data.Persist;
import io.github.ganyuke.cooperativeEndAccess.data.State;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import io.github.ganyuke.cooperativeEndAccess.config.Config.MessageKey;

import java.util.UUID;

public class PortalListener implements Listener {
    private final State state;
    private final Persist persist;
    private final Config config;
    private final PortalManager portalManager;

    public PortalListener(State state, Persist persist, Config config, PortalManager portalManager) {
        this.state = state;
        this.persist = persist;
        this.config = config;
        this.portalManager = portalManager;
    }

    private void handleEyePlacement(PortalUtils.PortalInteractionContext ctx) {
        var center = ctx.center();
        var uuid = ctx.playerId();
        var player = ctx.player();
        var event = ctx.event();
        var frame = ctx.frame();
        var block = ctx.block();

        // PLACING AN EYE
        if (state.countFramesOwnedBy(center, uuid) >= config.getMaxEyesPerPlayer()) {
            player.sendMessage(config.getMessage(MessageKey.MAX_EYES_ERROR));
            event.setCancelled(true);
            return;
        }

        BlockKey frameLoc = BlockKey.from(block.getLocation());

        // placing the last eye in vanilla creates the Ender Portal,
        // so we need to stop that from happening to (1) prevent
        // the actual ender portal from spawning without our green light
        // and (2) prevent the End Portal opening sound from automatically playing
        if (PortalUtils.countEndFrameEyes(center) == 11) {
            // cancel the placement event to prevent vanilla logic from running
            // (e.g. placing the end portal blocks, making noise)
            event.setCancelled(true);

            // manually replicate what placing an eye would usually do
            frame.setEye(true);
            block.setBlockData(frame);
            ItemStack item = event.getItem();
            if (item != null && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                item.setAmount(item.getAmount() - 1);
            }
            event.getPlayer().playSound(block.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
        }

        state.addEye(center, frameLoc, uuid);
        state.addName(uuid, player.getName());
        persist.saveData(state);
        player.sendMessage(config.getMessage(MessageKey.COMMITTED_NOTICE));
    }

    private void handleEyeRemoval(PortalUtils.PortalInteractionContext ctx) {
        var center = ctx.center();
        var uuid = ctx.playerId();
        var player = ctx.player();
        var event = ctx.event();
        var frame = ctx.frame();
        var block = ctx.block();

        // REMOVING AN EYE
        event.setCancelled(true);

        BlockKey frameLoc = BlockKey.from(block.getLocation());
        UUID ownerId = state.getEyeOwner(frameLoc);
        if (ownerId == null) return;

        // reject removal if the player is not the owner of the eye
        if (!ownerId.equals(player.getUniqueId())) {
            String ownerName = state.getName(ownerId);
            if (ownerName == null) ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
            player.sendMessage(config.getOwnerRescindWarning(ownerName));
            return;
        }

        // in theory, removeEye() method shouldn't return false at this point,
        // but I have it return a boolean so we'll just not run alter the world
        // if the player did not remove the eye successfully
        if (state.removeEye(frameLoc)) {
            frame.setEye(false);
            block.setBlockData(frame);
            player.getInventory().addItem(new ItemStack(Material.ENDER_EYE));
            player.sendMessage(config.getMessage(MessageKey.RESCIND_WARNING));
            persist.saveData(state);
            portalManager.updateTrackedPortals();
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // this plugin doesn't matter if the dragon is dead
        if (state.getDragonDefeatStatus()) return;
        // the follow actions don't matter if it wasn't a right click
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        // avoid double-firing the removal logic from the OFF_HAND interaction event
        if (event.getHand() == EquipmentSlot.OFF_HAND) return;

        var ctx = PortalUtils.PortalInteractionContext.from(event);

        if (ctx.isHoldingEye() && !ctx.isFrameFilled()) {
            this.handleEyePlacement(ctx);
        } else if (ctx.isHoldingNothing() && ctx.isFrameFilled()) {
            this.handleEyeRemoval(ctx);
        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        // track when the dragon dies so we can handle shutting down the plugin's effects
        // not necessarily the first dragon death, but we'll assume that people who want
        // this plugin have had it installed since the start of the server
        if (event.getEntityType() != EntityType.ENDER_DRAGON || state.getDragonDefeatStatus()) return;

        state.setDragonDefeatedStatus(true);
        persist.saveData(state);
        Bukkit.broadcast(config.getMessage(MessageKey.DRAGON_DEFEAT_NOTICE));
    }
}
