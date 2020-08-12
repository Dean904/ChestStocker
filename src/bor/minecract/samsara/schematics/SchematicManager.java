package bor.minecract.samsara.schematics;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import bor.minecraft.samsara.ChestStocker;
import bor.minecraft.samsara.ChestVO;
import bor.minecraft.samsara.DBManager;

// snakeyaml: https://code.google.com/archive/p/snakeyaml/wikis/Documentation.wiki
// example: https://dzone.com/articles/using-yaml-java-application

public class SchematicManager {

	private final ExecutorService executor = Executors.newCachedThreadPool();

	private static ChestStocker plugin = ChestStocker.getPlugin();
	private static DBManager dbManager;

	private static Map<String, StructureVO> structureMap;
	private static Map<String, File> structureFileMap;

	private static final String structureDirectory = "plugins/StructureSlinger/structureLibrary";
	private static final File structureConfig = new File("plugins/StructureSlinger/structureConfig.yml");

	public SchematicManager(DBManager dbManager) {

		this.dbManager = dbManager;

		try {

			File structureLibrary = new File(structureDirectory);
			if (!structureLibrary.exists())
				structureLibrary.mkdirs();

			// Load entries from YAML
			if (!structureConfig.exists())
				structureConfig.createNewFile();

			structureMap = new HashMap<>();
			Yaml yaml = new Yaml(new CustomClassLoaderConstructor(getClass().getClassLoader()));
			InputStream inputStream = new FileInputStream(structureConfig);

			for (Object object : yaml.loadAll(inputStream)) {
				StructureVO structureVO = (StructureVO) object;
				structureMap.put(structureVO.getName(), structureVO);
				System.out.println("Loaded existing file: " + structureVO.getName());
			}

			// Create new entries in YAML
			FileWriter writer = new FileWriter(structureConfig);
			List<StructureVO> newStructureList = new ArrayList<>();
			List<File> fileList = new ArrayList<File>(Arrays.asList(structureLibrary.listFiles()));
			System.out.println("Parsing structure library... " + fileList.size() + " structures found.");
			for (File structureFileEntry : fileList) {
				if (!structureMap.containsKey(structureFileEntry.getName())) {
					StructureVO structureVO = new StructureVO();
					structureVO.setDir(structureFileEntry.getPath());
					structureVO.setName(structureFileEntry.getName());
					structureVO.setSpawnRate((long) .01);
					structureVO.setSpawnY(-1);
					structureVO.setExcludedBiomes(defaultExcludedBiomes());
					structureVO.setRequiresFlatChunk(true);

					newStructureList.add(structureVO);
					structureMap.put(structureVO.getName(), structureVO);
					System.out.println("Created " + structureVO.getName());
				}
			}
			if (!newStructureList.isEmpty())
				yaml.dumpAll(newStructureList.iterator(), writer);
			writer.close();

			structureFileMap = new HashMap<>();
			for (File structureFileEntry : fileList) {
				structureFileMap.put(structureFileEntry.getName(), structureFileEntry);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("msg = " + e.getMessage());

			e.printStackTrace();

		}

	}

	private List<Biome> defaultExcludedBiomes() {
		List<Biome> biomeList = new ArrayList<>();

		biomeList.add(Biome.OCEAN);
		biomeList.add(Biome.COLD_OCEAN);
		biomeList.add(Biome.DEEP_COLD_OCEAN);
		biomeList.add(Biome.DEEP_FROZEN_OCEAN);
		biomeList.add(Biome.DEEP_LUKEWARM_OCEAN);
		biomeList.add(Biome.DEEP_OCEAN);
		biomeList.add(Biome.DEEP_WARM_OCEAN);
		biomeList.add(Biome.FROZEN_OCEAN);
		biomeList.add(Biome.LUKEWARM_OCEAN);
		biomeList.add(Biome.WARM_OCEAN);
		biomeList.add(Biome.THE_END);

		return biomeList;
	}

	public void loadStructureWithWorldGuard(String name, Location loc1) {
		System.out.print("Initializing " + name + "@" + "[" + loc1.getX() + ", " + loc1.getY() + ", " + loc1.getZ()
				+ "]");
		long startTime = System.currentTimeMillis();
		int[] dimensions = loadStructure(name, loc1);
		if (dimensions == null) {
			System.out.println("Error: Structure returned NULL dimensions.");
			return;
		}

		int x0 = loc1.getBlockX();
		int y0 = loc1.getBlockY();
		int z0 = loc1.getBlockZ();

		int x1 = x0 + dimensions[0];
		int y1 = y0 + dimensions[1];
		int z1 = z0 + dimensions[2];

		String regionName = name + "_" + x0 + "," + y0 + "," + z0;
		BlockVector3 min = BlockVector3.at(x0, y0, z0);
		BlockVector3 max = BlockVector3.at(x1, y1, z1);
		ProtectedRegion region = new ProtectedCuboidRegion(regionName, min, max);
		setRegionFlags(region);

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager regions = container.get(BukkitAdapter.adapt(loc1.getWorld()));
		regions.addRegion(region);

		processRegionForRestockingChests(new Location(loc1.getWorld(), x0, y0, z0),
				new Location(loc1.getWorld(), x1, y1, z1));

		System.out.print("Created " + name + "@" + "[" + loc1.getX() + ", " + loc1.getY() + ", " + loc1.getZ()
				+ "] took " + (startTime - System.currentTimeMillis() + " ms"));

	}

	private void processRegionForRestockingChests(Location min, Location max) {
		System.out.println("Processing region for chests...");

		int numChests = 0;
		World world = min.getWorld();
		for (int x = min.getBlockX(); x < max.getBlockX(); x++) {
			for (int z = min.getBlockZ(); z < max.getBlockZ(); z++) {
				int maxY = world.getHighestBlockYAt(x, z) < max.getBlockY() ? world.getHighestBlockYAt(x, z)
						: max.getBlockY();
				maxY++;
				for (int y = min.getBlockY(); y < maxY; y++) {

					Block block = world.getBlockAt(x, y, z);
					// System.out.print("[" + x + "," + y + "," + z + "] = " + block.getType());

					if (block.getState() instanceof Chest) {
						numChests++;
						processChestIntoDB((Chest) block.getState());
					}
				}
			}
		}
		System.out.println("Found " + numChests + " chests in structure region!");
	}

	private void processChestIntoDB(Chest chest) {

		final Runnable chestDbSync = new Runnable() {
			public void run() {

				Inventory chestInventory = chest.getBlockInventory();

				String id = chest.getX() + "," + chest.getY() + "," + chest.getZ();
				int offset = 15;
				ChestVO chestVO = dbManager.getChestForID(id);

				List<Inventory> allShulkerInvs = new ArrayList<>();
				List<ItemStack> chestContents = Arrays.asList(chestInventory.getContents());

				for (ItemStack stack : chestContents) {
					if (stack == null)
						continue;

					if (stack.getType() == Material.STRING) {
						offset = stack.getAmount();
					}
					if (SHULKERS.contains(stack.getType())) {

						BlockStateMeta meta = (BlockStateMeta) stack.getItemMeta();
						ShulkerBox shulkerBox = (ShulkerBox) meta.getBlockState();
						Inventory shulkerInv = shulkerBox.getInventory();
						allShulkerInvs.add(shulkerInv);

						dbManager.addInventoryToLootTable(id, shulkerInv.getContents(), 1);
					}

				}
				
				if (!chestContents.isEmpty() && !allShulkerInvs.isEmpty()) {
					dbManager.createChestInDB(id, chest, offset);
					
					Random r = new Random();
					int randomInvIndex = r.nextInt(allShulkerInvs.size());
					chestInventory.setContents(allShulkerInvs.get(randomInvIndex).getContents());

					System.out.println("Created chest" + id + " with offset " + offset);
				}


			}
		};

		executor.execute(chestDbSync);

	}

	private final Set<Material> SHULKERS = EnumSet.of(
			Material.SHULKER_BOX, Material.BLACK_SHULKER_BOX, Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX,
			Material.CYAN_SHULKER_BOX, Material.GRAY_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
			Material.LIGHT_BLUE_SHULKER_BOX, Material.LIGHT_GRAY_SHULKER_BOX, Material.LIME_SHULKER_BOX,
			Material.MAGENTA_SHULKER_BOX, Material.ORANGE_SHULKER_BOX, Material.PINK_SHULKER_BOX,
			Material.PURPLE_SHULKER_BOX, Material.RED_SHULKER_BOX, Material.WHITE_SHULKER_BOX,
			Material.YELLOW_SHULKER_BOX);

	private void setRegionFlags(ProtectedRegion region) {
		region.setFlag(Flags.GREET_MESSAGE, "Hi there!");
		region.setFlag(Flags.ENTITY_ITEM_FRAME_DESTROY, StateFlag.State.DENY);
		region.setFlag(Flags.ENTITY_PAINTING_DESTROY, StateFlag.State.DENY);
		region.setFlag(Flags.ENDER_BUILD, StateFlag.State.DENY);
		region.setFlag(Flags.ENDERPEARL, StateFlag.State.DENY);
		region.setFlag(Flags.ENDERDRAGON_BLOCK_DAMAGE, StateFlag.State.DENY);
		region.setFlag(Flags.SLEEP, StateFlag.State.DENY);
		region.setFlag(Flags.FIRE_SPREAD, StateFlag.State.DENY);
		region.setFlag(Flags.LAVA_FIRE, StateFlag.State.DENY);
		region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
		region.setFlag(Flags.GAME_MODE, com.sk89q.worldedit.world.gamemode.GameModes.ADVENTURE);
		region.setFlag(Flags.INTERACT, StateFlag.State.ALLOW);
	}

	public int[] loadStructure(String name, Location loc1) {
		StructureVO structureVO = structureMap.get(name);

		try {
			File structure = structureFileMap.get(name);
			return StructureService_v1_13_R2.loadAndInsertAny(structure, loc1);
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void saveStructure(String name, Location loc1, Location loc2) {
		File structureFile = new File(structureDirectory + "/" + name);
		if (!structureFile.exists()) {
			structureFile.mkdirs();
		}

		try {
			StructureService_v1_13_R2.createAndSaveAny(loc1, loc2, "", structureFile, name);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void structureSaveTest(World world) {
		File structureLibrary = new File(structureDirectory);
		if (!structureLibrary.exists()) {
			structureLibrary.mkdirs();
		}

		try {
			StructureService_v1_13_R2.createAndSaveAny(new Location(world, 0, 0, 0), new Location(world, 64, 200, 64),
					"Borox", structureLibrary, "test");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void structureLoadTest(World world) {
		File source = new File("plugins/StructureSlinger/test");
		try {
			StructureService_v1_13_R2.loadAndInsertAny(source, new Location(world, 0, 0, 0));
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | IOException e) {
			e.printStackTrace();
		}
	}

	public Map<String, StructureVO> getStructureConfigList() {
		return structureMap;
	}

	public void setStructureConfigList(Map<String, StructureVO> structureConfigList) {
		this.structureMap = structureConfigList;
	}
}
