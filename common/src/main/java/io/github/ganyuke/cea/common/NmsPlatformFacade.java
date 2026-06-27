// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.common;

import io.github.ganyuke.cea.core.util.*;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.stream.Collectors;

public final class NmsPlatformFacade implements PlatformFacade {
    private final MinecraftServer server;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final MinecraftServerAudiences audiences;

    public NmsPlatformFacade(MinecraftServer server) {
        this.server = server;
        this.audiences = MinecraftServerAudiences.of(server);
    }

    private static final Map<String, SoundSource> SOUND_SOURCE_MAP = Arrays.stream(SoundSource.values())
            .collect(Collectors.toMap(
                    source -> source.getName().toLowerCase(Locale.ROOT),
                    source -> source
            ));

    @Override
    public Optional<PlayerSnapshot> player(UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) return Optional.empty();
        return Optional.of(new PlayerSnapshot(uuid, player.getGameProfile().name(), worldName(player.level()), player.getX(), player.getY(), player.getZ(), player.isDeadOrDying()));
    }

    @Override
    public String offlineName(UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        return online == null ? null : online.getGameProfile().name();
    }

    @Override
    public String endWorldName() {
        ServerLevel end = server.getLevel(Level.END);
        return end == null ? null : worldName(end);
    }

    @Override
    public BlockType blockType(BlockKey loc) {
        ServerLevel level = level(loc.worldName());
        if (level == null) return BlockType.OTHER;
        BlockState state = level.getBlockState(pos(loc));
        if (state.is(Blocks.AIR)) return BlockType.AIR;
        if (state.is(Blocks.END_PORTAL)) return BlockType.END_PORTAL;
        if (state.is(Blocks.END_PORTAL_FRAME)) return BlockType.END_PORTAL_FRAME;
        return BlockType.OTHER;
    }

    @Override
    public Direction endFrameFacing(BlockKey loc) {
        ServerLevel level = level(loc.worldName());
        if (level == null) return null;
        BlockState state = level.getBlockState(pos(loc));
        if (!state.is(Blocks.END_PORTAL_FRAME)) return null;
        net.minecraft.core.Direction facing = state.getValue(EndPortalFrameBlock.FACING);
        return switch (facing) {
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case EAST -> Direction.EAST;
            case WEST -> Direction.WEST;
            default -> null;
        };
    }

    @Override
    public boolean endFrameHasEye(BlockKey loc) {
        ServerLevel level = level(loc.worldName());
        if (level == null) return false;
        BlockState state = level.getBlockState(pos(loc));
        return state.is(Blocks.END_PORTAL_FRAME) && state.getValue(EndPortalFrameBlock.HAS_EYE);
    }

    @Override
    public void setEndFrameEye(BlockKey loc, boolean hasEye) {
        ServerLevel level = level(loc.worldName());
        if (level == null) return;
        BlockPos pos = pos(loc);
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.END_PORTAL_FRAME))
            level.setBlock(pos, state.setValue(EndPortalFrameBlock.HAS_EYE, hasEye), 3);
    }

    @Override
    public void setBlock(BlockKey loc, BlockType type) {
        ServerLevel level = level(loc.worldName());
        if (level == null) return;
        BlockState state = switch (type) {
            case AIR -> Blocks.AIR.defaultBlockState();
            case END_PORTAL -> Blocks.END_PORTAL.defaultBlockState();
            case END_PORTAL_FRAME -> Blocks.END_PORTAL_FRAME.defaultBlockState();
            case OTHER -> null;
        };
        if (state != null) level.setBlock(pos(loc), state, 3);
    }

    @Override
    public void sendMessage(UUID playerId, String miniMessage) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) player.sendSystemMessage(component(miniMessage));
    }

    @Override
    public void sendActionBar(UUID playerId, String miniMessage) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) player.sendSystemMessage(component(miniMessage), true);
    }

    @Override
    public void broadcast(String miniMessage) {
        server.getPlayerList().broadcastSystemMessage(component(miniMessage), false);
    }

    @Override
    public void playSoundAt(SoundSpec sound, BlockKey loc) {
        ServerLevel level = level(loc.worldName());
        if (level == null) return;
        level.playSound(null, loc.x(), loc.y(), loc.z(), soundEvent(sound), soundSource(sound.source()), sound.volume(), sound.pitch());
    }

    @Override
    public void playSoundTo(UUID playerId, SoundSpec sound) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) player.playSound(soundEvent(sound), sound.volume(), sound.pitch());
    }

    @Override
    public void playPortalFrameFill(UUID playerId, BlockKey loc) {
        ServerLevel level = level(loc.worldName());
        if (level != null)
            level.playSound(null, loc.x(), loc.y(), loc.z(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    @Override
    public void spawnPortalOpenParticles(BlockKey center) {
        ServerLevel level = level(center.worldName());
        if (level != null)
            level.sendParticles(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0f), center.x(), center.y(), center.z(), 50, 1, 0, 1, 0.1);
    }

    @Override
    public void spawnGuidanceParticles(UUID playerId, BlockKey center) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) return;
        Vec3 start = player.position().add(0, 1.2, 0);
        Vec3 target = new Vec3(center.x(), center.y(), center.z());
        Vec3 dir = target.subtract(start).normalize().scale(0.5);
        ServerLevel level = player.level();
        Vec3 pos = start;
        for (int i = 0; i < 3; i++) {
            pos = pos.add(dir);
            level.sendParticles(ParticleTypes.WITCH, pos.x(), pos.y(), pos.z(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    public void giveEyeOrDrop(UUID playerId) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) return;
        ItemStack eye = new ItemStack(Items.ENDER_EYE);
        if (!player.getInventory().add(eye)) {
            ItemEntity item = player.drop(eye, false);
            if (item != null) item.setNoPickUpDelay();
        }
    }

    @Override
    public void consumeInteractionItem(UUID playerId, HandKind hand) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) return;
        player.getItemInHand(hand == HandKind.MAIN_HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND).shrink(1);
    }

    public ServerLevel level(String worldName) {
        for (ServerLevel level : server.getAllLevels()) if (worldName(level).equals(worldName)) return level;
        return null;
    }

    public static String worldName(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    public static BlockKey key(ServerLevel level, BlockPos pos) {
        return new BlockKey(worldName(level), pos.getX(), pos.getY(), pos.getZ());
    }

    private static BlockPos pos(BlockKey loc) {
        return new BlockPos(loc.x(), loc.y(), loc.z());
    }

    private Component component(String text) {
        return audiences.asNative(miniMessage.deserialize(text));
    }

    private static SoundEvent soundEvent(SoundSpec spec) {
        Identifier id = Identifier.parse(spec.key());
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(id);
        return sound != null ? sound : SoundEvents.NOTE_BLOCK_BASS.value();
    }

    private static SoundSource soundSource(SoundCategory kind) {
        return switch (kind) {
            case MASTER -> SoundSource.MASTER;
            case MUSIC -> SoundSource.MUSIC;
            case RECORDS -> SoundSource.RECORDS;
            case WEATHER -> SoundSource.WEATHER;
            case BLOCKS -> SoundSource.BLOCKS;
            case HOSTILE -> SoundSource.HOSTILE;
            case NEUTRAL -> SoundSource.NEUTRAL;
            case PLAYERS -> SoundSource.PLAYERS;
            case AMBIENT -> SoundSource.AMBIENT;
            case VOICE -> SoundSource.VOICE;
        };
    }
}
