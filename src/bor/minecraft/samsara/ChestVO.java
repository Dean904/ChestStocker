package bor.minecraft.samsara;

import java.util.Date;

public class ChestVO {

	String id;
	int x;
	int y;
	int z;
	String emptied;
	int delay;
	Date fillTS;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getX() {
		return x;
	}
	public void setX(int x) {
		this.x = x;
	}
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
	public String getEmptied() {
		return emptied;
	}
	public void setEmptied(String emptied) {
		this.emptied = emptied;
	}
	public int getDelay() {
		return delay;
	}
	public void setDelay(int delay) {
		this.delay = delay;
	}
	public Date getFillTS() {
		return fillTS;
	}
	public void setFillTS(Date fillTS) {
		this.fillTS = fillTS;
	}
	
	
}
