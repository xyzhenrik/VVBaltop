package de.imhenne187.baltop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SignSearchHandler implements Listener {

    private static final Map<UUID, BlockState> savedStates = new HashMap<>();
    private static final Map<UUID, Location> savedLocations = new HashMap<>();

    private final Plugin plugin;

    public SignSearchHandler(Plugin plugin) {
        this.plugin = plugin;
    }

    public static void openSearch(Player player, Plugin plugin) {
        Location loc = player.getLocation().getBlock().getLocation();

        plugin.getServer().getRegionScheduler().run(plugin, loc, task -> {
            Block block = loc.getBlock();

            savedStates.put(player.getUniqueId(), block.getState());
            savedLocations.put(player.getUniqueId(), loc);

            block.setType(Material.OAK_SIGN, false);

            BlockState state = block.getState();
            if (!(state instanceof Sign sign)) {
                cleanup(player.getUniqueId(), plugin);
                return;
            }

            sign.getSide(Side.FRONT).line(0, Component.text(""));
            sign.getSide(Side.FRONT).line(1, Component.text("^^^^^^^^^^"));
            sign.getSide(Side.FRONT).line(2, Component.text("INPUT HERE"));
            sign.getSide(Side.FRONT).line(3, Component.text(""));
            sign.update(true, false);



            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                BlockState freshState = block.getState();
                if (freshState instanceof Sign freshSign) {
                    player.openSign(freshSign, Side.FRONT);
                }
            }, 5L);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Location loc = savedLocations.get(event.getPlayer().getUniqueId());
        if (loc != null && loc.equals(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent event) {
        Location loc = savedLocations.get(event.getPlayer().getUniqueId());
        if (loc != null && loc.equals(event.getBlockState().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!savedStates.containsKey(uuid)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        String input = event.line(0) != null
                ? PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim()
                : "";

        cleanup(uuid, plugin);

        player.getScheduler().runDelayed(plugin, t -> {
            if (input.isEmpty()) {
                BaltopGUI.open(player, 1);
            } else {
                BaltopGUI.openSearchResult(player, input);
            }
        }, null, 1L);
    }

    private static void cleanup(UUID uuid, Plugin plugin) {
        BlockState original = savedStates.remove(uuid);
        Location loc = savedLocations.remove(uuid);
        if (original == null || loc == null) return;

        plugin.getServer().getRegionScheduler().run(plugin, loc, task ->
                original.update(true, false));
    }
}