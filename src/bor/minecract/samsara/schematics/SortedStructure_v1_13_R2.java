package bor.minecract.samsara.schematics;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.util.Vector;

import net.minecraft.server.v1_13_R2.DefinedStructure;
import net.minecraft.server.v1_13_R2.GameProfileSerializer;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.NBTTagCompound;
import net.minecraft.server.v1_13_R2.NBTTagList;

public class SortedStructure_v1_13_R2 {
	
	private Map<Vector, IBlockData> blocks;
	private Map<Vector, Material> materials;
	private int[] dimension;
	private DefinedStructure structure;
	
	public SortedStructure_v1_13_R2(DefinedStructure structure) {
		this.structure = structure;
		NBTTagCompound tag = structure.a(new NBTTagCompound());
		this.dimension = new int[] { tag.getList("size", 3).h(0), tag.getList("size", 3).h(1), tag.getList("size", 3).h(2) };
		NBTTagList states = tag.getList("palette", 10);
		NBTTagList blocks = tag.getList("blocks", 10);
		this.blocks = new HashMap<>();
		this.materials = new HashMap<>();
		for (int i = 0; i < blocks.size(); i++) {
			NBTTagCompound blockTag = blocks.getCompound(i);
			Vector pos = new Vector(blockTag.getList("pos", 3).h(0), blockTag.getList("pos", 3).h(1), blockTag.getList("pos", 3).h(2));
			this.blocks.put(pos, GameProfileSerializer.d(states.getCompound(blockTag.getInt("state"))));
			//Block block = Block.REGISTRY.get(new MinecraftKey(states.getCompound(blockTag.getInt("state")).getString("Name")));
			//this.materials.put(pos, CraftMagicNumbers.getMaterial(block));
			String mcName = states.getCompound(blockTag.getInt("state")).getString("Name").replace("minecraft:", "");
			Material bukkitMat = Material.matchMaterial(mcName);
			if (bukkitMat == null || (bukkitMat == Material.AIR && !mcName.equalsIgnoreCase("air"))) {
				throw new IllegalArgumentException("Minecraft key " + mcName + " can not be converted to a bukkit material.");
			} else {
				this.materials.put(pos, bukkitMat);
			}
		}
		if (this.blocks.size() != dimension[0] * dimension[1] * dimension[2]) throw new IllegalArgumentException("Structure dimension and amount of blocks doesn't match!");
	}
	
	public IBlockData getBlockAndStateAt(int x, int y, int z) {
		if (this.blocks.get(new Vector(x, y, z)) == null) throw new IllegalArgumentException("Position [" + Integer.toString(x) + "|" + Integer.toString(y) + "|" + Integer.toString(z) + "] out of range! Needs to be relative to the structure!");
		return this.blocks.get(new Vector(x, y, z));
	}
	
	public Material getBlockTypeAt(int x, int y, int z) {
		if (this.materials.get(new Vector(x, y, z)) == null) throw new IllegalArgumentException("Position [" + Integer.toString(x) + "|" + Integer.toString(y) + "|" + Integer.toString(z) + "] out of range! Needs to be relative to the structure!");
		return this.materials.get(new Vector(x, y, z));
	}
	
	public void insertNoPhysics(Location edge) {
		World world = edge.getWorld();
		for (int x = 0; x < this.dimension[0]; x++) {
			for (int y = 0; y < this.dimension[1]; y++) {
				for (int z = 0; z < this.dimension[2]; z++) {
					edge.add(x, y, z);
					world.getBlockAt(edge).setBlockData(CraftBlockData.fromData(this.getBlockAndStateAt(x, y, z)), false);
					edge.subtract(x, y, z);
				}
			}
		}
	}
	
	public DefinedStructure getStructure() {
		return this.structure;
	}
	public int[] getDimension() {
		return this.dimension;
	}
}