package com.server.settings;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerSettings extends JavaPlugin implements Listener {

    // เก็บสถานะผู้เล่นแต่ละคน
    private Map<UUID, Boolean> nightVision = new HashMap<>();
    private Map<UUID, Boolean> chatNotify = new HashMap<>();
    private Map<UUID, Boolean> scoreboardVisible = new HashMap<>();
    private Map<UUID, Boolean> playerVisibility = new HashMap<>();
    private Map<UUID, Boolean> autoSell = new HashMap<>();

    private File dataFile;
    private FileConfiguration dataConfig;

    private static final String MENU_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "⚙ ตั้งค่า";

    @Override
    public void onEnable() {
        dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAllData();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("PlayerSettings โหลดแล้ว!");
    }

    @Override
    public void onDisable() {
        saveAllData();
    }

    // ==================== คำสั่ง ====================
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "ใช้ได้เฉพาะในเกมเท่านั้น!");
            return true;
        }
        Player player = (Player) sender;
        openSettingsMenu(player);
        return true;
    }

    // ==================== เปิดเมนู ====================
    private void openSettingsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, MENU_TITLE);
        UUID uuid = player.getUniqueId();

        // กระจกตกแต่งขอบ
        ItemStack border = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Night Vision - ช่อง 10
        boolean nv = nightVision.getOrDefault(uuid, false);
        inv.setItem(10, makeItem(
            nv ? Material.GOLDEN_CARROT : Material.CARROT,
            (nv ? ChatColor.GREEN : ChatColor.RED) + "✦ Night Vision",
            Arrays.asList(
                ChatColor.GRAY + "มองเห็นในที่มืดได้",
                "",
                ChatColor.YELLOW + "สถานะ: " + (nv ? ChatColor.GREEN + "เปิด ✔" : ChatColor.RED + "ปิด ✘"),
                "",
                ChatColor.WHITE + "คลิกเพื่อสลับ"
            )
        ));

        // การแจ้งเตือนแชท - ช่อง 12
        boolean cn = chatNotify.getOrDefault(uuid, true);
        inv.setItem(12, makeItem(
            cn ? Material.BELL : Material.DEAD_BUSH,
            (cn ? ChatColor.GREEN : ChatColor.RED) + "✦ แจ้งเตือนแชท",
            Arrays.asList(
                ChatColor.GRAY + "รับการแจ้งเตือนเมื่อมีคนพูดถึงคุณ",
                "",
                ChatColor.YELLOW + "สถานะ: " + (cn ? ChatColor.GREEN + "เปิด ✔" : ChatColor.RED + "ปิด ✘"),
                "",
                ChatColor.WHITE + "คลิกเพื่อสลับ"
            )
        ));

        // Scoreboard - ช่อง 14
        boolean sb = scoreboardVisible.getOrDefault(uuid, true);
        inv.setItem(14, makeItem(
            sb ? Material.MAP : Material.PAPER,
            (sb ? ChatColor.GREEN : ChatColor.RED) + "✦ Scoreboard",
            Arrays.asList(
                ChatColor.GRAY + "แสดง/ซ่อน Scoreboard ด้านข้าง",
                "",
                ChatColor.YELLOW + "สถานะ: " + (sb ? ChatColor.GREEN + "เปิด ✔" : ChatColor.RED + "ปิด ✘"),
                "",
                ChatColor.WHITE + "คลิกเพื่อสลับ"
            )
        ));

        // ซ่อน/แสดงผู้เล่น - ช่อง 16
        boolean pv = playerVisibility.getOrDefault(uuid, true);
        inv.setItem(16, makeItem(
            pv ? Material.PLAYER_HEAD : Material.BARRIER,
            (pv ? ChatColor.GREEN : ChatColor.RED) + "✦ แสดงผู้เล่นอื่น",
            Arrays.asList(
                ChatColor.GRAY + "ซ่อน/แสดงผู้เล่นคนอื่นในเซิร์ฟ",
                "",
                ChatColor.YELLOW + "สถานะ: " + (pv ? ChatColor.GREEN + "เปิด ✔" : ChatColor.RED + "ปิด ✘"),
                "",
                ChatColor.WHITE + "คลิกเพื่อสลับ"
            )
        ));

        // Auto Sell - ช่อง 22
        boolean as = autoSell.getOrDefault(uuid, false);
        inv.setItem(22, makeItem(
            as ? Material.CHEST : Material.TRAPPED_CHEST,
            (as ? ChatColor.GREEN : ChatColor.RED) + "✦ Auto Sell",
            Arrays.asList(
                ChatColor.GRAY + "ขายของอัตโนมัติเมื่อกระเป๋าเต็ม",
                "",
                ChatColor.YELLOW + "สถานะ: " + (as ? ChatColor.GREEN + "เปิด ✔" : ChatColor.RED + "ปิด ✘"),
                "",
                ChatColor.WHITE + "คลิกเพื่อสลับ"
            )
        ));

        player.openInventory(inv);
    }

    // ==================== คลิกในเมนู ====================
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(MENU_TITLE)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        UUID uuid = player.getUniqueId();
        int slot = e.getSlot();

        switch (slot) {
            case 10: toggleNightVision(player, uuid); break;
            case 12: toggleChatNotify(player, uuid); break;
            case 14: toggleScoreboard(player, uuid); break;
            case 16: togglePlayerVisibility(player, uuid); break;
            case 22: toggleAutoSell(player, uuid); break;
        }

        if (slot == 10 || slot == 12 || slot == 14 || slot == 16 || slot == 22) {
            openSettingsMenu(player);
            saveAllData();
        }
    }

    // ==================== ฟังก์ชันสลับแต่ละอัน ====================
    private void toggleNightVision(Player player, UUID uuid) {
        boolean current = nightVision.getOrDefault(uuid, false);
        nightVision.put(uuid, !current);
        if (!current) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            player.sendMessage(ChatColor.GREEN + "✔ Night Vision " + ChatColor.WHITE + "เปิดแล้ว!");
        } else {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.sendMessage(ChatColor.RED + "✘ Night Vision " + ChatColor.WHITE + "ปิดแล้ว!");
        }
    }

    private void toggleChatNotify(Player player, UUID uuid) {
        boolean current = chatNotify.getOrDefault(uuid, true);
        chatNotify.put(uuid, !current);
        player.sendMessage((!current ? ChatColor.GREEN + "✔ การแจ้งเตือนแชท " : ChatColor.RED + "✘ การแจ้งเตือนแชท ") + ChatColor.WHITE + (!current ? "เปิดแล้ว!" : "ปิดแล้ว!"));
    }

    private void toggleScoreboard(Player player, UUID uuid) {
        boolean current = scoreboardVisible.getOrDefault(uuid, true);
        scoreboardVisible.put(uuid, !current);
        if (!current) {
            // คืน scoreboard เดิมของเซิร์ฟ (ถ้ามี plugin scoreboard อื่น จะทำงานอัตโนมัติ)
            player.sendMessage(ChatColor.GREEN + "✔ Scoreboard " + ChatColor.WHITE + "เปิดแล้ว!");
        } else {
            Scoreboard blank = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(blank);
            player.sendMessage(ChatColor.RED + "✘ Scoreboard " + ChatColor.WHITE + "ปิดแล้ว!");
        }
    }

    private void togglePlayerVisibility(Player player, UUID uuid) {
        boolean current = playerVisibility.getOrDefault(uuid, true);
        playerVisibility.put(uuid, !current);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;
            if (!current) player.showPlayer(this, other);
            else player.hidePlayer(this, other);
        }
        player.sendMessage((!current ? ChatColor.GREEN + "✔ แสดงผู้เล่นอื่น " : ChatColor.RED + "✘ ซ่อนผู้เล่นอื่น ") + ChatColor.WHITE + (!current ? "เปิดแล้ว!" : "ปิดแล้ว!"));
    }

    private void toggleAutoSell(Player player, UUID uuid) {
        boolean current = autoSell.getOrDefault(uuid, false);
        autoSell.put(uuid, !current);
        player.sendMessage((!current ? ChatColor.GREEN + "✔ Auto Sell " : ChatColor.RED + "✘ Auto Sell ") + ChatColor.WHITE + (!current ? "เปิดแล้ว!" : "ปิดแล้ว!"));
    }

    // ==================== โหลดการตั้งค่าเมื่อเข้าเกม ====================
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        // คืน Night Vision ถ้าเปิดไว้
        if (nightVision.getOrDefault(uuid, false)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        }

        // คืนการซ่อนผู้เล่น
        if (!playerVisibility.getOrDefault(uuid, true)) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.equals(player)) player.hidePlayer(this, other);
            }
        }
    }

    // ==================== บันทึก/โหลดข้อมูล ====================
    private void saveAllData() {
        for (UUID uuid : nightVision.keySet())
            dataConfig.set("players." + uuid + ".nightVision", nightVision.get(uuid));
        for (UUID uuid : chatNotify.keySet())
            dataConfig.set("players." + uuid + ".chatNotify", chatNotify.get(uuid));
        for (UUID uuid : scoreboardVisible.keySet())
            dataConfig.set("players." + uuid + ".scoreboard", scoreboardVisible.get(uuid));
        for (UUID uuid : playerVisibility.keySet())
            dataConfig.set("players." + uuid + ".playerVisibility", playerVisibility.get(uuid));
        for (UUID uuid : autoSell.keySet())
            dataConfig.set("players." + uuid + ".autoSell", autoSell.get(uuid));
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadAllData() {
        if (!dataConfig.contains("players")) return;
        for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            nightVision.put(uuid, dataConfig.getBoolean("players." + uuidStr + ".nightVision", false));
            chatNotify.put(uuid, dataConfig.getBoolean("players." + uuidStr + ".chatNotify", true));
            scoreboardVisible.put(uuid, dataConfig.getBoolean("players." + uuidStr + ".scoreboard", true));
            playerVisibility.put(uuid, dataConfig.getBoolean("players." + uuidStr + ".playerVisibility", true));
            autoSell.put(uuid, dataConfig.getBoolean("players." + uuidStr + ".autoSell", false));
        }
    }

    // ==================== Helper สร้างไอเทม ====================
    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ==================== Public getter สำหรับ AutoSell ====================
    public boolean isAutoSellEnabled(UUID uuid) {
        return autoSell.getOrDefault(uuid, false);
    }

    public boolean isChatNotifyEnabled(UUID uuid) {
        return chatNotify.getOrDefault(uuid, true);
    }
}
