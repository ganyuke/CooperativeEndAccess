// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea;

import io.github.ganyuke.cea.core.config.Config;
import io.github.ganyuke.cea.core.config.ConfigFactory;
import io.github.ganyuke.cea.core.data.Persist;
import io.github.ganyuke.cea.core.data.State;
import io.github.ganyuke.cea.core.portal.PortalController;
import io.github.ganyuke.cea.core.portal.PortalGeometry;
import io.github.ganyuke.cea.paper.PaperPlatformFacade;
import io.github.ganyuke.cea.paper.PaperPortalListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class CooperativeEndAccess extends JavaPlugin implements Listener {
    private Persist persist;
    private State state;
    private BukkitTask portalTask;
    private PortalController portalController;
    private final Logger logger = this.getSLF4JLogger();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        File dataFile = new File(getDataFolder(), "data.yml");
        try {
            if (dataFile.createNewFile()) getLogger().info("Created " + dataFile.getName());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create " + dataFile, e);
        }

        PaperPlatformFacade platform = new PaperPlatformFacade();
        PortalGeometry geometry = new PortalGeometry(platform);
        persist = new Persist(dataFile.toPath(), geometry);
        state = persist.loadData();

        if (state.getDragonDefeatStatus()) {
            getLogger().info("Plugin shutting down; `data.yml` indicates the Ender Dragon has already been defeated.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Path configPath = getDataFolder().toPath().resolve("config.yml");
        Config config = ConfigFactory.load(Persist.loadMap(configPath), logger);
        portalController = new PortalController(state, persist, config, platform, geometry);

        getServer().getPluginManager().registerEvents(new PaperPortalListener(portalController), this);
        portalTask = getServer().getScheduler().runTaskTimer(this, portalController::updateTrackedPortals, 20L, 10L);
    }

    @Override
    public void onDisable() {
        if (portalTask != null) {
            portalTask.cancel();
            portalTask = null;
        }
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll((Plugin) this);
        if (persist != null && state != null) persist.saveData(state);
        persist = null;
        state = null;
        portalController = null;
    }
}
