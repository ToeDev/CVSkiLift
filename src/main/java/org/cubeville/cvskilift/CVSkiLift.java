package org.cubeville.cvskilift;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.commons.commands.CommandParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CVSkiLift extends JavaPlugin implements Listener {

    private Logger logger;

    private Location liftSpawn;
    private int liftSpawnIncrement;
    private int liftsTotal;
    private int liftTaskID;

    private CommandParser skiLiftParser;
    private CVSkiLiftCommand pluginCommand;

    @Override
    public void onEnable() {
        this.logger = getLogger();

        final File dataDir = getDataFolder();
        if(!dataDir.exists()) {
            dataDir.mkdirs();
        }
        File configFile = new File(dataDir, "config.yml");
        if(!configFile.exists()) {
            try {
                configFile.createNewFile();
                final InputStream inputStream = this.getResource(configFile.getName());
                final FileOutputStream fileOutputStream = new FileOutputStream(configFile);
                final byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = Objects.requireNonNull(inputStream).read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch(IOException e) {
                logger.log(Level.WARNING, ChatColor.LIGHT_PURPLE + "Unable to generate config file", e);
                throw new RuntimeException(ChatColor.LIGHT_PURPLE + "Unable to generate config file", e);
            }
        }

        YamlConfiguration mainConfig = new YamlConfiguration();
        try {
            mainConfig.load(configFile);
            ConfigurationSection location = mainConfig.getConfigurationSection("Ski-Lift-Spawn");
            int x = location.getInt("x");
            int y = location.getInt("y");
            int z = location.getInt("z");
            String world = location.getString("world");
            liftSpawn = new Location(Bukkit.getWorld(world), x, y, z);
            logger.log(Level.INFO, ChatColor.LIGHT_PURPLE + "Ski Lift Spawn Location: " + ChatColor.GOLD + liftSpawn);
            liftSpawnIncrement = mainConfig.getInt("Ski-Lift-Spawn-Increment");
            logger.log(Level.INFO, ChatColor.LIGHT_PURPLE + "Ski Lift Spawning Increment: " + ChatColor.GOLD + liftSpawnIncrement);
            liftsTotal = mainConfig.getInt("Ski-Lifts-Total");
            logger.log(Level.INFO, ChatColor.LIGHT_PURPLE + "Total Ski Lifts on track at once: " + ChatColor.GOLD + liftsTotal);
        } catch(IOException | InvalidConfigurationException e) {
            logger.log(Level.WARNING, ChatColor.LIGHT_PURPLE + "Unable to load config file", e);
        }

        liftTaskID = 0;

        skiLiftParser = new CommandParser();
        pluginCommand = new CVSkiLiftCommand(this);
        skiLiftParser.addCommand(pluginCommand);

        logger.info(ChatColor.LIGHT_PURPLE + "Plugin Enabled Successfully");
    }

    public Location getLiftSpawn() {
        return this.liftSpawn;
    }

    public int getLiftSpawnIncrement() {
        return this.liftSpawnIncrement;
    }

    public int getLiftsTotal() {
        return this.liftsTotal;
    }

    public void putLiftTaskID(int taskID) {
        liftTaskID = taskID;
    }

    public int getLiftTaskID() {
        return this.liftTaskID;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("cvskilift")) {
            return skiLiftParser.execute(sender, args);
        }
        return false;
    }

    @Override
    public void onDisable() {
        if(liftTaskID != 0) {
            Bukkit.getScheduler().cancelTask(liftTaskID);
        }
        pluginCommand.clearLift();
        logger.info(ChatColor.LIGHT_PURPLE + "Plugin Disabled Successfully");
    }
}
