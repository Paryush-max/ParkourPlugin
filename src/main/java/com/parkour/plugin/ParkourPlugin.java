package com.parkour.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ParkourPlugin extends JavaPlugin implements Listener {
    
    private Map<UUID, ParkourSession> activeSessions = new HashMap<>();
    private Map<UUID, Set<Location>> playerBlocks = new HashMap<>();
    private Map<String, List<LeaderboardEntry>> leaderboards = new HashMap<>();
    
    @Override
    public void onEnable() {
        getLogger().info("Parkour Plugin Enabled!");
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // Initialize leaderboards
        leaderboards.put("EASY", new ArrayList<>());
        leaderboards.put("MEDIUM", new ArrayList<>());
        leaderboards.put("HARD", new ArrayList<>());
    }
    
    @Override
    public void onDisable() {
        for (ParkourSession session : activeSessions.values()) {
            cleanupBlocks(session.getPlayer().getUniqueId());
        }
        getLogger().info("Parkour Plugin Disabled!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (command.getName().equalsIgnoreCase("pk")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("start")) {
                openDifficultyGUI(player);
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
                stopParkour(player);
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("leaderboard")) {
                openLeaderboardGUI(player);
                return true;
            } else {
                player.sendMessage("§e§lPARKOUR §7» §fUsage:");
                player.sendMessage("§e/pk start §7- Start parkour");
                player.sendMessage("§e/pk stop §7- Stop parkour");
                player.sendMessage("§e/pk leaderboard §7- View leaderboards");
                return true;
            }
        }
        
        return false;
    }
    
    private void openDifficultyGUI(Player player) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage("§e§lPARKOUR §7» §cYou're already in a parkour session! Use /pk stop first.");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 27, "§e§lSelect Difficulty");
        
        // Easy Mode - Green
        ItemStack easy = new ItemStack(Material.LIME_WOOL);
        ItemMeta easyMeta = easy.getItemMeta();
        easyMeta.setDisplayName("§a§lEASY MODE");
        List<String> easyLore = new ArrayList<>();
        easyLore.add("§7Jump Distance: §a3-4 blocks");
        easyLore.add("§7Height Change: §a±1 block");
        easyLore.add("§7Island Size: §aLarge");
        easyLore.add("");
        easyLore.add("§6Reward every 10 jumps:");
        easyLore.add("§e• 5 Diamonds");
        easyLore.add("§e• 16 Golden Apples");
        easyLore.add("");
        easyLore.add("§eClick to start!");
        easyMeta.setLore(easyLore);
        easy.setItemMeta(easyMeta);
        gui.setItem(11, easy);
        
        // Medium Mode - Yellow
        ItemStack medium = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta mediumMeta = medium.getItemMeta();
        mediumMeta.setDisplayName("§e§lMEDIUM MODE");
        List<String> mediumLore = new ArrayList<>();
        mediumLore.add("§7Jump Distance: §e4-5 blocks");
        mediumLore.add("§7Height Change: §e±2 blocks");
        mediumLore.add("§7Island Size: §eMedium");
        mediumLore.add("");
        mediumLore.add("§6Reward every 10 jumps:");
        mediumLore.add("§e• 10 Diamonds");
        mediumLore.add("§e• 32 Golden Apples");
        mediumLore.add("§e• 1 Netherite Ingot");
        mediumLore.add("");
        mediumLore.add("§eClick to start!");
        mediumMeta.setLore(mediumLore);
        medium.setItemMeta(mediumMeta);
        gui.setItem(13, medium);
        
        // Hard Mode - Red
        ItemStack hard = new ItemStack(Material.RED_WOOL);
        ItemMeta hardMeta = hard.getItemMeta();
        hardMeta.setDisplayName("§c§lHARD MODE");
        List<String> hardLore = new ArrayList<>();
        hardLore.add("§7Jump Distance: §c5-6 blocks");
        hardLore.add("§7Height Change: §c±3 blocks");
        hardLore.add("§7Island Size: §cSmall");
        hardLore.add("");
        hardLore.add("§6Reward every 10 jumps:");
        hardLore.add("§e• 20 Diamonds");
        hardLore.add("§e• 64 Golden Apples");
        hardLore.add("§e• 3 Netherite Ingots");
        hardLore.add("§e• 1 Enchanted Golden Apple");
        hardLore.add("");
        hardLore.add("§eClick to start!");
        hardMeta.setLore(hardLore);
        hard.setItemMeta(hardMeta);
        gui.setItem(15, hard);
        
        // Leaderboard Button
        ItemStack leaderboard = new ItemStack(Material.BOOK);
        ItemMeta lbMeta = leaderboard.getItemMeta();
        lbMeta.setDisplayName("§6§lVIEW LEADERBOARDS");
        List<String> lbLore = new ArrayList<>();
        lbLore.add("§7Click to view top players");
        lbLore.add("§7for each difficulty!");
        lbMeta.setLore(lbLore);
        leaderboard.setItemMeta(lbMeta);
        gui.setItem(22, leaderboard);
        
        player.openInventory(gui);
    }
    
    private void openLeaderboardGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, "§6§lParkour Leaderboards");
        
        // Easy Leaderboard
        ItemStack easyLB = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta easyMeta = easyLB.getItemMeta();
        easyMeta.setDisplayName("§a§lEASY MODE - Top 5");
        List<String> easyLore = new ArrayList<>();
        List<LeaderboardEntry> easyTop = getTop5("EASY");
        for (int i = 0; i < easyTop.size(); i++) {
            LeaderboardEntry entry = easyTop.get(i);
            easyLore.add("§e#" + (i + 1) + " §f" + entry.playerName + " §7- §a" + entry.score + " jumps");
        }
        if (easyTop.isEmpty()) {
            easyLore.add("§7No records yet!");
        }
        easyMeta.setLore(easyLore);
        easyLB.setItemMeta(easyMeta);
        gui.setItem(11, easyLB);
        
        // Medium Leaderboard
        ItemStack mediumLB = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta mediumMeta = mediumLB.getItemMeta();
        mediumMeta.setDisplayName("§e§lMEDIUM MODE - Top 5");
        List<String> mediumLore = new ArrayList<>();
        List<LeaderboardEntry> mediumTop = getTop5("MEDIUM");
        for (int i = 0; i < mediumTop.size(); i++) {
            LeaderboardEntry entry = mediumTop.get(i);
            mediumLore.add("§e#" + (i + 1) + " §f" + entry.playerName + " §7- §e" + entry.score + " jumps");
        }
        if (mediumTop.isEmpty()) {
            mediumLore.add("§7No records yet!");
        }
        mediumMeta.setLore(mediumLore);
        mediumLB.setItemMeta(mediumMeta);
        gui.setItem(13, mediumLB);
        
        // Hard Leaderboard
        ItemStack hardLB = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta hardMeta = hardLB.getItemMeta();
        hardMeta.setDisplayName("§c§lHARD MODE - Top 5");
        List<String> hardLore = new ArrayList<>();
        List<LeaderboardEntry> hardTop = getTop5("HARD");
        for (int i = 0; i < hardTop.size(); i++) {
            LeaderboardEntry entry = hardTop.get(i);
            hardLore.add("§e#" + (i + 1) + " §f" + entry.playerName + " §7- §c" + entry.score + " jumps");
        }
        if (hardTop.isEmpty()) {
            hardLore.add("§7No records yet!");
        }
        hardMeta.setLore(hardLore);
        hardLB.setItemMeta(hardMeta);
        gui.setItem(15, hardLB);
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        
        String title = event.getView().getTitle();
        
        if (title.equals("§e§lSelect Difficulty")) {
            event.setCancelled(true);
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
            
            ItemStack clicked = event.getCurrentItem();
            
            if (clicked.getType() == Material.LIME_WOOL) {
                player.closeInventory();
                startParkour(player, Difficulty.EASY);
            } else if (clicked.getType() == Material.YELLOW_WOOL) {
                player.closeInventory();
                startParkour(player, Difficulty.MEDIUM);
            } else if (clicked.getType() == Material.RED_WOOL) {
                player.closeInventory();
                startParkour(player, Difficulty.HARD);
            } else if (clicked.getType() == Material.BOOK) {
                player.closeInventory();
                openLeaderboardGUI(player);
            }
        } else if (title.equals("§6§lParkour Leaderboards")) {
            event.setCancelled(true);
        }
    }
    
    private void startParkour(Player player, Difficulty difficulty) {
        cleanupBlocks(player.getUniqueId());
        
        Location startLoc = player.getLocation().clone();
        startLoc.setY(startLoc.getY() + 80);
        
        ParkourSession session = new ParkourSession(player, startLoc, difficulty);
        activeSessions.put(player.getUniqueId(), session);
        playerBlocks.put(player.getUniqueId(), new HashSet<>());
        
        generateIsland(startLoc, session, true);
        
        player.teleport(startLoc.clone().add(0, 1, 0));
        player.sendMessage("§e§lPARKOUR §7» " + difficulty.getColor() + "§l" + difficulty.name() + " MODE §aStarted!");
        player.sendMessage("§e§lPARKOUR §7» §7Jump to the next island!");
        player.sendMessage("§e§lPARKOUR §7» §7Use §f/pk stop §7to exit.");
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeSessions.containsKey(player.getUniqueId())) {
                    generateNextIsland(session);
                }
            }
        }.runTaskLater(this, 20L);
    }
    
    private void stopParkour(Player player) {
        ParkourSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§e§lPARKOUR §7» §cYou're not in a parkour session!");
            return;
        }
        
        cleanupBlocks(player.getUniqueId());
        updateLeaderboard(player.getName(), session.getScore(), session.getDifficulty());
        player.sendMessage("§e§lPARKOUR §7» §eSession ended! Final Score: §f" + session.getScore());
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ParkourSession session = activeSessions.get(player.getUniqueId());
        
        if (session == null) return;
        
        Location playerLoc = player.getLocation();
        
        if (playerLoc.getY() < session.getStartLocation().getY() - 10) {
            player.sendMessage("§e§lPARKOUR §7» §cYou fell! Final Score: §f" + session.getScore());
            updateLeaderboard(player.getName(), session.getScore(), session.getDifficulty());
            stopParkour(player);
            return;
        }
        
        Location target = session.getCurrentTarget();
        if (target != null && isOnIsland(playerLoc, target)) {
            session.incrementScore();
            player.sendMessage("§e§lPARKOUR §7» §a+1 §7(Score: §f" + session.getScore() + "§7)");
            
            // Check for reward (every 10 jumps)
            if (session.getScore() % 10 == 0) {
                giveReward(player, session.getDifficulty());
            }
            
            generateNextIsland(session);
        }
    }
    
    private void giveReward(Player player, Difficulty difficulty) {
        player.sendMessage("§e§lPARKOUR §7» §6§l✦ MILESTONE REWARD! ✦");
        
        switch (difficulty) {
            case EASY:
                player.getInventory().addItem(new ItemStack(Material.DIAMOND, 5));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
                player.sendMessage("§e§lPARKOUR §7» §aReceived: §e5 Diamonds, 16 Golden Apples");
                break;
            case MEDIUM:
                player.getInventory().addItem(new ItemStack(Material.DIAMOND, 10));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 32));
                player.getInventory().addItem(new ItemStack(Material.NETHERITE_INGOT, 1));
                player.sendMessage("§e§lPARKOUR §7» §aReceived: §e10 Diamonds, 32 Golden Apples, 1 Netherite Ingot");
                break;
            case HARD:
                player.getInventory().addItem(new ItemStack(Material.DIAMOND, 20));
                player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 64));
                player.getInventory().addItem(new ItemStack(Material.NETHERITE_INGOT, 3));
                player.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                player.sendMessage("§e§lPARKOUR §7» §aReceived: §e20 Diamonds, 64 Golden Apples, 3 Netherite, 1 Enchanted Apple");
                break;
        }
        
        player.playSound(player.getLocation(), "entity.player.levelup", 1.0f, 1.0f);
    }
    
    private void updateLeaderboard(String playerName, int score, Difficulty difficulty) {
        String diffKey = difficulty.name();
        List<LeaderboardEntry> lb = leaderboards.get(diffKey);
        
        // Remove old entry for this player if exists
        lb.removeIf(entry -> entry.playerName.equals(playerName));
        
        // Add new entry
        lb.add(new LeaderboardEntry(playerName, score));
        
        // Sort by score (highest first)
        lb.sort((a, b) -> Integer.compare(b.score, a.score));
    }
    
    private List<LeaderboardEntry> getTop5(String difficulty) {
        List<LeaderboardEntry> lb = leaderboards.get(difficulty);
        return lb.subList(0, Math.min(5, lb.size()));
    }
    
    private boolean isOnIsland(Location playerLoc, Location islandCenter) {
        double distance = playerLoc.distance(islandCenter);
        return distance <= 5 && Math.abs(playerLoc.getY() - islandCenter.getY()) <= 2;
    }
    
    private void generateIsland(Location center, ParkourSession session, boolean isStart) {
        World world = center.getWorld();
        Random random = new Random();
        UUID playerId = session.getPlayer().getUniqueId();
        Set<Location> blocks = playerBlocks.get(playerId);
        
        Material[] baseMaterials = {Material.STONE, Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.STONE_BRICKS};
        Material[] decorMaterials = {Material.OAK_LEAVES, Material.BIRCH_LEAVES, Material.GRASS_BLOCK, Material.MOSS_BLOCK};
        
        Material base = baseMaterials[random.nextInt(baseMaterials.length)];
        Material decor = decorMaterials[random.nextInt(decorMaterials.length)];
        
        int size = session.getDifficulty().getIslandSize();
        if (isStart) size = 5;
        
        for (int x = -size; x <= size; x++) {
            for (int z = -size; z <= size; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance <= size) {
                    Location blockLoc = center.clone().add(x, -1, z);
                    blockLoc.getBlock().setType(base);
                    blocks.add(blockLoc);
                    
                    if (random.nextInt(100) < 20 && distance < size - 1) {
                        Location decorLoc = center.clone().add(x, 0, z);
                        decorLoc.getBlock().setType(decor);
                        blocks.add(decorLoc);
                    }
                }
            }
        }
        
        if (isStart) {
            Location markerLoc = center.clone().add(0, 0, 0);
            markerLoc.getBlock().setType(Material.EMERALD_BLOCK);
            blocks.add(markerLoc);
        }
    }
    
    private void generateNextIsland(ParkourSession session) {
        Location lastLoc = session.getCurrentTarget() != null ? session.getCurrentTarget() : session.getStartLocation();
        Random random = new Random();
        Difficulty diff = session.getDifficulty();
        
        int distance = random.nextInt(diff.getMaxDistance() - diff.getMinDistance() + 1) + diff.getMinDistance();
        int angle = random.nextInt(360);
        int yDiff = random.nextInt(diff.getHeightRange() * 2 + 1) - diff.getHeightRange();
        
        double radians = Math.toRadians(angle);
        double xOffset = Math.cos(radians) * distance;
        double zOffset = Math.sin(radians) * distance;
        
        Location newTarget = lastLoc.clone().add(xOffset, yDiff, zOffset);
        session.setCurrentTarget(newTarget);
        
        generateIsland(newTarget, session, false);
        
        Location markerLoc = newTarget.clone();
        markerLoc.getBlock().setType(Material.GOLD_BLOCK);
        playerBlocks.get(session.getPlayer().getUniqueId()).add(markerLoc);
    }
    
    private void cleanupBlocks(UUID playerId) {
        Set<Location> blocks = playerBlocks.remove(playerId);
        if (blocks != null) {
            for (Location loc : blocks) {
                loc.getBlock().setType(Material.AIR);
            }
        }
    }
    
    private enum Difficulty {
        EASY("§a", 3, 4, 1, 5),
        MEDIUM("§e", 4, 5, 2, 4),
        HARD("§c", 5, 6, 3, 3);
        
        private final String color;
        private final int minDistance;
        private final int maxDistance;
        private final int heightRange;
        private final int islandSize;
        
        Difficulty(String color, int minDistance, int maxDistance, int heightRange, int islandSize) {
            this.color = color;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
            this.heightRange = heightRange;
            this.islandSize = islandSize;
        }
        
        public String getColor() { return color; }
        public int getMinDistance() { return minDistance; }
        public int getMaxDistance() { return maxDistance; }
        public int getHeightRange() { return heightRange; }
        public int getIslandSize() { return islandSize; }
    }
    
    private class ParkourSession {
        private Player player;
        private Location startLocation;
        private Location currentTarget;
        private int score;
        private Difficulty difficulty;
        
        public ParkourSession(Player player, Location startLocation, Difficulty difficulty) {
            this.player = player;
            this.startLocation = startLocation;
            this.score = 0;
            this.difficulty = difficulty;
        }
        
        public Player getPlayer() { return player; }
        public Location getStartLocation() { return startLocation; }
        public Location getCurrentTarget() { return currentTarget; }
        public void setCurrentTarget(Location target) { this.currentTarget = target; }
        public int getScore() { return score; }
        public void incrementScore() { this.score++; }
        public Difficulty getDifficulty() { return difficulty; }
    }
    
    private class LeaderboardEntry {
        String playerName;
        int score;
        
        public LeaderboardEntry(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
        }
    }
}
