package bor.minecraft.samsara;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldguard.WorldGuard;

import bor.minecract.samsara.schematics.ChunkLoadEventHandler;
import bor.minecract.samsara.schematics.SchematicManager;

public class ChestStocker extends JavaPlugin {

	private static ChestStocker plugin;
	private WorldGuard worldGuard;
	private SchematicManager schematicManager;

	DBManager dbManager;
	TaskManager taskManager;
	
	private int offset = 15;
	private boolean interactEventEnabled = false;
	
	@Override
	public void onEnable() {
		plugin = this;

		this.setWorldGuard(getWorldGuard());
		
		getLogger().info("Starting Samsara...");
		dbManager = new DBManager();
		dbManager.init();
		taskManager = new TaskManager();
		schematicManager = new SchematicManager(dbManager);
		
		new PlayerInteractEventHandler(this, dbManager, taskManager);
		new InventoryClickEventHandler(this, dbManager, taskManager);
		
		new ChunkLoadEventHandler(this, dbManager, schematicManager);
		
	}

	@Override
	public void onDisable() {
		dbManager.disable();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("samsara")) {
			sender.sendMessage("Samsara: the cycle of death and rebirth to which life in the material world is bound!");
			return true;
		}

		// Great reference:
		// https://www.spigotmc.org/threads/command-arguments-in-different-classes.79720/
		// if (sender.hasPermission("event.admin")){
		if (command.getName().equalsIgnoreCase("cs") || command.getName().equalsIgnoreCase("cheststocker")) {
			if (args.length == 0) {
				sender.sendMessage("Type '/cs help' for a list of commands!");
			} else if (args.length == 1) {
				if (args[0].equals("offset")) {
					sender.sendMessage("Current offset is " + offset);
				}
				if (args[0].equals("toggle")) {
					sender.sendMessage("Setting interact event handler to " + !interactEventEnabled);
					interactEventEnabled = !interactEventEnabled;
				}
			} else if (args.length == 2) {
				if (args[0].equals("offset")) {
					try {
						offset = Integer.parseInt(args[1]);
						sender.sendMessage("Offset is now " + offset);
					} catch (NumberFormatException e) {
						sender.sendMessage("/cr offset <minutes>");
					}
				}

			}
			return true;
		}

		if (command.getName().equalsIgnoreCase("testSave")) {
			Player player = (Player) sender;
			World world = player.getWorld();
			schematicManager.structureSaveTest(world);
		}

		if (command.getName().equalsIgnoreCase("testLoad")) {
			Player player = (Player) sender;
			World world = player.getWorld();
			schematicManager.structureLoadTest(world);
		}
		
		if(command.getName().equalsIgnoreCase("createStructure")) {
			if (args.length != 7) {
				sender.sendMessage("/createStructure name x1 y1 z1 x2 y2 z2");
			}
			else {
				Player player = (Player) sender;
				World world = player.getWorld();
				Location loc1 = new Location(world, Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
				Location loc2 = new Location(world, Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]));
				schematicManager.saveStructure(args[0], loc1, loc2);
			}
		}
		
		if(command.getName().equalsIgnoreCase("loadStructure")) {
			if (args.length != 1) {
				sender.sendMessage("/createStructure name");
			}
			else {
				Player player = (Player) sender;
				World world = player.getWorld();
				schematicManager.loadStructureWithWorldGuard(args[0], player.getLocation());
			}
		}

		if (command.getName().equalsIgnoreCase("freefalling")) {

			Player player = (Player) sender;
			File file;
			try {
				file = new File("freefalling_history.txt");

				if (!file.exists())
					file.createNewFile();

				FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line;
				while ((line = bufferedReader.readLine()) != null) {
					if (line.equals(player.getName())) {
						return false;
					}
				}
				fileReader.close();

				FileWriter fileWriter = new FileWriter(file, true);
				fileWriter.write(player.getName());
				fileWriter.write(System.getProperty("line.separator"));
				fileWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			PlayerInventory inventory = player.getInventory();

			ItemStack itemstack = new ItemStack(Material.COOKED_PORKCHOP, 13);
			inventory.addItem(itemstack);

			itemstack = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
			inventory.addItem(itemstack);

			itemstack = new ItemStack(Material.MUSHROOM_STEM, 1);
			inventory.addItem(itemstack);

			itemstack = new ItemStack(Material.RABBIT_STEW, 1);
			inventory.addItem(itemstack);

			itemstack = new ItemStack(Material.RED_MUSHROOM, 19);
			inventory.addItem(itemstack);

			itemstack = new ItemStack(Material.BROWN_MUSHROOM, 11);
			inventory.addItem(itemstack);

			itemstack = new ItemStack(Material.EGG, 12);
			inventory.addItem(itemstack);

			itemstack = new ItemStack(Material.GOLDEN_APPLE, 3);
			inventory.addItem(itemstack);

			itemstack = new ItemStack(Material.ELYTRA, 1);
			inventory.addItem(itemstack);

//			itemstack = new ItemStack(Material.POTION);
//			 PotionBrewer brewer = new PotionBrewer();
//			 PotionEffect effect = brewer.createEffect(PotionEffectType.SLOW_FALLING, 60, 1);
//			 PotionType SPEED;
//			inventory.addItem(itemstack);

			return true;
		}
		return false;
	}

	public static ChestStocker getPlugin() {
		return plugin;
	}
	
	private WorldGuard getWorldGuard() {
	    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

	    // WorldGuard may not be loaded
	    if (plugin == null || !(plugin instanceof WorldGuard)) {
	        return null; // Maybe you want throw an exception instead
	    }

	    return (WorldGuard) plugin;
	}

	public SchematicManager getSchematicManager() {
		return schematicManager;
	}

	public void setSchematicManager(SchematicManager schematicManager) {
		this.schematicManager = schematicManager;
	}

	public void setWorldGuard(WorldGuard worldGuard) {
		this.worldGuard = worldGuard;
	}

	public boolean isInteractEventEnabled() {
		return interactEventEnabled;
	}

	public void setInteractEventEnabled(boolean interactEventEnabled) {
		this.interactEventEnabled = interactEventEnabled;
	}
	
	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

}
