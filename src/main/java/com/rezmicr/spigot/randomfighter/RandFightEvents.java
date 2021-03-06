package com.rezmicr.spigot.randomfighter;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import org.bukkit.block.Block;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffect;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class RandFightEvents implements Listener {

    private final RandomFighter plugin;
    private final Map<Player,Location> deathLocs = new HashMap<Player,Location>();
    private final List<Material> itemPool = new ArrayList<Material>();
    private final Random gen = new Random();
    private final RandomTypes randTypes;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
       event.getPlayer().setScoreboard(this.plugin.getScoreBoard().getScoreboard());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD) {
            player.getWorld().strikeLightning(player.getTargetBlock((Set<Material>) null, 200).getLocation());
        }
    }

    public RandFightEvents(RandomFighter plugin, RandomTypes randTypes) {
        this.plugin = plugin;
        this.randTypes = randTypes;
    }

    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getScoreboardTags().contains("random_enemy")) 
            return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.getScoreboardTags().contains("random_enemy")) return;
        event.setDroppedExp(0);        
        // Adds to scoreboard
        Player killer = entity.getKiller();
        if (killer != null) {
        switch (entity.getType()) {
            case RAVAGER:
                this.plugin.getScoreBoard().updatePlayer(killer,10);
                killer.sendMessage("10 points kill");
                break;
            case VINDICATOR:
                this.plugin.getScoreBoard().updatePlayer(killer,2);
                killer.sendMessage("2 points kill");
                break;
            default:
                this.plugin.getScoreBoard().updatePlayer(killer);
                killer.sendMessage("1 point kill");
        }
        }
        EntityType type = entity.getType();
        List<ItemStack> items = event.getDrops();
        items.clear();
        // define special items to always drop for some specific creatures
        switch (type) {
            case CHICKEN:
                items.add(new ItemStack(Material.GOLDEN_CARROT,6));
                break;
        }
        // add a random item from a pre defined pool
        items.add(new ItemStack(randTypes.randItem(),gen.nextInt(2)));
    }

    @EventHandler
    public void onEntityDamaged(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
        Player player = (Player) event.getDamager();
        // Verify if the player is currently in a game
        if (player.getScoreboardTags().contains("random_fighter")) {
            Location loc = (Location) player.getLocation();
            loc.setY(loc.getY() - 1);
            Block bloke = (Block) loc.getBlock();
            // Different effects based on the weapon used
            if (player.getInventory().getItemInMainHand().getType() == Material.STONE_PICKAXE) {
                bloke.setType(Material.STONE);
            } else if (player.getInventory().getItemInMainHand().getType() == Material.BUCKET) {
                bloke.setType(Material.WATER);
            } else if (player.getInventory().getItemInMainHand().getType() == Material.FEATHER) {
                PotionEffect effect = new PotionEffect(PotionEffectType.LEVITATION,10*20,1);
                effect.apply((LivingEntity) event.getEntity());
            }
        }
        }
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
    Player player = event.getEntity();
    if (player.getScoreboardTags().contains("random_fighter")) {
        Location location = player.getLocation();
        this.deathLocs.put(player,location);
    }
    }
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
    Player player = event.getPlayer();
    if (player.getScoreboardTags().contains("random_fighter")) {
        Location loc = this.deathLocs.remove(player);
        BukkitTask task = new RespawnPlayer(player,loc).runTaskLater(this.plugin,20);
    }
    }
}

class RespawnPlayer extends BukkitRunnable {
    
    private Player player;
    private Location location;

    public RespawnPlayer(Player player, Location location) {
        this.player = player;
        this.location = location;
    }

    @Override
    public void run() {
        if (!player.isDead()) {
        this.player.teleport(this.location);
        }
    }
}

