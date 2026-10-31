# JEI Endless Tabs & Extra Inventory

A Forge modification for Minecraft 1.20.1 that expands the player's inventory capabilities and upgrades Just Enough Items (Build 15.74+) with an advanced multi-tab bookmarking system.

## 🚀 Features

### 1. Extended Inventory Layout
* Adds **extra slots** directly attached to the container interface.
* Perfectly anchors with standard and modded container slots without breaking visual consistency.

### 2. Multi-Tab JEI Bookmarks
* Expands the standard JEI bookmark screen into **9 independent arrays (tabs)**.
* Allows players to organize bookmarks by categories (e.g., Tab 1 for Tech, Tab 2 for Magic, Tab 3 for Building Blocks).
* **Automatic disk serialization**: All tabs are saved into a single `config/jei_endless_tabs.json` file in genuine NBT format, preserving item data, custom names, and enchantments.
* Fully persistent across game restarts—your first tab automatically restores upon world load.

## ⌨️ Controls

* **`Mouse Click` on Tabs (1-9)**: Instantly switches between different bookmark arrays.
* **`A` Key (over an item)**: Adds or removes items from the currently active tab.
* *Note: The system features smart focus blocking, preventing bookmark accidental overrides while typing in search bars or chat.*

## 🛠️ Technical Details

* **Platform**: Forge 1.20.1 (Built and tested against Forge `47.4.28`)
* **Dependency**: Just Enough Items (JEI) `15.74.0.229` or newer.
* **Architecture**: Uses clean reflection loops to inject custom `IBookmark` arrays safely into JEI's runtime controllers without relying on volatile mixins for list manipulation.

## 📦 Setup & Development

If you want to compile the project locally or contribute:

1. Clone the repository:
   ```bash
   git clone https://github.com
   ```
2. Setup the Gradle workspace:
   ```bash
   gradlew genEclipseRuns  # For Eclipse
   # OR
   gradlew genIntellijRuns # For IntelliJ IDEA
   ```
3. Build the mod:
   ```bash
   gradlew build
   ```
   The compiled `.jar` file will be located in `build/libs/`.

## 📜 License

This project is available under the MIT License. Feel free to use, modify, and distribute.
