package com.bubolee.rezero;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class WorldSnapshotManager {
    private final ReZeroPlugin plugin;
    private Map<String, Map<UUID, Map<Location, Material>>> worldBlockSnapshots;
    private Map<String, List<EntityData>> worldEntitySnapshots;

    public WorldSnapshotManager(ReZeroPlugin plugin) {
        this.plugin = plugin;
        this.worldBlockSnapshots = new HashMap<>();
        this.worldEntitySnapshots = new HashMap<>();
    }

    public void takeSnapshotForPlayer(Player player, Location checkpoint) {
        String worldName = player.getWorld().getName();
        World world = player.getWorld();
        UUID uuid = player.getUniqueId();

        Map<Location, Material> blockData = new HashMap<>();
        int centerX = checkpoint.getBlockX(), centerY = checkpoint.getBlockY(), centerZ = checkpoint.getBlockZ();

        for (int x = centerX - 50; x <= centerX + 50; x++) {
            for (int y = Math.max(world.getMinHeight(), centerY - 50); y <= Math.min(world.getMaxHeight() - 1, centerY + 50); y++) {
                for (int z = centerZ - 50; z <= centerZ + 50; z++) {
                    Location blockLoc = new Location(world, x, y, z);
                    blockData.put(blockLoc, world.getBlockAt(x, y, z).getType());
                }
            }
        }

        worldBlockSnapshots.computeIfAbsent(worldName, k -> new HashMap<>()).put(uuid, blockData);

        if (!worldEntitySnapshots.containsKey(worldName)) {
            List<EntityData> entities = new ArrayList<>();
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && entity.getType() != EntityType.PLAYER) {
                    entities.add(new EntityData(entity.getType(), entity.getLocation()));
                }
            }
            worldEntitySnapshots.put(worldName, entities);
        }
    }

    public void restoreSnapshots(String worldName, List<UUID> whitelist) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        Map<Location, Material> blocksToRestore = new HashMap<>();
        for (UUID uuid : whitelist) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                Map<Location, Material> blockData = worldBlockSnapshots.getOrDefault(worldName, new HashMap<>()).get(uuid);
                if (blockData != null) {
                    blocksToRestore.putAll(blockData);
                }
            }
        }

        for (Map.Entry<Location, Material> entry : blocksToRestore.entrySet()) {
            entry.getKey().getBlock().setType(entry.getValue());
        }

        List<EntityData> entities = worldEntitySnapshots.get(worldName);
        if (entities != null) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && entity.getType() != EntityType.PLAYER) {
                    entity.remove();
                }
            }
            for (EntityData data : entities) {
                world.spawnEntity(data.location, data.type);
            }
        }
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
