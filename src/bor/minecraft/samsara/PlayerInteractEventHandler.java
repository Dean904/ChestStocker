package bor.minecraft.samsara;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.BlockStateMeta;

public class PlayerInteractEventHandler implements Listener {

	ChestStocker mainPlugin;
	DBManager dbManager;
	TaskManager taskManager;

	public PlayerInteractEventHandler(ChestStocker plugin, DBManager dbManager, TaskManager taskManager) {
		System.out.print("Attaching player interact event listener... ");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		mainPlugin = plugin;
		this.dbManager = dbManager;
		this.taskManager = taskManager;
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {

		if (mainPlugin.isInteractEventEnabled()) {

			if (event.getClickedBlock() == null)
				return;

			if (event.getClickedBlock().getState() instanceof Chest) {
				Player player = event.getPlayer();
				Chest chest = (Chest) event.getClickedBlock().getState();
				String id = chest.getX() + "," + chest.getY() + "," + chest.getZ();

				// If right click on chest
				if (event.getAction() == event.getAction().RIGHT_CLICK_BLOCK) {

					Material itemInHand = event.getPlayer().getInventory().getItemInMainHand().getType();
					// If holding Shulker Box
					if (SHULKERS.contains(itemInHand)) {
						event.setCancelled(true);

						// Copy shulker inventory to chest
						BlockStateMeta meta = (BlockStateMeta) event.getPlayer().getInventory().getItemInMainHand()
								.getItemMeta();
						ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
						Inventory inv = chest.getInventory();
						inv.setStorageContents(shulkerBox.getInventory().getContents());

						ChestVO chestVO = dbManager.getChestForID(id);
						if (chestVO != null) {
							// chest already exists, update chest
							// Add inventory to loot table
							if (!dbManager.doesLootTableContainInventory(id, inv.getContents())) {
								dbManager.addInventoryToLootTable(id, inv.getContents(), 1);
								player.sendMessage("Adding to loot table for chest " + id);
							}
							int offset = mainPlugin.getOffset();
							if (chestVO.getDelay() != offset) {
								dbManager.updateChestOffsetInDB(id, offset);
								player.sendMessage("Updating chest [" + id + "] offset to " + offset);
							}

						} else {
							int offset = mainPlugin.getOffset();
							dbManager.createChestInDB(id, chest, offset);
							dbManager.addInventoryToLootTable(id, inv.getContents(), 1);
							player.sendMessage("Created chest [" + id + "] with offset: " + offset);
						}
					}
					// else, if holding bedrock, delete chest from DB
					else if (itemInHand == Material.BEDROCK) {
						if (dbManager.getChestForID(id) != null) {
							dbManager.deleteChestWithID(id);
							event.getClickedBlock().setType(Material.AIR);
							player.sendMessage("Deleted chest " + id);
						}
					}
				} else if (event.getAction() == event.getAction().LEFT_CLICK_BLOCK) {
					if (dbManager.getChestForID(id) != null) {
						event.setCancelled(true);
						player.sendMessage("Cannot destroy restocking chest! Right click with BEDROCK to delete.");
					}
				}
			}
		}
	}

	private static final Set<Material> SHULKERS = EnumSet.of(
			Material.SHULKER_BOX, Material.BLACK_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX,
			Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.GREEN_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX
			, Material.LIME_SHULKER_BOX, Material.MAGENTA_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX
			, Material.WHITE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX);
}
