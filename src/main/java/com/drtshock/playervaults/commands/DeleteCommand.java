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
import com.drtshock.playervaults.vaultmanagement.VaultManager;
import com.drtshock.playervaults.vaultmanagement.VaultOperations;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DeleteCommand extends BaseCommand {

    @CommandAlias("pvdelother")
    @CommandPermission("playervaults.delete.other")
    public void onDeleteOtherVault(final CommandSender sender, final OfflinePlayer target, final int number) {
        if (VaultOperations.isLocked()) {
            sender.sendMessage(Lang.TITLE + Lang.LOCKED.toString());
            return;
        }
        VaultOperations.deleteOtherVault(sender, target.getName(), String.valueOf(number));
    }

    @CommandAlias("pvdelall")
    @CommandPermission("playervaults.delete.all")
    public void onDeleteAllOtherVaults(final CommandSender sender, final OfflinePlayer target) {
        if (VaultOperations.isLocked()) {
            sender.sendMessage(Lang.TITLE + Lang.LOCKED.toString());
            return;
        }

        VaultManager.getInstance().deleteAllVaults(target.getName());
        sender.sendMessage(Lang.TITLE.toString() + Lang.DELETE_OTHER_VAULT_ALL.toString().replaceAll("%p", target.getName()));
        PlayerVaults.getInstance().getLogger().info(String.format("%s deleted ALL vaults belonging to %s", sender.getName(), target));
    }


    @Default
    @CommandAlias("pvdel")
    public void onDeleteVault(final Player player, final int number) {
        if (VaultOperations.isLocked()) {
            player.sendMessage(Lang.TITLE + Lang.LOCKED.toString());
            return;
        }
        VaultOperations.deleteOwnVault(player, String.valueOf(number));
    }
}