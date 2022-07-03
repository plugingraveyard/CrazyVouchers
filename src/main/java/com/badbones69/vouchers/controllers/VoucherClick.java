package com.badbones69.vouchers.controllers;

import com.badbones69.vouchers.Methods;
import com.badbones69.vouchers.api.FileManager;
import com.badbones69.vouchers.api.VouchersManager;
import com.badbones69.vouchers.api.enums.Messages;
import com.badbones69.vouchers.api.enums.Version;
import com.badbones69.vouchers.api.objects.ItemBuilder;
import com.badbones69.vouchers.api.objects.Voucher;
import com.badbones69.vouchers.api.enums.Support;
import com.badbones69.vouchers.api.events.RedeemVoucherEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class VoucherClick implements Listener {
    
    private final HashMap<Player, String> twoAuth = new HashMap<>();
    
    // This must run as highest, so it doesn't cause other plugins to check
    // the items that were added to the players inventory and replaced the item in the player's hand.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVoucherClick(PlayerInteractEvent e) {
        ItemStack item = getItemInHand(e.getPlayer());
        Player player = e.getPlayer();
        Action action = e.getAction();
        if (item != null && item.getType() != Material.AIR) {

            if (Version.isNewer(Version.v1_8_R3) && e.getHand() != EquipmentSlot.HAND) {
                return;
            }

            if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                Voucher voucher = VouchersManager.getVoucherFromItem(item);

                if (voucher != null && !voucher.isEdible()) {
                    e.setCancelled(true);
                    useVoucher(player, voucher, item);
                }
            }
        }
    }
    
    @EventHandler
    public void onItemConsume(PlayerItemConsumeEvent e) {
        ItemStack item = e.getItem();
        Voucher voucher = VouchersManager.getVoucherFromItem(item);
        if (voucher != null && voucher.isEdible()) {
            Player player = e.getPlayer();
            e.setCancelled(true);
            if (item.getAmount() > 1) {
                player.sendMessage(Messages.UNSTACK_ITEM.getMessage());
            } else {
                useVoucher(player, voucher, item);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onArmorStandClick(PlayerInteractEntityEvent e) {
        if (Version.isNewer(Version.v1_8_R3) && e.getHand() == EquipmentSlot.HAND && VouchersManager.getVoucherFromItem(getItemInHand(e.getPlayer())) != null) {
            e.setCancelled(true);
        }
    }
    
    private void useVoucher(Player player, Voucher voucher, ItemStack item) {
        FileConfiguration data = FileManager.Files.DATA.getFile();
        String argument = VouchersManager.getArgument(item, voucher);
        if (passesPermissionChecks(player, item, voucher, argument)) {
            String uuid = player.getUniqueId().toString();

            if (!player.hasPermission("voucher.bypass") && voucher.useLimiter() && data.contains("Players." + uuid + ".Vouchers." + voucher.getName())) {
                int amount = data.getInt("Players." + uuid + ".Vouchers." + voucher.getName());
                if (amount >= voucher.getLimiterLimit()) {
                    player.sendMessage(Messages.HIT_LIMIT.getMessage());
                    return;
                }
            }

            if (!voucher.isEdible() && voucher.useTwoStepAuthentication()) {
                if (twoAuth.containsKey(player)) {
                    if (!twoAuth.get(player).equalsIgnoreCase(voucher.getName())) {
                        player.sendMessage(Messages.TWO_STEP_AUTHENTICATION.getMessage());
                        twoAuth.put(player, voucher.getName());
                        return;
                    }
                } else {
                    player.sendMessage(Messages.TWO_STEP_AUTHENTICATION.getMessage());
                    twoAuth.put(player, voucher.getName());
                    return;
                }
            }

            twoAuth.remove(player);
            RedeemVoucherEvent event = new RedeemVoucherEvent(player, voucher, argument);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                voucherClick(player, item, voucher, argument);
            }
        }
    }
    
    @SuppressWarnings({"deprecation", "squid:CallToDeprecatedMethod"})
    private ItemStack getItemInHand(Player player) {
        if (Version.isNewer(Version.v1_8_R3)) {
            return player.getInventory().getItemInMainHand();
        } else {
            return player.getItemInHand();
        }
    }
    
    private boolean passesPermissionChecks(Player player, ItemStack item, Voucher voucher, String argument) {
        if (!player.isOp()) {
            HashMap<String, String> placeholders = new HashMap<>();
            placeholders.put("%Arg%", argument != null ? argument : "%arg%");
            placeholders.put("%Player%", player.getName());
            placeholders.put("%World%", player.getWorld().getName());
            placeholders.put("%X%", player.getLocation().getBlockX() + "");
            placeholders.put("%Y%", player.getLocation().getBlockY() + "");
            placeholders.put("%Z%", player.getLocation().getBlockZ() + "");
            if (voucher.useWhiteListPermissions()) {
                for (String permission : voucher.getWhitelistPermissions()) {
                    if (!player.hasPermission(permission.toLowerCase().replace("%arg%", argument != null ? argument : "%arg%"))) {
                        player.sendMessage(Messages.replacePlaceholders(placeholders, voucher.getWhitelistPermissionMessage()));
                        for (String command : voucher.getWhitelistCommands()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Messages.replacePlaceholders(placeholders, command));
                        }
                        return false;
                    }
                }
            }

            if (voucher.usesWhitelistWorlds() && !voucher.getWhitelistWorlds().contains(player.getWorld().getName().toLowerCase())) {
                player.sendMessage(Messages.replacePlaceholders(placeholders, voucher.getWhitelistWorldMessage()));
                for (String command : voucher.getWhitelistWorldCommands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Messages.replacePlaceholders(placeholders, command));
                }
                return false;
            }

            if (voucher.useBlackListPermissions()) {
                for (String permission : voucher.getBlackListPermissions()) {
                    if (player.hasPermission(permission.toLowerCase().replace("%arg%", argument != null ? argument : "%arg%"))) {
                        player.sendMessage(Messages.replacePlaceholders(placeholders, voucher.getBlackListMessage()));
                        for (String command : voucher.getBlacklistCommands()) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Messages.replacePlaceholders(placeholders, command));
                        }
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    private void voucherClick(Player player, ItemStack item, Voucher voucher, String argument) {
        Methods.removeItem(item, player);
        HashMap<String, String> placeholders = new HashMap<>();
        placeholders.put("%Arg%", argument != null ? argument : "%arg%");
        placeholders.put("%Player%", player.getName());
        placeholders.put("%World%", player.getWorld().getName());
        placeholders.put("%X%", player.getLocation().getBlockX() + "");
        placeholders.put("%Y%", player.getLocation().getBlockY() + "");
        placeholders.put("%Z%", player.getLocation().getBlockZ() + "");

        for (String command : voucher.getCommands()) {
            command = replacePlaceholders(command, player);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Messages.replacePlaceholders(placeholders, VouchersManager.replaceRandom(command)));
        }

        if (!voucher.getRandomCommands().isEmpty()) { // Picks a random command from the Random-Commands list.
            for (String command : voucher.getRandomCommands().get(getRandom(voucher.getRandomCommands().size())).getCommands()) {
                command = replacePlaceholders(command, player);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Messages.replacePlaceholders(placeholders, VouchersManager.replaceRandom(command)));
            }
        }

        if (!voucher.getChanceCommands().isEmpty()) { // Picks a command based on the chance system of the Chance-Commands list.
            for (String command : voucher.getChanceCommands().get(getRandom(voucher.getChanceCommands().size())).getCommands()) {
                command = replacePlaceholders(command, player);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Messages.replacePlaceholders(placeholders, VouchersManager.replaceRandom(command)));
            }
        }

        for (ItemBuilder itemBuilder : voucher.getItems()) {
            if (!Methods.isInventoryFull(player)) {
                player.getInventory().addItem(itemBuilder.build());
            } else {
                player.getWorld().dropItem(player.getLocation(), itemBuilder.build());
            }
        }

        if (voucher.playSounds()) {
            for (Sound sound : voucher.getSounds()) {
                player.playSound(player.getLocation(), sound, 1, 1);
            }
        }

        if (voucher.useFirework()) {
            Methods.fireWork(player.getLocation(), voucher.getFireworkColors());
        }

        if (!voucher.getVoucherUsedMessage().isEmpty()) {
            String message = replacePlaceholders(voucher.getVoucherUsedMessage(), player);
            player.sendMessage(Messages.replacePlaceholders(placeholders, message));
        }

        if (voucher.useLimiter()) {
            FileManager.Files.DATA.getFile().set("Players." + player.getUniqueId() + ".UserName", player.getName());
            FileManager.Files.DATA.getFile().set("Players." + player.getUniqueId() + ".Vouchers." + voucher.getName(), FileManager.Files.DATA.getFile().getInt("Players." + player.getUniqueId() + ".Vouchers." + voucher.getName()) + 1);
            FileManager.Files.DATA.saveFile();
        }
    }
    
    private String replacePlaceholders(String string, Player player) {
        if (Support.PLACEHOLDERAPI.isPluginLoaded()) {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, string);
        }
        return string;
    }
    
    private int getRandom(int max) {
        return ThreadLocalRandom.current().nextInt(max);
    }
    
}