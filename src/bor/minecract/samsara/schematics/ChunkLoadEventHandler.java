package bor.minecract.samsara.schematics;

import java.util.EnumSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import bor.minecraft.samsara.ChestStocker;
import bor.minecraft.samsara.DBManager;

public class ChunkLoadEventHandler implements Listener {

	private ChestStocker mainPlugin;

	SchematicManager schematicManager;
	Map<String, StructureVO> structureMap;
	
	public ChunkLoadEventHandler(ChestStocker plugin, DBManager dbManager, SchematicManager schematicManager) {
		System.out.print("Attaching chunk load event listener... ");
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		mainPlugin = plugin;

		this.schematicManager = schematicManager;
		structureMap = schematicManager.getStructureConfigList();


	}

	@EventHandler
	public void onChunkLoad(final ChunkLoadEvent event) {
		if (!event.isNewChunk())
			return;

		// 1 million chunks * .0005 = 500 structures
		
		if ( Math.random() < 0.001) {

			Object[] values = structureMap.values().toArray();
			
			if(values.length == 0)
				return;
			
			Random generator = new Random();
			StructureVO randomStructure = (StructureVO) values[generator.nextInt(values.length)];
			Chunk chunk = event.getChunk();

			if(randomStructure.getExcludedBiomes().contains(chunk.getBlock(0, 0, 0).getBiome())) {
				return;
			}
			
			if (randomStructure.isRequiresFlatChunk()) {

				Block surfaceCorner0 = calculateSurfaceBlock(chunk.getBlock(0, 0, 0));
				Block surfaceCorner1 = calculateSurfaceBlock(chunk.getBlock(0, 0, 15));
				Block surfaceCorner2 = calculateSurfaceBlock(chunk.getBlock(15, 0, 0));
				Block surfaceCorner3 = calculateSurfaceBlock(chunk.getBlock(15, 0, 15));

				if (Math.abs(surfaceCorner0.getY() - surfaceCorner3.getY()) > 5 ||
						Math.abs(surfaceCorner1.getY() - surfaceCorner2.getY()) > 5) {
					System.out.print("Canceling Structure: Chunk too steep");

					return;
				}
			}

			Block spawnBlock = calculateSurfaceBlock(event.getChunk().getBlock(7, 0, 7));
			Location spawnLoc = spawnBlock.getLocation();
			spawnLoc.setY(spawnLoc.getY() + 1);
			schematicManager.loadStructureWithWorldGuard(randomStructure.getName(), spawnLoc);			
		}
	}

	private static final Set<Material> TREELOGs = EnumSet.of(
			Material.ACACIA_LOG, Material.BIRCH_LOG, Material.DARK_OAK_LOG,
			Material.JUNGLE_LOG, Material.LEGACY_LOG, Material.LEGACY_LOG_2,
			Material.OAK_LOG, Material.SPRUCE_LOG);

	private Block calculateSurfaceBlock(Block block) {

		World world = block.getWorld();
		Block surfaceBlock = world.getHighestBlockAt(block.getX(), block.getZ());

		// Decrement Y until a block thats not Air Leaf or Log is found
		while (surfaceBlock.getBlockData() instanceof org.bukkit.block.data.type.Leaves ||
				TREELOGs.contains(surfaceBlock.getType()) || surfaceBlock.getType().equals(Material.AIR)) {

			surfaceBlock = world.getBlockAt(surfaceBlock.getX(), surfaceBlock.getY() - 1, surfaceBlock.getZ());
		}

		return surfaceBlock;

	}

}
