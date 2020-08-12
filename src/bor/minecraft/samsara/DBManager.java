package bor.minecraft.samsara;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class DBManager {

	// DataBase vars.
	final String username = "mc48342"; // Enter in your db username
	final String password = "4e5c14393c"; // Enter your password for the db
	final String url = "jdbc:mysql://54.39.244.58:3306/mc48342"; // Enter URL w/db name
	// jdbc:mysql://198.154.110.130:3306/mc46171
	// Connection vars
	static Connection connection; // This is the variable we will use to connect to database

	public void init() {
		
		try {
			Class.forName("com.mysql.jdbc.Driver"); // this accesses Driver in jdbc.
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err.println("jdbc driver unavailable!");
			return;
		}
		
		try {
			connection = DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
			System.err.println("Could not get connection to the DB!");
			e.printStackTrace();
		}

		if (connection != null) {
			String sql = "CREATE TABLE IF NOT EXISTS Chests("
					+ "id TEXT,"
					+ "x INT,"
					+ "y INT,"
					+ "z INT,"
					+ "emptied CHAR,"
					+ "delay INT,"
					+ "fillTS TIMESTAMP,"
					+ "PRIMARY KEY (id(255))"
					+ ");";

			try {
				PreparedStatement stmt = connection.prepareStatement(sql);
				stmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			sql = "CREATE TABLE IF NOT EXISTS LootTables("
					+ "id TEXT,"
					+ "items BLOB,"
					+ "chance INT);";
			// + "PRIMARY KEY (id(255), items(767))"
			try {
				PreparedStatement stmt = connection.prepareStatement(sql);
				stmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	public void disable() {
		// invoke on disable.
		try { // using a try catch to catch connection errors (like wrong sql password...)
			if (connection != null && !connection.isClosed()) { // checking if connection isn't null to
				// avoid receiving a nullpointer
				connection.close(); // closing the connection field variable.
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ChestVO getChestForID(String id) {
		ResultSet results = null;
		try {
			String sql = "SELECT * FROM Chests WHERE id=?";
			PreparedStatement stmt = connection.prepareStatement(sql);
			stmt.setString(1, id);
			results = stmt.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ChestVO chest = null;
		try {
			if (results.next()) {
				chest = new ChestVO();
				chest.setId(id);
				chest.setX(results.getInt(2));
				chest.setY(results.getInt(3));
				chest.setZ(results.getInt(4));

				chest.setEmptied(results.getString(5));
				chest.setDelay(results.getInt(6));
				chest.setFillTS(results.getDate(7));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return chest;
	}

	public void createChestInDB(String id, Chest chest, int offset) {
		try {
			String sql = "INSERT INTO Chests VALUES(?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement stmt = connection.prepareStatement(sql);

			stmt.setString(1, id); // id
			stmt.setInt(2, chest.getX());
			stmt.setInt(3, chest.getY());
			stmt.setInt(4, chest.getZ());
			stmt.setString(5, "N");
			stmt.setInt(6, offset);
			stmt.setTimestamp(7, null);

			stmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void updateChestOffsetInDB(String id, int offset) {
		try {
			String sql = "UPDATE Chests SET delay = ? WHERE id = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);

			stmt.setInt(1, offset);
			stmt.setString(2, id); // id

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void updateChestEmptiedStatus(String id, String filledStatus) {
		try {
			String sql = "UPDATE Chests SET emptied = ? " + "WHERE id = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);

			stmt.setString(1, filledStatus);
			stmt.setString(2, id);

			stmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void deleteChestWithID(String id) {
		try {
			String sql = "DELETE FROM Chests WHERE id = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);
			stmt.setString(1, id); // id
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			String sql = "DELETE FROM LootTables WHERE id = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);
			stmt.setString(1, id); // id
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public List<LootTableVO> getLootTableForChestID(String id) {

		ResultSet results = null;
		try {
			String sql = "SELECT * FROM LootTables WHERE id = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);
			stmt.setString(1, id);
			results = stmt.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<LootTableVO> lootList = new ArrayList<LootTableVO>();
		try {
			while (results.next()) {
				LootTableVO lootVO = new LootTableVO();

				Blob blob = results.getBlob(2);
				int blobLength = Math.toIntExact(blob.length());
				ItemStack[] items = deserializeChestContents(blob.getBytes(1, blobLength));
				lootVO.setItems(items);
				lootVO.setId(results.getString(1));
				lootVO.setChance(results.getInt(3));

				lootList.add(lootVO);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return lootList.size() == 0 ? null : lootList;
	}

	public boolean doesLootTableContainInventory(String id, ItemStack[] items) {
		ResultSet results = null;
		try {
			String sql = "SELECT * FROM LootTables WHERE id = ? AND items = ?";
			PreparedStatement stmt = connection.prepareStatement(sql);
			byte[] serializedBlob = serializeChestContents(items);
			stmt.setBytes(2, serializedBlob);
			stmt.setString(1, id);
			results = stmt.executeQuery();
		} catch (SQLException e) {
			System.err.println("Could not compare Loot Tables in DB.");
			e.printStackTrace();
		}

		try {
			return results.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void addInventoryToLootTable(String id, ItemStack[] items, int chance) {
		String sql = "INSERT INTO LootTables VALUES(?, ?, ?)";
		try {
			PreparedStatement stmt = connection.prepareStatement(sql);

			byte[] serializedBlob = serializeChestContents(items);
			stmt.setBytes(2, serializedBlob);
			stmt.setString(1, id);
			stmt.setInt(3, chance);

			stmt.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private byte[] serializeChestContents(ItemStack[] items) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			BukkitObjectOutputStream boos = new BukkitObjectOutputStream(baos);
			boos.writeObject(items);
			boos.close();
		} catch (IOException ioexception) {
			ioexception.printStackTrace();
		}

		return baos.toByteArray();
	}

	private ItemStack[] deserializeChestContents(byte[] bytes) {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		Object backFromTheDead = null;
		try {
			BukkitObjectInputStream bois = new BukkitObjectInputStream(bais);
			backFromTheDead = bois.readObject();
			bois.close();
		} catch (IOException ioexception) {
			ioexception.printStackTrace();
		} catch (ClassNotFoundException classNotFoundException) {
			classNotFoundException.printStackTrace();
		}
		ItemStack[] returnedItemList = (ItemStack[]) backFromTheDead;

		return returnedItemList;
	}

}
