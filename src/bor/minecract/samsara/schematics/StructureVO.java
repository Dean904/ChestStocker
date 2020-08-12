package bor.minecract.samsara.schematics;

import java.util.List;

import org.bukkit.block.Biome;

public class StructureVO {

	String name;
	String dir;
	
	List<Biome> excludedBiomes;
	Long spawnRate;
	
	int spawnY;
	boolean requiresFlatChunk;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public List<Biome> getExcludedBiomes() {
		return excludedBiomes;
	}

	public void setExcludedBiomes(List<Biome> activeBiomes) {
		this.excludedBiomes = activeBiomes;
	}

	public Long getSpawnRate() {
		return spawnRate;
	}

	public void setSpawnRate(Long spawnRate) {
		this.spawnRate = spawnRate;
	}

	public int getSpawnY() {
		return spawnY;
	}

	public void setSpawnY(int spawnY) {
		this.spawnY = spawnY;
	}

	public boolean isRequiresFlatChunk() {
		return requiresFlatChunk;
	}

	public void setRequiresFlatChunk(boolean requiresFlatChunk) {
		this.requiresFlatChunk = requiresFlatChunk;
	}
	
}
