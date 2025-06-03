package com.bubolee.rezero;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerListener implements Listener {
    private final ReZeroPlugin plugin;
    private final CheckpointManager checkpointManager;
    private final WorldSnapshotManager snapshotManager;

    public PlayerListener(ReZeroPlugin plugin, CheckpointManager checkpointManager, WorldSnapshotManager snapshotManager) {
        this.plugin = plugin;
        this.checkpointManager = checkpointManager;
        this.snapshotManager = snapshotManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (checkpointManager.isWhitelisted(player)) {
            checkpointManager.resetToCheckpoint(player, snapshotManager);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (checkpointManager.isWhitelisted(player) && event.getMessage().equalsIgnoreCase("return by death")) {
            checkpointManager.resetToCheckpoint(player, snapshotManager);
        }
    }
}
