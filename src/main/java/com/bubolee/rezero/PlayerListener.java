package com.bubolee.rezero;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.entity.Player;

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
        String message = event.getMessage().toLowerCase();
        if (checkpointManager.isWhitelisted(player) && 
            (message.contains("return by death") || message.contains("trở lại từ cái chết"))) {
            checkpointManager.resetToCheckpoint(player, snapshotManager);
        }
    }
}
