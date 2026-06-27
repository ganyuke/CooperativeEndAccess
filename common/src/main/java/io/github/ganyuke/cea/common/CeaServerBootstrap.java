// SPDX-License-Identifier: MPL-2.0
// SPDX-FileCopyrightText: ganyuke
package io.github.ganyuke.cea.common;

import io.github.ganyuke.cea.core.config.Config;
import io.github.ganyuke.cea.core.config.ConfigFactory;
import io.github.ganyuke.cea.core.data.Persist;
import io.github.ganyuke.cea.core.data.State;
import io.github.ganyuke.cea.core.portal.PortalController;
import io.github.ganyuke.cea.core.portal.PortalGeometry;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class CeaServerBootstrap {
    private final Path configDir;
    private NmsPlatformFacade platform;
    private PortalController controller;
    private final Logger logger;

    private static final int TICK_INTERVAL = 10;
    private static final String CONFIG_NAME = "config.yml";
    private static final String DATA_NAME = "data.yml";

    public CeaServerBootstrap(Path configDir, Logger logger) {
        this.configDir = configDir;
        this.logger = logger;
    }

    public void start(MinecraftServer server) {
        logger.info("Enabling plugin Cooperative End Access...");
        try {
            Files.createDirectories(configDir);
            copyDefault();
            Path dataFile = configDir.resolve(DATA_NAME);
            if (Files.notExists(dataFile)) Files.createFile(dataFile);

            platform = new NmsPlatformFacade(server);
            PortalGeometry geometry = new PortalGeometry(platform);
            Persist persist = new Persist(dataFile, geometry);
            State state = persist.loadData();
            if (state.getDragonDefeatStatus()) return;
            Config config = ConfigFactory.load(Persist.loadMap(configDir.resolve(CONFIG_NAME)), logger);
            controller = new PortalController(state, persist, config, platform, geometry);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Cooperative End Access", e);
        }
    }

    public void stop() {
        logger.info("Stopping plugin Cooperative End Access...");
        controller = null;
        platform = null;
    }

    public void tick(int currentTick) {
        if (controller == null) return;

        if (currentTick % TICK_INTERVAL == 0) {
            controller.updateTrackedPortals();
        }
    }

    public PortalController controller() { return controller; }

    private void copyDefault() throws IOException {
        Path target = configDir.resolve(CONFIG_NAME);
        if (Files.exists(target)) return;
        try (InputStream input = CeaServerBootstrap.class.getClassLoader().getResourceAsStream(CONFIG_NAME)) {
            if (input == null) throw new IOException("Missing bundled " + CONFIG_NAME);
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
