package me.kakalavala.jukebox.commands;

import java.util.UUID;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.kakalavala.jukebox.core.Core;

public class Command_Jukebox implements CommandExecutor {
	
	private Core core;
	
	public Command_Jukebox(final Core core) {
		this.core = core;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String command, String[] args) {
		final boolean isPly = (sender instanceof Player);
		
		if (args.length < 1 || args.length > 2) {
			this.displayHelp(sender);
			return false;
		} else {
			if (args.length == 1) {
				if (args[0].equalsIgnoreCase("toggle")) {
					if (!sender.hasPermission("jukebox.toggle")) {
						core.sendPluginMessage(sender, "§cYou don't have permission to do that!");
						return false;
					}
					
					if (!isPly) {
						core.sendPluginMessage(sender, "§cOnly players can use this!");
						return false;
					} else {
						final UUID uuid = ((Player) sender).getUniqueId();
						
						if (!core.toggledStatus.containsKey(uuid)) {
							core.toggledStatus.put(uuid, false);
							core.playRecord((Player) sender, "stop");
						} else core.toggledStatus.put(uuid, !core.toggledStatus.get(uuid));
						
						core.sendPluginMessage(sender, "§b§lMusic: " + core.boolToString(core.toggledStatus.get(uuid)));
						
						if (!core.toggledStatus.get(uuid))
							core.playRecord((Player) sender, "stop");
						else core.sendPluginMessage(sender, "§2Music with resume playing starting next song.");
						
						return true;
					}
				} else if (args[0].equalsIgnoreCase("endvote")) {
					if (!sender.hasPermission("jukebox.endvote")) {
						core.sendPluginMessage(sender, "§cYou don't have permission to do that!");
						return false;
					}
					
					if (!core.openVoting) {
						core.sendPluginMessage(sender, "§cVoting is already closed.");
						return false;
					} else {
						core.getServer().getScheduler().cancelTasks(core);
						core.openVoting = false;
						core.broadcastPluginMessage(String.format("§c§lVoting was closed by §4§l%s§c§l!", (isPly) ? sender.getName() : "CONSOLE"));
						
						return true;
					}
				} else if (args[0].equalsIgnoreCase("stop")) {
					if (!sender.hasPermission("jukebox.stop")) {
						core.sendPluginMessage(sender, "§cYou don't have permission to do that!");
						return false;
					}
					
					for (final Player ply : Bukkit.getOnlinePlayers())
						core.playRecord(ply, "stop");
					
					core.broadcastPluginMessage(String.format("§4§lMusic stopped by §c§l%s§4§l!", sender.getName()));
					
					return true;
				} else if (args[0].equalsIgnoreCase("vote")) {
					if (!sender.hasPermission("jukebox.vote")) {
						core.sendPluginMessage(sender, "§cYou don't have permission to do that!");
						return false;
					}
					
					if (!isPly) {
						String recs = "";
						
						for (final String r : core.recs)
							recs += WordUtils.capitalize(r) + ", ";
						
						recs = recs.substring(0, recs.length() - 2);
						
						core.sendPluginMessage(sender, "§2Valid records: §a" + recs);
						return true;
					} else {
						core.openVotingMenu((Player) sender);
						return true;
					}
				} else {
					this.displayHelp(sender);
					return false;
				}
			} else if (args.length == 2) {
				if (args[0].equalsIgnoreCase("vote")) {
					if (!sender.hasPermission("jukebox.vote")) {
						core.sendPluginMessage(sender, "§cYou don't have permission to do that!");
						return false;
					}
					
					if (!core.isRecord(args[1])) {
						core.sendPluginMessage(sender, String.format("§cSorry, §6%s§c isn't a valid record! §8(§4/%s help§8)", WordUtils.capitalize(args[1].toLowerCase()), command));
						return false;
					} else {
						core.openVoting(sender, args[1].toLowerCase());
						core.sendPluginMessage(sender, String.format("§aCasted vote for §2%s§a!", WordUtils.capitalize(args[1].toLowerCase())));
						return true;
					}
				} else if (args[0].equalsIgnoreCase("toggle")) {
					if (!sender.hasPermission("jukebox.toggle")) {
						core.sendPluginMessage(sender, "§cYou don't have permission to do that!");
						return false;
					}
					
					if (!isPly) {
						core.sendPluginMessage(sender, "§cOnly players can use this!");
						return false;
					} else {
						if (args[1].equalsIgnoreCase("on")) {
							core.toggledStatus.put(((Player) sender).getUniqueId(), true);
							core.sendPluginMessage(sender, "§b§lMusic: §a§lON");
							return true;
						} else if (args[1].equalsIgnoreCase("off")) {
							core.toggledStatus.put(((Player) sender).getUniqueId(), false);
							core.sendPluginMessage(sender, "§b§lMusic: §a§lOFF");
							return true;
						} else {
							((Player) sender).performCommand(command + " toggle");
							return true;
						}
					}
				} else {
					this.displayHelp(sender);
					return false;
				}
			} else {
				this.displayHelp(sender);
				return false;
			}
		}
	}
	
	private void displayHelp(final CommandSender sender) {
		final String[] help = {
			"§e-------- §8§l[§6§lJukebox Help§8§l] §e--------",
			this.getPermColour(sender, "toggle") + "/jukebox toggle [on|off] §6- §7Toggles music on and off.",
			this.getPermColour(sender, "vote") + "/jukebox vote [record name] §6- §7Vote for a record.",
			this.getPermColour(sender, "endvote") + "/jukebox endvote §6- §7Ends the current voting.",
			this.getPermColour(sender, "stop") + "/jukebox stop §6- §7Stops music for all players."
		};
		
		sender.sendMessage(help);
	}
	
	private String getPermColour(final CommandSender sender, final String cmd) {
		return (sender.hasPermission("jukebox." + cmd)) ? "§a" : "§c";
	}
}
