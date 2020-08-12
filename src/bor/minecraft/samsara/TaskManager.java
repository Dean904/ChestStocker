package bor.minecraft.samsara;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;

public class TaskManager {

	private DBManager dbManager = new DBManager();
	private static final int NUM_THREADS = 10;
	private final ScheduledExecutorService scheduler;
	private static List<ScheduledFuture<?>> taskFutureList;

	public TaskManager() {
		taskFutureList = new LinkedList<ScheduledFuture<?>>();
		scheduler = Executors.newScheduledThreadPool(NUM_THREADS);
	}

	private static final class ChestRefillTask implements Runnable {

		DBManager dbManager = new DBManager();
		String id;
		Chest chest;

		ChestRefillTask(String chestID, Chest chest) {
			this.id = chestID;
			this.chest = chest;
		}

		@Override
		public void run() {
			// Updates Chests DB status to filled & fills chest
			Inventory inv = chest.getInventory();

			// TODO: Fetch loot table & calculate inventory
			List<LootTableVO> lootList = dbManager.getLootTableForChestID(id);
			if (lootList != null) {
				Random r = new Random();
				int randomIndex = r.nextInt(lootList.size());
				inv.setContents(lootList.get(randomIndex).getItems());
				dbManager.updateChestEmptiedStatus(id, "N");
				System.out.print("Filling " + id + " with loot table index " + randomIndex);
			}
			else {
				System.err.print("Error no loot table for chest " + id);
			}
		}
	}

	public void scheduleRefillForChest(String id, Chest chest, ChestVO chestVO) {
		int offset = chestVO.getDelay();

		Runnable chestRefillTask = new ChestRefillTask(id, chest);
		taskFutureList.add(scheduler.schedule(chestRefillTask, offset, TimeUnit.MINUTES));
	}

	// Alarm shit

	/** Sound the alarm for a few seconds, then stop. */
	public void activateAlarmThenStop() {
		Runnable soundAlarmTask = new SoundAlarmTask();
		Runnable stopAlarm = new StopAlarmTask(
				scheduler.scheduleWithFixedDelay(soundAlarmTask, 10, 1, TimeUnit.SECONDS));
		scheduler.schedule(stopAlarm, 20, TimeUnit.SECONDS);
	}

	private static void log(String msg) {
		System.out.println(msg);
	}

	/** If invocations might overlap, you can specify more than a single thread. */
	private static final boolean DONT_INTERRUPT_IF_RUNNING = false;

	private static final class SoundAlarmTask implements Runnable {
		@Override
		public void run() {
			count--;
			log("meep " + count);
		}

		private int count = 10;
	}

	private final class StopAlarmTask implements Runnable {
		StopAlarmTask(ScheduledFuture<?> schedFuture) {
			this.schedFuture = schedFuture;
		}

		@Override
		public void run() {
			log("Stopping alarm.");
			schedFuture.cancel(DONT_INTERRUPT_IF_RUNNING);
		}

		private ScheduledFuture<?> schedFuture;
	}

}
