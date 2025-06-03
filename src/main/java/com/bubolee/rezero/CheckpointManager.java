package com.bubolee.rezero;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class CheckpointManager {
    private final ReZeroPlugin plugin;
    private Map<String, Location> worldCheckpoints;
    private Map<String, HashMap<UUID, PlayerState>> worldPlayerStates;
    private File playerDataFile;
    private FileConfiguration playerDataConfig;
    private Map<String, Long> worldLastCheckpointTimes;
    private List<UUID> whitelist;

    public CheckpointManager(ReZeroPlugin plugin) {
        this.plugin = plugin;
        this.worldCheckpoints = new HashMap<>();
        this.worldPlayerStates = new HashMap<>();
        this.worldLastCheckpointTimes = new HashMap<>();
        this.whitelist = new ArrayList<>();
        this.playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            plugin.saveResource("playerdata.yml", false);
        }
        this.playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        loadWhitelist();
    }

    private void loadWhitelist() {
        List<String> uuidStrings = plugin.getConfig().getStringList("whitelist");
        for (String uuidStr : uuidStrings) {
            whitelist.add(UUID.fromString(uuidStr));
        }
    }

    public void setCheckpoint(Player player) {
        String worldName = player.getWorld().getName();
        Location checkpointLocation = player.getLocation();
        worldCheckpoints.put(worldName, checkpointLocation);
        worldLastCheckpointTimes.put(worldName, System.currentTimeMillis());

        HashMap<UUID, PlayerState> playerStates = new HashMap<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Player p : player.getWorld().getPlayers()) {
                UUID uuid = p.getUniqueId();
                playerStates.put(uuid, new PlayerState(p));
                playerDataConfig.set(worldName + "." + uuid.toString() + ".location", checkpointLocation);
                playerDataConfig.set(worldName + "." + uuid.toString() + ".health", p.getHealth());
                playerDataConfig.set(worldName + "." + uuid.toString() + ".foodLevel", p.getFoodLevel());
                playerDataConfig.set(worldName + "." + uuid.toString() + ".exp", p.getExp());
                playerDataConfig.set(worldName + "." + uuid.toString() + ".inventory", p.getInventory().getContents());
                playerDataConfig.set(worldName + "." + uuid.toString() + ".armor", p.getInventory().getArmorContents());
            }
            worldPlayerStates.put(worldName, playerStates);
            try {
                playerDataConfig.save(playerDataFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save player data: " + e.getMessage());
            }
        });
    }

    public void resetToCheckpoint(Player triggerPlayer, WorldSnapshotManager snapshotManager) {
        String worldName = triggerPlayer.getWorld().getName();
        if (!worldCheckpoints.containsKey(worldName)) return;

        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (countdown > 0) {
                    Location checkpointLoc = worldCheckpoints.get(worldName);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld().getName().equals(worldName)) {
                            p.sendMessage("§cReturn by Death in " + countdown + " seconds...");
                            if (checkpointLoc != null) {
                                p.spawnParticle(Particle.END_ROD, checkpointLoc, 100, 50, 50, 50, 0.05);
                                p.spawnParticle(Particle.PORTAL, checkpointLoc, 100, 50, 50, 50, 0.1);
                            }
                        }
                    }
                    countdown--;
                } else {
                    performReset(worldName, snapshotManager);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void performReset(String worldName, WorldSnapshotManager snapshotManager) {
        // Reset vùng nhỏ 100x100x100
        snapshotManager.restoreSnapshot(worldName);

        // Reset tất cả người chơi trong world
        World world = Bukkit.getWorld(worldName);
        for (Player p : world.getPlayers()) {
            UUID uuid = p.getUniqueId();
            Location loc = (Location) playerDataConfig.get(worldName + "." + uuid.toString() + ".location");
            if (loc != null) p.teleport(loc);
            p.setHealth(playerDataConfig.getDouble(worldName + "." + uuid.toString() + ".health", 20.0));
            p.setFoodLevel(playerDataConfig.getInt(worldName + "." + uuid.toString() + ".foodLevel", 20));
            p.setExp((float) playerDataConfig.getDouble(worldName + "." + uuid.toString() + ".exp", 0));
            List<?> invList = playerDataConfig.getList(worldName + "." + uuid.toString() + ".inventory");
            List<?> armorList = playerDataConfig.getList(worldName + "." + uuid.toString() + ".armor");
            if (invList != null) p.getInventory().setContents(invList.toArray(new ItemStack[0]));
            if (armorList != null) p.getInventory().setArmorContents(armorList.toArray(new ItemStack[0]));
            p.sendTitle("Return by Death", "You have returned!", 10, 70, 20);
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 200, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 1));
            // Phát âm thanh từ file trong plugin
            p.playSound(p.getLocation(), "rezero.reset", SoundCategory.MASTER, 1.0f, 1.0f);
        }
    }

    public boolean isWhitelisted(Player player) {
        return whitelist.contains(player.getUniqueId());
    }

    public void addToWhitelist(Player player) {
        UUID uuid = player.getUniqueId();
        if (!whitelist.contains(uuid)) {
            whitelist.add(uuid);
            List<String> uuidStrings = new ArrayList<>();
            for (UUID id : whitelist) {
                uuidStrings.add(id.toString());
            }
            plugin.getConfig().set("whitelist", uuidStrings);
            plugin.saveConfig();
            player.sendMessage("§aYou have been granted Return by Death!");
            setCheckpoint(player);
        }
    }

    public void removeFromWhitelist(Player player) {
        UUID uuid = player.getUniqueId();
        whitelist.remove(uuid);
        List<String> uuidStrings = new ArrayList<>();
        for (UUID id : whitelist) {
            uuidStrings.add(id.toString());
        }
        plugin.getConfig().set("whitelist", uuidStrings);
        plugin.saveConfig();
        player.sendMessage("§cYour Return by Death ability has been revoked!");
    }
}

class PlayerState {
    private final Location location;
    private final double health;
    private final int foodLevel;
    private final float exp;
    private final ItemStack[] inventory;
    private final ItemStack[] armor;

    public PlayerState(Player player) {
        this.location = player.getLocation();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.exp = player.getExp();
        this.inventory = player.getInventory().getContents().clone();
        this.armor = player.getInventory().getArmorContents().clone();
    }
}
