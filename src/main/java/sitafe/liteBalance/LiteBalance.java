package sitafe.liteBalance;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.UUID;

public class LiteBalance extends JavaPlugin {

    private HashMap<UUID, Double> playerBalances = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("LiteBalance enabled!");
        // Инициализация или загрузка балансов (например, из файла)
    }

    public void addBalance(UUID playerId, double amount) {
        playerBalances.put(playerId, playerBalances.getOrDefault(playerId, 0.0) + amount);
    }

    public void removeBalance(UUID playerId, double amount) {
        double balance = playerBalances.getOrDefault(playerId, 0.0);
        if (balance >= amount) {
            playerBalances.put(playerId, balance - amount);
        }
    }

    public double getBalance(UUID playerId) {
        return playerBalances.getOrDefault(playerId, 0.0);
    }

    public void setBalance(UUID playerId, double amount) {
        playerBalances.put(playerId, amount);
    }
}