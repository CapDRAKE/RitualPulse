package fr.majestycraft.ritualpulse.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.majestycraft.ritualpulse.RitualPulsePlugin;
import fr.majestycraft.ritualpulse.model.PlayerRitualData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractSqlRitualStorage implements RitualStorage {

    protected final RitualPulsePlugin plugin;
    protected HikariDataSource dataSource;
    protected String tableName;

    protected AbstractSqlRitualStorage(RitualPulsePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init() throws Exception {
        HikariConfig hikari = buildHikariConfig();
        this.dataSource = new HikariDataSource(hikari);
        this.tableName = getTableName();
        createTable();
    }

    protected abstract HikariConfig buildHikariConfig();

    protected abstract String getCreateTableSql();

    protected abstract String getUpsertSql();

    protected String getTableName() {
        return plugin.getConfig().getString("storage.mysql.table-prefix", "ritualpulse_") + "players";
    }

    protected void createTable() throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate(getCreateTableSql());
        }
    }

    @Override
    public PlayerRitualData load(UUID uuid) {
        PlayerRitualData d = new PlayerRitualData(uuid);

        String sql = "SELECT last_claim_at, streak, freeze_tokens, total_claims FROM " + tableName + " WHERE uuid = ?";
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    d.setLastClaimAt(rs.getLong("last_claim_at"));
                    d.setStreak(rs.getInt("streak"));
                    d.setFreezeTokens(rs.getInt("freeze_tokens"));
                    d.setTotalClaims(rs.getInt("total_claims"));
                } else {
                    d.setFreezeTokens(plugin.getConfig().getInt("streak.freeze.start-tokens", 0));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur SQL load player " + uuid);
            e.printStackTrace();
        }

        return d;
    }

    @Override
    public void save(PlayerRitualData d) {
        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(getUpsertSql())) {
            bindUpsert(ps, d);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Erreur SQL save player " + d.getUuid());
            e.printStackTrace();
            throw new RuntimeException("sql save failed", e);
        }
    }

    protected void bindUpsert(PreparedStatement ps, PlayerRitualData d) throws SQLException {
        ps.setString(1, d.getUuid().toString());
        ps.setLong(2, d.getLastClaimAt());
        ps.setInt(3, d.getStreak());
        ps.setInt(4, d.getFreezeTokens());
        ps.setInt(5, d.getTotalClaims());
    }

    @Override
    public void saveAll(Map<UUID, PlayerRitualData> cache) {
        for (PlayerRitualData d : cache.values()) {
            save(d);
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
