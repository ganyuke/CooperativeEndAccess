// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cooperativeEndAccess;

import io.github.ganyuke.cooperativeEndAccess.config.Config;
import io.github.ganyuke.cooperativeEndAccess.data.Persist;
import io.github.ganyuke.cooperativeEndAccess.data.State;
import io.github.ganyuke.cooperativeEndAccess.portal.PortalListener;
import io.github.ganyuke.cooperativeEndAccess.portal.PortalManager;

import org.bukkit.*;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;

public class CooperativeEndAccess extends JavaPlugin implements Listener {
    private Config config;
    private Persist persist;
    private State state;
    private BukkitTask portalTask;
    private PortalManager portalManager;

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

        if (state.getDragonDefeatStatus()) {
            this.getLogger().info("Plugin shutting down; `data.yml` indicates the Ender Dragon has already been defeated.");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        config = new Config(this.getConfig());
        config.loadMessages();
        config.loadSounds();

        this.portalManager = new PortalManager(state, config);

        getServer().getPluginManager().registerEvents(new PortalListener(state, persist, config, portalManager), this);

        portalTask = getServer().getScheduler().runTaskTimer(this, portalManager::updateTrackedPortals, 20L, 10L);
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
        portalManager = null;
    }

}