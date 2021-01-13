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

import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.translations.Lang;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SignCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.hasPermission("playervaults.signs.set")) {
			sender.sendMessage(Lang.TITLE.toString() + Lang.NO_PERMS);
			return true;
		}

		if (!PlayerVaults.getInstance().getConf().isSigns()) {
			sender.sendMessage(Lang.TITLE.toString() + Lang.SIGNS_DISABLED.toString());
			return true;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage(Lang.TITLE.toString() + Lang.PLAYER_ONLY);
			return true;
		}

		if(Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
			Player player = (Player) sender;
			DataStore dataStore = GriefPrevention.instance.dataStore;
			PlayerData playerData = dataStore.getPlayerData(player.getUniqueId());
			Claim claim = dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
			if(claim == null || claim.allowBuild(player, Material.AIR ) == null) {
				sender.sendMessage(Lang.TITLE.toString() + "You do not have permission to build in this claim");
				return true;
			}
		}

		if (args.length == 1) {
			int i;
			try {
				i = Integer.parseInt(args[0]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(Lang.TITLE.toString() + Lang.MUST_BE_NUMBER);
				sender.sendMessage(Lang.TITLE.toString() + "Usage: /" + label + " [owner] <#>");
				return true;
			}
			PlayerVaults.getInstance().getSetSign().put(sender.getName(), new SignSetInfo(i));
			sender.sendMessage(Lang.TITLE.toString() + Lang.CLICK_A_SIGN);
		} else if (args.length >= 2) {
			int i;
			try {
				i = Integer.parseInt(args[1]);
			} catch (NumberFormatException nfe) {
				sender.sendMessage(Lang.TITLE.toString() + Lang.MUST_BE_NUMBER);
				sender.sendMessage(Lang.TITLE.toString() + "Usage: /" + label + " [owner] <#>");
				return true;
			}
			PlayerVaults.getInstance().getSetSign().put(sender.getName(), new SignSetInfo(args[0].toLowerCase(), i));
			sender.sendMessage(Lang.TITLE.toString() + Lang.CLICK_A_SIGN);
		} else {
			sender.sendMessage(Lang.TITLE.toString() + Lang.INVALID_ARGS);
			return false;
		}


		return true;
}
}