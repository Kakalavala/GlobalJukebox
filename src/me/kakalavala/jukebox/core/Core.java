package me.kakalavala.jukebox.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.kakalavala.jukebox.commands.Command_Jukebox;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.PacketPlayOutWorldEvent;

public class Core extends JavaPlugin {
	
	public final PluginDescriptionFile pf = this.getDescription();
	public final Logger log = Logger.getLogger(pf.getName());
	
	public final Map<UUID, Boolean> toggledStatus = new HashMap<UUID, Boolean>(); // Player, Music is On/Off
	public final Map<String, String> votes = new HashMap<String, String>(); // Voter (UUID), Record Name
	
	public final String[] recs = { "13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait" };
	public final String MENU_NAME = "§8§l[§6§lJukebox§8§l] §2Vote for a record.";
	
	public boolean openVoting = false;
	public double totalOnline = 1;
	
	private final Map<String, Integer> tallies = new HashMap<String, Integer>();
	
	private boolean hadMultipleVotes = false;
	
	public void onEnable() {
		this.getConfig().options().copyDefaults(true);
		
		this.registerCommands();
		this.registerListeners();
		
		this.totalOnline = Bukkit.getOnlinePlayers().size();
	}
	
	public void onDisable() {
		this.openVoting = false;
		this.getServer().getScheduler().cancelTasks(this);
	}
	
	private void registerCommands() {
		this.getCommand("jukebox").setExecutor(new Command_Jukebox(this));
	}
	
	private void registerListeners() {
		Bukkit.getPluginManager().registerEvents(new Listener() {
			@EventHandler(priority = EventPriority.HIGH)
			public void onJoin(final PlayerJoinEvent e) {
				totalOnline += 1;
			}
			
			@EventHandler(priority = EventPriority.HIGH)
			public void onLeave(final PlayerQuitEvent e) {
				totalOnline -= 1;
			}
			
			@SuppressWarnings("deprecation")
			@EventHandler(priority = EventPriority.HIGHEST)
			public void menu(final InventoryClickEvent e) {
				final Player ply = (Player) e.getWhoClicked();
				final Inventory inv = e.getClickedInventory();
				final ItemStack item = e.getCurrentItem();
				
				if (inv != null && item != null && inv.getTitle().equals(MENU_NAME)) {
					e.setResult(Result.DENY);
					e.setCancelled(true);
					
					if (item.getType().name().startsWith("MUSIC_DISC_"))
						ply.performCommand("jukebox vote " + item.getType().name().substring(11));
					
					if (item.getType() == Material.REDSTONE_BLOCK && item.hasItemMeta() && item.getItemMeta().getDisplayName().equals("§4§lStop Music"))
						ply.performCommand("jukebox stop");
					
					if (item.getType().name().contains("_CONCRETE") && item.hasItemMeta() && item.getItemMeta().getDisplayName().startsWith("§6§lToggle Music: "))
						ply.performCommand("jukebox toggle");
					
					ply.closeInventory();
				}
			}
		}, this);
	}
	
	public void sendPluginMessage(final CommandSender sender, final String msg) {
		sender.sendMessage(this.getPrefix() + msg);
	}
	
	public void broadcastPluginMessage(final String msg) {
		Bukkit.broadcastMessage(this.getPrefix() + msg);
	}
	
	public String getPrefix() {
		this.reloadConfig();
		return (this.getConfig().getString("prefix").length() > 0) ? ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("prefix") + " §r") : "";
	}
	
	public long getVotingTime() {
		this.reloadConfig();
		return (this.getConfig().getInt("voting-time") > 0) ? this.getConfig().getInt("voting-time") * 20 : 1200;
	}
	
	public boolean isRecord(final String rec) {
		boolean isRec = false;
		
		for (final String s : this.recs) {
			if (s.equalsIgnoreCase(rec)) {
				isRec = true;
				break;
			}
		}
		
		return isRec;
	}
	
	public void openVoting(final CommandSender sender, final String recordName) {
		votes.put((sender instanceof Player) ? ((Player) sender).getUniqueId().toString() : "CONSOLE", recordName);
		
		if (!this.openVoting) {
			this.openVoting = true;
			
			broadcastPluginMessage("§2§lVoting has started! Vote with §6/jukebox vote§2§l!");
			
			new BukkitRunnable() {
				@Override
				public void run() {
					broadcastPluginMessage("§e§lVoting ends in " + (int) ((Math.floor(getVotingTime() / 2)) / 20) + " seconds!");
					this.cancel();
				}
			}.runTaskLater(this, (long) (Math.floor(getVotingTime() / 2)));
			
			new BukkitRunnable() {
				@Override
				public void run() {
					tallies.clear();
					
					for (final String r : recs)
						tallies.put(r, 0);
					
					for (final String v : votes.values())
						tallies.put(v, tallies.get(v) + 1);
					
					String key = getWinningKey();
					
					if (key == null) {
						broadcastPluginMessage("§cNot enough players voted!");
					} else {
						broadcastPluginMessage(String.format("§2%s §awon the vote with §6%s§a vote%s!", WordUtils.capitalize(key), tallies.get(key), (hadMultipleVotes) ? "s" : ""));
						playGlobalRecord();
					}
					
					openVoting = false;
					votes.clear();
					this.cancel();
				}
			}.runTaskLater(this, this.getVotingTime());
		}
	}
	
	private String getWinningKey() {
		String key = "";
		int top = 0;
		
		if (!(this.votes.size() >= Math.floor(totalOnline / 2)))
			return null;
		
		for (final String k : this.tallies.keySet()) {
			if (this.tallies.get(k) > top) {
				key = k;
				top = this.tallies.get(k);
			}
		}
		
		this.hadMultipleVotes = (top > 1);
		
		return key;
	}
	
	private void playGlobalRecord() {
		for (final Player ply : Bukkit.getOnlinePlayers()) {
			this.playRecord(ply, "stop");
			
			if (!toggledStatus.containsKey(ply.getUniqueId()))
				toggledStatus.put(ply.getUniqueId(), true);
			
			if (toggledStatus.get(ply.getUniqueId()))
				this.playRecord(ply, this.getWinningKey());
		}
	}
	
	public void playRecord(final Player ply, final String rec) {
		final Location loc = ((Player) ply).getLocation();
		
		int id = 0;
		
		switch (rec.toLowerCase()) {
			case "13":
				id = Record.DISC_13.id;
				break;
			case "cat":
				id = Record.CAT.id;
				break;
			case "blocks":
				id = Record.BLOCKS.id;
				break;
			case "chirp":
				id = Record.CHIRP.id;
				break;
			case "far":
				id = Record.FAR.id;
				break;
			case "mall":
				id = Record.MALL.id;
				break;
			case "mellohi":
				id = Record.MELLOHI.id;
				break;
			case "stal":
				id = Record.STAL.id;
				break;
			case "strad":
				id = Record.STRAD.id;
				break;
			case "ward":
				id = Record.WARD.id;
				break;
			case "11":
				id = Record.DISC_11.id;
				break;
			case "wait":
				id = Record.WAIT.id;
				break;
			default:
				id = 0;
				break;
		}
		
		((CraftPlayer) ply).getHandle().playerConnection.sendPacket(new PacketPlayOutWorldEvent(1010, new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), id, false));
	}
	
	public void openVotingMenu(final Player ply) {
		if (!this.toggledStatus.containsKey(ply.getUniqueId()))
			this.toggledStatus.put(ply.getUniqueId(), true);
		
		final Inventory menu = Bukkit.createInventory(null, 27, this.MENU_NAME);
		
		final ItemStack stopItem = new ItemStack(Material.REDSTONE_BLOCK, 1); {
			final ItemMeta m = stopItem.getItemMeta();
			
			m.setDisplayName("§4§lStop Music");
			m.setLore(Arrays.asList("§cThis will stop the music for all players."));
			
			stopItem.setItemMeta(m);
		};
		
		final ItemStack toggleItem = new ItemStack((this.toggledStatus.get(ply.getUniqueId()) ? Material.LIME_CONCRETE : Material.RED_CONCRETE), 1); {
			final ItemMeta m = toggleItem.getItemMeta();
			
			m.setDisplayName("§6§lToggle Music: " + this.boolToString(!this.toggledStatus.get(ply.getUniqueId())));
			
			toggleItem.setItemMeta(m);
		};
		
		menu.setItem(0, new ItemStack(Material.MUSIC_DISC_13));
		menu.setItem(2, new ItemStack(Material.MUSIC_DISC_CAT));
		menu.setItem(4, new ItemStack(Material.MUSIC_DISC_BLOCKS));
		menu.setItem(6, new ItemStack(Material.MUSIC_DISC_CHIRP));
		menu.setItem(8, new ItemStack(Material.MUSIC_DISC_FAR));
		menu.setItem(10, new ItemStack(Material.MUSIC_DISC_MALL));
		menu.setItem(12, new ItemStack(Material.MUSIC_DISC_MELLOHI));
		menu.setItem(14, new ItemStack(Material.MUSIC_DISC_STAL));
		menu.setItem(16, new ItemStack(Material.MUSIC_DISC_STRAD));
		menu.setItem(20, new ItemStack(Material.MUSIC_DISC_WARD));
		menu.setItem(22, new ItemStack(Material.MUSIC_DISC_11));
		menu.setItem(24, new ItemStack(Material.MUSIC_DISC_WAIT));
		
		menu.setItem(26, toggleItem);
		
		if (ply.hasPermission("jukebox.stop"))
			menu.setItem(18, stopItem);
		
		ply.openInventory(menu);
	}
	
	public String boolToString(final boolean val) {
		return (val) ? "§a§lON" : "§c§lOFF";
	}
}
