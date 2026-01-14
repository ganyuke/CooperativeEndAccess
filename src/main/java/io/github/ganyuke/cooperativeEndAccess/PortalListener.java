package io.github.ganyuke.cooperativeEndAccess;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import io.github.ganyuke.cooperativeEndAccess.Config.MessageKey;

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

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // this plugin doesn't matter if the dragon is dead
        if (state.getDragonDefeatStatus()) return;
        // the follow actions don't matter if it wasn't a right click
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.END_PORTAL_FRAME) return;

        Location center = Util.findPortalCenter(block.getLocation());
        if (center == null) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        EndPortalFrame frame = (EndPortalFrame) block.getBlockData();

        if (isEye(event.getItem())) {
            // PLACING AN EYE
            if (!frame.hasEye()) {
                if (state.getContributions(center, uuid) >= config.getMaxEyesPerPlayer()) {
                    player.sendMessage(config.getMessage(MessageKey.MAX_EYES_ERROR));
                    event.setCancelled(true);
                    return;
                }

                // placing the last eye in vanilla creates the Ender Portal,
                // so we need to stop that from happening to (1) prevent
                // the actual ender portal from spawning without our green light
                // and (2) prevent the End Portal opening sound from automatically playing
                if (Util.countEndFrameEyes(center) == 11) {
                    // cancel the placement event to prevent vanilla logic from running
                    event.setCancelled(true);

                    // manually replicate what placing an eye would usually do
                    frame.setEye(true);
                    block.setBlockData(frame);
                    ItemStack item = event.getItem();
                    if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
                        item.setAmount(item.getAmount() - 1);
                    }
                    event.getPlayer().playSound(block.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
                }

                state.addContributor(center, uuid);
                persist.saveData(state);
                player.sendMessage(config.getMessage(MessageKey.COMMITTED_NOTICE));
            }
        } else if (event.getItem() == null && frame.hasEye()) {
            // REMOVING AN EYE
            if (state.removeContributor(center, uuid)) {
                frame.setEye(false);
                block.setBlockData(frame);
                player.getInventory().addItem(new ItemStack(Material.ENDER_EYE));
                player.sendMessage(config.getMessage(MessageKey.RESCIND_WARNING));
                persist.saveData(state);
                portalManager.update();
            }
        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() == EntityType.ENDER_DRAGON) {
            if (!state.getDragonDefeatStatus()) {
                state.setDragonDefeatedStatus(true);
                persist.saveData(state);
                Bukkit.broadcast(config.getMessage(MessageKey.DRAGON_DEFEAT_NOTICE));
            }
        }
    }

    private boolean isEye(ItemStack item) {
        return item != null && item.getType() == Material.ENDER_EYE;
    }
}
