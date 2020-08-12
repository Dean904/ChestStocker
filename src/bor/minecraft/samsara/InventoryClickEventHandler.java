package bor.minecraft.samsara;

import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class InventoryClickEventHandler implements Listener {

	ChestStocker mainPlugin;
	DBManager dbManager;
	TaskManager taskManager;

	public InventoryClickEventHandler(ChestStocker plugin, DBManager dbManager, TaskManager taskManager) {
		System.out.print("Attaching inventory click event listener... ");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		mainPlugin = plugin;
		this.dbManager = dbManager;
		this.taskManager = taskManager;
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getClickedInventory() != null) {
			InventoryHolder holder = event.getClickedInventory().getHolder();
			// TODO: Add adventure mode check?
			// event.getWhoClicked().getGameMode();
			if (holder instanceof Chest) {
				Chest chest = (Chest) holder;
				String id = chest.getX() + "," + chest.getY() + "," + chest.getZ();
				ChestVO chestVO = dbManager.getChestForID(id);
				if (chestVO != null && chestVO.getEmptied().equals("N")) {
					// Start timer to refill chests
					System.out.println("Updating status to Y & scheduling refill (+" + chestVO.getDelay() + ")");
					dbManager.updateChestEmptiedStatus(id, "Y");

					taskManager.scheduleRefillForChest(id, chest, chestVO);
				}
			}
		}
	}
}
