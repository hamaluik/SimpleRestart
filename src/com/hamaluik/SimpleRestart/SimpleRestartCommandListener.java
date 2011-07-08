package com.hamaluik.SimpleRestart;

import java.util.ArrayList;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SimpleRestartCommandListener implements CommandExecutor {
	private final SimpleRestart plugin;
	
	public SimpleRestartCommandListener(SimpleRestart instance) {
		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(label.equalsIgnoreCase("restart") || label.equalsIgnoreCase("reboot")) {			
			if(args.length == 1 && args[0].equalsIgnoreCase("now")) {
				// make sure they have appropriate permission
				if(sender instanceof Player) {
					// only if they're a player
					if(!plugin.hasPermission((Player)sender, "simplerestart.restart")) {
						// no permission!
						plugin.returnMessage(sender, "&cYou don't have permission to do that!");
						return true;
					}
				}
				
				// restarting NOW
				plugin.returnMessage(sender, "&cOk, you asked for it!");
				plugin.stopServer();
				return true;
			}
			else if(args.length == 1 && args[0].equalsIgnoreCase("time")) {
				// report the amount of time before the next restart
				// make sure they have appropriate permission
				if(sender instanceof Player) {
					// only if they're a player
					if(!plugin.hasPermission((Player)sender, "simplerestart.time")) {
						// no permission!
						plugin.returnMessage(sender, "&cYou don't have permission to do that!");
						return true;
					}
				}
				
				
				if(!plugin.autoRestart) {
					// make sure there IS an auto-restart
					plugin.returnMessage(sender, "&cThere is no auto-restart scheduled!");
					return true;
				}
				
				// ok, now see how long is left!
				// (in seconds)
				double timeLeft = (plugin.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
				int hours = (int)(timeLeft / 3600);
				int minutes = (int)((timeLeft - hours * 3600) / 60);
				int seconds = (int)timeLeft % 60;
				
				plugin.returnMessage(sender, "&bThe server will be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
				
				return true;
			}
			else if(args.length == 1 && args[0].equalsIgnoreCase("help")) {
				// make sure they have appropriate permission
				if(sender instanceof Player) {
					// only if they're a player
					if(!plugin.hasPermission((Player)sender, "simplerestart.restart")) {
						// no permission!
						plugin.returnMessage(sender, "&cYou don't have permission to do that!");
						return true;
					}
				}
				
				// show help!
				showHelp(sender);
				return true;
			}
			else if(args.length == 1 && args[0].equalsIgnoreCase("on")) {
				// make sure they have appropriate permission
				if(sender instanceof Player) {
					// only if they're a player
					if(!plugin.hasPermission((Player)sender, "simplerestart.restart")) {
						// no permission!
						plugin.returnMessage(sender, "&cYou don't have permission to do that!");
						return true;
					}
				}
				
				// only if we're not already auto-restarting
				if(plugin.autoRestart) {
					plugin.returnMessage(sender, "&cThe server was already automatically restarting!");
					return true;
				}

				// turn auto-restarts back on..
				plugin.autoRestart = true;
				plugin.log.info("[SimpleRestart] scheduling restart tasks...");
				// schedule the warning task
				// note: scheduled tasks need times in "server ticks"
				// 1 server tick = 1/20th of a second
				// so to get from seconds to ticks, x20
				plugin.remindTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						// warn about the impending reboot
						plugin.getServer().broadcastMessage(plugin.processColours(plugin.warningMessage.replaceAll("%t", "" + plugin.warnTime)));
						plugin.log.info("[SimpleRestart] " + plugin.stripColours(plugin.warningMessage.replaceAll("%t", "" + plugin.warnTime)));
						
						// ok, now schedule the reboot task to be warn-time minutes later!
						// this will get scheduled when the warning fires
						plugin.restartTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
							public void run() {
								plugin.stopServer();
							}
						}, (long)(plugin.warnTime * 60.0 * 20.0));
					}
				}, (long)(((plugin.restartInterval * 60.0) - plugin.warnTime) * 60.0 * 20.0));
				plugin.startTimestamp = System.currentTimeMillis();
				// throw out some log messages
				plugin.log.info("[SimpleRestart] warning scheduled for " + (long)(((plugin.restartInterval * 60.0) - plugin.warnTime) * 60.0) + " seconds from now!");
				plugin.log.info("[SimpleRestart] reboot scheduled for " + (long)(plugin.warnTime * 60.0) + " seconds after that!");
				
				// and inform!
				plugin.returnMessage(sender, "&bAutomatic restarts have been turned on!");
				plugin.log.info("[SimpleRestart] " + sender.toString() + " turned automatic restarts on!");
				
				// ok, now see how long is left!
				// (in seconds)
				double timeLeft = (plugin.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
				int hours = (int)(timeLeft / 3600);
				int minutes = (int)((timeLeft - hours * 3600) / 60);
				int seconds = (int)timeLeft % 60;
				
				plugin.returnMessage(sender, "&bThe server will be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
				
				return true;
			}
			else if(args.length == 1 && args[0].equalsIgnoreCase("off")) {
				// make sure they have appropriate permission
				if(sender instanceof Player) {
					// only if they're a player
					if(!plugin.hasPermission((Player)sender, "simplerestart.restart")) {
						// no permission!
						plugin.returnMessage(sender, "&cYou don't have permission to do that!");
						return true;
					}
				}
				
				// only if we're not already auto-restarting
				if(!plugin.autoRestart) {
					plugin.returnMessage(sender, "&cThe server already wasn't automatically restarting!");
					return true;
				}
				
				// ok, cancel all the tasks associated with this plugin!
				plugin.getServer().getScheduler().cancelTasks(plugin);
				plugin.autoRestart = false;
				plugin.remindTaskId = -1;
				plugin.restartTaskId = -1;
				
				// and inform!
				plugin.returnMessage(sender, "&bAutomatic restarts have been turned off!");
				plugin.log.info("[SimpleRestart] " + sender.toString() + " turned automatic restarts off!");
				
				return true;
			}
			else if(args.length == 2) {
				// restarting in a set time
				// note: doing it this way DOES NOT give a restart warning
				// make sure they have appropriate permission
				if(sender instanceof Player) {
					// only if they're a player
					if(!plugin.hasPermission((Player)sender, "simplerestart.restart")) {
						// no permission!
						plugin.returnMessage(sender, "&cYou don't have permission to do that!");
						return true;
					}
				}
				
				
				String timeFormat = args[0];
				double timeAmount = 0;
				try {
					 timeAmount = Double.parseDouble(args[1]);
				}
				catch(Exception e) {
					plugin.returnMessage(sender, "&cBad time!");
					return true;
				}
				
				// "parse" the restart time
				double restartTime = 0; // in seconds
				if(timeFormat.equalsIgnoreCase("h")) {
					restartTime = timeAmount * 3600;
				}
				else if(timeFormat.equalsIgnoreCase("m")) {
					restartTime = timeAmount * 60;
				}
				else if(timeFormat.equalsIgnoreCase("s")) {
					restartTime = timeAmount;
				}
				else {
					plugin.returnMessage(sender, "&cInvalid time scale!");
					plugin.returnMessage(sender, "&bUse 'h' for time in hours, etc");
					return true;
				}
				
				// log to console
				plugin.log.info("[SimpleRestart] " + sender.toString() + " is setting a new restart time...");
				
				// ok, we have the proper time
				// if the scheduler is already going, cancel it!
				if(plugin.autoRestart) {
					// ok, cancel all the tasks associated with this plugin!
					plugin.getServer().getScheduler().cancelTasks(plugin);
					plugin.autoRestart = false;
					plugin.remindTaskId = -1;
					plugin.restartTaskId = -1;
				}
				
				// and set the restart interval for /restart time
				plugin.restartInterval = restartTime / 3600.0;
				
				// now, start it up again!
				plugin.autoRestart = true;
				plugin.log.info("[SimpleRestart] scheduling restart tasks...");
				// schedule the warning task
				// note: scheduled tasks need times in "server ticks"
				// 1 server tick = 1/20th of a second
				// so to get from seconds to ticks, x20
				plugin.restartTaskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						plugin.stopServer();
					}
				}, (long)(restartTime * 20.0));
				plugin.startTimestamp = System.currentTimeMillis();
				// throw out some log messages
				plugin.log.info("[SimpleRestart] restart scheduled for " + (long)(restartTime) + " seconds from now!");
				
				// and inform!
				double timeLeft = (plugin.restartInterval * 3600) - ((double)(System.currentTimeMillis() - plugin.startTimestamp) / 1000);
				int hours = (int)(timeLeft / 3600);
				int minutes = (int)((timeLeft - hours * 3600) / 60);
				int seconds = (int)timeLeft % 60;
				
				plugin.returnMessage(sender, "&bThe server will now be restarting in &f" + hours + "h" + minutes + "m" + seconds + "s");
				
				return true;
			}
		}
		else if(label.equalsIgnoreCase("memory")) {
			// first, make sure they have permission to deal with restart
			if(sender instanceof Player) {
				// only if they're a player
				if(!plugin.hasPermission((Player)sender, "simplerestart.memory")) {
					// no permission!
					plugin.returnMessage(sender, "&cYou don't have permission to do that!");
					return true;
				}
			}
			
			// show memory usage
			float freeMemory = (float)java.lang.Runtime.getRuntime().freeMemory() / (1024F * 1024F);
			float totalMemory = (float)java.lang.Runtime.getRuntime().totalMemory() / (1024F * 1024F);
			float maxMemory = (float)java.lang.Runtime.getRuntime().maxMemory() / (1024F * 1024F);

			plugin.returnMessage(sender, "&cFree memory: &f" + String.format("%.1f", freeMemory) + "MB");
			plugin.returnMessage(sender, "&cTotal memory: &f" + String.format("%.1f", totalMemory) + "MB");
			plugin.returnMessage(sender, "&cMax memory: &f" + String.format("%.1f", maxMemory) + "MB");
			
			return true;
		}

		// by default tell them they boofed it and show them the help
		plugin.returnMessage(sender, "&cInvalid command usage!");
		showHelp(sender);
		return true;
	}
	
	void showHelp(CommandSender sender) {
		// title..
		plugin.returnMessage(sender, "&f--- &3Restart &bHelp &f---");
		
		ArrayList<HelpItem> helpList = new ArrayList<HelpItem>();
		
		// start with default commands
		helpList.add(new HelpItem("&3/restart &bhelp", "&7shows this help"));
		helpList.add(new HelpItem("&3/restart &bnow", "&7restarts the server NOW"));
		helpList.add(new HelpItem("&3/restart &btime", "&7informs you how much time is left before restarting"));
		helpList.add(new HelpItem("&3/restart &7(&bh&7|&bm&7|&bs&7) &f<time>", "&7restarts the server after a given amount of time"));
		helpList.add(new HelpItem("&3/restart &bon", "&7turns auto-restarts on"));
		helpList.add(new HelpItem("&3/restart &boff", "&7turns auto-restarts off"));
		
		// send the help..
		for(int i = 0; i < helpList.size(); i++) {
			plugin.returnMessage(sender, helpList.get(i).command);
			plugin.returnMessage(sender, "     " + helpList.get(i).description);
		}
	}
}
