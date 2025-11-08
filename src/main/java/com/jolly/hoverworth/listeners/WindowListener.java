package com.jolly.hoverworth.listeners;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.jolly.hoverworth.HoverWorth;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WindowListener implements PacketListener, Listener {
    private final HoverWorth plugin;
    private final String symbol;
    MiniMessage mm = MiniMessage.miniMessage();
    public WindowListener(HoverWorth plugin) {
        this.plugin = plugin;
        this.symbol = plugin.getConfig().getString("settings.currency-symbol", "$");
    }



    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
            List<ItemStack> items = wrapper.getItems();
            for (int i = 0; i < items.size(); i++) {
                items.set(i, addLore(items.get(i)));
            }
            wrapper.setItems(items);
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper =
                    new WrapperPlayServerSetSlot(event);

            ItemStack item = wrapper.getItem();
            if (item != null && !item.isEmpty()) {
                wrapper.setItem(addLore(item));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        org.bukkit.inventory.ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        //org.bukkit.inventory.ItemStack itemClone = event.getCurrentItem().clone();
        //todo, add dynamic lore based in item amount
        event.setCurrentItem(addLoreBukkit(item));
    }

    private ItemStack addLore(ItemStack packetItem) {
        if (packetItem == null || packetItem.isEmpty())
            return packetItem;

        org.bukkit.inventory.ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(packetItem);
        org.bukkit.inventory.ItemStack updated = addLoreBukkit(bukkitItem);
        return SpigotConversionUtil.fromBukkitItemStack(updated);
    }

    private org.bukkit.inventory.ItemStack addLoreBukkit(org.bukkit.inventory.ItemStack bukkitItem) {
        if (bukkitItem == null || bukkitItem.getType() == Material.AIR)
            return bukkitItem;

        ItemMeta meta = bukkitItem.getItemMeta();
        if (meta == null) return bukkitItem;

        Material item = bukkitItem.getType();
        String itemKey = item.name().toUpperCase();

        String integration = plugin.getConfig().getString("settings.integration", "none");
        if (!plugin.getWorthFile().get().contains(itemKey) && integration.equals("none"))
            return bukkitItem;

        List<Component> lore = meta.lore() != null ? meta.lore() : new ArrayList<>();
        String symbol = plugin.getConfig().getString("settings.currency-symbol", "$");
        
        double worth = plugin.getWorthFile().get().getDouble(itemKey + ".worth");

        if (integration.equalsIgnoreCase("EconomyShopGUI")) {
            Map<Material, Double> esgui = plugin.getEconomyShopGUI().esguiWorths;
            if (esgui.containsKey(item)) { worth = esgui.get(item); }
        }

        if (plugin.debug) plugin.getLogger().info(symbol + " " + integration + " " + worth);

        lore = updateWorthLine(lore, worth, symbol);

        meta.lore(lore);
        bukkitItem.setItemMeta(meta);

        addDescriptionLine(meta, lore, itemKey);
        bukkitItem.setItemMeta(meta);

        return bukkitItem;
    }

    private List<Component> updateWorthLine(List<Component> lore, double worth, String symbol) {
        String template = plugin.getConfig().getString(
                "settings.lore-message",
                "<white>Worth: <gold>{currency-symbol}{worth}<white>/pc"
        );

        Component worthLine = mm.deserialize(
                template.replace("{currency-symbol}", symbol)
                        .replace("{worth}", String.valueOf(worth))
        ).decoration(TextDecoration.ITALIC, false);

        lore.removeIf(line -> PlainTextComponentSerializer.plainText()
                .serialize(line).startsWith("Worth:"));

        lore.addFirst(worthLine);
        return lore;
    }

    private void addDescriptionLine(ItemMeta meta, List<Component> lore, String itemKey) {
        String desc = plugin.getWorthFile().get().getString(itemKey + ".description", "");
        if (desc.isEmpty()) return;

        String plainDesc = PlainTextComponentSerializer.plainText()
                .serialize(mm.deserialize(desc)).trim();

        boolean hasDesc = lore.stream()
                .map(line -> PlainTextComponentSerializer.plainText().serialize(line).trim())
                .anyMatch(lineText -> lineText.equalsIgnoreCase(plainDesc));

        if (hasDesc) return;

        Component descLine = mm.deserialize(desc);
        if (!desc.contains("<italic>") && !desc.contains("<i>")) {
            descLine = descLine.decoration(TextDecoration.ITALIC, false);
        }

        lore.addLast(descLine);
        meta.lore(lore);
    }


}