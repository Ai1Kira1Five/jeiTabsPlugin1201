package com.aikirafive.jeiendlesstabs.jei;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IBookmarkOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JeiTabManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<List<JeiBookmarkElement>> tabsBookmarksUids = new ArrayList<>(9);
    private static int currentTab = 0;
    private static boolean isUpdatingJei = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static File getSaveFile() {
        return new File(Minecraft.getInstance().gameDirectory, "config/jei_endless_tabs.json");
    }

    static {
        for (int i = 0; i < 9; i++) {
            tabsBookmarksUids.add(new ArrayList<>());
        }
    }

    public static int getCurrentTab() {
        return currentTab;
    }

    public static boolean isUpdatingJei() {
        return isUpdatingJei;
    }

    public static class JeiBookmarkElement {
        public String type;
        public String value;

        public JeiBookmarkElement(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    /**
     * Считывает текущее состояние экрана JEI и пишет в кэш активной вкладки.
     */
    public static void syncAndSaveActiveTab() {
        if (isUpdatingJei) {
            LOGGER.warn("[JeiTabs DBG] syncAndSaveActiveTab blocked: isUpdatingJei is true");
            return;
        }
        IJeiRuntime runtime = JeiAddonPlugin.getRuntime();
        if (runtime == null) {
            LOGGER.warn("[JeiTabs DBG] syncAndSaveActiveTab aborted: IJeiRuntime is null");
            return;
        }

        LOGGER.info("[JeiTabs DBG] Starting active tab sync for tab index: {}", currentTab);
        try {
            IBookmarkOverlay bookmarkOverlay = runtime.getBookmarkOverlay();
            List<Object> currentJeiList = findJeiLiveList(bookmarkOverlay);

            if (currentJeiList != null) {
                LOGGER.info("[JeiTabs DBG] Found live JEI list with {} elements", currentJeiList.size());
                List<JeiBookmarkElement> currentScreenElements = new ArrayList<>();

                for (Object bookmarkObj : currentJeiList) {
                    if (bookmarkObj == null) continue;

                    ItemStack stack = null;

                    // Стратегия 1: Вызов методов без аргументов
                    try {
                        for (java.lang.reflect.Method method : bookmarkObj.getClass().getDeclaredMethods()) {
                            method.setAccessible(true);
                            if (method.getParameterCount() == 0) {
                                Object res = method.invoke(bookmarkObj);
                                if (res instanceof ItemStack s && !s.isEmpty()) {
                                    stack = s;
                                    break;
                                }
                                if (res instanceof ITypedIngredient<?> typed && typed.getIngredient() instanceof ItemStack s) {
                                    stack = s;
                                    break;
                                }
                            }
                        }
                    } catch (Throwable ignored) {}

                    // Стратегия 2: Потрошим поля объекта напрямую
                    if (stack == null) {
                        try {
                            for (java.lang.reflect.Field field : bookmarkObj.getClass().getDeclaredFields()) {
                                field.setAccessible(true);
                                Object value = field.get(bookmarkObj);
                                if (value == null) continue;

                                if (value instanceof ItemStack s && !s.isEmpty()) {
                                    stack = s;
                                    break;
                                }
                                if (value instanceof ITypedIngredient<?> typed && typed.getIngredient() instanceof ItemStack s) {
                                    stack = s;
                                    break;
                                }

                                for (java.lang.reflect.Field subField : value.getClass().getDeclaredFields()) {
                                    subField.setAccessible(true);
                                    Object subValue = subField.get(value);
                                    if (subValue instanceof ItemStack s && !s.isEmpty()) {
                                        stack = s;
                                        break;
                                    }
                                }
                                if (stack != null) break;
                            }
                        } catch (Throwable ignored) {}
                    }

                    if (stack != null && !stack.isEmpty()) {
                        CompoundTag nbt = stack.save(new CompoundTag());
                        currentScreenElements.add(new JeiBookmarkElement("item_stack", nbt.toString()));
                    }
                }

                tabsBookmarksUids.set(currentTab, currentScreenElements);
                LOGGER.info("[JeiTabs DBG] Successfully cached {} elements for tab {}", currentScreenElements.size(), currentTab);
                saveTabsToDisk();
            }
        } catch (Throwable t) {
            LOGGER.error("[JeiTabs DBG] Critical error during list sync", t);
        }
    }

    /**
     * Переключение вкладок через прямую подмену контента списка JEI.
     */
    public static void switchToTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= 9 || tabIndex == currentTab) return;
        if (isUpdatingJei) return;

        IJeiRuntime runtime = JeiAddonPlugin.getRuntime();
        if (runtime == null) {
            currentTab = tabIndex;
            return;
        }

        LOGGER.info("[JeiTabs DBG] Switching tab: {} -> {}", currentTab, tabIndex);
        isUpdatingJei = true;

        try {
            IBookmarkOverlay bookmarkOverlay = runtime.getBookmarkOverlay();
            List<Object> currentJeiBookmarks = findJeiLiveList(bookmarkOverlay);

            if (currentJeiBookmarks != null) {
                currentJeiBookmarks.clear();
                currentTab = tabIndex;

                List<JeiBookmarkElement> targetCache = tabsBookmarksUids.get(currentTab);
                var ingredientManager = runtime.getIngredientManager();
                var itemType = mezz.jei.api.constants.VanillaTypes.ITEM_STACK;
                int restoredCount = 0;

                for (JeiBookmarkElement element : targetCache) {
                    if (!"item_stack".equals(element.type)) continue;

                    try {
                        CompoundTag nbt = TagParser.parseTag(element.value);
                        ItemStack stack = ItemStack.of(nbt);

                        if (!stack.isEmpty()) {
                            Optional<? extends ITypedIngredient<?>> typedIngredient = ingredientManager.createTypedIngredient(
                                    (mezz.jei.api.ingredients.IIngredientType) itemType, stack
                            );

                            if (typedIngredient.isPresent()) {
                                Object bookmarkObj = createJeiBookmarkObject(bookmarkOverlay, typedIngredient.get());
                                if (bookmarkObj != null) {
                                    currentJeiBookmarks.add(bookmarkObj);
                                    restoredCount++;
                                }
                            }
                        }
                    } catch (Throwable e) {
                        LOGGER.error("[JeiTabs DBG] Failed to deserialize item: " + element.value, e);
                    }
                }

                LOGGER.info("[JeiTabs DBG] Successfully injected {} valid IBookmark objects", restoredCount);
                forceRefreshJei(bookmarkOverlay);

                // === ИСПРАВЛЕНИЕ ЗАЛИПАНИЯ ФОКУСА КНОПКИ ===
                // Принудительно сбрасываем фокус с GUI-кнопки обратно на базовый экран
                if (Minecraft.getInstance().screen != null) {
                    Minecraft.getInstance().screen.setFocused(null);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("[JeiTabs DBG] Exception caught during tab switch transaction", t);
        } finally {
            isUpdatingJei = false;
            LOGGER.info("[JeiTabs DBG] Tab switch transaction finalized. Current active tab: {}", currentTab);
        }
    }


    public static void loadTabsFromDisk() {
        File file = getSaveFile();
        LOGGER.info("[JeiTabs DBG] Request to load config from disk. Path: {}", file.getAbsolutePath());
        if (!file.exists()) {
            LOGGER.info("[JeiTabs DBG] Config file not found. Creating a fresh template...");
            saveTabsToDisk();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<List<JeiBookmarkElement>>>() {}.getType();
            List<List<JeiBookmarkElement>> serializedTabs = GSON.fromJson(reader, type);

            if (serializedTabs != null) {
                int totalElementsLoaded = 0;
                for (int i = 0; i < Math.min(serializedTabs.size(), 9); i++) {
                    tabsBookmarksUids.get(i).clear();
                    tabsBookmarksUids.get(i).addAll(serializedTabs.get(i));
                    totalElementsLoaded += serializedTabs.get(i).size();
                    LOGGER.debug("[JeiTabs DBG] Loaded {} items for internal tab list index [{}]", serializedTabs.get(i).size(), i);
                }
                LOGGER.info("[JeiTabs DBG] Configuration parsing successful! Total items loaded: {}", totalElementsLoaded);

                // === АВТОМАТИЧЕСКАЯ ПРОГРУЗКА ПЕРВОЙ ВКЛАДКИ ===
                currentTab = 0; // Сбрасываем индекс на первую вкладку
                IJeiRuntime runtime = JeiAddonPlugin.getRuntime();
                if (runtime != null) {
                    LOGGER.info("[JeiTabs DBG] Automatically initializing screen with Tab 0 content on world load");

                    // Вызываем логику наполнения экрана первой вкладкой
                    IBookmarkOverlay bookmarkOverlay = runtime.getBookmarkOverlay();
                    List<Object> currentJeiBookmarks = findJeiLiveList(bookmarkOverlay);
                    if (currentJeiBookmarks != null) {
                        currentJeiBookmarks.clear();
                        var ingredientManager = runtime.getIngredientManager();
                        var itemType = mezz.jei.api.constants.VanillaTypes.ITEM_STACK;

                        for (JeiBookmarkElement element : tabsBookmarksUids.get(0)) {
                            if (!"item_stack".equals(element.type)) continue;
                            try {
                                CompoundTag nbt = TagParser.parseTag(element.value);
                                ItemStack stack = ItemStack.of(nbt);
                                if (!stack.isEmpty()) {
                                    var typedIngredient = ingredientManager.createTypedIngredient(
                                            (mezz.jei.api.ingredients.IIngredientType) itemType, stack
                                    );
                                    if (typedIngredient.isPresent()) {
                                        Object bookmarkObj = createJeiBookmarkObject(bookmarkOverlay, (ITypedIngredient<?>) typedIngredient.get());
                                        if (bookmarkObj != null) currentJeiBookmarks.add(bookmarkObj);
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                        forceRefreshJei(bookmarkOverlay);

                        // Сбрасываем фокус при старте
                        if (Minecraft.getInstance().screen != null) {
                            Minecraft.getInstance().screen.setFocused(null);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("[JeiTabs DBG] Crash occurred while parsing json file from disk", t);
        }
    }


    public static void saveTabsToDisk() {
        File file = getSaveFile();
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(tabsBookmarksUids, writer);
                writer.flush();
            }
        } catch (Throwable t) {
            LOGGER.error("[JeiTabs DBG] Critical IO error during configuration serialization", t);
        }
    }
    @SuppressWarnings("unchecked")
    private static List findJeiLiveList(IBookmarkOverlay bookmarkOverlay) throws Exception {
        if (bookmarkOverlay == null) return null;
        for (java.lang.reflect.Field field : bookmarkOverlay.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object fieldValue = field.get(bookmarkOverlay);
            if (fieldValue == null) continue;
            if (List.class.isAssignableFrom(field.getType())) {
                return (List) fieldValue;
            }
            String typeName = field.getType().getSimpleName();
            if (typeName.contains("Bookmark") || typeName.contains("List") || typeName.contains("Grid")) {
                for (java.lang.reflect.Field subField : fieldValue.getClass().getDeclaredFields()) {
                    subField.setAccessible(true);
                    Object subValue = subField.get(fieldValue);
                    if (subValue instanceof List) {
                        return (List) subValue;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Исправленное создание обертки IBookmark под версию JEI 15.74+
     */
    private static Object createJeiBookmarkObject(IBookmarkOverlay bookmarkOverlay, ITypedIngredient<?> typedIngredient) {
        try {
            Class<?> bookmarkClass = Class.forName("mezz.jei.gui.bookmarks.IngredientBookmark");

            // Сканируем конструкторы класса IngredientBookmark
            for (java.lang.reflect.Constructor<?> constructor : bookmarkClass.getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 2) {
                    Class<?>[] params = constructor.getParameterTypes();

                    // Проверяем, что первый параметр конструктора принимает наш ингредиент
                    if (params[0].isAssignableFrom(typedIngredient.getClass())) {
                        constructor.setAccessible(true);

                        Object commandFactory = null;
                        // Ищем подходящую фабрику/команду внутри полей BookmarkOverlay
                        for (java.lang.reflect.Field field : bookmarkOverlay.getClass().getDeclaredFields()) {
                            field.setAccessible(true);
                            Object val = field.get(bookmarkOverlay);
                            if (val != null && params[1].isAssignableFrom(val.getClass())) {
                                commandFactory = val;
                                break;
                            }
                        }

                        if (commandFactory != null) {
                            return constructor.newInstance(typedIngredient, commandFactory);
                        }
                    }
                }
            }

            // Фоллбэк на старый однопараметрический конструктор
            java.lang.reflect.Constructor<?> singleConstructor = bookmarkClass.getDeclaredConstructor(mezz.jei.api.ingredients.ITypedIngredient.class);
            singleConstructor.setAccessible(true);
            return singleConstructor.newInstance(typedIngredient);

        } catch (Throwable t) {
            LOGGER.error("[JeiTabs DBG] Failed to build true IBookmark object. Attempting factory method...", t);
            try {
                Class<?> bookmarkClass = Class.forName("mezz.jei.gui.bookmarks.IngredientBookmark");
                for (java.lang.reflect.Method method : bookmarkClass.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 1) {
                        if (method.getParameterTypes()[0].isAssignableFrom(typedIngredient.getClass())) {
                            method.setAccessible(true);
                            return method.invoke(null, typedIngredient);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static void forceRefreshJei(IBookmarkOverlay bookmarkOverlay) throws Exception {
        for (java.lang.reflect.Method method : bookmarkOverlay.getClass().getDeclaredMethods()) {
            String name = method.getName().toLowerCase();
            if (name.contains("update") || name.contains("refresh") || name.contains("recalculate")) {
                method.setAccessible(true);
                try {
                    if (method.getParameterCount() == 0) {
                        method.invoke(bookmarkOverlay);
                    } else if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == boolean.class) {
                        method.invoke(bookmarkOverlay, true);
                    }
                } catch (Throwable ignored) {}
            }
        }
        for (java.lang.reflect.Field field : bookmarkOverlay.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object childObj = field.get(bookmarkOverlay);
            if (childObj == null) continue;
            String typeName = field.getType().getSimpleName().toLowerCase();
            if (typeName.contains("bookmark") || typeName.contains("grid") || typeName.contains("navigation")) {
                for (java.lang.reflect.Method method : childObj.getClass().getDeclaredMethods()) {
                    String name = method.getName().toLowerCase();
                    if (name.contains("notify") || name.contains("update") || name.contains("refresh") || name.contains("changed")) {
                        method.setAccessible(true);
                        try {
                            if (method.getParameterCount() == 0) {
                                method.invoke(childObj);
                            } else if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == boolean.class) {
                                method.invoke(childObj, true);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            }
        }
    }
}