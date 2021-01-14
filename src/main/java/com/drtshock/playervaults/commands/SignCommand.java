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
import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.translations.Lang;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class SignCommand extends BaseCommand {

	@Default
	@CommandAlias("pvsign")
	@CommandPermission("playervaults.signs.set")
	public void onSetSign(final Player player, final int number) {
		onSetOtherSign(player,player,number);
	}

	@CommandAlias("pvsignother")
	@CommandPermission("playervaults.signs.set.other")
	public void onSetOtherSign(final Player player, final OfflinePlayer target, final int number) {
		if (!PlayerVaults.getInstance().getConf().isSigns()) {
			player.sendMessage(Lang.TITLE.toString() + Lang.SIGNS_DISABLED.toString());
			return;
		}

		if(Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
			DataStore dataStore = GriefPrevention.instance.dataStore;
			PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
			Claim claim = dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
			if(claim == null || claim.allowBuild(player, Material.AIR ) == null) {
				player.sendMessage(Lang.TITLE.toString() + "You do not have permission to build in this claim");
				return;
			}
		}

		PlayerVaults.getInstance().getSetSign().put(player.getName(), new SignSetInfo(target.getName().toLowerCase(), number));
		player.sendMessage(Lang.TITLE.toString() + Lang.CLICK_A_SIGN);
	}

}