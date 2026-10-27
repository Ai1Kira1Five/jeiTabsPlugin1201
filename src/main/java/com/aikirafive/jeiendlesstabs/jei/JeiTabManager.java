package com.aikirafive.jeiendlesstabs.jei;

import mezz.jei.api.runtime.IJeiRuntime;
import java.util.ArrayList;
import java.util.List;

public class JeiTabManager {
    // Храним текстовые ID предметов для 9 вкладок, чтобы не зависеть от API JEI
    private static final List<List<String>> tabsBookmarksIds = new ArrayList<>(9);
    private static int currentTab = 0;

    static {
        for (int i = 0; i < 9; i++) {
            tabsBookmarksIds.add(new ArrayList<>());
        }
    }

    public static int getCurrentTab() {
        return currentTab;
    }

    public static void switchToTab(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= 9) return;

        // Просто переключаем индекс. Нам больше НЕ НУЖНО вызывать отсутствующие методы JEI!
        currentTab = tabIndex;

        // Игра больше никогда не упадет в StackOverflowError
    }

    // Метод для добавления ID предмета во вкладку (вызывайте при необходимости сохранить)
    public static void addIdToTab(int tab, String itemRegistryName) {
        if (!tabsBookmarksIds.get(tab).contains(itemRegistryName)) {
            tabsBookmarksIds.get(tab).add(itemRegistryName);
        }
    }
}