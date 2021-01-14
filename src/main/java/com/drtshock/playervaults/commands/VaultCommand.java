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

package com.drtshock.playervaults.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.translations.Lang;
import com.drtshock.playervaults.vaultmanagement.VaultManager;
import com.drtshock.playervaults.vaultmanagement.VaultOperations;
import com.drtshock.playervaults.vaultmanagement.VaultViewInfo;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.checkerframework.checker.units.qual.C;

@CommandAlias("pv|vault|chest|playervaults|vc")
public class VaultCommand extends BaseCommand {
	private final VaultManager vaultManager = VaultManager.getInstance();

	@Subcommand("other")
	@CommandPermission("playervaults.admin")
	public void onOpenOtherVault(final Player player, final OfflinePlayer target, @Optional final int number) {
		if (VaultOperations.isLocked()) {
			player.sendMessage(Lang.TITLE + Lang.LOCKED.toString());
			return;
		}

		if (PlayerVaults.getInstance().getInVault().containsKey(player.getUniqueId().toString())) {
			// don't let them open another vault.
			return;
		}

		if (number == 0) {
			player.sendMessage(getVaultList(target.getName()));
			return;
		}

		if (VaultOperations.openOtherVault(player, target.getName(), number)) {
			PlayerVaults.getInstance().getInVault().put(player.getUniqueId().toString(), new VaultViewInfo(target.getName(), number));
		}

	}

	// Return the vaults a player has, or a message if he doesn't any at all.
	private String getVaultList(final String target) {
		YamlConfiguration file = vaultManager.getPlayerVaultFile(target, false);
		if (file == null) {
			return Lang.TITLE.toString() + Lang.VAULT_DOES_NOT_EXIST.toString();
		}
		StringBuilder sb = new StringBuilder();
		for (String key : file.getKeys(false)) {
			sb.append(key.replace("vault", "")).append(" ");
		}

		return Lang.TITLE.toString() + Lang.EXISTING_VAULTS.toString().replaceAll("%p", target).replaceAll("%v", sb.toString().trim());
	}

	@Default
	public void onOpenVault(final Player player, @Optional final int number) {
		onOpenOtherVault(player, player, number);
	}
}
