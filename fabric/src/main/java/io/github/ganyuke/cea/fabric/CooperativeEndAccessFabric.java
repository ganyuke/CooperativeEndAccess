// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke

package io.github.ganyuke.cea.fabric;

import io.github.ganyuke.cea.common.CeaServerBootstrap;
import io.github.ganyuke.cea.common.NmsPlatformFacade;
import io.github.ganyuke.cea.core.util.BlockType;
import io.github.ganyuke.cea.core.util.HandKind;
import io.github.ganyuke.cea.core.portal.PortalController;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CooperativeEndAccessFabric implements ModInitializer {
    public static final String MOD_ID = "cooperativeendaccess";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private final CeaServerBootstrap bootstrap = new CeaServerBootstrap(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID), LOGGER);

    @Override
    public void onInitialize() {
        // hook core into server lifecycle
        ServerLifecycleEvents.SERVER_STARTED.register(bootstrap::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> bootstrap.stop());
        ServerTickEvents.END_SERVER_TICK.register(server -> bootstrap.tick(server.getTickCount()));

        /*
         * Listen for eye placement events so we can get intercept them.
         */
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
                return InteractionResult.PASS;
            }
            BlockState state = level.getBlockState(hitResult.getBlockPos());
            if (!state.is(Blocks.END_PORTAL_FRAME)) return InteractionResult.PASS;
            PortalController controller = bootstrap.controller();
            if (controller == null) return InteractionResult.PASS;

            boolean holdingEye = serverPlayer.getItemInHand(hand).is(Items.ENDER_EYE);
            boolean holdingNothing = serverPlayer.getItemInHand(hand).isEmpty();
            boolean offHandHasEye = serverPlayer.getOffhandItem().is(Items.ENDER_EYE);
            PortalController.PortalInteraction interaction = new PortalController.PortalInteraction(
                    serverPlayer.getUUID(),
                    serverPlayer.getGameProfile().name(),
                    NmsPlatformFacade.key(level, hitResult.getBlockPos()),
                    BlockType.END_PORTAL_FRAME,
                    state.getValue(EndPortalFrameBlock.HAS_EYE),
                    holdingEye,
                    holdingNothing,
                    offHandHasEye,
                    serverPlayer.isCreative(),
                    hand == InteractionHand.OFF_HAND ? HandKind.OFF_HAND : HandKind.MAIN_HAND
            );
            return controller.interact(interaction).cancelled() ? InteractionResult.FAIL : InteractionResult.PASS;
        });

        /*
         * Listen for block break events to update the End Portal.
         */
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level) || !state.is(Blocks.END_PORTAL_FRAME)) return;
            PortalController controller = bootstrap.controller();
            if (controller != null) controller.breakBlock(NmsPlatformFacade.key(level, pos));
        });

        /*
         * Listen for when the Ender Dragon dies so we can activate permanent portal opening.
         * For some reason this event only fires when you actually punch the dragon to death. It
         * doesn't fire if you use `/kill @e[type=minecraft:ender_dragon]`. So I guess don't do that.
         */
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity.getType() != EntityType.ENDER_DRAGON) return;
            PortalController controller = bootstrap.controller();
            if (controller != null) controller.dragonDeath();
        });
    }
}