package com.drtshock.playervaults.tasks;

import com.drtshock.playervaults.PlayerVaults;
import net.lingala.zip4j.ZipFile;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;


public class ZipBackups extends BukkitRunnable {

	@Override
	public void run() {

		final String sourceFolder = PlayerVaults.getInstance().getDataFolder() + "/base64vaults/backups";
		final String destination = PlayerVaults.getInstance().getDataFolder() + "/base64vaults/zips";
		final String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(System.currentTimeMillis());
		File sourceFile = new File(sourceFolder);
		File destinationFile = new File(destination);
		if(!destinationFile.isDirectory()){
			destinationFile.mkdirs();
		}
		if(!sourceFile.isDirectory()) {
			PlayerVaults.getInstance().getLogger().warning("Backup path doesn't point to a directory, aborting task.");
			return;
		}
		try {
			backupFolder(sourceFile,destination, timestamp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void backupFolder(final File sourceFolder,final String destination,final String timestamp) throws IOException {
		final String bFolder = destination + File.separator + timestamp;
		ZipFile zipFile = new ZipFile(bFolder + ".zip");
		for (File file : sourceFolder.listFiles()) {
			if (file.isDirectory())
				zipFile.addFolder(file);
			else
				zipFile.addFile(file);
		}
	}
}
