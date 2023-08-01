package cupcqkeee.me.transmitter;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class Transmitter extends JavaPlugin implements Listener {

    private static final String TRANSMITTER_KEY = "custom_transmitter";
    private static final String TRANSMITTER_NBT_KEY = "transmitter_item";
    private static final String TRANSMITTER_STATUS_KEY = "transmitter_status";

    @Override
    public void onEnable() {
        // Регистрируем события
        getServer().getPluginManager().registerEvents(this, this);

        // Регистрируем предмет и рецепт
        ItemStack transmitterItem = createTransmitterItem();
        if (getServer().getRecipe(new NamespacedKey(this, "transmitter_recipe")) == null) {
            // Регистрируем рецепт только если его нет
            ShapedRecipe transmitterRecipe = createTransmitterRecipe(transmitterItem);
            getServer().addRecipe(transmitterRecipe);
        }

        // Создаем конфигурацию (config.yml) и загружаем значения
        saveDefaultConfig();
        loadCraftRecipeFromConfig();
    }

    @Override
    public void onDisable() {
        // Здесь можно выполнить необходимые действия при отключении плагина
    }

    // Создаем предмет "Transmitter"
    private ItemStack createTransmitterItem() {
        FileConfiguration config = getConfig();
        ItemStack itemStack = new ItemStack(Material.COMPASS);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("transmitter.display_name", "&aTransmitter")));

        // Добавляем NBT-тег для идентификации предмета "Transmitter"
        itemMeta.getPersistentDataContainer().set(new NamespacedKey(this, TRANSMITTER_NBT_KEY), PersistentDataType.STRING, TRANSMITTER_KEY);

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }


    // Создаем рецепт для предмета "Transmitter" из конфигурации
    private ShapedRecipe createTransmitterRecipe(ItemStack resultItem) {
        FileConfiguration config = getConfig();
        ConfigurationSection recipeSection = config.getConfigurationSection("transmitter_recipe");
        String[] shape = recipeSection.getStringList("shape").toArray(new String[0]);
        List<Character> keys = recipeSection.getStringList("keys").get(0).chars().mapToObj(c -> (char) c).toList();
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(this, "transmitter_recipe"), resultItem);
        recipe.shape(shape);
        for (int i = 0; i < keys.size(); i++) {
            char key = keys.get(i);
            ItemStack ingredient = getIngredientFromConfig(key);
            recipe.setIngredient(key, ingredient.getType());
        }
        return recipe;
    }

    // Получаем предмет из конфига по ключу (букве в рецепте)
    private ItemStack getIngredientFromConfig(char key) {
        FileConfiguration config = getConfig();
        String path = "ingredients." + key;
        Material material = Material.valueOf(config.getString(path + ".type", "AIR"));
        int amount = config.getInt(path + ".amount", 1);
        return new ItemStack(material, amount);
    }

    // Загружаем значения рецепта из config.yml
    private void loadCraftRecipeFromConfig() {
        if (!getConfig().contains("transmitter_recipe")) {
            getConfig().createSection("transmitter_recipe");
            getConfig().set("transmitter_recipe.shape", List.of("III", "IDI", "III"));
            getConfig().set("transmitter_recipe.keys", List.of("IDI"));
            getConfig().createSection("ingredients.I");
            getConfig().set("ingredients.I.type", "IRON_BLOCK");
            getConfig().createSection("ingredients.D");
            getConfig().set("ingredients.D.type", "OAK_WOOD");
            saveConfig();
        }
    }

    // Обработка события нажатия правой кнопкой мыши
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int previousSlot = event.getPreviousSlot();
        int newSlot = event.getNewSlot();

        // Получаем предметы из активной и предыдущей руки
        ItemStack previousItem = player.getInventory().getItem(previousSlot);
        ItemStack newItem = player.getInventory().getItem(newSlot);

        // Проверяем, является ли предмет "Transmitter"
        if (isTransmitter(previousItem)) {
            // Получаем статус предмета
            int status = getItemStatus(previousItem);

            // Если предмет "Transmitter" активирован и меняется на другой слот
            if (status == 1 && isTransmitter(newItem)) {
                player.performCommand("spy");
                // Ваша команда, которую нужно выполнить при активации предмета
                // Например:
                // player.performCommand("chatty spy on");
            } else if (status == 1 && !isTransmitter(newItem)) {
                player.performCommand("spy");
                // Ваша команда, которую нужно выполнить при деактивации предмета
                // Например:
                // player.performCommand("chatty spy off");
                setItemStatus(previousItem, 2); // Устанавливаем статус 2, предмет деактивирован
            }
        }
    }

    // Обработка события нажатия предметов правой кнопкой мыши
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Проверяем, что предмет в руке - "Transmitter"
        if (isTransmitter(itemInHand)) {
            // Проверяем наличие права "Test.use" у игрока
            if (player.hasPermission("Test.use")) {
                // При нажатии правой кнопкой мыши прописываем команду "Вы активировали предмет"
                int status = getItemStatus(itemInHand);
                if (event.getAction().name().contains("RIGHT_CLICK") && status != 1) {
                    player.performCommand("spy");
//                    player.performCommand("spy");
                    //player.sendMessage(ChatColor.GREEN + "Вы активировали предмет Transmitter!");
                    // Ваша команда, которую нужно выполнить при активации предмета
                    // Например:
                    // player.performCommand("say Вы активировали предмет");
                    setItemStatus(itemInHand, 1); // Устанавливаем статус 1, предмет активирован
                } else if (event.getAction().name().contains("LEFT_CLICK") || event.getAction().name().contains("PHYSICAL")) {
                    // Проверяем, что предмет убран из активного слота
                    if (player.getInventory().getHeldItemSlot() != player.getInventory().getHeldItemSlot()) {
//                        int status = getItemStatus(itemInHand);
                        if (status == 1) {
                            player.performCommand("spy");
//                            player.performCommand("spy");
//                            player.sendMessage(ChatColor.RED + "Вы деактивировали предмет Transmitter!");
                            // Ваша команда, которую нужно выполнить при деактивации предмета
                            // Например:
                            // player.performCommand("say Вы деактивировали предмет");
                            setItemStatus(itemInHand, 2); // Устанавливаем статус 2, предмет деактивирован
                        }
                    }
                }
            }
        }
    }

    // Проверка, является ли предмет "Transmitter"
    private boolean isTransmitter(ItemStack itemStack) {
        if (itemStack != null && itemStack.getType() == Material.DIAMOND) {
            // Проверяем наличие NBT-тега для идентификации предмета "Transmitter"
            ItemMeta itemMeta = itemStack.getItemMeta();
            return itemMeta != null && itemMeta.getPersistentDataContainer().has(new NamespacedKey(this, TRANSMITTER_NBT_KEY), PersistentDataType.STRING);
        }
        return false;
    }

    private int getItemStatus(ItemStack itemStack) {
        if (itemStack != null) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta != null && itemMeta.getPersistentDataContainer().has(new NamespacedKey(this, TRANSMITTER_STATUS_KEY), PersistentDataType.INTEGER)) {
                return itemMeta.getPersistentDataContainer().get(new NamespacedKey(this, TRANSMITTER_STATUS_KEY), PersistentDataType.INTEGER);
            }
        }
        return 0; // По умолчанию статус 0, предмет не имеет статуса
    }

    private void setItemStatus(ItemStack itemStack, int status) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.getPersistentDataContainer().set(new NamespacedKey(this, TRANSMITTER_STATUS_KEY), PersistentDataType.INTEGER, status);
        itemStack.setItemMeta(itemMeta);
    }


    // Обработка команды /transmitter для выдачи предмета "Transmitter"
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("transmitter")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                // Проверяем наличие права "Transmitter.admin" у игрока
                if (player.hasPermission("Transmitter.admin")) {
                    // Выдаем предмет "Transmitter"
                    ItemStack transmitterItem = createTransmitterItem();
                    player.getInventory().addItem(transmitterItem);
                    player.sendMessage(ChatColor.GREEN + "Вы получили предмет Transmitter!");
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "У вас нет права на использование команды!");
                    return true;
                }
            } else {
                sender.sendMessage("Команда доступна только для игроков.");
                return true;
            }
        }
        return false;
    }
}
