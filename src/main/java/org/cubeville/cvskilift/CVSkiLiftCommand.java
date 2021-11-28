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

import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;

import java.util.*;

public class CVSkiLiftCommand extends Command {

    final private CVSkiLift plugin;
    final private BukkitScheduler scheduler;
    final private Location liftSpawn;
    final private int liftSpawnIncrement;
    final private int liftsTotal;
    private boolean liftStarted;
    final private HashMap<Minecart, Passengers> lifts;

    class Passengers {
	Passengers(Snowman snowman, Pig pig) { this.snowman = snowman; this.pig = pig; }
	public Snowman snowman;
	public Pig pig;
    }
    
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
            for(Minecart minecart: lifts.keySet()) {
		Snowman snowman = lifts.get(minecart).snowman;
                Pig pig = lifts.get(minecart).pig;
                if(minecart.getVelocity().equals(new Vector(0, 0, 0)) || pig.isDead()) {
                    minecart.remove();
                    snowman.remove();
                    pig.remove();
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
		snowman.setAI(false);
                minecart.addPassenger(snowman);
		Location pigSpawn = liftSpawn.clone();
		pigSpawn.subtract(0, 5, 0);
                Pig pig = world.spawn(pigSpawn, Pig.class);
                pig.setInvulnerable(true);
                pig.setSaddle(true);
                pig.setInvisible(true);
		pig.setAI(false);
                pig.setLeashHolder(minecart);
		lifts.put(minecart, new Passengers(snowman, pig));
		System.out.println(pig.getUniqueId());
            }
        }, 0, (long) liftSpawnIncrement * 20).getTaskId();

	int pigTaskID = scheduler.runTaskTimer(plugin, () -> {
		for(Minecart m: lifts.keySet()) {
		    Pig pig = lifts.get(m).pig;
		    Location pigLocation = m.getLocation().clone();
		    pigLocation.subtract(0, 5, 0);
		    float yaw = pigLocation.getYaw() + 90.0f;
		    if(yaw >= 360.0f) yaw -= 360.0f;
		    ((CraftEntity) pig).getHandle().setPositionRotation(pigLocation.getX(), pigLocation.getY(), pigLocation.getZ(), yaw, 0);
		}
	    }, 1, 1).getTaskId();

        plugin.putLiftTaskID(taskID, pigTaskID);
	    
    }

    public void stopLift() {
        scheduler.cancelTask(plugin.getLiftTaskID());
	scheduler.cancelTask(plugin.getPigLiftTaskID());
    }

    public void clearLift() {
        List<Minecart> toDelete = new ArrayList<>();
        for(Minecart minecart: lifts.keySet()) {
            minecart.remove();
	    lifts.get(minecart).pig.remove();
	    lifts.get(minecart).snowman.remove();
	    toDelete.add(minecart);
        }
        for(Minecart minecart : toDelete) {
            lifts.remove(minecart);
        }
    }
}
