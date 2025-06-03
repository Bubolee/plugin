package com.bubolee.rezero;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.List;

public class CommandHandler implements CommandExecutor {
    private final ReZeroPlugin plugin;
    private final CheckpointManager checkpointManager;

    public CommandHandler(ReZeroPlugin plugin, CheckpointManager checkpointManager) {
        this.plugin = plugin;
        this.checkpointManager = checkpointManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage("Usage: /rbd [setcheckpoint|add|remove|list|reload]");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "setcheckpoint":
                checkpointManager.setCheckpointForPlayer(player, player.getLocation()); // Sửa: Dùng setCheckpointForPlayer
                player.sendMessage("§aCheckpoint set at your location!");
                break;
            case "add":
                if (args.length != 2) {
                    player.sendMessage("Usage: /rbd add <player>");
                    return true;
                }
                Player targetAdd = Bukkit.getPlayer(args[1]);
                if (targetAdd == null) {
                    player.sendMessage("§cPlayer not found!");
                    return true;
                }
                checkpointManager.addToWhitelist(targetAdd);
                break;
            case "remove":
                if (args.length != 2) {
                    player.sendMessage("Usage: /rbd remove <player>");
                    return true;
                }
                Player targetRemove = Bukkit.getPlayer(args[1]);
                if (targetRemove == null) {
                    player.sendMessage("§cPlayer not found!");
                    return true;
                }
                checkpointManager.removeFromWhitelist(targetRemove);
                break;
            case "list":
                List<String> whitelist = plugin.getConfig().getStringList("whitelist");
                player.sendMessage("§aWhitelisted players: " + whitelist.toString());
                break;
            case "reload":
                plugin.reloadConfig();
                player.sendMessage("§aConfig reloaded!");
                break;
            default:
                player.sendMessage("Usage: /rbd [setcheckpoint|add|remove|list|reload]");
        }
        return true;
    }
}
