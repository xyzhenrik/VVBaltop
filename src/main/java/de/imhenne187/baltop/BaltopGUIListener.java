package de.imhenne187.baltop;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BaltopGUIListener implements Listener {

    private static final long RATE_LIMIT_MS = 30000; // 30 seconds
    private static final long MESSAGE_COOLDOWN_MS = 5000; // 5 seconds
    private final Map<UUID, Long> lastRefresh = new HashMap<>();
    private final Map<UUID, Long> lastMessage = new HashMap<>();
    private final Plugin plugin;

    public BaltopGUIListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();

        if (title.startsWith(BaltopGUI.TITLE_SEARCH)) {
            event.setCancelled(true);
            if (slot == 49) {
                plugin.getServer().getRegionScheduler().run(plugin, player.getLocation(), task ->
                        BaltopGUI.open(player, 1));
            }
            return;
        }

        if (!title.startsWith(BaltopGUI.TITLE_PREFIX)) return;
        event.setCancelled(true);

        int currentPage = 1;
        try {
            currentPage = Integer.parseInt(title.replace(BaltopGUI.TITLE_PREFIX, "").trim());
        } catch (Exception ignored) {}

        final int page = currentPage;

        switch (slot) {
            case 45 -> plugin.getServer().getRegionScheduler().run(plugin, player.getLocation(), task ->
                    BaltopGUI.open(player, page - 1));
            case 53 -> plugin.getServer().getRegionScheduler().run(plugin, player.getLocation(), task ->
                    BaltopGUI.open(player, page + 1));
            case 48 -> {
                UUID uuid = player.getUniqueId();
                long now = System.currentTimeMillis();
                Long lastRefreshTime = lastRefresh.get(uuid);

                if (lastRefreshTime != null && (now - lastRefreshTime) < RATE_LIMIT_MS) {
                    Long lastMsgTime = lastMessage.get(uuid);
                    if (lastMsgTime == null || (now - lastMsgTime) >= MESSAGE_COOLDOWN_MS) {
                        long remainingSeconds = (RATE_LIMIT_MS - (now - lastRefreshTime)) / 1000;
                        player.sendMessage("§cPlease wait §e" + remainingSeconds + " §cseconds before refreshing again.");
                        lastMessage.put(uuid, now);
                    }
                } else {
                    lastRefresh.put(uuid, now);
                    DatabaseManager.updateAllPlayers();
                    plugin.getServer().getRegionScheduler().run(plugin, player.getLocation(), task ->
                            BaltopGUI.open(player, page));
                }
            }
            case 50 -> plugin.getServer().getRegionScheduler().run(plugin, player.getLocation(), task -> {
                player.closeInventory();
                SignSearchHandler.openSearch(player, plugin);
            });
        }
    }
}
