package com.hamaluik.SimpleRestart;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

import ru.tehkode.permissions.bukkit.PermissionsEx;
import ru.tehkode.permissions.PermissionManager;

import de.bananaco.bpermissions.api.WorldManager;

public class SimpleRestart extends JavaPlugin {
	// the basics
	Logger log = Logger.getLogger("Minecraft");
	public PermissionHandler permissions3;
	public PermissionManager permissionsEx;
	public WorldManager bpermissions;
	
	// keep track of ourself!
	SimpleRestart plugin = this;
	// and the command executor!
	SimpleRestartCommandListener commandListener = new SimpleRestartCommandListener(this);
	
	// options
	boolean autoRestart = true;
	double restartInterval = 1;
	List<Double> warnTimes;
	boolean delayUntilEmpty = false;
	int maxPlayersConsideredEmpty = 0;
	String warningMessage = new String("&cServer will be restarting in %t minutes!");
	String restartMessage = new String("&cServer is restarting, we'll be right back!");
	
	// timers
	public ArrayList<Timer> warningTimers = new ArrayList<Timer>();
	public Timer rebootTimer;
	
	// keep track of when we started the scheduler
	// so that we know how much time is left
	long startTimestamp;
	
	// startup routine..
	public void onEnable() {		
		// set up the plugin..
		this.setupPermissions();
		this.loadConfiguration();
		this.getCommand("restart").setExecutor(commandListener);
		this.getCommand("memory").setExecutor(commandListener);
		log.info("[SimpleRestart] plugin enabled");
		
		// ok, now if we want to schedule a restart, do so!
		if(autoRestart) {
			scheduleTasks();
		}
		else {
			log.info("[SimpleRestart] No automatic restarts scheduled!");
		}
	}

	// shutdown routine
	public void onDisable() {
		cancelTasks();
		log.info("[SimpleRestart] plugin disabled");
	}
	
	// load the permissions plugin..
	private void setupPermissions() {
		if (this.bpermissions == null) {
			if (this.getServer().getPluginManager().isPluginEnabled("bPermissions")) {
				this.bpermissions = WorldManager.getInstance();
				log.info("[SimpleRestart] permissions (bPermissions-Plugin) successfully loaded");
				return;
			}
		}
		if (this.permissionsEx == null) {
			if (this.getServer().getPluginManager().isPluginEnabled("PermissionsEx")) {
				this.permissionsEx = PermissionsEx.getPermissionManager();
				log.info("[SimpleRestart] permissions (PermissionsEx-Plugin) successfully loaded");
				return;
			}
		}
		if (this.permissions3 == null) {
			Plugin permissions3Plugin = this.getServer().getPluginManager().getPlugin("Permissions");
			if (permissions3Plugin != null) {
				this.permissions3 = ((Permissions)permissions3Plugin).getHandler();
				log.info("[SimpleRestart] permissions (Permissions-Plugin) successfully loaded");
				return;
			}
		} else {
			log.info("[SimpleRestart] permission system not detected, defaulting to OP");
			return;
		}
	}
	
	// just an interface function for checking permissions
	// if permissions are down, default to OP status.
	public boolean hasPermission(Player player, String permission) {
		if(permissions3 != null) {
			return (permissions3.has(player, permission));
		} else if (permissionsEx != null) {
			return (permissionsEx.has(player, permission));
		} else if (bpermissions != null) {
			return player.hasPermission(permission);
		} else {
			return player.isOp();
		}
	}
	
	private void checkConfiguration() {
		// first, check to see if the file exists
		File configFile = new File(getDataFolder() + "/config.yml");
		if(!configFile.exists()) {
			// file doesn't exist yet :/
			log.info("[SimpleRestart] config file not found, will attempt to create a default!");
			new File(getDataFolder().toString()).mkdir();
			try {
				// create the file
				configFile.createNewFile();
				// and attempt to write the defaults to it
				FileWriter out = new FileWriter(getDataFolder() + "/config.yml");
				out.write("---\r\n");
				out.write("# enable / disable auto-restart\r\n");
				out.write("auto-restart: yes\r\n");
				out.write("\r\n");
				out.write("# in hours (decimal points ok -- ex: 2.5)\r\n");
				out.write("# must be > (warn-time / 60)\r\n");
				out.write("auto-restart-interval: 24\r\n");
				out.write("\r\n");
				out.write("# warning times before reboot in minutes (decimal points ok -- ex: 2.5)\r\n");
				out.write("warn-times: [10, 5, 2, 1]\r\n");
				out.write("\r\n");
				out.write("# what to tell players when warning about server reboot\r\n");
				out.write("# CAN use minecraft classic server protocol colour codes\r\n");
				out.write("# use %t to indicate time left to reboot\r\n");
				out.write("warning-message: \"&cServer will be restarting in %t minutes!\"\r\n");
				out.write("\r\n");
				out.write("# what to tell players when server reboots\r\n");
				out.write("# CAN use minecraft classic server protocol colour codes\r\n");
				out.write("restart-message: \"&cServer is restarting, we'll be right back!\"\r\n");
				out.close();
			} catch(IOException ex) {
				// something went wrong :/
				log.info("[SimpleRestart] error: config file does not exist and could not be created");
			}
		}
	}

	public void loadConfiguration() {
		// make sure the config exists
		// and if it doesn't, make it!
		this.checkConfiguration();
		
		// ge the configuration..
		FileConfiguration config = getConfig();
		this.autoRestart = config.getBoolean("auto-restart", true);
		this.restartInterval = config.getDouble("auto-restart-interval", 8);
		this.warnTimes = config.getDoubleList("warn-times");
		this.warningMessage = config.getString("warning-message", "&cServer will be restarting in %t minutes!");
		this.restartMessage = config.getString("restart-message", "&cServer is restarting, we'll be right back!");
	}
	
	// allow for colour tags to be used in strings..
	public String processColours(String str) {
		return str.replaceAll("(&([a-f0-9]))", "\u00A7$2");
	}
	
	// strip colour tags from strings..
	public String stripColours(String str) {
		return str.replaceAll("(&([a-f0-9]))", "");
	}
	
	public void returnMessage(CommandSender sender, String message) {
		if(sender instanceof Player) {
			sender.sendMessage(plugin.processColours(message));
		}
		else {
			sender.sendMessage(plugin.stripColours(message));
		}
	}
	
	void cancelTasks() {
		//plugin.getServer().getScheduler().cancelTasks(plugin);
		for(int i = 0; i < warningTimers.size(); i++) warningTimers.get(i).cancel();
		warningTimers.clear();
		if(rebootTimer != null) rebootTimer.cancel();
		rebootTimer = new Timer();
		plugin.autoRestart = false;
	}
	
	void scheduleTasks() {
		cancelTasks();
		// start the warning tasks
		for(int i = 0; i < warnTimes.size(); i++) {
			if(restartInterval * 60 - warnTimes.get(i) > 0) {
				// only do "positive" warning times
				// start the warning task
				final double warnTime = warnTimes.get(i);
				/*getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
					public void run() {
						getServer().broadcastMessage(processColours(warningMessage.replaceAll("%t", "" + warnTime)));
						plugin.log.info("[SimpleRestart] " + stripColours(warningMessage.replaceAll("%t", "" + warnTime)));
					}
				}, (long)((restartInterval * 60 - warnTimes.get(i)) * 60.0 * 20.0));
				
				log.info("[SimpleRestart] warning scheduled for " + (long)((restartInterval * 60 - warnTimes.get(i)) * 60.0) + " seconds from now!");*/
				Timer warnTimer = new Timer();
				warningTimers.add(warnTimer);
				warnTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						getServer().broadcastMessage(processColours(warningMessage.replaceAll("%t", "" + warnTime)));
						plugin.log.info("[SimpleRestart] " + stripColours(warningMessage.replaceAll("%t", "" + warnTime)));
					}
				}, (long)((restartInterval * 60 - warnTimes.get(i)) * 60000.0));
				log.info("[SimpleRestart] warning scheduled for " + (long)((restartInterval * 60 - warnTimes.get(i)) * 60.0) + " seconds from now!");
			}
		}

		// start the restart task
		/*getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				stopServer();
			}
		}, (long)(restartInterval * 3600.0 * 20.0));*/
		rebootTimer = new Timer();
		rebootTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				stopServer();
			}
		}, (long)(restartInterval * 3600000.0));
		
		log.info("[SimpleRestart] reboot scheduled for " + (long)(restartInterval  * 3600.0) + " seconds from now!");
		plugin.autoRestart = true;
		plugin.startTimestamp = System.currentTimeMillis();
	}
	
	// kick all players from the server
	// with a friendly message!
	void clearServer() {
		this.getServer().broadcastMessage(processColours(restartMessage));
		Player[] players = this.getServer().getOnlinePlayers();
		for (Player player : players) {
			player.kickPlayer(stripColours(restartMessage));
		}
	}
	
	// shut the server down!
	// hack into craftbukkit in order to do this
	// since bukkit doesn't normally allow access -_-
	// full kudos to the Redecouverte at:
	// http://forums.bukkit.org/threads/send-commands-to-console.3241/
	// for the code on executing commands as the console
	boolean stopServer() {
		// log it and empty out the server first
		log.info("[SimpleRestart] Restarting...");
		clearServer();
		try {
			File file = new File(this.getDataFolder().getAbsolutePath() + File.separator + "restart.txt");
			log.info("[SimpleRestart] Touching restart.txt at: " + file.getAbsolutePath());
			if (file.exists()) {
				file.setLastModified(System.currentTimeMillis());
			} else {
				file.createNewFile();
			}
		} catch (Exception e) {
			log.info("[SimpleRestart] Something went wrong while touching restart.txt!");
			return false;
		}
		try {
            this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "save-all");
            this.getServer().dispatchCommand(this.getServer().getConsoleSender(), "stop");
		} catch (Exception e) {
			log.info("[SimpleRestart] Something went wrong while saving & stoping!");
			return false;
		}
		return true;
	}
}
