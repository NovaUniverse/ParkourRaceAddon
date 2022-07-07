package net.novauniverse.games.parkourrace.addon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.novauniverse.games.parkourrace.NovaParkourRace;
import net.novauniverse.games.parkourrace.game.data.PlayerData;
import net.novauniverse.games.parkourrace.game.event.ParkourRacePlayerCompleteEvent;
import net.novauniverse.games.parkourrace.game.event.ParkourRacePlayerCompleteLapEvent;
import net.zeeraa.novacore.commons.utils.Callback;
import net.zeeraa.novacore.commons.utils.TextUtils;

public class ParkourRaceAddon extends JavaPlugin implements Listener {
	private BossBar timerBar;
	private Map<UUID, BossBar> playerBar;
	private List<UUID> completed;

	@Override
	public void onEnable() {
		this.playerBar = new HashMap<UUID, BossBar>();
		this.completed = new ArrayList<UUID>();

		timerBar = Bukkit.createBossBar(ChatColor.AQUA + "" + ChatColor.BOLD + "Time left: --:--", BarColor.RED, BarStyle.SOLID);
		timerBar.setProgress(1);
		timerBar.setVisible(true);

		Bukkit.getServer().getPluginManager().registerEvents(this, this);

		NovaParkourRace.getInstance().getGame().addTimerDecrementCallback(new Callback() {
			@Override
			public void execute() {
				int timeLeft = NovaParkourRace.getInstance().getGame().getTimeLeft();
				double p = (double) timeLeft / (double) NovaParkourRace.getInstance().getGame().getConfig().getGameTime();

				timerBar.setProgress(p);
				timerBar.setTitle(ChatColor.AQUA + "" + ChatColor.BOLD + "Time left: " + TextUtils.secondsToTime(timeLeft));
			}
		});
	}

	public void updatePlayer(Player player) {
		boolean inGame = false;
		if (NovaParkourRace.getInstance().getGame().getPlayers().contains(player.getUniqueId())) {
			inGame = true;
		}

		BossBar bar;
		UUID uuid = player.getUniqueId();
		if (!playerBar.containsKey(uuid)) {
			if (!inGame) {
				return;
			}
			bar = Bukkit.createBossBar("Loading...", BarColor.PURPLE, BarStyle.SOLID);
			bar.setProgress(0);
			bar.setVisible(true);
			bar.addPlayer(player);
			playerBar.put(uuid, bar);
		} else {
			bar = playerBar.get(uuid);
		}

		if (completed.contains(uuid)) {
			bar.setTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "Completed");
			bar.setProgress(1);
			return;
		}

		PlayerData playerData = NovaParkourRace.getInstance().getGame().getPlayerData(player);
		if (playerData != null) {
			int laps = NovaParkourRace.getInstance().getGame().getConfig().getLaps();
			int playerLap = playerData.getLap();

			double p = (double) (playerLap - 1) / (double) laps;
			bar.setProgress(p);
			bar.setTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "Lap " + playerLap);
		}
	}

	@Override
	public void onDisable() {
		timerBar.removeAll();

		HandlerList.unregisterAll((Plugin) this);
		Bukkit.getScheduler().cancelTasks(this);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerJoin(PlayerJoinEvent e) {
		Player player = e.getPlayer();
		timerBar.addPlayer(player);
		updatePlayer(player);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent e) {
		Player player = e.getPlayer();
		timerBar.removePlayer(player);
		UUID uuid = player.getUniqueId();
		if (playerBar.containsKey(uuid)) {
			playerBar.get(uuid).removeAll();
			playerBar.remove(uuid);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onParkourRacePlayerCompleteLap(ParkourRacePlayerCompleteEvent e) {
		Player player = e.getPlayer();
		completed.add(player.getUniqueId());
		updatePlayer(player);
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onParkourRacePlayerCompleteLap(ParkourRacePlayerCompleteLapEvent e) {
		updatePlayer(e.getPlayer());
	}
}