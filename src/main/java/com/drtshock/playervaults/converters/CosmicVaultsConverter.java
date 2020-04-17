package com.drtshock.playervaults.converters;

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.vaultmanagement.VaultManager;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.UUID;

public class CosmicVaultsConverter implements Converter {
	private final PlayerVaults plugin = PlayerVaults.getInstance();
	private final VaultManager vaults = VaultManager.getInstance();

	@Override
	public int run(final CommandSender initiator) {
		File destination = new File(plugin.getDataFolder().getParentFile(), "CosmicVaults" + File.separator + "player-data");
		if (!destination.exists())
			return -1;

		int converted = 0;

		File[] players = Preconditions.checkNotNull(destination.listFiles(), "There are no files.");
		for (File file : players) {
			if(convert(file))
				converted++;
		}
		return converted;
	}


	private boolean convert(File playerFile) {
		long lastUpdate = 0;
		if (playerFile.isFile() && playerFile.getName().toLowerCase().endsWith(".yml")) {
			try {
				final OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(playerFile.getName().substring(0, playerFile.getName().lastIndexOf('.'))));
				if (player == null || player.getUniqueId() == null) {
					plugin.getLogger().warning("Unable to convert cosmic-vaults for :" + (player != null ? player.getName() : playerFile.getName()));
				} else {
					FileConfiguration yaml = YamlConfiguration.loadConfiguration(playerFile);

					final ConfigurationSection allVaultsSection = yaml.getConfigurationSection("vault");
					if (allVaultsSection == null) return false;
					if (allVaultsSection.getKeys(false).isEmpty()) return false; //no vaults

					int converted = convertVaults(allVaultsSection,player.getUniqueId());
					if (System.currentTimeMillis() - lastUpdate >= 1500) {
						plugin.getLogger().info(converted + " vaults have been converted in " + playerFile.getAbsolutePath());
						lastUpdate = System.currentTimeMillis();
					}
				}
			} catch (Exception e) {
				plugin.getLogger().warning("Error converting " + playerFile.getAbsolutePath());
				plugin.getLogger().warning(e.getMessage());
			}
		}
		return true;
	}

	private int convertVaults(final ConfigurationSection allVaultsSection,final UUID uuid) {
		int converted = 0;
		for (String vaultNumber : allVaultsSection.getKeys(false)) {
			final ConfigurationSection vaultSection = allVaultsSection.getConfigurationSection(vaultNumber);
			Inventory vault = vaults.getVault(uuid.toString(), Integer.parseInt(vaultNumber)+1);

			if (vault == null) {
				//vaultSection.getKeys(false).size()
				vault = plugin.getServer().createInventory(null, 54); //54 for conversion only
			}

			for (String key : vaultSection.getKeys(false)) {
				final ItemStack item = vaultSection.getItemStack(key);
				if (item == null) {
					continue;
				}


				vault.setItem(Integer.parseInt(key), item);
				plugin.debug(String.format("Set %s at slot: %s",item,key));
			}
			vaults.saveVault(vault, uuid.toString(), Integer.parseInt(vaultNumber)+1);
			converted++;
		}
		return converted;
	}


	@Override
	public boolean canConvert() {
		final File expectedFolder = new File(plugin.getDataFolder().getParentFile(), "CosmicVaults");
		if (!expectedFolder.exists()) return false;
		final File cosmicVaultsDir = new File(expectedFolder, "player-data");
		return cosmicVaultsDir.exists();
	}

	@Override
	public String getName() {
		return "CosmicVaults";
	}
}
