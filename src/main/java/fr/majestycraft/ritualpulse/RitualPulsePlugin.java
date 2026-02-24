package fr.majestycraft.ritualpulse;

import fr.majestycraft.ritualpulse.command.RitualCommand;
import fr.majestycraft.ritualpulse.gui.RitualGui;
import fr.majestycraft.ritualpulse.listener.RitualListener;
import fr.majestycraft.ritualpulse.placeholder.RitualPlaceholderExpansion;
import fr.majestycraft.ritualpulse.service.ConfigDoctor;
import fr.majestycraft.ritualpulse.service.LangManager;
import fr.majestycraft.ritualpulse.service.RitualService;
import fr.majestycraft.ritualpulse.storage.MySqlRitualStorage;
import fr.majestycraft.ritualpulse.storage.RitualStorage;
import fr.majestycraft.ritualpulse.storage.SqliteRitualStorage;
import fr.majestycraft.ritualpulse.storage.YamlRitualStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RitualPulsePlugin extends JavaPlugin {

    private LangManager lang;
    private RitualStorage storage;
    private RitualService ritualService;
    private RitualGui ritualGui;
    private List<String> lastConfigWarnings = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.lang = new LangManager(this);
        this.lang.load();

        setupStorageAndServices();
        registerCommandsAndListeners();
        registerPlaceholderIfNeeded();
        runConfigDoctorAndLog("startup");

        getLogger().info("RitualPulse activé.");
        getServer().getConsoleSender().sendMessage(lang.prefixed("storage-ready").replace("%storage%", storage.getName()));
    }

    @Override
    public void onDisable() {
        try {
            if (ritualService != null) ritualService.saveAll();
        } catch (Exception e) {
            getLogger().warning("Erreur saveAll a la fermeture");
            e.printStackTrace();
        }

        try {
            if (storage != null) storage.close();
        } catch (Exception e) {
            getLogger().warning("Erreur fermeture storage");
            e.printStackTrace();
        }
    }

    private void setupStorageAndServices() {
        this.storage = buildStorageSafe();
        this.ritualService = new RitualService(this, storage);
        this.ritualGui = new RitualGui(this, ritualService);
    }

    private RitualStorage buildStorageSafe() {
        String type = getConfig().getString("storage.type", "YAML").toUpperCase();
        RitualStorage selected;

        switch (type) {
            case "SQLITE" -> selected = new SqliteRitualStorage(this);
            case "MYSQL" -> selected = new MySqlRitualStorage(this);
            default -> selected = new YamlRitualStorage(this);
        }

        try {
            selected.init();
            return selected;
        } catch (Exception e) {
            getLogger().severe("Storage init failed for type=" + type + ", fallback YAML");
            e.printStackTrace();
            try {
                RitualStorage yaml = new YamlRitualStorage(this);
                yaml.init();
                if (lang != null) getServer().getConsoleSender().sendMessage(lang.prefixed("storage-fallback"));
                return yaml;
            } catch (Exception ex) {
                throw new RuntimeException("Impossible d'initialiser un storage valide", ex);
            }
        }
    }

    private void registerCommandsAndListeners() {
        RitualCommand cmdExecutor = new RitualCommand(this, ritualService, ritualGui);
        PluginCommand cmd = getCommand("ritual");
        if (cmd == null) {
            throw new IllegalStateException("Commande /ritual absente de plugin.yml");
        }
        cmd.setExecutor(cmdExecutor);
        cmd.setTabCompleter(cmdExecutor);

        getServer().getPluginManager().registerEvents(new RitualListener(this, ritualGui, ritualService), this);
    }

    private void registerPlaceholderIfNeeded() {
        if (!getConfig().getBoolean("placeholders.enabled", true)) return;
        if (!getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) return;

        try {
            new RitualPlaceholderExpansion(this, ritualService).register();
            debug("PlaceholderAPI hook ok");
        } catch (Throwable t) {
            getLogger().warning("Impossible d'enregistrer l'expansion PlaceholderAPI");
            t.printStackTrace();
        }
    }

    public void reloadEverything() {
        reloadConfig();
        if (lang == null) lang = new LangManager(this);
        lang.load();

        // reload storage a chaud = pas ideal, donc on garde le storage actuel (restart pour changer le type)
        if (ritualService != null) ritualService.reloadConfigDependentValues();
        runConfigDoctorAndLog("reload");
    }

    public int runConfigDoctorAndLog(String reason) {
        this.lastConfigWarnings = new ArrayList<>(ConfigDoctor.validate(this));
        if (lastConfigWarnings.isEmpty()) {
            getLogger().info("[doctor] config ok (" + reason + ")");
            return 0;
        }

        getLogger().warning("[doctor] " + lastConfigWarnings.size() + " warning(s) config détecté(s) (" + reason + ")");
        int maxConsole = Math.min(12, lastConfigWarnings.size());
        for (int i = 0; i < maxConsole; i++) {
            getLogger().warning("[doctor] - " + lastConfigWarnings.get(i));
        }
        if (lastConfigWarnings.size() > maxConsole) {
            getLogger().warning("[doctor] ... +" + (lastConfigWarnings.size() - maxConsole) + " autres warnings");
        }
        return lastConfigWarnings.size();
    }

    public List<String> getLastConfigWarnings() {
        return Collections.unmodifiableList(lastConfigWarnings);
    }

    public LangManager getLang() {
        return lang;
    }

    public RitualService getRitualService() {
        return ritualService;
    }

    public ZoneId getPluginZoneId() {
        String zone = getConfig().getString("time-zone", "Europe/Paris");
        try {
            return ZoneId.of(zone);
        } catch (Exception e) {
            return ZoneId.of("Europe/Paris");
        }
    }

    public void debug(String msg) {
        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("[debug] " + msg);
        }
    }
}
