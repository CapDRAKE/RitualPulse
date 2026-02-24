package fr.majestycraft.ritualpulse.storage;

import fr.majestycraft.ritualpulse.model.PlayerRitualData;

import java.util.Map;
import java.util.UUID;

public interface RitualStorage {

    void init() throws Exception;

    PlayerRitualData load(UUID uuid);

    void save(PlayerRitualData data);

    void saveAll(Map<UUID, PlayerRitualData> cache);

    void close();

    String getName();
}
