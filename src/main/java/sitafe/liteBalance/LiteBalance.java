package sitafe.liteBalance;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class LiteBalance extends JavaPlugin {

    private HashMap<UUID, Double> playerBalances = new HashMap<>();
    private File configFile;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        getLogger().info("LiteBalance enabled!");
        configFile = new File(getDataFolder(), "balances.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        loadBalances();

        // Регистрация кастомных команд
        CommandMap commandMap = getServer().getCommandMap();
        commandMap.register("litebalance", new BalanceCommand("balance", this));
        commandMap.register("litebalance", new PayCommand("pay", this));
        commandMap.register("litebalance", new SetBalanceCommand("setbalance", this));
    }

    private void loadBalances() {
        if (config.contains("balances")) {
            for (String uuid : config.getConfigurationSection("balances").getKeys(false)) {
                playerBalances.put(UUID.fromString(uuid), config.getDouble("balances." + uuid));
            }
        }
    }

    private void saveBalances() {
        for (UUID uuid : playerBalances.keySet()) {
            config.set("balances." + uuid.toString(), playerBalances.get(uuid));
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().severe("Could not save balances: " + e.getMessage());
        }
    }

    public void addBalance(UUID playerId, double amount) {
        playerBalances.put(playerId, playerBalances.getOrDefault(playerId, 0.0) + amount);
        saveBalances();
    }

    public void removeBalance(UUID playerId, double amount) {
        double balance = playerBalances.getOrDefault(playerId, 0.0);
        if (balance >= amount) {
            playerBalances.put(playerId, balance - amount);
        }
        saveBalances();
    }

    public double getBalance(UUID playerId) {
        return playerBalances.getOrDefault(playerId, 0.0);
    }

    public void setBalance(UUID playerId, double amount) {
        playerBalances.put(playerId, amount);
        saveBalances();
    }

    @Override
    public void onDisable() {
        saveBalances();
    }

    private class BalanceCommand extends Command {
        private final LiteBalance plugin;

        public BalanceCommand(String name, LiteBalance plugin) {
            super(name);
            this.plugin = plugin;
            setPermission("litebalance.balance");
            setDescription("Check your balance");
            setUsage("/balance");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }
            Player player = (Player) sender;
            UUID playerId = player.getUniqueId();
            double balance = plugin.getBalance(playerId);
            player.sendMessage("Your balance: " + balance);
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return Collections.emptyList();
        }
    }

    private class PayCommand extends Command {
        private final LiteBalance plugin;

        public PayCommand(String name, LiteBalance plugin) {
            super(name);
            this.plugin = plugin;
            setPermission("litebalance.pay");
            setDescription("Pay money to another player");
            setUsage("/pay <player> <amount>");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage("Usage: /pay <player> <amount>");
                return true;
            }
            Player player = (Player) sender;
            UUID fromId = player.getUniqueId();
            Player target = getServer().getPlayer(args[0]);
            if (target == null) {
                player.sendMessage("Player not found!");
                return true;
            }
            UUID toId = target.getUniqueId();
            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid amount!");
                return true;
            }
            if (amount <= 0) {
                player.sendMessage("Amount must be positive!");
                return true;
            }
            if (plugin.getBalance(fromId) >= amount) {
                plugin.removeBalance(fromId, amount);
                plugin.addBalance(toId, amount);
                player.sendMessage("Sent " + amount + " to " + target.getName());
                target.sendMessage(player.getName() + " sent you " + amount);
            } else {
                player.sendMessage("Insufficient funds!");
            }
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                String partialName = args[0].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String playerName = player.getName().toLowerCase();
                    if (playerName.startsWith(partialName)) {
                        completions.add(player.getName());
                    }
                }
            }
            return completions;
        }
    }

    private class SetBalanceCommand extends Command {
        private final LiteBalance plugin;

        public SetBalanceCommand(String name, LiteBalance plugin) {
            super(name);
            this.plugin = plugin;
            setPermission("litebalance.setbalance");
            setDescription("Set a player's balance");
            setUsage("/setbalance <player> <amount>");
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            if (!sender.hasPermission("litebalance.setbalance")) {
                sender.sendMessage("No permission!");
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage("Usage: /setbalance <player> <amount>");
                return true;
            }
            Player target = getServer().getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("Player not found!");
                return true;
            }
            UUID targetId = target.getUniqueId();
            double amount;
            try {
                amount = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid amount!");
                return true;
            }
            if (amount < 0) {
                sender.sendMessage("Amount cannot be negative!");
                return true;
            }
            plugin.setBalance(targetId, amount);
            sender.sendMessage("Set " + target.getName() + "'s balance to " + amount);
            return true;
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                String partialName = args[0].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String playerName = player.getName().toLowerCase();
                    if (playerName.startsWith(partialName)) {
                        completions.add(player.getName());
                    }
                }
            }
            return completions;
        }
    }
}