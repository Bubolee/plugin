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
        // Chỉ cho phép operator (OP)
        if (!sender.isOp()) {
            sender.sendMessage("§cThis command can only be used by operators!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /rbd [setcheckpoint|add|remove|list|reload]");
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "setcheckpoint":
                if (args.length != 2) {
                    sender.sendMessage("Usage: /rbd setcheckpoint <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return true;
                }
                checkpointManager.setCheckpointForPlayer(target, target.getLocation());
                sender.sendMessage("§aCheckpoint set at " + target.getName() + "'s location!");
                break;
            case "add":
                if (args.length != 2) {
                    sender.sendMessage("Usage: /rbd add <player>");
                    return true;
                }
                Player targetAdd = Bukkit.getPlayer(args[1]);
                if (targetAdd == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return true;
                }
                checkpointManager.addToWhitelist(targetAdd);
                sender.sendMessage("§aAdded " + targetAdd.getName() + " to whitelist!");
                break;
            case "remove":
                if (args.length != 2) {
                    sender.sendMessage("Usage: /rbd remove <player>");
                    return true;
                }
                Player targetRemove = Bukkit.getPlayer(args[1]);
                if (targetRemove == null) {
                    sender.sendMessage("§cPlayer not found!");
                    return true;
                }
                checkpointManager.removeFromWhitelist(targetRemove);
                sender.sendMessage("§aRemoved " + targetRemove.getName() + " from whitelist!");
                break;
            case "list":
                List<String> whitelist = plugin.getConfig().getStringList("whitelist");
                sender.sendMessage("§aWhitelisted players: " + whitelist.toString());
                break;
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§aConfig reloaded!");
                break;
            default:
                sender.sendMessage("Usage: /rbd [setcheckpoint|add|remove|list|reload]");
        }
        return true;
    }
}
