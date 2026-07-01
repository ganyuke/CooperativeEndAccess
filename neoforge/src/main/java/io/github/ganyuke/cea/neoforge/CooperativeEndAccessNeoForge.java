// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.neoforge;

import com.mojang.logging.LogUtils;
import io.github.ganyuke.cea.common.CeaServerBootstrap;
import io.github.ganyuke.cea.common.NmsPlatformFacade;
import io.github.ganyuke.cea.core.util.BlockType;
import io.github.ganyuke.cea.core.util.HandKind;
import io.github.ganyuke.cea.core.portal.PortalController;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

@Mod(CooperativeEndAccessNeoForge.MOD_ID)
public final class CooperativeEndAccessNeoForge {
    public static final String MOD_ID = "cooperativeendaccess";
    private final CeaServerBootstrap bootstrap;

    public CooperativeEndAccessNeoForge() {
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener(this::onServerTick);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(this::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(this::onLivingDeath);

        Logger LOGGER = LogUtils.getLogger();
        this.bootstrap = new CeaServerBootstrap(FMLPaths.CONFIGDIR.get().resolve(MOD_ID), LOGGER);

    }

    private void onServerStarted(ServerStartedEvent event) {
        bootstrap.start(event.getServer());
    }

    private void onServerStopping(ServerStoppingEvent event) {
        bootstrap.stop();
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level))
            return;
        BlockState state = level.getBlockState(event.getPos());
        if (!state.is(Blocks.END_PORTAL_FRAME)) return;
        PortalController controller = bootstrap.controller();
        if (controller == null) return;

        InteractionHand hand = event.getHand();
        PortalController.PortalInteraction interaction = new PortalController.PortalInteraction(
                player.getUUID(),
                player.getGameProfile().name(),
                NmsPlatformFacade.key(level, event.getPos()),
                BlockType.END_PORTAL_FRAME,
                state.getValue(EndPortalFrameBlock.HAS_EYE),
                player.getItemInHand(hand).is(Items.ENDER_EYE),
                player.getItemInHand(hand).isEmpty(),
                player.getOffhandItem().is(Items.ENDER_EYE),
                player.isCreative(),
                hand == InteractionHand.OFF_HAND ? HandKind.OFF_HAND : HandKind.MAIN_HAND
        );

        if (controller.interact(interaction).cancelled()) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private void onServerTick(ServerTickEvent.Post event) {
        bootstrap.tick(event.getServer().getTickCount());
    }

    private void onBlockBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getState().is(Blocks.END_PORTAL_FRAME)) return;
        PortalController controller = bootstrap.controller();
        if (controller != null) controller.breakBlock(NmsPlatformFacade.key(level, event.getPos()));
    }

    private void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().getType() != EntityType.ENDER_DRAGON) return;
        PortalController controller = bootstrap.controller();
        if (controller != null) controller.dragonDeath();
    }
}
