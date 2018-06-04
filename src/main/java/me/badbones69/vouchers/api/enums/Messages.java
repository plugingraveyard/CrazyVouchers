package me.badbones69.vouchers.api.enums;

import me.badbones69.vouchers.Methods;
import me.badbones69.vouchers.api.FileManager.Files;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public enum Messages {
	
	RELOAD("Reload-Config", "&7You have just reloaded the Config.yml"),
	INVENTORY_FULL("Inventory-Full", "&cYour inventory is to full. Please open up some space to buy that."),
	PLAYERS_ONLY("Reload", "&cOnly players can use this command."),
	NO_PERMISSION("No-Permission", "&cYou do not have permission to use that command!"),
	NO_PERMISSION_TO_VOUCHER("No-Permission-To-Voucher", "&cYou do not have permission to use that voucher."),
	NOT_ONLINE("Not-Online", "&cThat player is not online at this time."),
	NOT_A_NUMBER("Not-A-Number", "&c%Arg% is not a number."),
	NOT_A_VOUCHER("Not-A-Voucher", "&cThat is not a Voucher Type."),
	NOT_A_PLAYER("Not-A-Player", "&cYou must be a player to use this command."),
	CODE_UNAVAILABLE("Code-Unavailable", "&cThe Voucher code &6%Arg% &cis incorrect or unavailable at this time."),
	GIVEN_A_VOUCHER("Given-A-Voucher", "&3You have just given &6%Player% &3a &6%Voucher% &3voucher."),
	GIVEN_ALL_PLAYERS_VOUCHER("Given-All-Players-Voucher", "&3You have just given all players a &6%Voucher% &3voucher."),
	HIT_LIMIT("Hit-Limit", "&cYou have hit your limit for using this voucher."),
	TWO_STEP_AUTHENTICATION("Two-Step-Authentication", "&7Right click again to confirm that you want to use this voucher."),
	HAS_BLACKLIST_PERMISSION("Has-Blacklist-Permission", "&cSorry but you can not use this voucher because you have a black-listed permission."),
	HELP("Help",
	Arrays.asList(
	"&8- &6/Voucher Help &3Lists all the commands for vouchers.",
	"&8- &6/Voucher Types &3Lists all types of vouchers and codes.",
	"&8- &6/Voucher Redeem <Code> &3Allows player to redeem a voucher code.",
	"&8- &6/Voucher Give <Type> [Amount] [Player] [Arguments] &3Gives a player a voucher.",
	"&8- &6/Voucher GiveAll <Type> [Amount] [Arguments] &3Gives all players a voucher.",
	"&8- &6/Voucher Open [Page] &3Opens a GUI so you can get vouchers easy.",
	"&8- &6/Voucher Reload &3Reloadeds the config.yml."));
	
	private String path;
	private String defaultMessage;
	private List<String> defaultListMessage;
	
	private Messages(String path, String defaultMessage) {
		this.path = path;
		this.defaultMessage = defaultMessage;
	}
	
	private Messages(String path, List<String> defaultListMessage) {
		this.path = path;
		this.defaultListMessage = defaultListMessage;
	}
	
	public String getMessage() {
		if(isList()) {
			if(exists()) {
				return Methods.color(convertList(Files.MESSAGES.getFile().getStringList("Messages." + path)));
			}else {
				return Methods.color(convertList(getDefaultListMessage()));
			}
		}else {
			if(exists()) {
				return Methods.getPrefix(Files.MESSAGES.getFile().getString("Messages." + path));
			}else {
				return Methods.getPrefix(getDefaultMessage());
			}
		}
	}
	
	public String getMessage(HashMap<String, String> placeholders) {
		String message;
		if(isList()) {
			if(exists()) {
				message = Methods.color(convertList(Files.MESSAGES.getFile().getStringList("Messages." + path), placeholders));
			}else {
				message = Methods.color(convertList(getDefaultListMessage(), placeholders));
			}
		}else {
			if(exists()) {
				message = Methods.getPrefix(Files.MESSAGES.getFile().getString("Messages." + path));
			}else {
				message = Methods.getPrefix(getDefaultMessage());
			}
			for(String ph : placeholders.keySet()) {
				if(message.contains(ph)) {
					message = message.replaceAll(ph, placeholders.get(ph));
				}
			}
		}
		return message;
	}
	
	public String getMessageNoPrefix() {
		if(isList()) {
			if(exists()) {
				return Methods.color(convertList(Files.MESSAGES.getFile().getStringList("Messages." + path)));
			}else {
				return Methods.color(convertList(getDefaultListMessage()));
			}
		}else {
			if(exists()) {
				return Methods.color(Files.MESSAGES.getFile().getString("Messages." + path));
			}else {
				return Methods.color(getDefaultMessage());
			}
		}
	}
	
	public String getMessageNoPrefix(HashMap<String, String> placeholders) {
		String message;
		if(isList()) {
			if(exists()) {
				message = Methods.color(convertList(Files.MESSAGES.getFile().getStringList("Messages." + path), placeholders));
			}else {
				message = Methods.color(convertList(getDefaultListMessage(), placeholders));
			}
		}else {
			if(exists()) {
				message = Methods.color(Files.MESSAGES.getFile().getString("Messages." + path));
			}else {
				message = Methods.color(getDefaultMessage());
			}
			for(String ph : placeholders.keySet()) {
				if(message.contains(ph)) {
					message = message.replaceAll(ph, placeholders.get(ph));
				}
			}
		}
		return message;
	}
	
	public static String convertList(List<String> list) {
		String message = "";
		for(String m : list) {
			message += Methods.color(m) + "\n";
		}
		return message;
	}
	
	public static String convertList(List<String> list, HashMap<String, String> placeholders) {
		String message = "";
		for(String m : list) {
			message += Methods.color(m) + "\n";
		}
		for(String ph : placeholders.keySet()) {
			message = Methods.color(message.replaceAll(ph, placeholders.get(ph)));
		}
		return message;
	}
	
	public static void addMissingMessages() {
		FileConfiguration messages = Files.MESSAGES.getFile();
		Boolean saveFile = false;
		for(Messages message : values()) {
			if(!messages.contains("Messages." + message.getPath())) {
				System.out.println(message.getPath());
				saveFile = true;
				if(message.getDefaultMessage() != null) {
					messages.set("Messages." + message.getPath(), message.getDefaultMessage());
				}else {
					messages.set("Messages." + message.getPath(), message.getDefaultListMessage());
				}
			}
		}
		if(saveFile) {
			Files.MESSAGES.saveFile();
		}
	}
	
	private Boolean exists() {
		return Files.MESSAGES.getFile().contains("Messages." + path);
	}
	
	private Boolean isList() {
		if(Files.MESSAGES.getFile().contains("Messages." + path)) {
			return !Files.MESSAGES.getFile().getStringList("Messages." + path).isEmpty();
		}else {
			return defaultMessage == null;
		}
	}
	
	private String getPath() {
		return path;
	}
	
	private String getDefaultMessage() {
		return defaultMessage;
	}
	
	private List<String> getDefaultListMessage() {
		return defaultListMessage;
	}
	
}