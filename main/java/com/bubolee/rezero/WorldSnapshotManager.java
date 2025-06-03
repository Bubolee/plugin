package com.bubolee.rezero;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;

public class WorldSnapshotManager {
    private final ReZeroPlugin plugin;
    private Map<String, Location> worldCheckpointLocations;
    private Map<String, Map<Location, Material>> blockSnapshots; // Lưu block trong vùng
    private Map<String, List<EntityData>> entitySnapshots; // Lưu mobs trong world
    private File snapshotFile;
    private FileConfiguration snapshotConfig;

    public WorldSnapshotManager(ReZeroPlugin plugin) {
        this.plugin = plugin;
        this.worldCheckpointLocations = new HashMap<>();
        this.blockSnapshots = new HashMap<>();
        this.entitySnapshots = new HashMap<>();
        this.snapshotFile = new File(plugin.getDataFolder(), "snapshots.yml");
        if (!snapshotFile.exists()) {
            plugin.saveResource("snapshots.yml", false);
        }
        this.snapshotConfig = YamlConfiguration.loadConfiguration(snapshotFile);
    }

    public void takeSnapshot(Location loc) {
        String worldName = loc.getWorld().getName();
        worldCheckpointLocations.put(worldName, loc);

        // Snapshot vùng 100x100x100
        Map<Location, Material> blockData = new HashMap<>();
        World world = loc.getWorld();
        int centerX = loc.getBlockX(), centerY = loc.getBlockY(), centerZ = loc.getBlockZ();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = centerX - 50; x <= centerX + 50; x++) {
                    for (int y = Math.max(world.getMinHeight(), centerY - 50); y <= Math.min(world.getMaxHeight() - 1, centerY + 50); y++) {
                        for (int z = centerZ - 50; z <= centerZ + 50; z++) {
                            Location blockLoc = new Location(world, x, y, z);
                            blockData.put(blockLoc, world.getBlockAt(x, y, z).getType());
                            snapshotConfig.set(worldName + ".blocks." + x + "_" + y + "_" + z + ".material", world.getBlockAt(x, y, z).getType().toString());
                        }
                    }
                }
                blockSnapshots.put(worldName, blockData);

                // Snapshot mobs trong world
                List<EntityData> entities = new ArrayList<>();
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof LivingEntity && entity.getType() != EntityType.PLAYER) {
                        entities.add(new EntityData(entity.getType(), entity.getLocation()));
                        snapshotConfig.set(worldName + ".entities." + entity.getUniqueId().toString() + ".type", entity.getType().toString());
                        snapshotConfig.set(worldName + ".entities." + entity.getUniqueId().toString() + ".location", entity.getLocation());
                    }
                }
                entitySnapshots.put(worldName, entities);

                try {
                    snapshotConfig.save(snapshotFile);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to save snapshot: " + e.getMessage());
                }
                plugin.getLogger().info("Snapshot taken for 100x100x100 region in world " + worldName);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void restoreSnapshot(String worldName) {
        Location checkpointLoc = worldCheckpointLocations.get(worldName);
        if (checkpointLoc == null) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                // Restore vùng block
                Map<Location, Material> blockData = blockSnapshots.get(worldName);
                if (blockData != null) {
                    for (Map.Entry<Location, Material> entry : blockData.entrySet()) {
                        Location loc = entry.getKey();
                        Block block = loc.getBlock();
                        block.setType(entry.getValue());
                    }
                }

                // Xóa mobs hiện tại (trừ người chơi)
                World world = Bukkit.getWorld(worldName);
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof LivingEntity && entity.getType() != EntityType.PLAYER) {
                        entity.remove();
                    }
                }

                // Restore mobs
                List<EntityData> entities = entitySnapshots.get(worldName);
                if (entities != null) {
                    for (EntityData data : entities) {
                        world.spawnEntity(data.location, data.type);
                    }
                }

                plugin.getLogger().info("Restored 100x100x100 region and entities in world " + worldName);
            }
        }.runTaskAsynchronously(plugin);
    }
}

class EntityData {
    public EntityType type;
    public Location location;

    public EntityData(EntityType type, Location location) {
        this.type = type;
        this.location = location;
    }
}
