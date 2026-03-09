package de.imhenne187.baltop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.text.NumberFormat;
import java.util.*;

public class BaltopGUI {
    public static final int PLAYERS_PER_PAGE = 45;
    public static final String TITLE_PREFIX = "Baltop | Page ";
    public static final String TITLE_SEARCH = "Baltop | Searched: ";

    private static Plugin plugin;

    public static void init(Plugin p) {
        plugin = p;
    }

    private static void openDelayed(Player viewer, Inventory gui) {
        plugin.getServer().getRegionScheduler().runDelayed(plugin, viewer.getLocation(), task -> {
            viewer.openInventory(gui);
        }, 5L);
    }

    public static void open(Player viewer, int page) {
        List<DatabaseManager.PlayerData> sorted = DatabaseManager.getTopPlayers();

        int viewerRank = DatabaseManager.getPlayerRank(viewer.getUniqueId());

        int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / PLAYERS_PER_PAGE));
        if (page < 1) page = totalPages;
        if (page > totalPages) page = 1;

        String title = TITLE_PREFIX + page;
        Inventory gui = Bukkit.createInventory(null, 54, title);

        int startIndex = (page - 1) * PLAYERS_PER_PAGE;
        for (int slot = 0; slot < PLAYERS_PER_PAGE; slot++) {
            int playerIndex = startIndex + slot;
            if (playerIndex < sorted.size()) {
                DatabaseManager.PlayerData playerData = sorted.get(playerIndex);
                boolean isViewer = playerData.getUuid().equals(viewer.getUniqueId());
                gui.setItem(slot, buildPlayerHead(playerData, playerIndex + 1, isViewer));
            }
        }

        gui.setItem(45, buildItem(Material.ARROW, "§aPrevious Page", null));
        gui.setItem(46, buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", null));
        gui.setItem(47, buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", null));
        gui.setItem(48, buildItem(Material.EMERALD, "§aRefresh", "§7Click to refresh the list"));

        gui.setItem(49, buildOwnHead(viewer, viewerRank));

        gui.setItem(50, buildItem(Material.OAK_SIGN, "§bSearch Player", "§7Click to search for a player"));
        gui.setItem(51, buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", null));
        gui.setItem(52, buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", null));
        gui.setItem(53, buildItem(Material.ARROW, "§aNext Page", null));

        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }

        openDelayed(viewer, gui);
    }

    public static void openSearchResult(Player viewer, String searchName) {
        List<DatabaseManager.PlayerData> players = DatabaseManager.getTopPlayers();
        DatabaseManager.PlayerData foundPlayer = null;
        int foundRank = -1;

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equalsIgnoreCase(searchName)) {
                foundPlayer = players.get(i);
                foundRank = i + 1;
                break;
            }
        }

        Inventory gui = Bukkit.createInventory(null, 54, TITLE_SEARCH + searchName);

        if (foundPlayer != null) {
            boolean isViewer = foundPlayer.getUuid().equals(viewer.getUniqueId());
            gui.setItem(22, buildPlayerHead(foundPlayer, foundRank, isViewer));
        } else {
            gui.setItem(22, buildItem(Material.BARRIER, "§cPlayer not found", ""));
        }

        gui.setItem(49, buildItem(Material.ARROW, "§aBack", "§7Back to Baltop"));

        ItemStack filler = buildItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }

        openDelayed(viewer, gui);
    }

    static ItemStack buildPlayerHead(DatabaseManager.PlayerData playerData, int rank, boolean isViewer) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerData.getUuid());
        meta.setOwningPlayer(offlinePlayer);

        String rankStr = rank == -1 ? "" : " §7| §a#" + rank;
        String name = isViewer
                ? "§7" + playerData.getName() + " §2◄ You"
                : "§7" + playerData.getName();
        meta.setDisplayName(name);
        meta.setLore(List.of("§2" + formatBalance(playerData.getBalance()) + rankStr ));
        skull.setItemMeta(meta);
        return skull;
    }

    static ItemStack buildOwnHead(Player player, int rank) {
        Optional<DatabaseManager.PlayerData> playerDataOpt = DatabaseManager.getPlayer(player.getUniqueId());
        double balance = playerDataOpt.map(DatabaseManager.PlayerData::getBalance).orElse(0.0);

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(player);
        meta.setDisplayName("§7" + player.getName());
        meta.setLore(List.of("§2" + formatBalance(balance) + " §7| §a#" + rank));
        skull.setItemMeta(meta);
        return skull;
    }

    private static String formatBalance(double balance) {
        if (balance >= 1_000_000_000_000_000L) {
            return String.format("%.2fQ", balance / 1_000_000_000_000_000L);
        } else if (balance >= 1_000_000_000_000L) {
            return String.format("%.2fT", balance / 1_000_000_000_000L);
        } else if (balance >= 1_000_000_000) {
            return String.format("%.2fB", balance / 1_000_000_000);
        } else if (balance >= 1_000_000) {
            return String.format("%.2fM", balance / 1_000_000);
        } else if (balance >= 1_000) {
            return String.format("%.2fK", balance / 1_000);
        } else {
            return String.format("%.2f", balance);
        }
    }

    static ItemStack buildItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore != null) meta.setLore(List.of(lore));
        item.setItemMeta(meta);
        return item;
    }
}