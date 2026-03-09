package de.imhenne187.baltop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import me.clip.placeholderapi.PlaceholderAPI;

import java.io.File;
import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private static Connection connection;
    private static Plugin plugin;

    public static void init(Plugin p) {
        plugin = p;
        try {
            Class.forName("org.sqlite.JDBC");

            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "baltop.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            createTables();
            plugin.getLogger().info("SQLite database initialized successfully.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    private static void createTables() {
        String sql = """
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                balance REAL DEFAULT 0,
                last_updated INTEGER
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    public static void updatePlayerBalance(UUID uuid, String name, double balance) {
        String sql = """
            INSERT INTO players (uuid, name, balance, last_updated)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET
                name = excluded.name,
                balance = excluded.balance,
                last_updated = excluded.last_updated
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, name);
            pstmt.setDouble(3, balance);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update player balance: " + e.getMessage());
        }
    }

    public static List<PlayerData> getTopPlayers() {
        List<PlayerData> players = new ArrayList<>();
        String sql = "SELECT uuid, name, balance FROM players ORDER BY balance DESC";

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                players.add(new PlayerData(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("name"),
                    rs.getDouble("balance")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get top players: " + e.getMessage());
        }

        return players;
    }

    public static Optional<PlayerData> getPlayer(UUID uuid) {
        String sql = "SELECT uuid, name, balance FROM players WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(new PlayerData(
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("name"),
                    rs.getDouble("balance")
                ));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player: " + e.getMessage());
        }

        return Optional.empty();
    }

    public static int getPlayerRank(UUID uuid) {
        String sql = "SELECT COUNT(*) + 1 as rank FROM players WHERE balance > (SELECT balance FROM players WHERE uuid = ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("rank");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get player rank: " + e.getMessage());
        }

        return -1;
    }

    public static void updateAllPlayers() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().warning("PlaceholderAPI not found");
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String balanceStr = PlaceholderAPI.setPlaceholders(player, "%vault_eco_balance%");
            double balance = parseBalance(balanceStr);
            updatePlayerBalance(player.getUniqueId(), player.getName(), balance);
        }
    }

    private static double parseBalance(String balanceStr) {
        try {
            String cleaned = balanceStr.replaceAll("[^0-9.,]", "").replace(",", ".");
            if (cleaned.isEmpty()) return 0;
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
        }
    }

    public static class PlayerData {
        private final UUID uuid;
        private final String name;
        private final double balance;

        public PlayerData(UUID uuid, String name, double balance) {
            this.uuid = uuid;
            this.name = name;
            this.balance = balance;
        }

        public UUID getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public double getBalance() {
            return balance;
        }
    }
}
