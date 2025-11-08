package com.jolly.hoverworth.integrations;

import com.jolly.hoverworth.HoverWorth;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EconomyShopGUI {
    public Map<Material, Double> esguiWorths = new HashMap<>();
    private static HoverWorth plugin;

    public EconomyShopGUI(HoverWorth plugin) {
        EconomyShopGUI.plugin = plugin;
    }

    public void loadESGUI() {
        File shopFolder = new File(Bukkit.getPluginManager().getPlugin("EconomyShopGUI").getDataFolder(), "shops");
        if (shopFolder.exists()) {
            for (File file : Objects.requireNonNull(shopFolder.listFiles((dir, name) -> name.endsWith(".yml")))) {
                YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
                ConfigurationSection pages = yml.getConfigurationSection("pages");
                if (pages == null) continue;

                for (String pageKey : pages.getKeys(false)) {
                    ConfigurationSection items = pages.getConfigurationSection(pageKey + ".items");
                    if (items == null) continue;

                    for (String slotKey : items.getKeys(false)) {
                        String mat = yml.getString("pages." + pageKey + ".items." + slotKey + ".material");
                        double sell = yml.getDouble("pages." + pageKey + ".items." + slotKey + ".sell");
                        if (mat != null && sell > 0) {
                            Material material = Material.matchMaterial(mat);
                            if (material != null) esguiWorths.put(material, sell);
                        }
                    }
                }
            }
        }
    }
}
