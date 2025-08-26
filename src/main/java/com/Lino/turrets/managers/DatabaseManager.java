package com.Lino.turrets.managers;

import com.Lino.turrets.Turrets;
import com.Lino.turrets.models.Turret;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private final Turrets plugin;
    private Connection connection;

    public DatabaseManager(Turrets plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            connection = DriverManager.getConnection("jdbc:sqlite:" +
                    new File(dataFolder, "turrets.db").getAbsolutePath());

            createTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS turrets (" +
                "id TEXT PRIMARY KEY," +
                "owner_id TEXT NOT NULL," +
                "owner_name TEXT NOT NULL," +
                "world TEXT NOT NULL," +
                "x REAL NOT NULL," +
                "y REAL NOT NULL," +
                "z REAL NOT NULL," +
                "level INTEGER NOT NULL," +
                "kills INTEGER NOT NULL," +
                "ammo INTEGER NOT NULL," +
                "target_mode TEXT DEFAULT 'ALL_ENTITIES'" +
                ")";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);

            try {
                stmt.execute("ALTER TABLE turrets ADD COLUMN target_mode TEXT DEFAULT 'ALL_ENTITIES'");
            } catch (SQLException ignored) {
            }
        }
    }

    public void saveTurrets(List<Turret> turrets) {
        try {
            connection.setAutoCommit(false);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("DELETE FROM turrets");
            }

            String sql = "INSERT INTO turrets VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (Turret turret : turrets) {
                    pstmt.setString(1, turret.getId().toString());
                    pstmt.setString(2, turret.getOwnerId().toString());
                    pstmt.setString(3, turret.getOwnerName());
                    pstmt.setString(4, turret.getLocation().getWorld().getName());
                    pstmt.setDouble(5, turret.getLocation().getX());
                    pstmt.setDouble(6, turret.getLocation().getY());
                    pstmt.setDouble(7, turret.getLocation().getZ());
                    pstmt.setInt(8, turret.getLevel());
                    pstmt.setInt(9, turret.getKills());
                    pstmt.setInt(10, turret.getAmmo());
                    pstmt.setString(11, turret.getTargetMode().name());
                    pstmt.executeUpdate();
                }
            }

            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Turret> loadTurrets() {
        List<Turret> turrets = new ArrayList<>();
        String sql = "SELECT * FROM turrets";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID ownerId = UUID.fromString(rs.getString("owner_id"));
                String ownerName = rs.getString("owner_name");
                String worldName = rs.getString("world");
                double x = rs.getDouble("x");
                double y = rs.getDouble("y");
                double z = rs.getDouble("z");
                int level = rs.getInt("level");
                int kills = rs.getInt("kills");
                int ammo = rs.getInt("ammo");

                Turret.TargetMode targetMode = Turret.TargetMode.ALL_ENTITIES;
                try {
                    String mode = rs.getString("target_mode");
                    if (mode != null) {
                        targetMode = Turret.TargetMode.valueOf(mode);
                    }
                } catch (Exception ignored) {
                }

                if (Bukkit.getWorld(worldName) != null) {
                    Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                    Turret turret = new Turret(id, ownerId, ownerName, location, level, kills, ammo, targetMode);
                    turrets.add(turret);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return turrets;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}