package fr.majestycraft.ritualpulse.storage;

import com.zaxxer.hikari.HikariConfig;
import fr.majestycraft.ritualpulse.RitualPulsePlugin;

public class MySqlRitualStorage extends AbstractSqlRitualStorage {

    public MySqlRitualStorage(RitualPulsePlugin plugin) {
        super(plugin);
    }

    @Override
    protected HikariConfig buildHikariConfig() {
        String host = plugin.getConfig().getString("storage.mysql.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        String db = plugin.getConfig().getString("storage.mysql.database", "minecraft");
        String user = plugin.getConfig().getString("storage.mysql.username", "root");
        String pass = plugin.getConfig().getString("storage.mysql.password", "");
        boolean ssl = plugin.getConfig().getBoolean("storage.mysql.use-ssl", false);
        int poolSize = Math.max(2, plugin.getConfig().getInt("storage.mysql.pool-size", 10));

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=" + ssl + "&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(poolSize);
        cfg.setPoolName("RitualPulse-MySQL");
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return cfg;
    }

    @Override
    protected String getCreateTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "uuid VARCHAR(36) NOT NULL PRIMARY KEY,"
                + "last_claim_at BIGINT NOT NULL DEFAULT 0,"
                + "streak INT NOT NULL DEFAULT 0,"
                + "freeze_tokens INT NOT NULL DEFAULT 0,"
                + "total_claims INT NOT NULL DEFAULT 0"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    }

    @Override
    protected String getUpsertSql() {
        return "INSERT INTO " + tableName + " (uuid, last_claim_at, streak, freeze_tokens, total_claims) VALUES (?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "last_claim_at=VALUES(last_claim_at), "
                + "streak=VALUES(streak), "
                + "freeze_tokens=VALUES(freeze_tokens), "
                + "total_claims=VALUES(total_claims)";
    }

    @Override
    public String getName() {
        return "MYSQL";
    }
}
