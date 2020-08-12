package bor.minecract.samsara.schematics;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandLocals;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.util.command.argument.CommandArgs;
import com.sk89q.worldedit.util.command.argument.MissingArgumentException;
import com.sk89q.worldedit.util.command.composition.CommandExecutor;


/**
 * Example class pulled from:
 * https://www.spigotmc.org/threads/how-do-i-use-the-worldedit-api.206157/
 * 
 * @author socce
 *
 */
public class ClaimListener implements CommandExecutor, Listener {

	private Selection s;

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		// if (cmd.getName().equalsIgnoreCase("koth")) {

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "The console cannot set up a koth.");
			return true;
		}
		Player p = (Player) sender;

		// s = getWorldEdit().getSelection(p);

		if (s == null) {
			// Chat.message("Make a selection!");
			return true;
		}

		// Chat.message("Set Area!");
		return true;
		// }

//        return true;
	}

	public WorldEditPlugin getWorldEdit() {
		Plugin p = (Plugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
		if (p instanceof WorldEditPlugin)
			return (WorldEditPlugin) p;
		else
			return null;
	}

	@Override
	public Object call(CommandArgs arg0, CommandLocals arg1) throws CommandException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List getSuggestions(CommandArgs arg0, CommandLocals arg1) throws MissingArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUsage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean testPermission(CommandLocals arg0) {
		// TODO Auto-generated method stub
		return false;
	}
}