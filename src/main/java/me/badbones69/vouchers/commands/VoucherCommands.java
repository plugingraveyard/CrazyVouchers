package me.badbones69.vouchers.commands;

import me.badbones69.vouchers.Methods;
import me.badbones69.vouchers.Vouchers;
import me.badbones69.vouchers.api.objects.ItemBuilder;
import me.badbones69.vouchers.api.objects.Voucher;
import me.badbones69.vouchers.controllers.GUI;
import me.badbones69.vouchers.api.FileManager;
import me.badbones69.vouchers.api.FileManager.Files;
import me.badbones69.vouchers.api.CrazyManager;
import me.badbones69.vouchers.api.enums.Messages;
import me.badbones69.vouchers.api.events.RedeemVoucherCodeEvent;
import me.badbones69.vouchers.api.objects.VoucherCode;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Random;

public class VoucherCommands implements CommandExecutor {

    private final Vouchers plugin = Vouchers.getPlugin();
    private final FileManager fileManager = plugin.getFileManager();
    private final CrazyManager crazyManager = plugin.getCrazyManager();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (args.length == 0) {
            plugin.getServer().dispatchCommand(sender, "voucher help");
            return true;
        } else {
            switch (args[0].toLowerCase()) {
                case "help":

                    if (Methods.hasPermission(sender, "access")) {
                        sender.sendMessage(Messages.HELP.getMessageNoPrefix());
                    }

                    return true;
                case "reload":
                    if (Methods.hasPermission(sender, "admin")) {
                        fileManager.reloadAllFiles();
                        fileManager.setup();

                        if (!Files.DATA.getFile().contains("Players")) {
                            Files.DATA.getFile().set("Players.Clear", null);
                            Files.DATA.saveFile();
                        }

                        crazyManager.load();
                        sender.sendMessage(Messages.RELOAD.getMessage());
                    }

                    return true;
                case "open":
                case "admin":
                    if (Methods.hasPermission(sender, "admin")) {
                        int page = 1;

                        if (args.length >= 2) page = Methods.isInt(args[1]) ? Integer.parseInt(args[1]) : 1;

                        GUI.openGUI((Player) sender, page);
                    }

                    return true;
                case "types":
                case "list":
                    if (Methods.hasPermission(sender, "admin")) {
                        StringBuilder vouchers = new StringBuilder();
                        StringBuilder codes = new StringBuilder();

                        for (Voucher voucher : crazyManager.getVouchers()) {
                            vouchers.append("&a").append(voucher.getName()).append("&8, ");
                        }

                        for (VoucherCode code : crazyManager.getVoucherCodes()) {
                            codes.append("&a").append(code.getCode()).append("&8, ");
                        }

                        vouchers = new StringBuilder((vouchers.length() == 0) ? "&cNone" : vouchers.substring(0, vouchers.length() - 2));
                        codes = new StringBuilder((codes.length() == 0) ? "&cNone" : codes.substring(0, codes.length() - 2));
                        sender.sendMessage(Methods.color("&e&lVouchers #" + crazyManager.getVouchers().size() + ":&f " + vouchers));
                        sender.sendMessage(Methods.color("&e&lVoucher Codes #" + crazyManager.getVoucherCodes().size() + ":&f " + codes));
                    }

                    return true;
                case "redeem":
                    if (Methods.hasPermission(sender, "redeem")) {
                        if (args.length >= 2) {
                            String code = args[1];

                            if (!(sender instanceof Player)) {
                                sender.sendMessage(Messages.NOT_A_PLAYER.getMessage());
                                return true;
                            }

                            Player player = (Player) sender;
                            HashMap<String, String> placeholders = new HashMap<>();

                            placeholders.put("%Arg%", code);
                            placeholders.put("%Player%", player.getName());
                            placeholders.put("%World%", player.getWorld().getName());
                            placeholders.put("%X%", player.getLocation().getBlockX() + "");
                            placeholders.put("%Y%", player.getLocation().getBlockY() + "");
                            placeholders.put("%Z%", player.getLocation().getBlockZ() + "");

                            if (crazyManager.isVoucherCode(code)) {
                                VoucherCode voucherCode = crazyManager.getVoucherCode(code);

                                // Checking the permissions of the code.
                                if (!player.isOp() && !player.hasPermission("voucher.bypass")) {
                                    if (voucherCode.useWhiteListPermissions()) {
                                        for (String permission : voucherCode.getWhitelistPermissions()) {
                                            if (!player.hasPermission(permission)) {
                                                player.sendMessage(Messages.NO_PERMISSION_TO_VOUCHER.getMessage(placeholders));

                                                for (String command : voucherCode.getWhitelistCommands()) {
                                                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), Messages.replacePlaceholders(placeholders, crazyManager.replaceRandom(command)));
                                                }

                                                return true;
                                            }
                                        }
                                    }

                                    if (voucherCode.useWhitelistWorlds()) {
                                        if (voucherCode.getWhitelistWorlds().contains(player.getWorld().getName().toLowerCase())) {
                                            player.sendMessage(Methods.getPrefix(Messages.replacePlaceholders(placeholders, voucherCode.getWhitelistWorldMessage())));

                                            for (String command : voucherCode.getWhitelistWorldCommands()) {
                                                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), Messages.replacePlaceholders(placeholders, crazyManager.replaceRandom(command)));
                                            }

                                            return true;
                                        }
                                    }

                                    if (voucherCode.useBlacklistPermissions()) {
                                        for (String permission : voucherCode.getBlacklistPermissions()) {
                                            if (player.hasPermission(permission.toLowerCase())) {
                                                player.sendMessage(Methods.getPrefix(Messages.replacePlaceholders(placeholders, voucherCode.getBlacklistMessage())));

                                                for (String command : voucherCode.getBlacklistCommands()) {
                                                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), Messages.replacePlaceholders(placeholders, crazyManager.replaceRandom(command)));
                                                }

                                                return true;
                                            }
                                        }
                                    }
                                }

                                // Has permission to continue.
                                FileConfiguration data = Files.DATA.getFile();
                                String uuid = player.getUniqueId().toString();
                                // Checking if the player has used the code before.

                                if (data.contains("Players." + uuid)) {
                                    if (data.contains("Players." + uuid + ".Codes." + voucherCode.getName())) {
                                        if (data.getString("Players." + uuid + ".Codes." + voucherCode.getName()).equalsIgnoreCase("used")) {
                                            player.sendMessage(Messages.CODE_USED.getMessage(placeholders));
                                            return true;
                                        }
                                    }
                                }

                                //Checking the limit of the code.
                                if (voucherCode.useLimiter()) {
                                    if (data.contains("Voucher-Limit." + voucherCode.getName())) {
                                        if (data.getInt("Voucher-Limit." + voucherCode.getName()) < 1) {
                                            player.sendMessage(Messages.CODE_UNAVAILABLE.getMessage(placeholders));
                                            return true;
                                        }

                                        data.set("Voucher-Limit." + voucherCode.getName(), (data.getInt("Voucher-Limit." + voucherCode.getName()) - 1));
                                    } else {
                                        data.set("Voucher-Limit." + voucherCode.getName(), (voucherCode.getLimit() - 1));
                                    }

                                    Files.DATA.saveFile();
                                }

                                // Gives the reward to the player.
                                RedeemVoucherCodeEvent event = new RedeemVoucherCodeEvent(player, voucherCode);
                                plugin.getServer().getPluginManager().callEvent(event);

                                if (!event.isCancelled()) {
                                    data.set("Players." + uuid + ".Codes." + voucherCode.getName(), "used");
                                    Files.DATA.saveFile();

                                    for (String command : voucherCode.getCommands()) {
                                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), Messages.replacePlaceholders(placeholders, crazyManager.replaceRandom(command)));
                                    }

                                    if (voucherCode.getRandomCommands().size() >= 1) { // Picks a random command from the Random-Commands list.
                                        for (String command : voucherCode.getRandomCommands().get(new Random().nextInt(voucherCode.getRandomCommands().size())).getCommands()) {
                                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), Messages.replacePlaceholders(placeholders, crazyManager.replaceRandom(command)));
                                        }
                                    }

                                    if (voucherCode.getChanceCommands().size() >= 1) { // Picks a command based on the chance system of the Chance-Commands list.
                                        for (String command : voucherCode.getChanceCommands().get(new Random().nextInt(voucherCode.getChanceCommands().size())).getCommands()) {
                                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), Messages.replacePlaceholders(placeholders, crazyManager.replaceRandom(command)));
                                        }
                                    }

                                    for (ItemBuilder itemBuilder : voucherCode.getItems()) {
                                        if (!Methods.isInventoryFull(player)) {
                                            player.getInventory().addItem(itemBuilder.build());
                                        } else {
                                            player.getWorld().dropItem(player.getLocation(), itemBuilder.build());
                                        }
                                    }

                                    if (voucherCode.useSounds()) {
                                        for (Sound sound : voucherCode.getSounds()) {
                                            player.playSound(player.getLocation(), sound, 1, 1);
                                        }
                                    }

                                    if (voucherCode.useFireworks()) Methods.fireWork(player.getLocation(), voucherCode.getFireworkColors());

                                    if (!voucherCode.getMessage().equals("")) player.sendMessage(Methods.color(Messages.replacePlaceholders(placeholders, voucherCode.getMessage())));
                                }
                            } else {
                                player.sendMessage(Messages.CODE_UNAVAILABLE.getMessage(placeholders));
                                return true;
                            }

                            return true;
                        }
                        sender.sendMessage(Methods.getPrefix("&c/voucher redeem <code>"));
                    }
                    return true;
                case "give": // /Voucher 0Give 1<Type> 2[Amount] 3[Player] 4[Arguments]
                    if (Methods.hasPermission(sender, "admin")) {

                        if (args.length > 1) {
                            String name = sender.getName();

                            if (!crazyManager.isVoucherName(args[1])) {
                                sender.sendMessage(Messages.NOT_A_VOUCHER.getMessage());
                                return true;
                            }

                            Voucher voucher = crazyManager.getVoucher(args[1]);
                            int amount = 1;

                            if (args.length >= 3) {
                                if (!Methods.isInt(sender, args[2])) return true;
                                amount = Integer.parseInt(args[2]);
                            }

                            if (args.length >= 4) {
                                name = args[3];
                                if (!Methods.isOnline(sender, name)) return true;
                            }

                            Player player = plugin.getServer().getPlayer(name);
                            String argument = "";

                            if (args.length >= 5) {
                                // Gives a random number as the argument.
                                // /Voucher give test 1 %player% %random%:1-1000
                                argument = crazyManager.replaceRandom(args[4]);
                            }

                            ItemStack item = args.length >= 5 ? voucher.buildItem(argument, amount) : voucher.buildItem(amount);

                            if (Methods.isInventoryFull(player)) {
                                player.getWorld().dropItem(player.getLocation(), item);
                            } else {
                                player.getInventory().addItem(item);
                                player.updateInventory();
                            }

                            HashMap<String, String> placeholders = new HashMap<>();
                            placeholders.put("%Player%", player.getName());
                            placeholders.put("%Voucher%", voucher.getName());

                            if (!Messages.GIVEN_A_VOUCHER.isBlank()) sender.sendMessage(Messages.GIVEN_A_VOUCHER.getMessage(placeholders));

                            return true;
                        }

                        sender.sendMessage(Methods.getPrefix("&c/voucher give <type> [amount] [player] [arguments]"));
                        return true;
                    }
                    return true;
                case "giveall": // /Voucher 0GiveAll 1<Type> 2[Amount] 3[Arguments]
                    if (Methods.hasPermission(sender, "admin")) {

                        if (args.length > 1) {

                            if (!crazyManager.isVoucherName(args[1])) {
                                sender.sendMessage(Messages.NOT_A_VOUCHER.getMessage());
                                return true;
                            }

                            Voucher voucher = crazyManager.getVoucher(args[1]);
                            int amount = 1;

                            if (args.length >= 3) {
                                if (!Methods.isInt(sender, args[2])) return true;
                                amount = Integer.parseInt(args[2]);
                            }

                            String argument = "";

                            if (args.length >= 4) {
                                // Gives a random number as the argument.
                                // /voucher give test 1 %player% %random%:1-1000
                                argument = crazyManager.replaceRandom(args[3]);
                            }

                            ItemStack item = args.length >= 4 ? voucher.buildItem(argument, amount) : voucher.buildItem(amount);

                            for (Player player : plugin.getServer().getOnlinePlayers()) {
                                if (Methods.isInventoryFull(player)) {
                                    player.getWorld().dropItem(player.getLocation(), item);
                                } else {
                                    player.getInventory().addItem(item);
                                    player.updateInventory();
                                }
                            }

                            HashMap<String, String> placeholders = new HashMap<>();
                            placeholders.put("%Voucher%", voucher.getName());
                            sender.sendMessage(Messages.GIVEN_ALL_PLAYERS_VOUCHER.getMessage(placeholders));
                            return true;
                        }

                        sender.sendMessage(Methods.getPrefix("&c/voucher giveall <type> [amount] [arguments]"));
                        return true;
                    }
                    return true;
                default:
                    sender.sendMessage(Methods.getPrefix("&cPlease do /voucher help for more information."));
                    return true;
            }
        }
    }
}