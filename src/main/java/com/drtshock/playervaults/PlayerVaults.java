/*
 * PlayerVaultsX
 * Copyright (C) 2013 Trent Hensler
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.drtshock.playervaults;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import com.drtshock.playervaults.commands.ConvertCommand;
import com.drtshock.playervaults.commands.DeleteCommand;
import com.drtshock.playervaults.commands.SignCommand;
import com.drtshock.playervaults.commands.SignSetInfo;
import com.drtshock.playervaults.commands.VaultCommand;
import com.drtshock.playervaults.config.Loader;
import com.drtshock.playervaults.config.file.Config;
import com.drtshock.playervaults.listeners.Listeners;
import com.drtshock.playervaults.listeners.SignListener;
import com.drtshock.playervaults.listeners.VaultPreloadListener;
import com.drtshock.playervaults.tasks.Base64Conversion;
import com.drtshock.playervaults.tasks.Cleanup;
import com.drtshock.playervaults.tasks.UUIDConversion;
import com.drtshock.playervaults.tasks.ZipBackups;
import com.drtshock.playervaults.translations.Lang;
import com.drtshock.playervaults.translations.Language;
import com.drtshock.playervaults.vaultmanagement.UUIDVaultManager;
import com.drtshock.playervaults.vaultmanagement.VaultManager;
import com.drtshock.playervaults.vaultmanagement.VaultViewInfo;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.ReloadCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class PlayerVaults extends JavaPlugin {
	private static PlayerVaults instance;
	private final HashMap<String, SignSetInfo> setSign = new HashMap<>();
	// Player name - VaultViewInfo
	private final HashMap<String, VaultViewInfo> inVault = new HashMap<>();
	// VaultViewInfo - Inventory
	private final HashMap<String, Inventory> openInventories = new HashMap<>();
	private final Set<Material> blockedMats = new HashSet<>();
	private Economy economy;
	private boolean useVault;
	private YamlConfiguration signs;
	private File signsFile;
	private boolean saveQueued;
	private boolean backupsEnabled;
	private File backupsFolder;
	private File uuidData;
	private File vaultData;
	private String _versionString;
	private int maxVaultAmountPermTest;
	private Metrics metrics;
	private Config config = new Config();


	private ZipBackups zipBackups;

	public static PlayerVaults getInstance() {
		return instance;
	}

	public static void debug(String s, long start) {
		debug("{0} took {1}ms", new Object[]{s, (System.currentTimeMillis() - start)});
	}


	public static void debug(final String msg, final Object[] params) {
		if (getInstance().getConf().isDebug()) {
				instance.getLogger().log(Level.INFO,msg,params);
		}
	}

	public static void debug(String s) {
		debug(s,null);
	}

	private void loadVaultData() {
		long time = System.currentTimeMillis();
		uuidData = new File(this.getDataFolder(), "uuidvaults");
		vaultData = new File(this.getDataFolder(), "base64vaults");
		debug("vaultdata", time);
	}

	private void registerListeners() {
		long time = System.currentTimeMillis();
		getServer().getPluginManager().registerEvents(new Listeners(this), this);
		getServer().getPluginManager().registerEvents(new VaultPreloadListener(), this);
		getServer().getPluginManager().registerEvents(new SignListener(this), this);
		debug("registering listeners", time);
	}


	private void registerCommands() {
		long time = System.currentTimeMillis();
		PaperCommandManager commandManager = new PaperCommandManager(this);
		commandManager.registerCommand(new ConvertCommand());
		commandManager.registerCommand(new VaultCommand());
		commandManager.registerCommand(new ReloadCommand());
		commandManager.registerCommand(new DeleteCommand());
		commandManager.registerCommand(new SignCommand());
		debug("registered commands", time);
	}


	private void setupMetrics() {
		this.metrics = new Metrics(this, 6905);
		Plugin vault = getServer().getPluginManager().getPlugin("Vault");
		this.metricsDrillPie("vault", () -> this.metricsPluginInfo(vault));
		if (vault != null) {
			this.metricsDrillPie("vault_econ", () -> {
				Map<String, Map<String, Integer>> map = new HashMap<>();
				Map<String, Integer> entry = new HashMap<>();
				entry.put(economy == null ? "none" : economy.getName(), 1);
				map.put(isEconomyEnabled() ? "enabled" : "disabled", entry);
				return map;
			});
			if (isEconomyEnabled()) {
				String name = economy.getName();
				if (name.equals("Essentials Economy")) {
					name = "Essentials";
				}
				Plugin plugin = getServer().getPluginManager().getPlugin(name);
				if (plugin != null) {
					this.metricsDrillPie("vault_econ_plugins", () -> {
						Map<String, Map<String, Integer>> map = new HashMap<>();
						Map<String, Integer> entry = new HashMap<>();
						entry.put(plugin.getDescription().getVersion(), 1);
						map.put(plugin.getName(), entry);
						return map;
					});
				}
			}
		}

		if (vault != null) {
			RegisteredServiceProvider<Permission> provider = getServer().getServicesManager().getRegistration(Permission.class);
			if (provider != null) {
				Permission perm = provider.getProvider();
				String name = perm.getName();
				Plugin plugin = getServer().getPluginManager().getPlugin(name);
				final String version;
				if (plugin == null) {
					version = "unknown";
				} else {
					version = plugin.getDescription().getVersion();
				}
				this.metricsDrillPie("vault_perms", () -> {
					Map<String, Map<String, Integer>> map = new HashMap<>();
					Map<String, Integer> entry = new HashMap<>();
					entry.put(version, 1);
					map.put(name, entry);
					return map;
				});
			}
		}

		this.metricsSimplePie("signs", () -> getConf().isSigns() ? "enabled" : "disabled");
		this.metricsSimplePie("cleanup", () -> getConf().getPurge().isEnabled() ? "enabled" : "disabled");
		this.metricsSimplePie("language", () -> getConf().getLanguage());

		this.metricsDrillPie("block_items", () -> {
			Map<String, Map<String, Integer>> map = new HashMap<>();
			Map<String, Integer> entry = new HashMap<>();
			if (getConf().getItemBlocking().isEnabled()) {
				for (Material material : blockedMats) {
					entry.put(material.toString(), 1);
				}
			}
			if (entry.isEmpty()) {
				entry.put("none", 1);
			}
			map.put(getConf().getItemBlocking().isEnabled() ? "enabled" : "disabled", entry);
			return map;
		});
	}

	@Override
	public void onEnable() {
		instance = this;
		long start = System.currentTimeMillis();
		loadConfig();
		loadVaultData();
		new UUIDConversion().runTask(this);
		new VaultManager();
		new Base64Conversion().runTask(this);
		loadLang();
		new UUIDVaultManager();
		registerListeners();
		this.backupsEnabled = this.getConf().getStorage().getFlatFile().isBackups();
		this.maxVaultAmountPermTest = this.getConf().getMaxVaultAmountPermTest();
		loadSigns();
		registerCommands();
		long time = System.currentTimeMillis();
		useVault = setupEconomy();
		debug("setup economy", time);

		if (getConf().getPurge().isEnabled()) {
			getServer().getScheduler().runTaskAsynchronously(this, new Cleanup(getConf().getPurge().getDaysSinceLastEdit()));
		}

		new BukkitRunnable() {
			@Override
			public void run() {
				if (saveQueued) {
					saveSignsFile();
				}
			}
		}.runTaskTimer(this, 20, 20);

		loadTask();
		setupMetrics();

		patchSpigotItemStorageBug();

		this.getLogger().info("Loaded! Took " + (System.currentTimeMillis() - start) + "ms");
	}

	private void patchSpigotItemStorageBug() {
		try {
			Class<?> clazz = Class.forName(this.getServer().getClass().getPackage().getName() + ".util.CraftNBTTagConfigSerializer");
			Field field = clazz.getDeclaredField("INTEGER");
			field.setAccessible(true);
			Field modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			Pattern pattern = (Pattern) field.get(null);
			if (pattern.pattern().equals("[-+]?(?:0|[1-9][0-9]*)?i")) {
				field.set(null, Pattern.compile("[-+]?(?:0|[1-9][0-9]*)i", Pattern.CASE_INSENSITIVE));
			}
			this.getLogger().info("Patched Spigot item storage bug.");
		} catch (Exception ignored) {
			// Don't worry about it.
		}
	}

	private void metricsLine(String name, Callable<Integer> callable) {
		this.metrics.addCustomChart(new Metrics.SingleLineChart(name, callable));
	}

	private void metricsDrillPie(String name, Callable<Map<String, Map<String, Integer>>> callable) {
		this.metrics.addCustomChart(new Metrics.DrilldownPie(name, callable));
	}

	private void metricsSimplePie(String name, Callable<String> callable) {
		this.metrics.addCustomChart(new Metrics.SimplePie(name, callable));
	}

	private Map<String, Map<String, Integer>> metricsPluginInfo(Plugin plugin) {
		return this.metricsInfo(plugin, () -> plugin.getDescription().getVersion());
	}

	private Map<String, Map<String, Integer>> metricsInfo(Object plugin, Supplier<String> versionGetter) {
		Map<String, Map<String, Integer>> map = new HashMap<>();
		Map<String, Integer> entry = new HashMap<>();
		entry.put(plugin == null ? "nope" : versionGetter.get(), 1);
		map.put(plugin == null ? "absent" : "present", entry);
		return map;
	}

	@Override
	public void onDisable() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (this.inVault.containsKey(player.getUniqueId().toString())) {
				Inventory inventory = player.getOpenInventory().getTopInventory();
				if (inventory.getViewers().size() == 1) {
					VaultViewInfo info = this.inVault.get(player.getUniqueId().toString());
					VaultManager.getInstance().saveVault(inventory, player.getUniqueId().toString(), info.getNumber());
					this.openInventories.remove(info.toString());
					// try this to make sure that they can't make further edits if the process hangs.
					player.closeInventory();
				}

				this.inVault.remove(player.getUniqueId().toString());
				debug("Closing vault for " + player.getName());
				player.closeInventory();
			}
		}

		if (getConf().getPurge().isEnabled()) {
			saveSignsFile();
		}
	}

	@CommandAlias("pvreload")
	@CommandPermission("playervaults.admin")
	public class ReloadCommand extends BaseCommand {
		public void onReload(final CommandSender sender) {
			reloadConfig();
			loadConfig(); // To update blocked materials.
			reloadSigns();
			loadLang();
			loadTask();
			sender.sendMessage(ChatColor.GREEN + "Reloaded PlayerVault's configuration and lang files.");
		}
	}


	private void loadTask() {
		if (zipBackups == null) {
			zipBackups = new ZipBackups();
		} else if (!zipBackups.isCancelled()) {
			zipBackups.cancel();
		}
		zipBackups.runTaskTimer(this, 20, getConf().getStorage().getFlatFile().getInterval());
	}

	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}

		RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
		if (provider == null) {
			return false;
		}

		economy = provider.getProvider();
		return economy != null;
	}

	private void loadConfig() {
		long time = System.currentTimeMillis();
		File configYaml = new File(this.getDataFolder(), "config.yml");
		if (!(new File(this.getDataFolder(), "config.conf").exists()) && configYaml.exists()) {
			this.config.setFromConfig(this.getLogger(), this.getConfig());
			try {
				Files.move(configYaml.toPath(), this.getDataFolder().toPath().resolve("old_unused_config.yml"));
			} catch (Exception e) {
				this.getLogger().log(Level.SEVERE, "Failed to move config for backup", e);
				configYaml.deleteOnExit();
			}
		}

		try {
			Loader.loadAndSave("config", this.config);
		} catch (IOException | IllegalAccessException e) {
			this.getLogger().log(Level.SEVERE, "Could not load config.", e);
		}

		// Clear just in case this is a reload.
		blockedMats.clear();
		if (getConf().getItemBlocking().isEnabled()) {
			for (String s : getConf().getItemBlocking().getList()) {
				Material mat = Material.matchMaterial(s);
				if (mat != null) {
					blockedMats.add(mat);
					getLogger().log(Level.INFO, "Added {0} to list of blocked materials.", mat.name());
				}
			}
		}

		debug("config", time);
	}

	public Config getConf() {
		return this.config;
	}

	private void loadSigns() {
		long time = System.currentTimeMillis();
		File signs = new File(getDataFolder(), "signs.yml");
		if (!signs.exists()) {
			try {
				signs.createNewFile();
			} catch (IOException e) {
				getLogger().severe("PlayerVaults has encountered a fatal error trying to load the signs file.");
				getLogger().severe("Please report this error on GitHub @ https://github.com/drtshock/PlayerVaults/");
				e.printStackTrace();
			}
		}
		this.signsFile = signs;
		this.signs = YamlConfiguration.loadConfiguration(signs);
		debug("loaded signs", time);
	}

	private void reloadSigns() {
		if (!getConf().isSigns()) {
			return;
		}
		if (!signsFile.exists()) loadSigns();
		try {
			signs.load(signsFile);
		} catch (IOException | InvalidConfigurationException e) {
			getLogger().severe("PlayerVaults has encountered a fatal error trying to reload the signs file.");
			getLogger().severe("Please report this error on GitHub @ https://github.com/drtshock/PlayerVaults/");
			e.printStackTrace();
		}
	}

	/**
	 * Get the signs.yml config.
	 *
	 * @return The signs.yml config.
	 */
	public YamlConfiguration getSigns() {
		return this.signs;
	}

	/**
	 * Save the signs.yml file.
	 */
	public void saveSigns() {
		saveQueued = true;
	}

	private void saveSignsFile() {
		if (!getConf().isSigns()) {
			return;
		}

		saveQueued = false;
		try {
			signs.save(this.signsFile);
		} catch (IOException e) {
			getLogger().severe("PlayerVaults has encountered an error trying to save the signs file.");
			getLogger().severe("Please report this error on GitHub @ https://github.com/drtshock/PlayerVaults/");
			e.printStackTrace();
		}
	}

	public void loadLang() {
		long time = System.currentTimeMillis();
		File folder = new File(getDataFolder(), "lang");
		if (!folder.exists()) {
			folder.mkdir();
		}

		String definedLanguage = getConf().getLanguage();

		// Save as default just incase.
		File english = null;
		File definedFile = null;

		for (Language lang : Language.values()) {
			String fileName = lang.getFriendlyName() + ".yml";
			File file = new File(folder, fileName);
			if (lang == Language.ENGLISH) {
				english = file;
			}

			if (definedLanguage.equalsIgnoreCase(lang.getFriendlyName())) {
				definedFile = file;
			}

			// Have Bukkit save the file.
			if (!file.exists()) {
				saveResource("lang/" + fileName, false);
			}
		}

		if (definedFile != null && !definedFile.exists()) {
			getLogger().severe("Failed to load language for " + definedLanguage + ". Defaulting to English.");
			definedFile = english;
		}

		if (definedFile == null) {
			getLogger().severe("Failed to load custom language settings. Loading plugin defaults. This should never happen, go ask for help.");
			return;
		}

		Lang.setFile(YamlConfiguration.loadConfiguration(definedFile));
		getLogger().info("Loaded lang for " + definedLanguage);
		debug("lang", time);
	}

	public Map<String, SignSetInfo> getSetSign() {
		return this.setSign;
	}

	public Map<String, VaultViewInfo> getInVault() {
		return this.inVault;
	}

	public Map<String, Inventory> getOpenInventories() {
		return this.openInventories;
	}

	public Economy getEconomy() {
		return this.economy;
	}

	public boolean isEconomyEnabled() {
		return this.getConf().getEconomy().isEnabled() && this.useVault;
	}

	public File getVaultData() {
		return this.vaultData;
	}

	/**
	 * Get the legacy UUID vault data folder.
	 * Deprecated in favor of base64 data.
	 *
	 * @return
	 */
	@Deprecated
	public File getUuidData() {
		return this.uuidData;
	}

	public boolean isBackupsEnabled() {
		return this.backupsEnabled;
	}

	public File getBackupsFolder() {
		// having this in #onEnable() creates the 'uuidvaults' directory, preventing the conversion from running
		if (this.backupsFolder == null) {
			this.backupsFolder = new File(this.getVaultData(), "backups");
			this.backupsFolder.mkdirs();
		}

		return this.backupsFolder;
	}

	/**
	 * Tries to get a name from a given String that we hope is a UUID.
	 *
	 * @param potentialUUID - potential UUID to try to get the name for.
	 * @return the player's name if we can find it, otherwise return what got passed to us.
	 */
	public String getNameIfPlayer(String potentialUUID) {
		UUID uuid;
		try {
			uuid = UUID.fromString(potentialUUID);
		} catch (Exception e) {
			return potentialUUID;
		}

		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
		return offlinePlayer != null ? offlinePlayer.getName() : potentialUUID;
	}

	public boolean isBlockedMaterial(Material mat) {
		return blockedMats.contains(mat);
	}

	/**
	 * Tries to grab the server version as a string.
	 *
	 * @return Version as raw string
	 */
	public String getVersion() {
		if (_versionString == null) {
			final String name = Bukkit.getServer().getClass().getPackage().getName();
			_versionString = name.substring(name.lastIndexOf(46) + 1) + ".";
		}
		return _versionString;
	}

	public int getDefaultVaultRows() {
		int def = this.config.getDefaultVaultRows();
		return (def >= 1 && def <= 6) ? def : 6;
	}

	public int getDefaultVaultSize() {
		return this.getDefaultVaultRows() * 9;
	}

	public boolean isSign(Material mat) {
		return mat.name().toUpperCase().contains("SIGN");
	}

	public int getMaxVaultAmountPermTest() {
		return this.maxVaultAmountPermTest;
	}
}
