package com.bubolee.rezero;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ReZeroPlugin extends JavaPlugin {
    private CheckpointManager checkpointManager;
    private WorldSnapshotManager snapshotManager;
    private Random random;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        checkpointManager = new CheckpointManager(this);
        snapshotManager = new WorldSnapshotManager(this);
        random = new Random();
        getServer().getPluginManager().registerEvents(new PlayerListener(this, checkpointManager, snapshotManager), this);
        getCommand("rbd").setExecutor(new CommandHandler(this, checkpointManager));
        startAutoCheckpointTask();
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

    private void startAutoCheckpointTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<String> whitelist = getConfig().getStringList("whitelist");
                if (whitelist.isEmpty()) {
                    getLogger().info("No players in whitelist, skipping auto-checkpoint."); // Thông báo panel
                    scheduleNextRun();
                    return;
                }

                boolean checkpointSet = false;
                for (String uuidStr : whitelist) {
                    UUID uuid = UUID.fromString(uuidStr);
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        checkpointManager.setCheckpointForPlayer(player, player.getLocation());
                        snapshotManager.takeSnapshotForPlayer(player, player.getLocation());
                        player.sendMessage("§eA new checkpoint has been set at your location (auto-set).");
                        checkpointSet = true;
                    }
                }

                if (!checkpointSet) {
                    getLogger().info("No whitelisted players online, skipping checkpoint."); // Thông báo panel
                } else {
                    getLogger().info("Auto-checkpoint set for online whitelisted players."); // Thông báo panel
                }

                scheduleNextRun();
            }

            private void scheduleNextRun() {
                long nextRunTicks = (random.nextInt(60) + 30) * 20L; // 30-90 giây
                runTaskLater(ReZeroPlugin.this, nextRunTicks);
            }
        }.runTaskLater(this, 20L);
    }
}
