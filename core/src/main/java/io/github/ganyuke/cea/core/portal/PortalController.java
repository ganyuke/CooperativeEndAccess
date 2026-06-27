// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.core.portal;

import io.github.ganyuke.cea.core.config.Config;
import io.github.ganyuke.cea.core.config.MessageKey;
import io.github.ganyuke.cea.core.config.SoundKey;
import io.github.ganyuke.cea.core.data.Persist;
import io.github.ganyuke.cea.core.data.State;
import io.github.ganyuke.cea.core.util.BlockKey;
import io.github.ganyuke.cea.core.util.BlockType;
import io.github.ganyuke.cea.core.util.HandKind;
import io.github.ganyuke.cea.core.util.PlatformFacade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PortalController {
    private final State state;
    private final Persist persist;
    private final Config config;
    private final PlatformFacade platform;
    private final PortalGeometry geometry;
    private final Map<BlockKey, Set<UUID>> lastKnownNearby = new HashMap<>();
    private final Map<BlockKey, Boolean> portalPhysicalState = new HashMap<>();
    private final Set<UUID> debounce = new HashSet<>();

    public PortalController(State state, Persist persist, Config config, PlatformFacade platform, PortalGeometry geometry) {
        this.state = state;
        this.persist = persist;
        this.config = config;
        this.platform = platform;
        this.geometry = geometry;
    }

    public InteractionDecision interact(PortalInteraction interaction) {
        if (state.getDragonDefeatStatus()) return InteractionDecision.pass();
        if (interaction.blockType() != BlockType.END_PORTAL_FRAME) return InteractionDecision.pass();

        if (interaction.hand() == HandKind.OFF_HAND && debounce.contains(interaction.playerId())) {
            debounce.remove(interaction.playerId());
            return InteractionDecision.cancel();
        }

        if (interaction.hand() == HandKind.MAIN_HAND) {
            if (interaction.holdingEye() && !interaction.frameFilled()) return handleEyePlacement(interaction);
            if (interaction.holdingNothing() && interaction.frameFilled()) {
                InteractionDecision decision = handleEyeRemoval(interaction);
                if (interaction.offHandHasEye()) debounce.add(interaction.playerId());
                return decision;
            }
        } else if (interaction.holdingEye() && !interaction.frameFilled()) {
            return handleEyePlacement(interaction);
        }
        return InteractionDecision.pass();
    }

    private InteractionDecision handleEyePlacement(PortalInteraction interaction) {
        BlockKey center = geometry.findPortalCenter(interaction.frameLoc());

        // if they ARE looking at an end frame, but it isn't a real portal,
        // I personally don't want to handle that
        // it's OK for removal though
        if (center == null) return InteractionDecision.pass();

        UUID uuid = interaction.playerId();

        // at this point, we know that the player is trying to place an eye in a frame
        // but we can early-exit if their eye count is already over the maximum
        if (state.countFramesOwnedBy(center, uuid) >= config.maxEyesPerPlayer()) {
            platform.sendMessage(uuid, config.getMessage(MessageKey.MAX_EYES_ERROR));
            return InteractionDecision.cancel();
        }

        // placing the last eye in vanilla creates the Ender Portal,
        // so we need to stop that from happening to (1) prevent
        // the actual ender portal from spawning without our green light
        // and (2) prevent the End Portal opening sound from automatically playing
        if (geometry.countEndFrameEyes(center) == 11) {
            platform.setEndFrameEye(interaction.frameLoc(), true);
            if (!interaction.creative()) platform.consumeInteractionItem(uuid, interaction.hand());
            platform.playPortalFrameFill(uuid, interaction.frameLoc());
        }

        state.addEye(center, interaction.frameLoc(), uuid);
        state.addName(uuid, interaction.playerName());
        persist.saveData(state);
        platform.sendMessage(uuid, config.getMessage(MessageKey.COMMITTED_NOTICE));
        return geometry.countEndFrameEyes(center) == 12 ? InteractionDecision.cancel() : InteractionDecision.pass();
    }

    private InteractionDecision handleEyeRemoval(PortalInteraction interaction) {
        UUID uuid = interaction.playerId();
        BlockKey frameLoc = interaction.frameLoc();
        UUID ownerId = state.getEyeOwner(frameLoc);
        if (ownerId == null) return InteractionDecision.cancel();

        if (!ownerId.equals(uuid)) {
            String ownerName = state.getName(ownerId);
            if (ownerName == null) ownerName = platform.offlineName(ownerId);
            platform.sendMessage(uuid, config.getNonOwnerRescindWarning(ownerName == null ? "Offline Player" : ownerName));
            return InteractionDecision.cancel();
        }

        if (state.removeEyeOwner(frameLoc)) {
            platform.setEndFrameEye(frameLoc, false);
            if (!interaction.creative()) platform.giveEyeOrDrop(uuid);
            platform.sendMessage(uuid, config.getMessage(MessageKey.RESCIND_WARNING));
            persist.saveData(state);
            updateTrackedPortals();
        }
        return InteractionDecision.cancel();
    }

    public void breakBlock(BlockKey frameLoc) {
        if (state.getDragonDefeatStatus()) return;
        if (platform.blockType(frameLoc) != BlockType.END_PORTAL_FRAME) return;
        BlockKey center = geometry.findPortalCenter(frameLoc);
        boolean success = state.removeEyeOwner(frameLoc);
        if (!success) return;
        if (center != null) {
            forgetPortal(center);
            updateTrackedPortals();
        }
        persist.saveData(state);
    }

    public void dragonDeath() {
        if (state.getDragonDefeatStatus()) return;
        openAllTrackedPortals();
        state.setDragonDefeatedStatus(true);
        persist.saveData(state);
        platform.broadcast(config.getMessage(MessageKey.DRAGON_DEFEAT_NOTICE));
    }

    public void openAllTrackedPortals() {
        for (BlockKey center : state.getPortalCenters()) {
            lastKnownNearby.remove(center);
            if (geometry.isEndPortalFilled(center)) updatePhysicalPortal(center, true);
        }
    }

    public void forgetPortal(BlockKey center) {
        lastKnownNearby.remove(center);
        updatePhysicalPortal(center, false);
        state.removePortalCenter(center);
    }

    public void updateTrackedPortals() {
        if (state.getDragonDefeatStatus()) return;
        double activationRadiusSq = Math.pow(config.activationRadius(), 2);
        double actionBarRadiusSq = Math.pow(config.actionBarRadius(), 2);
        String endWorldName = platform.endWorldName();

        for (BlockKey center : new HashSet<>(state.getPortalCenters())) {
            Set<UUID> committed = state.getCommittedPlayers(center);
            if (!geometry.isEndPortalFilled(center)) {
                updatePhysicalPortal(center, false);
                lastKnownNearby.remove(center);
                continue;
            }

            PortalResult result = evaluatePortalStatus(center, committed, endWorldName, activationRadiusSq);
            updatePhysicalPortal(center, result.isActive);
            handleFeedback(center, committed, result, actionBarRadiusSq);
            lastKnownNearby.put(center, result.nearbyPlayers);
        }
    }

    private PortalResult evaluatePortalStatus(BlockKey center, Set<UUID> committed, String endWorldName, double radiusSq) {
        PortalResult res = new PortalResult();
        Set<UUID> portalContributors = state.getCommittedPlayers(center);
        for (UUID id : portalContributors) {
            Optional<PlatformFacade.PlayerSnapshot> maybePlayer = platform.player(id);
            if (maybePlayer.isEmpty()) {
                String playerName = state.getName(id);
                res.missingNames.add(playerName == null ? "Offline Player" : playerName);
                continue;
            }
            PlatformFacade.PlayerSnapshot p = maybePlayer.get();
            if (p.worldName().equals(endWorldName) && !p.dead()) res.stabilizerName = p.name();
            if (config.isUnlimitedActivation() || (p.worldName().equals(center.worldName()) && p.distanceSquared(center) <= radiusSq)) {
                res.nearbyPlayers.add(id);
            } else {
                res.missingNames.add(p.name());
            }
        }
        res.isActive = res.stabilizerName != null || res.nearbyPlayers.size() == committed.size();
        return res;
    }

    private void handleFeedback(BlockKey center, Set<UUID> committed, PortalResult result, double actionBarRadiusSq) {
        Set<UUID> previousNearby = lastKnownNearby.getOrDefault(center, Collections.emptySet());
        for (UUID id : committed) {
            Optional<PlatformFacade.PlayerSnapshot> maybePlayer = platform.player(id);
            if (maybePlayer.isEmpty()) continue;
            PlatformFacade.PlayerSnapshot player = maybePlayer.get();
            if (!player.worldName().equals(center.worldName())) continue;
            double distSq = player.distanceSquared(center);
            if (result.nearbyPlayers.contains(id)) {
                platform.spawnGuidanceParticles(id, center);
                if (!previousNearby.contains(id)) platform.playSoundAt(config.getSound(SoundKey.PLAYER_ENTER), center);
            } else if (previousNearby.contains(id)) {
                platform.playSoundAt(config.getSound(SoundKey.PLAYER_LEAVE), center);
            }
            if (config.isUnlimitedActionBar() || distSq <= actionBarRadiusSq) {
                platform.sendActionBar(id, statusMessage(result));
            }
        }
    }

    private String statusMessage(PortalResult result) {
        if (result.stabilizerName != null) return config.getStabilizedActionBar(result.stabilizerName);
        if (result.isActive) return config.getMessage(MessageKey.ACTIVE_ACTION_BAR);
        return config.getWaitingActionBar(String.join(", ", result.missingNames));
    }

    private void fillPortal(BlockKey center, BlockType type) {
        for (int offX = -1; offX <= 1; offX++) {
            for (int offZ = -1; offZ <= 1; offZ++) {
                BlockKey loc = new BlockKey(center.worldName(), center.x() + offX, center.y(), center.z() + offZ);
                if (platform.blockType(loc) != type) platform.setBlock(loc, type);
            }
        }
    }

    private void updatePhysicalPortal(BlockKey center, boolean shouldBeOpen) {
        if (!portalPhysicalState.containsKey(center)) {
            boolean isActuallyOpenInWorld = platform.blockType(center) == BlockType.END_PORTAL;
            portalPhysicalState.put(center, isActuallyOpenInWorld);
        }
        boolean wasOpen = portalPhysicalState.getOrDefault(center, false);
        if (shouldBeOpen != wasOpen) {
            if (shouldBeOpen) {
                fillPortal(center, BlockType.END_PORTAL);
                platform.playSoundAt(config.getSound(SoundKey.PORTAL_OPEN), center);
                platform.spawnPortalOpenParticles(center);
            } else {
                fillPortal(center, BlockType.AIR);
                platform.playSoundAt(config.getSound(SoundKey.PORTAL_CLOSE), center);
            }
            portalPhysicalState.put(center, shouldBeOpen);
        }
    }

    private static final class PortalResult {
        boolean isActive;
        String stabilizerName;
        final Set<UUID> nearbyPlayers = new HashSet<>();
        final List<String> missingNames = new ArrayList<>();
    }

    public record PortalInteraction(
            UUID playerId,
            String playerName,
            BlockKey frameLoc,
            BlockType blockType,
            boolean frameFilled, boolean holdingEye, boolean holdingNothing,
            boolean offHandHasEye, boolean creative, HandKind hand) {
    }

    public record InteractionDecision(boolean cancelled) {
        public static InteractionDecision pass() {
            return new InteractionDecision(false);
        }

        public static InteractionDecision cancel() {
            return new InteractionDecision(true);
        }
    }
}
