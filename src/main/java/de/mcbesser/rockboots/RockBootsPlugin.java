package de.mcbesser.rockboots;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.Base64;

public final class RockBootsPlugin extends JavaPlugin implements Listener {
    private static final String MINUTE_BAR_SEGMENT = "\u25A0";
    private static final Material BOOTS_SLOT_PLACEHOLDER_MATERIAL = Material.LIME_STAINED_GLASS_PANE;
    private static final Material UPGRADE_SLOT_PLACEHOLDER_MATERIAL = Material.PURPLE_STAINED_GLASS_PANE;
    private static final String GUI_TITLE = ChatColor.DARK_GRAY + "Rock Boots Upgrade";
    private static final int GUI_SLOT_TYPE_BOOTS = 10;
    private static final int GUI_SLOT_TARGET_BOOTS = 13;
    private static final int GUI_SLOT_BOOK_UNBREAKING = 15;
    private static final int GUI_SLOT_BOOK_FROST = 16;
    private static final int GUI_SLOT_BOOK_EFFICIENCY = 21;
    private static final int GUI_SLOT_BOOK_FEATHER = 22;
    private static final int GUI_SLOT_BOOK_SOUL = 23;
    private static final float ROCK_BOOTS_FLY_SPEED = 0.05f;
    private static final float VANILLA_FLY_SPEED = 0.1f;
    private static final int DOUBLE_JUMP_WINDOW_TICKS = 14;
    private static final int MAX_ENERGY_CAP_SECONDS = 900;
    private static final double FEATHER_GLIDE_SINK_SPEED = 0.14;
    private static final long ACTION_BAR_PAUSE_MILLIS = 2500L;
    private static final Sound[] FLIGHT_SOUND_POOL = new Sound[]{
            Sound.ITEM_ELYTRA_FLYING,
            Sound.ENTITY_PHANTOM_FLAP,
            Sound.ENTITY_ENDER_DRAGON_FLAP
    };

    private NamespacedKey recipeKey;
    private NamespacedKey keyRockBoots;
    private NamespacedKey keyEnergy;
    private NamespacedKey keyMaxEnergy;
    private NamespacedKey keySlotTypeBoots;
    private NamespacedKey keySlotBookUnbreaking;
    private NamespacedKey keySlotBookFrost;
    private NamespacedKey keySlotBookEfficiency;
    private NamespacedKey keySlotBookFeather;
    private NamespacedKey keySlotBookSoul;
    private NamespacedKey keyUpgradeUnbreaking;
    private NamespacedKey keyUpgradeFrost;
    private NamespacedKey keyUpgradeEfficiency;
    private NamespacedKey keyUpgradeFeather;
    private NamespacedKey keyUpgradeSoul;
    private BukkitTask tickTask;

    private final Map<UUID, Integer> hoverTickCounter = new HashMap<>();
    private final Map<UUID, Set<Block>> carpetBlocks = new HashMap<>();
    private final Map<UUID, Integer> barTickCounter = new HashMap<>();
    private final Map<UUID, Integer> jumpWindowTicks = new HashMap<>();
    private final Set<UUID> elytraSoundActive = new HashSet<>();
    private final Map<UUID, ItemStack> upgradeBaseBoots = new HashMap<>();
    private final Map<UUID, Integer> upgradeTargetSlot = new HashMap<>();
    private final Map<UUID, Integer> carpetDescendCooldown = new HashMap<>();
    private final Map<UUID, Integer> carpetAscendCooldown = new HashMap<>();
    private final Map<UUID, Integer> featherGlideRemainingTicks = new HashMap<>();
    private final Map<UUID, Integer> lastWarnedSecond = new HashMap<>();
    private final Map<UUID, Long> actionBarPauseUntil = new HashMap<>();
    private final Map<UUID, Boolean> lastDisplayState = new HashMap<>();
    private final Set<UUID> manuallyDisabledFlight = new HashSet<>();

    @Override
    public void onEnable() {
        recipeKey = new NamespacedKey(this, "rock_boots_recipe");
        keyRockBoots = new NamespacedKey(this, "rock_boots");
        keyEnergy = new NamespacedKey(this, "energy");
        keyMaxEnergy = new NamespacedKey(this, "max_energy");
        keySlotTypeBoots = new NamespacedKey(this, "slot_type_boots");
        keySlotBookUnbreaking = new NamespacedKey(this, "slot_book_unbreaking");
        keySlotBookFrost = new NamespacedKey(this, "slot_book_frost");
        keySlotBookEfficiency = new NamespacedKey(this, "slot_book_efficiency");
        keySlotBookFeather = new NamespacedKey(this, "slot_book_feather");
        keySlotBookSoul = new NamespacedKey(this, "slot_book_soul");
        keyUpgradeUnbreaking = new NamespacedKey(this, "upgrade_unbreaking");
        keyUpgradeFrost = new NamespacedKey(this, "upgrade_frost");
        keyUpgradeEfficiency = new NamespacedKey(this, "upgrade_efficiency");
        keyUpgradeFeather = new NamespacedKey(this, "upgrade_feather");
        keyUpgradeSoul = new NamespacedKey(this, "upgrade_soul");

        Bukkit.getPluginManager().registerEvents(this, this);
        registerRecipe();

        for (Player player : Bukkit.getOnlinePlayers()) {
            unlockRecipe(player);
        }

        tickTask = Bukkit.getScheduler().runTaskTimer(this, this::tickPlayers, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        for (UUID uuid : new HashSet<>(carpetBlocks.keySet())) {
            clearCarpet(uuid);
        }
    }

    private void registerRecipe() {
        ItemStack result = createRockBoots();
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape("C C", "C C", "   ");
        recipe.setIngredient('C', Material.COBBLESTONE);
        Bukkit.addRecipe(recipe);
    }

    private ItemStack createRockBoots() {
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
        if (meta == null) {
            return boots;
        }

        meta.setDisplayName(ChatColor.GRAY + "Rock Boots");
        meta.setColor(Color.GRAY);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyRockBoots, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyMaxEnergy, PersistentDataType.INTEGER, 60);
        pdc.set(keyEnergy, PersistentDataType.INTEGER, 5);
        refreshLore(meta, 5, 60);

        boots.setItemMeta(meta);
        ItemMeta freshMeta = boots.getItemMeta();
        if (freshMeta instanceof Damageable damageable) {
            damageable.setDamage(1);
            boots.setItemMeta((ItemMeta) damageable);
        }
        return boots;
    }

    private boolean isRockBoots(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte marker = meta.getPersistentDataContainer().get(keyRockBoots, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        unlockRecipe(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clearCarpet(uuid);
        hoverTickCounter.remove(uuid);
        barTickCounter.remove(uuid);
        jumpWindowTicks.remove(uuid);
        elytraSoundActive.remove(uuid);
        upgradeBaseBoots.remove(uuid);
        upgradeTargetSlot.remove(uuid);
        carpetDescendCooldown.remove(uuid);
        carpetAscendCooldown.remove(uuid);
        featherGlideRemainingTicks.remove(uuid);
        lastWarnedSecond.remove(uuid);
        actionBarPauseUntil.remove(uuid);
        lastDisplayState.remove(uuid);
        manuallyDisabledFlight.remove(uuid);
    }

    @EventHandler
    public void onExpGain(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        if (!isRockBoots(boots)) {
            return;
        }

        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int maxEnergy = Optional.ofNullable(pdc.get(keyMaxEnergy, PersistentDataType.INTEGER)).orElse(60);
        int energy = Optional.ofNullable(pdc.get(keyEnergy, PersistentDataType.INTEGER)).orElse(maxEnergy);
        if (energy >= maxEnergy) {
            return;
        }

        int repaired = Math.max(1, event.getAmount()) * 2;
        int updated = Math.min(maxEnergy, energy + repaired);
        pdc.set(keyEnergy, PersistentDataType.INTEGER, updated);
        refreshLore(meta, updated, maxEnergy);
        boots.setItemMeta(meta);
    }

    private void unlockRecipe(Player player) {
        player.discoverRecipe(recipeKey);
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();

        if (!isRockBoots(boots) || player.getGameMode().name().contains("CREATIVE") || player.isGliding()) {
            return;
        }

        if (!event.isFlying() && player.isFlying() && !player.isOnGround()) {
            manuallyDisabledFlight.add(player.getUniqueId());
            player.setFlying(false);
            player.setAllowFlight(false);
            return;
        }

        int energy = getEnergy(boots);
        if (energy <= 0) {
            player.setFlying(false);
            player.setAllowFlight(false);
            sendPriorityActionBar(player, ChatColor.RED + "Rock Boots: keine Energie");
            return;
        }

        if (event.isFlying()) {
            manuallyDisabledFlight.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (!isRockBoots(mainHand)) {
            return;
        }

        // Preserve vanilla instant-equip behavior; open menu only on explicit sneak + right click.
        if (!player.isSneaking()) {
            return;
        }

        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);

        Bukkit.getScheduler().runTask(this, () -> {
            player.updateInventory();
            openUpgradeMenu(player);
        });
    }

    private void openUpgradeMenu(Player player) {
        Inventory inv = Bukkit.createInventory(player, 27, GUI_TITLE);
        for (int slot = 0; slot < inv.getSize(); slot++) {
            inv.setItem(slot, namedPane(Material.BLACK_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + " "));
        }
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (isRockBoots(handItem)) {
            ItemStack target = handItem.clone();
            target.setAmount(1);
            inv.setItem(GUI_SLOT_TARGET_BOOTS, target);
            upgradeBaseBoots.put(player.getUniqueId(), createSanitizedBaseBoots(target));
            upgradeTargetSlot.put(player.getUniqueId(), player.getInventory().getHeldItemSlot());
            loadStoredUpgradeItems(inv, target);
        }
        ensureUpgradePlaceholders(inv);
        recalcTargetPreview(inv, player.getUniqueId());
        player.openInventory(inv);
    }

    private ItemStack namedPane(Material mat, String name) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack describedPane(Material mat, String name, List<String> lore) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private boolean isPlaceholder(ItemStack item) {
        return item != null
                && (item.getType() == BOOTS_SLOT_PLACEHOLDER_MATERIAL
                || item.getType() == UPGRADE_SLOT_PLACEHOLDER_MATERIAL)
                && item.hasItemMeta();
    }

    private boolean isFillerPane(ItemStack item) {
        return item != null
                && item.getType() == Material.BLACK_STAINED_GLASS_PANE
                && item.hasItemMeta();
    }

    private boolean isEmptyOrPlaceholder(ItemStack item) {
        return item == null || item.getType() == Material.AIR || isPlaceholder(item) || isFillerPane(item);
    }

    private NamespacedKey slotKey(int slot) {
        if (slot == GUI_SLOT_TYPE_BOOTS) {
            return keySlotTypeBoots;
        }
        if (slot == GUI_SLOT_BOOK_UNBREAKING) {
            return keySlotBookUnbreaking;
        }
        if (slot == GUI_SLOT_BOOK_FROST) {
            return keySlotBookFrost;
        }
        if (slot == GUI_SLOT_BOOK_EFFICIENCY) {
            return keySlotBookEfficiency;
        }
        if (slot == GUI_SLOT_BOOK_FEATHER) {
            return keySlotBookFeather;
        }
        if (slot == GUI_SLOT_BOOK_SOUL) {
            return keySlotBookSoul;
        }
        return null;
    }

    private String serializeItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            BukkitObjectOutputStream data = new BukkitObjectOutputStream(output);
            data.writeObject(item);
            data.close();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException ignored) {
            return null;
        }
    }

    private ItemStack deserializeItem(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream stream = new BukkitObjectInputStream(input);
            Object obj = stream.readObject();
            stream.close();
            return obj instanceof ItemStack item ? item : null;
        } catch (IOException | ClassNotFoundException ignored) {
            return null;
        }
    }

    private void loadStoredUpgradeItems(Inventory inv, ItemStack target) {
        ItemMeta meta = target.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int[] slots = new int[]{
                GUI_SLOT_TYPE_BOOTS,
                GUI_SLOT_BOOK_UNBREAKING,
                GUI_SLOT_BOOK_FROST,
                GUI_SLOT_BOOK_EFFICIENCY,
                GUI_SLOT_BOOK_FEATHER,
                GUI_SLOT_BOOK_SOUL
        };
        for (int slot : slots) {
            NamespacedKey key = slotKey(slot);
            if (key == null) {
                continue;
            }
            String data = pdc.get(key, PersistentDataType.STRING);
            ItemStack stored = deserializeItem(data);
            if (stored != null) {
                inv.setItem(slot, stored);
            }
        }
    }

    private void persistUpgradeItems(ItemStack target, Inventory inv) {
        ItemMeta meta = target.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int[] slots = new int[]{
                GUI_SLOT_TYPE_BOOTS,
                GUI_SLOT_BOOK_UNBREAKING,
                GUI_SLOT_BOOK_FROST,
                GUI_SLOT_BOOK_EFFICIENCY,
                GUI_SLOT_BOOK_FEATHER,
                GUI_SLOT_BOOK_SOUL
        };
        for (int slot : slots) {
            NamespacedKey key = slotKey(slot);
            if (key == null) {
                continue;
            }
            ItemStack stack = inv.getItem(slot);
            if (isEmptyOrPlaceholder(stack)) {
                pdc.remove(key);
                continue;
            }
            String data = serializeItem(stack);
            if (data == null) {
                pdc.remove(key);
            } else {
                pdc.set(key, PersistentDataType.STRING, data);
            }
        }
        target.setItemMeta(meta);
    }

    private int customUpgrade(ItemMeta meta, NamespacedKey key) {
        if (meta == null) {
            return 0;
        }
        return Optional.ofNullable(meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER)).orElse(0);
    }

    private int totalUpgradeLevel(ItemMeta meta, Enchantment enchantment, NamespacedKey key) {
        if (meta == null) {
            return 0;
        }
        return Math.max(meta.getEnchantLevel(enchantment), customUpgrade(meta, key));
    }

    private ItemStack createSanitizedBaseBoots(ItemStack source) {
        ItemStack base = source.clone();
        base.setType(Material.LEATHER_BOOTS);

        ItemMeta meta = base.getItemMeta();
        if (!(meta instanceof LeatherArmorMeta leatherMeta)) {
            return base;
        }

        leatherMeta.setColor(Color.GRAY);
        leatherMeta.setDisplayName(ChatColor.GRAY + "Rock Boots");
        leatherMeta.setUnbreakable(true);
        leatherMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

        for (Enchantment enchantment : new HashSet<>(leatherMeta.getEnchants().keySet())) {
            leatherMeta.removeEnchant(enchantment);
        }

        PersistentDataContainer pdc = leatherMeta.getPersistentDataContainer();
        pdc.set(keyRockBoots, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyUpgradeUnbreaking, PersistentDataType.INTEGER, 0);
        pdc.set(keyUpgradeFrost, PersistentDataType.INTEGER, 0);
        pdc.set(keyUpgradeEfficiency, PersistentDataType.INTEGER, 0);
        pdc.set(keyUpgradeFeather, PersistentDataType.INTEGER, 0);
        pdc.set(keyUpgradeSoul, PersistentDataType.INTEGER, 0);

        int maxEnergy = Optional.ofNullable(pdc.get(keyMaxEnergy, PersistentDataType.INTEGER)).orElse(60);
        int energy = Optional.ofNullable(pdc.get(keyEnergy, PersistentDataType.INTEGER)).orElse(Math.min(5, maxEnergy));
        pdc.set(keyMaxEnergy, PersistentDataType.INTEGER, maxEnergy);
        pdc.set(keyEnergy, PersistentDataType.INTEGER, energy);
        refreshLore(leatherMeta, energy, maxEnergy);

        base.setItemMeta(leatherMeta);
        return base;
    }

    private ItemStack buildSlotPlaceholder(int slot) {
        if (slot == GUI_SLOT_TYPE_BOOTS) {
            return describedPane(BOOTS_SLOT_PLACEHOLDER_MATERIAL, ChatColor.GREEN + "Stiefel", List.of(
                    ChatColor.GRAY + "Lege Stiefel ein, um den Typ",
                    ChatColor.GRAY + "der Rock Boots zu aendern.",
                    ChatColor.DARK_GRAY + "Eigene Verzauberungen werden uebernommen."
            ));
        }
        if (slot == GUI_SLOT_TARGET_BOOTS) {
            return describedPane(UPGRADE_SLOT_PLACEHOLDER_MATERIAL, ChatColor.LIGHT_PURPLE + "Rock Boots (Basis)", List.of(
                    ChatColor.GRAY + "Vorschau deiner Rock Boots.",
                    ChatColor.GRAY + "Dieser Slot ist nicht direkt aenderbar."
            ));
        }
        if (slot == GUI_SLOT_BOOK_UNBREAKING) {
            return describedPane(UPGRADE_SLOT_PLACEHOLDER_MATERIAL, ChatColor.LIGHT_PURPLE + "Haltbarkeit", List.of(
                    ChatColor.GRAY + "Mehr maximale Flugzeit.",
                    ChatColor.DARK_GRAY + "Nur Custom-Effekt, keine echte Verzauberung."
            ));
        }
        if (slot == GUI_SLOT_BOOK_FROST) {
            return describedPane(UPGRADE_SLOT_PLACEHOLDER_MATERIAL, ChatColor.LIGHT_PURPLE + "Eislaeufer", List.of(
                    ChatColor.GRAY + "Aktiviert den Glasteppich",
                    ChatColor.GRAY + "unter deinen Fuessen."
            ));
        }
        if (slot == GUI_SLOT_BOOK_EFFICIENCY) {
            return describedPane(UPGRADE_SLOT_PLACEHOLDER_MATERIAL, ChatColor.LIGHT_PURPLE + "Effizienz", List.of(
                    ChatColor.GRAY + "Erhoeht die Fluggeschwindigkeit."
            ));
        }
        if (slot == GUI_SLOT_BOOK_FEATHER) {
            return describedPane(UPGRADE_SLOT_PLACEHOLDER_MATERIAL, ChatColor.LIGHT_PURPLE + "Federfall", List.of(
                    ChatColor.GRAY + "Laesst dich nach Energieende",
                    ChatColor.GRAY + "noch einige Bloecke schweben."
            ));
        }
        return describedPane(UPGRADE_SLOT_PLACEHOLDER_MATERIAL, ChatColor.LIGHT_PURPLE + "Seelentempo", List.of(
                ChatColor.GRAY + "Mehr Speed beim Sprinten",
                ChatColor.GRAY + "waehrend des Fluges."
        ));
    }

    private boolean isUpgradeSlot(int slot) {
        return slot == GUI_SLOT_TYPE_BOOTS
                || slot == GUI_SLOT_TARGET_BOOTS
                || slot == GUI_SLOT_BOOK_UNBREAKING
                || slot == GUI_SLOT_BOOK_FROST
                || slot == GUI_SLOT_BOOK_EFFICIENCY
                || slot == GUI_SLOT_BOOK_FEATHER
                || slot == GUI_SLOT_BOOK_SOUL;
    }

    private Enchantment bookEnchantForSlot(int slot) {
        if (slot == GUI_SLOT_BOOK_UNBREAKING) {
            return Enchantment.UNBREAKING;
        }
        if (slot == GUI_SLOT_BOOK_FROST) {
            return Enchantment.FROST_WALKER;
        }
        if (slot == GUI_SLOT_BOOK_EFFICIENCY) {
            return Enchantment.EFFICIENCY;
        }
        if (slot == GUI_SLOT_BOOK_FEATHER) {
            return Enchantment.FEATHER_FALLING;
        }
        if (slot == GUI_SLOT_BOOK_SOUL) {
            return Enchantment.SOUL_SPEED;
        }
        return null;
    }

    private int findBookSlotFor(ItemStack stack) {
        if (stack == null || stack.getType() != Material.ENCHANTED_BOOK) {
            return -1;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta storageMeta)) {
            return -1;
        }
        if (storageMeta.getStoredEnchants().containsKey(Enchantment.UNBREAKING)) {
            return GUI_SLOT_BOOK_UNBREAKING;
        }
        if (storageMeta.getStoredEnchants().containsKey(Enchantment.FROST_WALKER)) {
            return GUI_SLOT_BOOK_FROST;
        }
        if (storageMeta.getStoredEnchants().containsKey(Enchantment.EFFICIENCY)) {
            return GUI_SLOT_BOOK_EFFICIENCY;
        }
        if (storageMeta.getStoredEnchants().containsKey(Enchantment.FEATHER_FALLING)) {
            return GUI_SLOT_BOOK_FEATHER;
        }
        if (storageMeta.getStoredEnchants().containsKey(Enchantment.SOUL_SPEED)) {
            return GUI_SLOT_BOOK_SOUL;
        }
        return -1;
    }

    private boolean isValidForSlot(int slot, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        if (slot == GUI_SLOT_TARGET_BOOTS) {
            return false;
        }
        if (slot == GUI_SLOT_TYPE_BOOTS) {
            return stack.getType().name().endsWith("_BOOTS");
        }
        Enchantment required = bookEnchantForSlot(slot);
        if (required == null || stack.getType() != Material.ENCHANTED_BOOK) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta storageMeta)) {
            return false;
        }
        return storageMeta.getStoredEnchants().containsKey(required);
    }

    private void ensureUpgradePlaceholders(Inventory inv) {
        int[] slots = new int[]{
                GUI_SLOT_TYPE_BOOTS,
                GUI_SLOT_TARGET_BOOTS,
                GUI_SLOT_BOOK_UNBREAKING,
                GUI_SLOT_BOOK_FROST,
                GUI_SLOT_BOOK_EFFICIENCY,
                GUI_SLOT_BOOK_FEATHER,
                GUI_SLOT_BOOK_SOUL
        };
        for (int slot : slots) {
            ItemStack it = inv.getItem(slot);
            if (slot == GUI_SLOT_TARGET_BOOTS) {
                if (it == null || it.getType() == Material.AIR || isPlaceholder(it) || isFillerPane(it) || !isRockBoots(it)) {
                    inv.setItem(slot, buildSlotPlaceholder(slot));
                }
                continue;
            }
            if (it == null || it.getType() == Material.AIR || isFillerPane(it)) {
                inv.setItem(slot, buildSlotPlaceholder(slot));
            }
        }
    }

    @EventHandler
    public void onUpgradeClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null || !event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        Inventory inv = event.getInventory();
        int raw = event.getRawSlot();
        int topSize = inv.getSize();

        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getClick() == ClickType.NUMBER_KEY) {
            event.setCancelled(true);
            return;
        }

        if (raw < topSize) {
            event.setCancelled(true);
            if (!isUpgradeSlot(raw)) {
                return;
            }

            if (raw == GUI_SLOT_TARGET_BOOTS) {
                return;
            }

            ItemStack current = inv.getItem(raw);
            ItemStack cursor = event.getCursor();
            boolean cursorEmpty = cursor == null || cursor.getType() == Material.AIR;

            if (!isEmptyOrPlaceholder(current) && cursorEmpty) {
                event.setCursor(current);
                inv.setItem(raw, null);
                Bukkit.getScheduler().runTask(this, () -> recalcTargetPreview(inv, event.getWhoClicked().getUniqueId()));
                return;
            }

            if (cursorEmpty) {
                return;
            }

            if (!isValidForSlot(raw, cursor)) {
                return;
            }

            ItemStack toPlace = cursor.clone();
            toPlace.setAmount(1);
            if (!isEmptyOrPlaceholder(current)) {
                HashMap<Integer, ItemStack> overflow = event.getWhoClicked().getInventory().addItem(current);
                for (ItemStack over : overflow.values()) {
                    event.getWhoClicked().getWorld().dropItemNaturally(event.getWhoClicked().getLocation(), over);
                }
            }
            inv.setItem(raw, toPlace);
            int left = cursor.getAmount() - 1;
            if (left <= 0) {
                event.setCursor(null);
            } else {
                cursor.setAmount(left);
                event.setCursor(cursor);
            }
            Bukkit.getScheduler().runTask(this, () -> recalcTargetPreview(inv, event.getWhoClicked().getUniqueId()));
            return;
        }

        if (event.getCurrentItem() != null) {
            ItemStack clicked = event.getCurrentItem();
            int targetSlot = -1;
            if (clicked.getType().name().endsWith("_BOOTS") && !isRockBoots(clicked)) {
                targetSlot = GUI_SLOT_TYPE_BOOTS;
            } else if (clicked.getType() == Material.ENCHANTED_BOOK) {
                targetSlot = findBookSlotFor(clicked);
            }

            if (targetSlot != -1) {
                ItemStack existing = inv.getItem(targetSlot);
                if (existing == null || existing.getType() == Material.AIR || isPlaceholder(existing)) {
                    event.setCancelled(true);
                    ItemStack one = clicked.clone();
                    one.setAmount(1);
                    inv.setItem(targetSlot, one);
                    int left = clicked.getAmount() - 1;
                    if (left <= 0) {
                        event.setCurrentItem(null);
                    } else {
                        clicked.setAmount(left);
                    }
                    Bukkit.getScheduler().runTask(this, () -> recalcTargetPreview(inv, event.getWhoClicked().getUniqueId()));
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTask(this, () -> {
            ensureUpgradePlaceholders(inv);
            recalcTargetPreview(inv, event.getWhoClicked().getUniqueId());
        });
    }

    @EventHandler
    public void onUpgradeClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        UUID uuid = event.getPlayer().getUniqueId();
        upgradeBaseBoots.remove(uuid);
        upgradeTargetSlot.remove(uuid);
        carpetDescendCooldown.remove(uuid);
    }

    private void dropBack(HumanEntity player, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return;
        }
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        for (ItemStack over : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), over);
        }
    }

    private int bookLevelFromSlot(Inventory inv, int slot) {
        ItemStack stack = inv.getItem(slot);
        if (stack == null || isPlaceholder(stack) || stack.getType() != Material.ENCHANTED_BOOK) {
            return 0;
        }
        Enchantment enchant = bookEnchantForSlot(slot);
        if (enchant == null) {
            return 0;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta storageMeta)) {
            return 0;
        }
        return storageMeta.getStoredEnchants().getOrDefault(enchant, 0);
    }

    private void recalcTargetPreview(Inventory inv, UUID uuid) {
        ItemStack target = inv.getItem(GUI_SLOT_TARGET_BOOTS);
        if (target == null || target.getType() == Material.AIR || isPlaceholder(target) || !isRockBoots(target)) {
            ensureUpgradePlaceholders(inv);
            return;
        }

        ItemStack base = upgradeBaseBoots.get(uuid);
        if (base == null || !isRockBoots(base)) {
            base = createSanitizedBaseBoots(target);
            upgradeBaseBoots.put(uuid, base.clone());
        }
        ItemStack preview = base.clone();

        ItemStack typeBoots = inv.getItem(GUI_SLOT_TYPE_BOOTS);
        if (typeBoots != null && !isPlaceholder(typeBoots) && typeBoots.getType().name().endsWith("_BOOTS")) {
            preview.setType(typeBoots.getType());
        }

        ItemMeta meta = preview.getItemMeta();
        if (meta == null) {
            inv.setItem(GUI_SLOT_TARGET_BOOTS, preview);
            return;
        }

        if (typeBoots != null && !isPlaceholder(typeBoots)) {
            ItemMeta typeMeta = typeBoots.getItemMeta();
            if (typeMeta != null) {
                for (Map.Entry<Enchantment, Integer> entry : typeMeta.getEnchants().entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
            }
        }

        int unbreaking = bookLevelFromSlot(inv, GUI_SLOT_BOOK_UNBREAKING);
        int frost = bookLevelFromSlot(inv, GUI_SLOT_BOOK_FROST);
        int eff = bookLevelFromSlot(inv, GUI_SLOT_BOOK_EFFICIENCY);
        int feather = bookLevelFromSlot(inv, GUI_SLOT_BOOK_FEATHER);
        int soul = bookLevelFromSlot(inv, GUI_SLOT_BOOK_SOUL);

        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyRockBoots, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyUpgradeUnbreaking, PersistentDataType.INTEGER, unbreaking);
        pdc.set(keyUpgradeFrost, PersistentDataType.INTEGER, frost);
        pdc.set(keyUpgradeEfficiency, PersistentDataType.INTEGER, eff);
        pdc.set(keyUpgradeFeather, PersistentDataType.INTEGER, feather);
        pdc.set(keyUpgradeSoul, PersistentDataType.INTEGER, soul);

        ItemMeta baseMeta = base.getItemMeta();
        int oldMax = 60;
        int oldEnergy = 5;
        if (baseMeta != null) {
            PersistentDataContainer oldPdc = baseMeta.getPersistentDataContainer();
            oldMax = Optional.ofNullable(oldPdc.get(keyMaxEnergy, PersistentDataType.INTEGER)).orElse(60);
            oldEnergy = Optional.ofNullable(oldPdc.get(keyEnergy, PersistentDataType.INTEGER)).orElse(5);
        }

        int finalUnbreaking = totalUpgradeLevel(meta, Enchantment.UNBREAKING, keyUpgradeUnbreaking);
        int finalFrost = totalUpgradeLevel(meta, Enchantment.FROST_WALKER, keyUpgradeFrost);
        int maxEnergy = calculateMaxEnergySeconds(finalUnbreaking, finalFrost);
        int energy = oldMax > 0 ? Math.max(0, Math.min(maxEnergy, (oldEnergy * maxEnergy) / oldMax)) : Math.min(oldEnergy, maxEnergy);
        pdc.set(keyMaxEnergy, PersistentDataType.INTEGER, maxEnergy);
        pdc.set(keyEnergy, PersistentDataType.INTEGER, energy);
        refreshLore(meta, energy, maxEnergy);

        preview.setItemMeta(meta);
        persistUpgradeItems(preview, inv);
        inv.setItem(GUI_SLOT_TARGET_BOOTS, preview);
        Player player = Bukkit.getPlayer(uuid);
        Integer heldSlot = upgradeTargetSlot.get(uuid);
        if (player != null && heldSlot != null) {
            player.getInventory().setItem(heldSlot, preview.clone());
        }
        ensureUpgradePlaceholders(inv);
    }

    private int calculateMaxEnergySeconds(int unbreakingLevel, int frostWalkerLevel) {
        int calculated = 60 * Math.max(1, (1 + (unbreakingLevel * 3) + (frostWalkerLevel * 4)));
        return Math.min(MAX_ENERGY_CAP_SECONDS, calculated);
    }

    private void refreshLore(ItemMeta meta, int energy, int maxEnergy) {
        int shownEnergy = Math.max(0, energy);
        int shownMax = Math.max(1, maxEnergy);
        int frostWalker = customUpgrade(meta, keyUpgradeFrost);
        int unbreaking = customUpgrade(meta, keyUpgradeUnbreaking);
        int efficiency = customUpgrade(meta, keyUpgradeEfficiency);
        int featherFalling = customUpgrade(meta, keyUpgradeFeather);
        int soulSpeed = customUpgrade(meta, keyUpgradeSoul);

        meta.setLore(List.of(
                ChatColor.GRAY + "Schwebe mit Leertaste/Shift",
                ChatColor.GRAY + "Upgrade: Ducken + Rechtsklick",
                ChatColor.GRAY + "Flug-Energie: " + shownEnergy + "s / " + shownMax + "s",
                ChatColor.GRAY + "Frost Walker: " + frostWalker,
                ChatColor.DARK_GRAY + " -> Glasteppich unter dir",
                ChatColor.GRAY + "Unbreaking: " + unbreaking,
                ChatColor.DARK_GRAY + " -> Mehr maximale Flugzeit",
                ChatColor.GRAY + "Effizienz: " + efficiency,
                ChatColor.DARK_GRAY + " -> Schnellere Fluggeschwindigkeit",
                ChatColor.GRAY + "Feather Falling: " + featherFalling,
                ChatColor.DARK_GRAY + " -> Schweben nach Energieende",
                ChatColor.GRAY + "Soul Speed: " + soulSpeed,
                ChatColor.DARK_GRAY + " -> Mehr Speed beim Sprint-Flug"
        ));
    }

    private void tickPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            int descendCooldown = carpetDescendCooldown.getOrDefault(uuid, 0);
            if (descendCooldown > 0) {
                carpetDescendCooldown.put(uuid, descendCooldown - 1);
            }
            int ascendCooldown = carpetAscendCooldown.getOrDefault(uuid, 0);
            if (ascendCooldown > 0) {
                carpetAscendCooldown.put(uuid, ascendCooldown - 1);
            }
            ItemStack boots = player.getInventory().getBoots();
            boolean active = isRockBoots(boots);
            if (!active) {
                player.setAllowFlight(player.getGameMode().name().contains("CREATIVE"));
                player.setFlySpeed(VANILLA_FLY_SPEED);
                stopElytraSound(player);
                clearCarpet(uuid);
                barTickCounter.remove(uuid);
                jumpWindowTicks.remove(uuid);
                carpetDescendCooldown.remove(uuid);
                carpetAscendCooldown.remove(uuid);
                featherGlideRemainingTicks.remove(uuid);
                lastWarnedSecond.remove(uuid);
                manuallyDisabledFlight.remove(uuid);
                lastDisplayState.remove(uuid);
                continue;
            }

            int energy = getEnergy(boots);
            normalizeBootEnergyData(boots);
            if (!player.getGameMode().name().contains("CREATIVE") && !player.isGliding()) {
                if (player.isOnGround()) {
                    manuallyDisabledFlight.remove(uuid);
                }
                boolean manualOff = manuallyDisabledFlight.contains(uuid);
                if (energy <= 0) {
                    boolean glideActive = featherGlideRemainingTicks.getOrDefault(uuid, 0) > 0;
                    player.setAllowFlight(player.isFlying() || glideActive);
                    jumpWindowTicks.remove(uuid);
                } else if (player.isFlying()) {
                    manuallyDisabledFlight.remove(uuid);
                    player.setAllowFlight(true);
                    jumpWindowTicks.remove(uuid);
                } else if (player.isOnGround()) {
                    player.setAllowFlight(true);
                    jumpWindowTicks.put(uuid, DOUBLE_JUMP_WINDOW_TICKS);
                } else {
                    int window = jumpWindowTicks.getOrDefault(uuid, 0);
                    boolean nearGround = player.getLocation().clone().subtract(0.0, 1.25, 0.0).getBlock().getType().isSolid();
                    boolean falling = player.getVelocity().getY() < -0.08;
                    if (manualOff) {
                        player.setAllowFlight(!nearGround);
                    } else if (window > 0) {
                        jumpWindowTicks.put(uuid, window - 1);
                        player.setAllowFlight(true);
                    } else if (falling && !nearGround) {
                        player.setAllowFlight(true);
                    } else {
                        player.setAllowFlight(false);
                    }
                }
                ItemMeta meta = boots.getItemMeta();
                int efficiency = meta == null ? 0 : customUpgrade(meta, keyUpgradeEfficiency);
                int soulSpeed = meta == null ? 0 : customUpgrade(meta, keyUpgradeSoul);
                float speed = ROCK_BOOTS_FLY_SPEED + (0.008f * efficiency);
                if (player.isFlying() && player.isSprinting() && soulSpeed > 0) {
                    speed += 0.01f * soulSpeed;
                }
                if (speed > VANILLA_FLY_SPEED) {
                    speed = VANILLA_FLY_SPEED;
                }
                player.setFlySpeed(speed);
            }

            handleHover(player, boots);
            announceDisplayStateIfChanged(player, boots);
            showEnergyBar(player, boots);
            playWarningIfNeeded(player, boots);
        }
    }

    private void announceDisplayStateIfChanged(Player player, ItemStack boots) {
        UUID uuid = player.getUniqueId();
        boolean active = isDisplayActive(player, boots);
        Boolean previous = lastDisplayState.put(uuid, active);
        if (previous != null && previous == active) {
            return;
        }
        String text = buildEnergyBarText(player, boots, active);
        if (text != null) {
            sendPriorityActionBar(player, text);
        }
    }

    private boolean isDisplayActive(Player player, ItemStack boots) {
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return false;
        }
        int glideTicks = featherGlideRemainingTicks.getOrDefault(player.getUniqueId(), 0);
        return player.isFlying() || glideTicks > 0 || isStandingOnOwnCarpet(player, player.getUniqueId());
    }

    private int getEnergy(ItemStack boots) {
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int maxFromEnchant = calculateMaxEnergySeconds(
                totalUpgradeLevel(meta, Enchantment.UNBREAKING, keyUpgradeUnbreaking),
                totalUpgradeLevel(meta, Enchantment.FROST_WALKER, keyUpgradeFrost));
        int maxEnergy = Optional.ofNullable(pdc.get(keyMaxEnergy, PersistentDataType.INTEGER)).orElse(maxFromEnchant);
        return Optional.ofNullable(pdc.get(keyEnergy, PersistentDataType.INTEGER)).orElse(maxEnergy);
    }

    private void showEnergyBar(Player player, ItemStack boots) {
        UUID uuid = player.getUniqueId();
        int tick = barTickCounter.getOrDefault(uuid, 0) + 1;
        barTickCounter.put(uuid, tick);
        if (tick % 5 != 0) {
            return;
        }

        if (isActionBarPaused(uuid)) {
            return;
        }

        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int maxFromEnchant = calculateMaxEnergySeconds(
                totalUpgradeLevel(meta, Enchantment.UNBREAKING, keyUpgradeUnbreaking),
                totalUpgradeLevel(meta, Enchantment.FROST_WALKER, keyUpgradeFrost));
        int maxEnergy = Math.max(1, Optional.ofNullable(pdc.get(keyMaxEnergy, PersistentDataType.INTEGER)).orElse(maxFromEnchant));
        int energy = Math.max(0, Optional.ofNullable(pdc.get(keyEnergy, PersistentDataType.INTEGER)).orElse(maxEnergy));
        int customFeatherLevel = customUpgrade(meta, keyUpgradeFeather);
        int maxGlideTicks = Math.max(0, customFeatherLevel * 40);
        int glideTicks = featherGlideRemainingTicks.getOrDefault(uuid, 0);
        boolean active = isDisplayActive(player, boots);
        if (!active && player.isOnGround()) {
            return;
        }
        ChatColor energyColor = barColor(energy, maxEnergy);
        int minuteReserveMax = Math.max(0, (maxEnergy - 60) / 60);
        int minuteReserveNow = Math.max(0, (energy - 60 + 59) / 60);
        if (minuteReserveNow > minuteReserveMax) {
            minuteReserveNow = minuteReserveMax;
        }

        int secondBars = 30;
        int visibleSeconds = Math.min(60, Math.max(0, energy));
        int secondFilled = (int) Math.round((visibleSeconds / 60.0) * secondBars);
        if (secondFilled < 0) {
            secondFilled = 0;
        } else if (secondFilled > secondBars) {
            secondFilled = secondBars;
        }

        String minuteBar = "";
        if (minuteReserveMax > 0) {
            minuteBar = ChatColor.AQUA + MINUTE_BAR_SEGMENT.repeat(minuteReserveNow)
                    + ChatColor.DARK_GRAY + MINUTE_BAR_SEGMENT.repeat(Math.max(0, minuteReserveMax - minuteReserveNow))
                    + ChatColor.GRAY + " ";
        }
        String secondBar = energyColor + "|".repeat(secondFilled) + ChatColor.DARK_GRAY + "|".repeat(secondBars - secondFilled);
        String energyBar = minuteBar + secondBar;
        String glideText = "";
        if (customFeatherLevel > 0) {
            int displayGlideTicks = glideTicks > 0 ? glideTicks : maxGlideTicks;
            int safeMaxGlideTicks = Math.max(1, maxGlideTicks);
            int glideBars = 20;
            int glideFilled = (int) Math.round((displayGlideTicks / (double) safeMaxGlideTicks) * glideBars);
            if (glideFilled < 0) {
                glideFilled = 0;
            } else if (glideFilled > glideBars) {
                glideFilled = glideBars;
            }
            ChatColor glideColor = barColor(displayGlideTicks / 20.0, safeMaxGlideTicks / 20.0);
            String glideBar = glideColor + "|".repeat(glideFilled) + ChatColor.DARK_GRAY + "|".repeat(glideBars - glideFilled);
            glideText = ChatColor.GRAY + " Sinkflug " + glideBar;
        }
        String state = active ? ChatColor.GREEN + "[AN] " : ChatColor.DARK_GRAY + "[AUS] ";
        String text = ChatColor.GRAY + "Rock Boots " + state + energyBar + glideText;
        player.sendActionBar(Component.text(text));
    }

    private String buildEnergyBarText(Player player, ItemStack boots, boolean active) {
        UUID uuid = player.getUniqueId();
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int maxFromEnchant = calculateMaxEnergySeconds(
                totalUpgradeLevel(meta, Enchantment.UNBREAKING, keyUpgradeUnbreaking),
                totalUpgradeLevel(meta, Enchantment.FROST_WALKER, keyUpgradeFrost));
        int maxEnergy = Math.max(1, Optional.ofNullable(pdc.get(keyMaxEnergy, PersistentDataType.INTEGER)).orElse(maxFromEnchant));
        int energy = Math.max(0, Optional.ofNullable(pdc.get(keyEnergy, PersistentDataType.INTEGER)).orElse(maxEnergy));
        int customFeatherLevel = customUpgrade(meta, keyUpgradeFeather);
        int maxGlideTicks = Math.max(0, customFeatherLevel * 40);
        int glideTicks = featherGlideRemainingTicks.getOrDefault(uuid, 0);
        ChatColor energyColor = barColor(energy, maxEnergy);
        int minuteReserveMax = Math.max(0, (maxEnergy - 60) / 60);
        int minuteReserveNow = Math.max(0, (energy - 60 + 59) / 60);
        if (minuteReserveNow > minuteReserveMax) {
            minuteReserveNow = minuteReserveMax;
        }

        int secondBars = 30;
        int visibleSeconds = Math.min(60, Math.max(0, energy));
        int secondFilled = (int) Math.round((visibleSeconds / 60.0) * secondBars);
        if (secondFilled < 0) {
            secondFilled = 0;
        } else if (secondFilled > secondBars) {
            secondFilled = secondBars;
        }

        String minuteBar = "";
        if (minuteReserveMax > 0) {
            minuteBar = ChatColor.AQUA + MINUTE_BAR_SEGMENT.repeat(minuteReserveNow)
                    + ChatColor.DARK_GRAY + MINUTE_BAR_SEGMENT.repeat(Math.max(0, minuteReserveMax - minuteReserveNow))
                    + ChatColor.GRAY + " ";
        }
        String secondBar = energyColor + "|".repeat(secondFilled) + ChatColor.DARK_GRAY + "|".repeat(secondBars - secondFilled);
        String energyBar = minuteBar + secondBar;
        String glideText = "";
        if (customFeatherLevel > 0) {
            int displayGlideTicks = glideTicks > 0 ? glideTicks : maxGlideTicks;
            int safeMaxGlideTicks = Math.max(1, maxGlideTicks);
            int glideBars = 20;
            int glideFilled = (int) Math.round((displayGlideTicks / (double) safeMaxGlideTicks) * glideBars);
            if (glideFilled < 0) {
                glideFilled = 0;
            } else if (glideFilled > glideBars) {
                glideFilled = glideBars;
            }
            ChatColor glideColor = barColor(displayGlideTicks / 20.0, safeMaxGlideTicks / 20.0);
            String glideBar = glideColor + "|".repeat(glideFilled) + ChatColor.DARK_GRAY + "|".repeat(glideBars - glideFilled);
            glideText = ChatColor.GRAY + " Sinkflug " + glideBar;
        }
        String state = active ? ChatColor.GREEN + "[AN] " : ChatColor.DARK_GRAY + "[AUS] ";
        return ChatColor.GRAY + "Rock Boots " + state + energyBar + glideText;
    }

    private void sendPriorityActionBar(Player player, String text) {
        pauseEnergyBar(player.getUniqueId(), ACTION_BAR_PAUSE_MILLIS);
        player.sendActionBar(Component.text(text));
    }

    private void pauseEnergyBar(UUID uuid, long millis) {
        long pauseUntil = System.currentTimeMillis() + Math.max(0L, millis);
        actionBarPauseUntil.merge(uuid, pauseUntil, Math::max);
    }

    private boolean isActionBarPaused(UUID uuid) {
        Long pauseUntil = actionBarPauseUntil.get(uuid);
        if (pauseUntil == null) {
            return false;
        }
        if (pauseUntil <= System.currentTimeMillis()) {
            actionBarPauseUntil.remove(uuid);
            return false;
        }
        return true;
    }

    private ChatColor barColor(double remaining, double maximum) {
        if (remaining <= 1.0) {
            return ChatColor.DARK_RED;
        }
        if (remaining <= 3.0) {
            return ChatColor.RED;
        }
        if (remaining <= 5.0) {
            return ChatColor.GOLD;
        }
        if (remaining <= 15.0) {
            return ChatColor.YELLOW;
        }
        return ChatColor.GREEN;
    }

    private void playWarningIfNeeded(Player player, ItemStack boots) {
        UUID uuid = player.getUniqueId();
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            lastWarnedSecond.remove(uuid);
            return;
        }

        int glideTicks = featherGlideRemainingTicks.getOrDefault(uuid, 0);
        if (glideTicks > 0) {
            lastWarnedSecond.remove(uuid);
            int maxGlideTicks = Math.max(1, customUpgrade(meta, keyUpgradeFeather) * 40);
            int third = Math.max(1, maxGlideTicks / 3);
            int interval;
            float pitch;
            if (glideTicks > third * 2) {
                interval = 10;
                pitch = 1.5f;
            } else if (glideTicks > third) {
                interval = 5;
                pitch = 1.75f;
            } else {
                interval = 2;
                pitch = 1.95f;
            }
            if (glideTicks % interval == 0) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1.15f, pitch);
            }
            return;
        }

        int secondsLeft = Math.max(0, getEnergy(boots));
        if (secondsLeft > 15) {
            lastWarnedSecond.remove(uuid);
            return;
        }

        Set<Integer> thresholds = Set.of(15, 10, 5, 3, 2, 1);
        if (!thresholds.contains(secondsLeft)) {
            return;
        }

        Integer last = lastWarnedSecond.get(uuid);
        if (last != null && last == secondsLeft) {
            return;
        }

        lastWarnedSecond.put(uuid, secondsLeft);
        float pitch;
        if (secondsLeft <= 1) {
            pitch = 1.8f;
        } else if (secondsLeft <= 3) {
            pitch = 1.5f;
        } else if (secondsLeft <= 5) {
            pitch = 1.25f;
        } else {
            pitch = 1.0f;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.8f, pitch);
    }

    private void handleHover(Player player, ItemStack boots) {
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int maxFromEnchant = calculateMaxEnergySeconds(
                totalUpgradeLevel(meta, Enchantment.UNBREAKING, keyUpgradeUnbreaking),
                totalUpgradeLevel(meta, Enchantment.FROST_WALKER, keyUpgradeFrost));
        int maxEnergy = Optional.ofNullable(pdc.get(keyMaxEnergy, PersistentDataType.INTEGER)).orElse(maxFromEnchant);
        int energy = Optional.ofNullable(pdc.get(keyEnergy, PersistentDataType.INTEGER)).orElse(maxEnergy);

        UUID uuid = player.getUniqueId();
        int customFrostLevel = customUpgrade(meta, keyUpgradeFrost);
        int customFeatherLevel = customUpgrade(meta, keyUpgradeFeather);
        boolean hasCarpetPower = customFrostLevel > 0;
        boolean standingOnCarpet = isStandingOnOwnCarpet(player, uuid);
        boolean carpetActive = hasCarpetPower && carpetBlocks.containsKey(uuid) && !carpetBlocks.get(uuid).isEmpty();
        boolean carpetJumping = carpetActive && !standingOnCarpet && player.getVelocity().getY() > 0.18;
        boolean carpetModeActive = standingOnCarpet || carpetActive || carpetJumping;
        boolean carpetShiftedThisTick = false;
        Block below = player.getLocation().subtract(0.0, 1.0, 0.0).getBlock();

        if (player.isSneaking() && player.isOnGround() && below.getType().isSolid() && !standingOnCarpet) {
            player.setFlying(false);
            player.setAllowFlight(false);
            stopElytraSound(player);
            hoverTickCounter.remove(uuid);
            clearCarpet(uuid);
            return;
        }

        if (energy <= 0) {
            boolean wasFlying = player.isFlying() || carpetModeActive;
            boolean startedFromCarpet = carpetModeActive;
            if (carpetModeActive) {
                clearCarpet(uuid);
            }
            boolean glideEligible = !player.isSwimming() && !player.isInsideVehicle() && (!player.isOnGround() || carpetModeActive);
            if (wasFlying && customFeatherLevel > 0 && !featherGlideRemainingTicks.containsKey(uuid)) {
                featherGlideRemainingTicks.put(uuid, customFeatherLevel * 40);
            }

            int remainingGlideTicks = featherGlideRemainingTicks.getOrDefault(uuid, 0);

            // Keep flight enabled during the Feather Falling glide window.
            if (remainingGlideTicks > 0 && glideEligible) {
                player.setAllowFlight(true);
                player.setFlying(true);
                Vector vel = player.getVelocity();
                player.setVelocity(new Vector(vel.getX() * 0.98, -FEATHER_GLIDE_SINK_SPEED, vel.getZ() * 0.98));
                featherGlideRemainingTicks.put(uuid, remainingGlideTicks - 1);
            } else if ((wasFlying || remainingGlideTicks > 0) && !player.isOnGround() && !player.isSwimming() && !player.isInsideVehicle()) {
                player.setFlying(false);
                player.setAllowFlight(false);
                stopElytraSound(player);
                Vector vel = player.getVelocity();
                double y = Math.max(vel.getY(), -0.08);
                player.setVelocity(new Vector(vel.getX() * 0.98, y, vel.getZ() * 0.98));
            } else {
                player.setFlying(false);
                player.setAllowFlight(false);
                stopElytraSound(player);
            }

            if (player.isOnGround() && !startedFromCarpet) {
                featherGlideRemainingTicks.remove(uuid);
                player.setFlying(false);
                player.setAllowFlight(false);
                stopElytraSound(player);
            }
            hoverTickCounter.remove(uuid);
            clearCarpet(uuid);
            return;
        }

        featherGlideRemainingTicks.remove(uuid);

        if (!player.isFlying() && !carpetModeActive) {
            stopElytraSound(player);
            hoverTickCounter.remove(uuid);
            clearCarpet(uuid);
            return;
        }

        if (carpetJumping && carpetAscendCooldown.getOrDefault(uuid, 0) == 0) {
            updateGlassCarpet(player, Math.max(1, customFrostLevel), -1);
            carpetAscendCooldown.put(uuid, 6);
        }

        if (standingOnCarpet && player.isSneaking() && carpetDescendCooldown.getOrDefault(uuid, 0) == 0) {
            updateGlassCarpet(player, Math.max(1, customFrostLevel), 1);
            Vector vel = player.getVelocity();
            player.teleport(player.getLocation().clone().add(0.0, -0.08, 0.0));
            player.setAllowFlight(true);
            player.setFlying(false);
            player.setVelocity(new Vector(vel.getX() * 0.45, -0.32, vel.getZ() * 0.45));
            carpetDescendCooldown.put(uuid, 6);
            standingOnCarpet = false;
            carpetShiftedThisTick = true;
        }

        int ticks = hoverTickCounter.getOrDefault(uuid, 0) + 1;
        hoverTickCounter.put(uuid, ticks);

        if (ticks % 20 == 0) {
            energy--;
            pdc.set(keyEnergy, PersistentDataType.INTEGER, energy);
            refreshLore(meta, energy, maxEnergy);
            boots.setItemMeta(meta);
        }

        if ((player.isFlying() || carpetModeActive) && ticks % 16 == 0) {
            playRandomFlightSound(player);
            elytraSoundActive.add(uuid);
        } else if (!player.isFlying() && !carpetModeActive) {
            stopElytraSound(player);
        }

        if (hasCarpetPower) {
            int carpetLevel = Math.max(1, customFrostLevel);
            if (!carpetShiftedThisTick) {
                updateGlassCarpet(player, carpetLevel, 0);
            }
        } else {
            clearCarpet(uuid);
        }
    }

    private void updateGlassCarpet(Player player, int frostWalkerLevel, int verticalOffset) {
        UUID uuid = player.getUniqueId();
        Set<Block> previous = carpetBlocks.computeIfAbsent(uuid, ignored -> new HashSet<>());
        Set<Block> now = new HashSet<>();
        World world = player.getWorld();
        int radius = Math.min(1 + Math.max(0, frostWalkerLevel), 4);

        int baseX = player.getLocation().getBlockX();
        int baseY = player.getLocation().getBlockY() - 1 - verticalOffset;
        int baseZ = player.getLocation().getBlockZ();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int bx = baseX + x;
                int by = baseY;
                int bz = baseZ + z;
                Block block = world.getBlockAt(bx, by, bz);
                if (block.getType() == Material.GLASS && previous.contains(block)) {
                    now.add(block);
                    continue;
                }
                if (!block.getType().isAir()) {
                    continue;
                }
                block.setType(Material.GLASS, false);
                now.add(block);
            }
        }

        for (Block old : previous) {
            if (now.contains(old)) {
                continue;
            }
            if (old.getType() == Material.GLASS) {
                old.setType(Material.AIR, false);
            }
        }

        carpetBlocks.put(uuid, now);
    }

    private boolean isStandingOnOwnCarpet(Player player, UUID uuid) {
        Set<Block> blocks = carpetBlocks.get(uuid);
        if (blocks == null || blocks.isEmpty()) {
            return false;
        }
        Block below = player.getLocation().subtract(0.0, 1.0, 0.0).getBlock();
        return blocks.contains(below) && below.getType() == Material.GLASS;
    }

    private void clearCarpet(UUID uuid) {
        Set<Block> blocks = carpetBlocks.remove(uuid);
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        for (Block block : blocks) {
            if (block.getType() == Material.GLASS) {
                block.setType(Material.AIR, false);
            }
        }
    }

    private void stopElytraSound(Player player) {
        UUID uuid = player.getUniqueId();
        if (!elytraSoundActive.remove(uuid)) {
            return;
        }
        for (Sound sound : FLIGHT_SOUND_POOL) {
            player.stopSound(sound);
        }
    }

    private void playRandomFlightSound(Player player) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        Sound selected;
        if (roll < 70) {
            selected = Sound.ITEM_ELYTRA_FLYING;
        } else if (roll < 90) {
            selected = Sound.ENTITY_PHANTOM_FLAP;
        } else {
            selected = Sound.ENTITY_ENDER_DRAGON_FLAP;
        }

        float volume;
        float pitch;
        if (selected == Sound.ITEM_ELYTRA_FLYING) {
            volume = 0.35f;
            pitch = 1.0f;
        } else if (selected == Sound.ENTITY_PHANTOM_FLAP) {
            volume = 0.20f;
            pitch = 1.15f;
        } else {
            volume = 0.12f;
            pitch = 1.35f;
        }
        player.playSound(player.getLocation(), selected, volume, pitch);
    }

    private void normalizeBootEnergyData(ItemStack boots) {
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int unbreaking = totalUpgradeLevel(meta, Enchantment.UNBREAKING, keyUpgradeUnbreaking);
        int frostWalker = totalUpgradeLevel(meta, Enchantment.FROST_WALKER, keyUpgradeFrost);
        int desiredMax = calculateMaxEnergySeconds(unbreaking, frostWalker);

        int currentMax = Optional.ofNullable(pdc.get(keyMaxEnergy, PersistentDataType.INTEGER)).orElse(desiredMax);
        int currentEnergy = Optional.ofNullable(pdc.get(keyEnergy, PersistentDataType.INTEGER)).orElse(Math.min(5, desiredMax));
        if (currentMax == desiredMax && currentEnergy <= desiredMax) {
            return;
        }
        int adjustedEnergy;
        if (currentMax > 0) {
            adjustedEnergy = Math.max(0, Math.min(desiredMax, (currentEnergy * desiredMax) / currentMax));
        } else {
            adjustedEnergy = Math.min(currentEnergy, desiredMax);
        }
        pdc.set(keyMaxEnergy, PersistentDataType.INTEGER, desiredMax);
        pdc.set(keyEnergy, PersistentDataType.INTEGER, adjustedEnergy);
        refreshLore(meta, adjustedEnergy, desiredMax);
        boots.setItemMeta(meta);
    }
}

