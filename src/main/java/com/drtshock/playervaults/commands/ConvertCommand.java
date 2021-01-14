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
import co.aikar.commands.annotation.Description;
import com.drtshock.playervaults.PlayerVaults;
import com.drtshock.playervaults.converters.BackpackConverter;
import com.drtshock.playervaults.converters.Converter;
import com.drtshock.playervaults.converters.CosmicVaultsConverter;
import com.drtshock.playervaults.translations.Lang;
import com.drtshock.playervaults.vaultmanagement.VaultOperations;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

@CommandAlias("pvconvert|vaultconvert")
@CommandPermission("playervaults.convert")
@Description("Convert from a compatible plugin.")
public class ConvertCommand extends BaseCommand {

	private final List<Converter> converters = new ArrayList<>();

	public ConvertCommand() {
		converters.add(new BackpackConverter());
		converters.add(new CosmicVaultsConverter());
	}

	private List<Converter> getApplicableConverters(String name) {
		final List<Converter> applicableConverters = new ArrayList<>();
		if (name.equalsIgnoreCase("all")) {
			applicableConverters.addAll(converters);
		} else {
			for (Converter converter : converters) {
				if (converter.getName().equalsIgnoreCase(name)) {
					applicableConverters.add(converter);
				}
			}
		}
		return applicableConverters;
	}

	@Default
	public void onConvert(final CommandSender sender, final String name /*Add completion*/) {
		final List<Converter> applicableConverters = getApplicableConverters(name);
		if (applicableConverters.isEmpty()) {
			sender.sendMessage(Lang.TITLE.toString() + Lang.CONVERT_PLUGIN_NOT_FOUND);
			return;
		}


		// Fork into background
		sender.sendMessage(Lang.TITLE + Lang.CONVERT_BACKGROUND.toString());
		new ConvertRunnable(applicableConverters, sender).runTaskLaterAsynchronously(PlayerVaults.getInstance(),5);
	}

	public static class ConvertRunnable extends BukkitRunnable {
		private final List<Converter> applicableConverters;
		private final CommandSender sender;

		public ConvertRunnable(final List<Converter> applicableConverters, final CommandSender sender) {
			this.applicableConverters = applicableConverters;
			this.sender = sender;
		}

		@Override
		public void run() {
			int converted = 0;
			VaultOperations.setLocked(true);
			for (Converter converter : applicableConverters) {
				if (converter.canConvert()) {
					converted += converter.run(sender);
				}
			}
			VaultOperations.setLocked(false);
			sender.sendMessage(Lang.TITLE + Lang.CONVERT_COMPLETE.toString().replace("%converted", converted + ""));
		}
	}
}