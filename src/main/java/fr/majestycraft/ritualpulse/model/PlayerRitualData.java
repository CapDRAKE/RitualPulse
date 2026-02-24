package fr.majestycraft.ritualpulse.model;

import java.util.UUID;

public class PlayerRitualData {

    private final UUID uuid;
    private long lastClaimAt;
    private int streak;
    private int freezeTokens;
    private int totalClaims;

    public PlayerRitualData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getLastClaimAt() {
        return lastClaimAt;
    }

    public void setLastClaimAt(long lastClaimAt) {
        this.lastClaimAt = Math.max(0L, lastClaimAt);
    }

    public int getStreak() {
        return streak;
    }

    public void setStreak(int streak) {
        this.streak = Math.max(0, streak);
    }

    public int getFreezeTokens() {
        return freezeTokens;
    }

    public void setFreezeTokens(int freezeTokens) {
        this.freezeTokens = Math.max(0, freezeTokens);
    }

    public int getTotalClaims() {
        return totalClaims;
    }

    public void setTotalClaims(int totalClaims) {
        this.totalClaims = Math.max(0, totalClaims);
    }
}
