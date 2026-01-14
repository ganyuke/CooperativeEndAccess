package io.github.ganyuke.cooperativeEndAccess;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import io.github.ganyuke.cooperativeEndAccess.Config.MessageKey;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CooperativeEndAccess extends JavaPlugin implements Listener {
    private Config config;
    private Persist persist;
    private State state;
    private BukkitTask portalTask;
    private World endWorld;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        var dataFile = new File(this.getDataFolder(), "data.yml");
        try {
            if (dataFile.createNewFile()) {
                this.getLogger().info("Created " + dataFile.getName());
            } // else it already existed
        } catch (IOException e) {
            throw new RuntimeException("Failed to create " + dataFile, e);
        }

        persist = new Persist(dataFile);
        state = persist.loadData();
        config = new Config(this.getConfig());
        config.loadMessages();

        // Periodic check for player presence and portal state
        portalTask = getServer().getScheduler().runTaskTimer(this, this::updatePortals, 20L, 20L);
    }

    @Override
    public void onDisable() {
        if (portalTask != null) {
            portalTask.cancel();
            portalTask = null;
        }
        Bukkit.getScheduler().cancelTasks(this);
        org.bukkit.event.HandlerList.unregisterAll((Plugin) this);

        if (persist != null && state != null) {
            persist.saveData(state);
        }

        config = null;
        persist = null;
        state = null;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (state.getDragonDefeatStatus()) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.END_PORTAL_FRAME) return;

        EndPortalFrame frame = (EndPortalFrame) block.getBlockData();
        Location center = findPortalCenter(block.getLocation());
        if (center == null) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // override vanilla logic for side effects of placing Eye of Ender
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null && event.getItem().getType() == Material.ENDER_EYE) {
            if (!frame.hasEye()) {
                Integer currentContribution = state.getContributions(center, uuid);

                if (currentContribution >= getConfig().getInt("max_eyes_per_player")) {
                    player.sendMessage(config.getMessage(MessageKey.MAX_EYES_ERROR));
                    event.setCancelled(true);
                    return;
                }

                state.addContributor(center, uuid);
                persist.saveData(state);

                player.sendMessage(config.getMessage(MessageKey.COMMITTED_NOTICE));
                player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
            }
        }
        // support removing an Eye of Ender
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() == null && frame.hasEye()) {
            if (state.removeContributor(center, uuid)) {
                frame.setEye(false);
                block.setBlockData(frame);
                player.getInventory().addItem(new ItemStack(Material.ENDER_EYE));
                player.sendMessage(config.getMessage(MessageKey.RESCIND_WARNING));
                persist.saveData(state);
                updatePortals(); // Force immediate check
            }

        }
    }

    @EventHandler
    public void onDragonDeath(EntityDeathEvent event) {
        if (event.getEntityType() == org.bukkit.entity.EntityType.ENDER_DRAGON) {
            if (!state.getDragonDefeatStatus()) {
                state.setDragonDefeatedStatus(true);
                persist.saveData(state);
                Bukkit.getServer().broadcast(config.getMessage(MessageKey.DRAGON_DEFEAT_NOTICE));
            }
        }
    }


    private World getEndWorld() {
        if (endWorld == null) {
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == World.Environment.THE_END) {
                    endWorld = world;
                    break;
                }
            }
        }
        return endWorld;
    }

    private void updatePortals() {
        if (state.getDragonDefeatStatus()) return;

        double radiusSq = Math.pow(config.getActivationRadius(), 2);
        World end = getEndWorld();

        for (Map.Entry<Location, Map<UUID, Integer>> entry : state.getAllPortalContributors().entrySet()) {
            Location center = entry.getKey();
            Set<UUID> committed = entry.getValue().keySet();

            boolean stabilized = isStabilized(committed, end);

            boolean shouldFill = isEndPortalFilled(center) && (stabilized || areAllPlayersNearby(center, committed, radiusSq));

            fillPortal(center, shouldFill ? Material.END_PORTAL : Material.AIR);
        }
    }

    /*
        This particular method is for dragon fights. The portal will remain
        "stabilized" (open) while players committed to that portal are
        still alive in the End while the dragon has not been defeated.
     */
    private boolean isStabilized(Set<UUID> committed, World end) {
        if (end == null) return false;
        for (UUID uuid : committed) {
            Player p = Bukkit.getPlayer(uuid);
            // fast fail on first presence of player in the End
            if (p != null && p.getWorld().equals(end) && !p.isDead()) {
                return true;
            }
        }
        return false;
    }

    private boolean areAllPlayersNearby(Location center, Set<UUID> committed, double radiusSq) {
        World world = center.getWorld();
        for (UUID uuid : committed) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.getWorld().equals(world)) return false;

            // could do sqrt here, but I don't care about decimals.
            // just need to know if distance exceeds radius. avoiding
            // sqrt here should in theory be faster.
            if (p.getLocation().distanceSquared(center) > radiusSq) {
                return false;
            }
        }
        return true;
    }

    private static final int[][] FRAME_OFFSETS = {
            {2, -1}, {2, 0}, {2, 1},   // East side
            {-2, -1}, {-2, 0}, {-2, 1}, // West side
            {-1, 2}, {0, 2}, {1, 2},    // South side
            {-1, -2}, {0, -2}, {1, -2}  // North side
    };

    private boolean isEndPortalValid(Location center, boolean shouldBeFilled) {
        // calculate if End Portal frames surround the center and (optionally) are
        // all filled with eyes.
        // assumes that the End Portal is a standard 5x5 End Portal without corners.
        // assumes that all frames are facing the correct direction.
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY(); // End Portals are always horizontal, so no Y-offset needed
        int cz = center.getBlockZ();

        for (int[] offset : FRAME_OFFSETS) {
            int x = cx + offset[0];
            int z = cz + offset[1];

            Block block = world.getBlockAt(x, cy, z);
            if (block.getType() != Material.END_PORTAL_FRAME) {
                return false;
            }

            if (shouldBeFilled) {
                EndPortalFrame frame = (EndPortalFrame) block.getBlockData();
                if (!frame.hasEye()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isEndPortalFilled(Location center) {
        return isEndPortalValid(center, true);
    }

    private boolean isEndPortalCenter(Location center) {
        return isEndPortalValid(center, false);
    }

    private void fillPortal(Location center, Material material) {
        World world = center.getWorld();
        boolean changed = false;

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = center.clone().add(x, 0, z).getBlock();
                if (b.getType() != material) {
                    b.setType(material, false);
                    changed = true;
                }
            }
        }

        if (changed && material == Material.END_PORTAL) {
            world.spawnParticle(
                    Particle.DRAGON_BREATH,
                    center,
                    50,
                    1, 0, 1,
                    0.1,
                    1.0f
            );
        }
    }

    private Location findPortalCenter(Location frameLoc) {
        World world = frameLoc.getWorld();
        Block b = world.getBlockAt(frameLoc);
        if (b.getType() != Material.END_PORTAL_FRAME) return null;

        EndPortalFrame data = (EndPortalFrame) b.getBlockData();
        BlockFace facing = data.getFacing();

        int fx = b.getX();
        int fy = b.getY();
        int fz = b.getZ();

        int inwardX = facing.getModX();
        int inwardZ = facing.getModZ();

        // center of portal is always 2 blocks inward from the frame
        // so we just need to figure out the direction of the frame
        // and whether it is a north-south or east-west frame
        int centerXAxis = fx + (inwardX * 2);
        int centerZAxis = fz + (inwardZ * 2);

        // the center is now three possible locations: to the left, to the right, or in the middle
        Location[] candidates = {
                new Location(world, centerXAxis, fy, centerZAxis),
                // swap inwardX/Z to get sideways direction
                new Location(world, centerXAxis + inwardZ, fy, centerZAxis + inwardX),
                new Location(world, centerXAxis - inwardZ, fy, centerZAxis - inwardX)
        };

        for (Location loc : candidates) {
            if (isEndPortalCenter(loc)) return loc;
        }

        return null;
    }
}