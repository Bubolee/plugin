package com.bubolee.rezero;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

public class WorldSnapshotManager {
    private final ReZeroPlugin plugin;
    private Map<String, Map<Location, Material>> worldBlockSnapshots; // Lưu toàn bộ map
    private Map<String, List<EntityData>> worldEntitySnapshots;

    public WorldSnapshotManager(ReZeroPlugin plugin) {
        this.plugin = plugin;
        this.worldBlockSnapshots = new HashMap<>();
        this.worldEntitySnapshots = new HashMap<>();
    }

    public void takeSnapshotForPlayer(Player player, Location checkpoint) {
        String worldName = player.getWorld().getName();
        World world = player.getWorld();

        Map<Location, Material> blockData = new HashMap<>();
        // Lưu toàn bộ block trong world
        for (int x = world.getMinHeight(); x <= world.getMaxHeight() - 1; x++) {
            for (int z = -30000000; z <= 30000000; z += 16) { // Giới hạn để tránh quá tải
                for (int y = -30000000; y <= 30000000; y += 16) {
                    Location blockLoc = new Location(world, x, y, z);
                    if (world.isChunkLoaded(x >> 4, z >> 4)) {
                        blockData.put(blockLoc, world.getBlockAt(x, y, z).getType());
                    }
                }
            }
        }

        worldBlockSnapshots.put(worldName, blockData);

        // Lưu mobs
        List<EntityData> entities = new ArrayList<>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof LivingEntity && entity.getType() != EntityType.PLAYER) {
                entities.add(new EntityData(entity.getType(), entity.getLocation()));
            }
        }
        worldEntitySnapshots.put(worldName, entities);
    }

    public void restoreSnapshots(String worldName, List<UUID> whitelist) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        // Xóa toàn bộ vật phẩm lơ lửng
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Item) {
                entity.remove();
            }
        }

        // Reset toàn bộ block
        Map<Location, Material> blocksToRestore = worldBlockSnapshots.getOrDefault(worldName, new HashMap<>());
        for (Map.Entry<Location, Material> entry : blocksToRestore.entrySet()) {
            Location loc = entry.getKey();
            Material material = entry.getValue();
            if (world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                loc.getBlock().setType(material);
            }
        }

        // Reset mobs
        List<EntityData> entities = worldEntitySnapshots.get(worldName);
        if (entities != null) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity && entity.getType() != EntityType.PLAYER) {
                    entity.remove();
                }
            }
            for (EntityData data : entities) {
                if (world.isChunkLoaded(data.location.getBlockX() >> 4, data.location.getBlockZ() >> 4)) {
                    world.spawnEntity(data.location, data.type);
                }
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
