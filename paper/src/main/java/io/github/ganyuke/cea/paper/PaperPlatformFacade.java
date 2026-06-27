// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.paper;

import io.github.ganyuke.cea.core.util.BlockKey;
import io.github.ganyuke.cea.core.util.BlockType;
import io.github.ganyuke.cea.core.util.Direction;
import io.github.ganyuke.cea.core.util.HandKind;
import io.github.ganyuke.cea.core.util.PlatformFacade;
import io.github.ganyuke.cea.core.util.SoundSpec;
import net.kyori.adventure.key.InvalidKeyException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.EndPortalFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public final class PaperPlatformFacade implements PlatformFacade {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final boolean below1_21_9;

    public PaperPlatformFacade() {
        this.below1_21_9 = checkIsBelow1_21_9();
    }

    private boolean checkIsBelow1_21_9() {
        String[] parts = Bukkit.getMinecraftVersion().split("\\.");
        int minor = Integer.parseInt(parts[1]);
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return minor == 21 && patch < 9;
    }

    @Override
    public Optional<PlayerSnapshot> player(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return Optional.empty();
        return Optional.of(new PlayerSnapshot(uuid, p.getName(), p.getWorld().getName(), p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ(), p.isDead()));
    }

    @Override
    public String offlineName(UUID uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName();
    }

    @Override
    public String endWorldName() {
        return Bukkit.getWorlds().stream().filter(w -> w.getEnvironment() == World.Environment.THE_END).map(World::getName).findFirst().orElse(null);
    }

    @Override
    public BlockType blockType(BlockKey loc) {
        World world = Bukkit.getWorld(loc.worldName());
        if (world == null) return BlockType.OTHER;
        Material type = world.getBlockAt(loc.x(), loc.y(), loc.z()).getType();
        if (type == Material.AIR) return BlockType.AIR;
        if (type == Material.END_PORTAL) return BlockType.END_PORTAL;
        if (type == Material.END_PORTAL_FRAME) return BlockType.END_PORTAL_FRAME;
        return BlockType.OTHER;
    }

    @Override
    public Direction endFrameFacing(BlockKey loc) {
        EndPortalFrame frame = frameAt(loc);
        if (frame == null) return null;
        return switch (frame.getFacing()) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
            default -> null;
        };
    }

    @Override
    public boolean endFrameHasEye(BlockKey loc) {
        EndPortalFrame frame = frameAt(loc);
        return frame != null && frame.hasEye();
    }

    @Override
    public void setEndFrameEye(BlockKey loc, boolean hasEye) {
        World world = Bukkit.getWorld(loc.worldName());
        if (world == null) return;
        Block block = world.getBlockAt(loc.x(), loc.y(), loc.z());
        if (!(block.getBlockData() instanceof EndPortalFrame frame)) return;
        frame.setEye(hasEye);
        block.setBlockData(frame);
    }

    @Override
    public void setBlock(BlockKey loc, BlockType type) {
        World world = Bukkit.getWorld(loc.worldName());
        if (world == null) return;
        Material material = switch (type) {
            case AIR -> Material.AIR;
            case END_PORTAL -> Material.END_PORTAL;
            case END_PORTAL_FRAME -> Material.END_PORTAL_FRAME;
            case OTHER -> null;
        };
        if (material != null) world.getBlockAt(loc.x(), loc.y(), loc.z()).setType(material, false);
    }

    @Override
    public void sendMessage(UUID playerId, String miniMessageText) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) player.sendMessage(component(miniMessageText));
    }

    @Override
    public void sendActionBar(UUID playerId, String miniMessageText) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) player.sendActionBar(component(miniMessageText));
    }

    @Override
    public void broadcast(String miniMessageText) {
        Bukkit.broadcast(component(miniMessageText));
    }

    @Override
    public void playSoundAt(SoundSpec sound, BlockKey loc) {
        World world = Bukkit.getWorld(loc.worldName());
        if (world == null) return;
        world.playSound(sound(sound), loc.x(), loc.y(), loc.z());
    }

    @Override
    public void playSoundTo(UUID playerId, SoundSpec sound) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) player.playSound(sound(sound), Sound.Emitter.self());
    }

    @Override
    public void playPortalFrameFill(UUID playerId, BlockKey loc) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        player.playSound(new Location(player.getWorld(), loc.x(), loc.y(), loc.z()), org.bukkit.Sound.BLOCK_END_PORTAL_FRAME_FILL, 1f, 1f);
    }

    @Override
    public void spawnPortalOpenParticles(BlockKey center) {
        World world = Bukkit.getWorld(center.worldName());
        if (world == null) return;
        Location location = new Location(world, center.x(), center.y(), center.z());
        if (below1_21_9) {
            world.spawnParticle(Particle.DRAGON_BREATH, location, 50, 1, 0, 1, 0.1, null);
        } else {
            world.spawnParticle(Particle.DRAGON_BREATH, location, 50, 1, 0, 1, 0.1, 1.0f);
        }
    }

    @Override
    public void spawnGuidanceParticles(UUID playerId, BlockKey center) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        Location target = new Location(player.getWorld(), center.x(), center.y(), center.z());
        Location start = player.getLocation().add(0, 1.2, 0);
        Vector dir = target.toVector().subtract(start.toVector()).normalize().multiply(0.5);
        for (int i = 0; i < 3; i++) player.getWorld().spawnParticle(Particle.WITCH, start.add(dir), 1, 0, 0, 0, 0);
    }

    @Override
    public void giveEyeOrDrop(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(Material.ENDER_EYE));
        for (ItemStack item : leftover.values()) player.getWorld().dropItemNaturally(player.getLocation(), item);
    }

    @Override
    public void consumeInteractionItem(UUID playerId, HandKind hand) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;
        EquipmentSlot slot = hand == HandKind.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND;
        ItemStack item = player.getInventory().getItem(slot);
        if (item.getType() != Material.AIR) item.setAmount(item.getAmount() - 1);
    }

    private EndPortalFrame frameAt(BlockKey loc) {
        World world = Bukkit.getWorld(loc.worldName());
        if (world == null) return null;
        Block block = world.getBlockAt(loc.x(), loc.y(), loc.z());
        return block.getBlockData() instanceof EndPortalFrame frame ? frame : null;
    }

    private Component component(String text) {
        return miniMessage.deserialize(text);
    }

    private Sound sound(SoundSpec spec) {
        Key key;
        try {
            key = Key.key(spec.key());
        } catch (InvalidKeyException e) {
            key = Key.key("minecraft:block.note_block.bass");
        }
        Sound.Source source = Sound.Source.NAMES.value(spec.source().name().toLowerCase());
        if (source == null) source = Sound.Source.BLOCK;
        return Sound.sound(key, source, spec.volume(), spec.pitch());
    }
}
