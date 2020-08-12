package bor.minecraft.samsara;

import org.bukkit.inventory.ItemStack;

public class LootTableVO {

	String id;
	ItemStack[] items;
	int chance;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public ItemStack[] getItems() {
		return items;
	}
	public void setItems(ItemStack[] items) {
		this.items = items;
	}
	public int getChance() {
		return chance;
	}
	public void setChance(int chance) {
		this.chance = chance;
	}
	
	
	
}
