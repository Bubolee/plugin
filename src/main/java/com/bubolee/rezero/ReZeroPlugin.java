package com.bubolee.rezero;

import org.bukkit.plugin.java.JavaPlugin;

public class ReZeroPlugin extends JavaPlugin {
    private CheckpointManager checkpointManager;
    private WorldSnapshotManager snapshotManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        checkpointManager = new CheckpointManager(this);
        snapshotManager = new WorldSnapshotManager(this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, checkpointManager, snapshotManager), this);
        getCommand("rbd").setExecutor(new CommandHandler(this, checkpointManager));
        getLogger().info("ReZero Return by Death plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ReZero Return by Death plugin disabled!");
    }

    public CheckpointManager getCheckpointManager() {
        return checkpointManager;
    }

    public WorldSnapshotManager getSnapshotManager() {
        return snapshotManager;
    }
}
