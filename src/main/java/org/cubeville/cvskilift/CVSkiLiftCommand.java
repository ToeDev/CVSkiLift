package org.cubeville.cvskilift;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.cubeville.commons.commands.Command;
import org.cubeville.commons.commands.CommandParameterString;
import org.cubeville.commons.commands.CommandResponse;

import java.util.*;

public class CVSkiLiftCommand extends Command {

    final private CVSkiLift plugin;
    final private BukkitScheduler scheduler;
    final private Location liftSpawn;
    final private int liftSpawnIncrement;
    final private int liftsTotal;
    private boolean liftStarted;
    final private HashMap<Minecart, HashMap<Snowman, Pig>> lifts;

    public CVSkiLiftCommand(CVSkiLift plugin) {
        super("");
        addBaseParameter(new CommandParameterString()); //start or stop

        this.liftSpawn = plugin.getLiftSpawn();
        this.liftSpawnIncrement = plugin.getLiftSpawnIncrement();
        this.liftsTotal = plugin.getLiftsTotal();
        this.liftStarted = false;

        this.lifts = new HashMap<>();

        this.scheduler = Bukkit.getScheduler();
        this.plugin = plugin;
    }

    @Override
    public CommandResponse execute(Player sender, Set<String> set, Map<String, Object> map, List<Object> baseParameters) {

        if(baseParameters.get(0).toString().equalsIgnoreCase("start")) {
            if(liftStarted) {
                return new CommandResponse(ChatColor.RED + "Lift already started!");
            }
            startLift();
            liftStarted = true;
            return new CommandResponse(ChatColor.GOLD + "Ski Lift started!");
        } else if(baseParameters.get(0).toString().equalsIgnoreCase("stop")) {
            if(!liftStarted) {
                return new CommandResponse(ChatColor.RED + "Lift hasn't been started yet!");
            }
            stopLift();
            liftStarted = false;
            return new CommandResponse(ChatColor.GOLD + "Ski Lifts have stopped spawning! To clear current lifts, use /cvskilift clear");
        } else if(baseParameters.get(0).toString().equalsIgnoreCase("clear")) {
            clearLift();
            return new CommandResponse(ChatColor.GOLD + "All lifts cleared from track.");
        }
        return new CommandResponse(ChatColor.RED + "Invalid command!" + ChatColor.LIGHT_PURPLE + " Usage: /cvskilift <start | stop | clear>");
    }

    public void startLift() {
        int taskID = scheduler.runTaskTimer(plugin, () -> {
            List<Minecart> toDelete = new ArrayList<>();
            for(Map.Entry<Minecart, HashMap<Snowman, Pig>> lift : lifts.entrySet()) {
                Minecart minecart = lift.getKey();
                if(minecart.getVelocity().equals(new Vector(0, 0, 0))) {
                    minecart.remove();
                    HashMap<Snowman, Pig> nestedHashMap = lift.getValue();
                    for(Map.Entry<Snowman, Pig> pair : nestedHashMap.entrySet()) {
                        pair.getKey().remove();
                        pair.getValue().remove();
                    }
                    toDelete.add(minecart);
                }
            }
            for(Minecart minecart : toDelete) {
                lifts.remove(minecart);
            }
            if(lifts.size() < liftsTotal) {
                World world = liftSpawn.getWorld();
                Minecart minecart = world.spawn(liftSpawn, Minecart.class);
                minecart.setInvulnerable(true);
                Snowman snowman = world.spawn(liftSpawn, Snowman.class);
                snowman.setInvulnerable(true);
                snowman.setDerp(true);
                minecart.addPassenger(snowman);
                Pig pig = world.spawn(liftSpawn, Pig.class);
                pig.setInvulnerable(true);
                pig.setSaddle(true);
                pig.setInvisible(true);
                pig.setLeashHolder(minecart);
                HashMap<Snowman, Pig> nestedHashMap = new HashMap<>();
                nestedHashMap.put(snowman, pig);
                lifts.put(minecart, nestedHashMap);
            }
        }, 0, (long) liftSpawnIncrement * 20).getTaskId();
        plugin.putLiftTaskID(taskID);
    }

    public void stopLift() {
        scheduler.cancelTask(plugin.getLiftTaskID());
    }

    public void clearLift() {
        List<Minecart> toDelete = new ArrayList<>();
        for(Map.Entry<Minecart, HashMap<Snowman, Pig>> lift : lifts.entrySet()) {
            Minecart minecart = lift.getKey();
            minecart.remove();
            HashMap<Snowman, Pig> nestedHashMap = lift.getValue();
            for(Map.Entry<Snowman, Pig> pair : nestedHashMap.entrySet()) {
                pair.getKey().remove();
                pair.getValue().remove();
                toDelete.add(minecart);
            }
        }
        for(Minecart minecart : toDelete) {
            lifts.remove(minecart);
        }
    }
}
