package com.whoslucid.cobblemarket;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.whoslucid.cobblelib.CobbleLib;
import com.whoslucid.cobblemarket.command.CommandTree;
import com.whoslucid.cobblemarket.config.Config;
import com.whoslucid.cobblemarket.config.Lang;
import com.whoslucid.cobblemarket.listing.ListingManager;
import com.whoslucid.cobblemarket.history.HistoryManager;
import com.whoslucid.cobblemarket.moderation.TimeoutManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Mod(CobbleMarket.MOD_ID)
public class CobbleMarket {
    public static final String MOD_ID = "cobblemarket";
    public static final String MOD_NAME = "CobbleMarket";
    public static final String PATH = "/config/cobblemarket";
    public static final String PATH_LANG = "/config/cobblemarket/lang/";
    public static final String PATH_LISTINGS = "/config/cobblemarket/listings/";
    public static final String PATH_EXPIRED = "/config/cobblemarket/expired/";
    public static final String PATH_HISTORY = "/config/cobblemarket/history/";

    public static MinecraftServer server;
    public static Config config = new Config();
    public static Lang language = new Lang();
    public static ListingManager listingManager;
    public static HistoryManager historyManager;
    public static TimeoutManager timeoutManager;

    public static final Executor EXECUTOR = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("cobblemarket-%d").setDaemon(true).build()
    );

    public static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2,
            new ThreadFactoryBuilder().setNameFormat("cobblemarket-scheduler-%d").setDaemon(true).build()
    );

    private int tickCounter = 0;
    private static final int TICKS_PER_EXPIRATION_CHECK = 1200; // Check every 60 seconds (20 ticks/sec * 60)

    public CobbleMarket(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
    }

    public static void load() {
        CobbleLib.info(MOD_NAME, "1.0.0", "WhosLucid");
        config.init();
        language.init();

        // Initialize managers
        listingManager = new ListingManager();
        historyManager = new HistoryManager();
        timeoutManager = new TimeoutManager();

        // Load data
        listingManager.loadAll();
        timeoutManager.load();
    }

    public static void reload() {
        config.init();
        language.init();
        CobbleLib.LOGGER.info("CobbleMarket configuration reloaded.");
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        load();

        // Schedule auction checks
        SCHEDULER.scheduleAtFixedRate(() -> {
            if (server != null && listingManager != null) {
                server.execute(() -> listingManager.checkAuctionEndings());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        // Save all data
        if (listingManager != null) {
            listingManager.saveAll();
        }
        if (timeoutManager != null) {
            timeoutManager.save();
        }

        // Shutdown executors
        SCHEDULER.shutdown();
        try {
            if (!SCHEDULER.awaitTermination(5, TimeUnit.SECONDS)) {
                SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            SCHEDULER.shutdownNow();
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= TICKS_PER_EXPIRATION_CHECK) {
            tickCounter = 0;
            if (listingManager != null) {
                listingManager.checkExpirations();
            }
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandTree.register(event.getDispatcher());
    }
}
