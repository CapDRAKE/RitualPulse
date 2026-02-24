package fr.majestycraft.ritualpulse.storage;

import com.zaxxer.hikari.HikariConfig;
import fr.majestycraft.ritualpulse.RitualPulsePlugin;

import java.io.File;

public class SqliteRitualStorage extends AbstractSqlRitualStorage {

    public SqliteRitualStorage(RitualPulsePlugin plugin) {
        super(plugin);
    }

    @Override
    protected HikariConfig buildHikariConfig() {
        HikariConfig cfg = new HikariConfig();
        String dbFileName = plugin.getConfig().getString("storage.sqlite.file", "ritualpulse.db");
        File dbFile = new File(plugin.getDataFolder(), dbFileName);
        cfg.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        cfg.setMaximumPoolSize(1); // sqlite aime pas trop le multi pool de malade
        cfg.setPoolName("RitualPulse-SQLite");
        cfg.addDataSourceProperty("foreign_keys", "true");
        return cfg;
    }

    @Override
    protected String getTableName() {
        return "ritualpulse_players";
    }

    @Override
    protected String getCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "uuid TEXT PRIMARY KEY,"
                + "last_claim_at INTEGER NOT NULL DEFAULT 0,"
                + "streak INTEGER NOT NULL DEFAULT 0,"
                + "freeze_tokens INTEGER NOT NULL DEFAULT 0,"
                + "total_claims INTEGER NOT NULL DEFAULT 0"
                + ")";
    }

    @Override
    protected String getUpsertSql() {
        return "INSERT INTO " + tableName + " (uuid, last_claim_at, streak, freeze_tokens, total_claims) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT(uuid) DO UPDATE SET "
                + "last_claim_at=excluded.last_claim_at, "
                + "streak=excluded.streak, "
                + "freeze_tokens=excluded.freeze_tokens, "
                + "total_claims=excluded.total_claims";
    }

    @Override
    public String getName() {
        return "SQLITE";
    }
}
