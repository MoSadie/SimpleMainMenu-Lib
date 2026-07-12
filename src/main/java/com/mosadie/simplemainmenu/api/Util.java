package com.mosadie.simplemainmenu.api;

import com.mosadie.simplemainmenu.client.SimpleMainMenuLibClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.RandomSource;

import java.time.LocalDate;

public class Util {
    /**
     * An implementation of rolling odds based on a month, day, and days before system.
     * Always returns false daysBefore days before the month/day
     * Then has an increasing chance to return true as the month/day approaches.
     * Returns true always on the month/day.
     * Has a decreasing chance over the daysAfter days after month/day
     * @param month Month of the year, with 1 = January, 2 = February, etc.
     * @param day The day of the month.
     * @param daysBefore Number of days before the month/day the theme should start appearing.
     * @param daysAfter Number of days after the month/day the theme should start appearing.
     * @return True if the theme should be selected, false otherwise. Usable in {@link MenuTheme#rollOdds}
     */
    public static boolean rollOddsMonthDay(int month, int day, int daysBefore, int daysAfter) {
        RandomSource random = RandomSource.create();

        LocalDate now = LocalDate.now();
        LocalDate themeDate = LocalDate.of(now.getYear(), month, day);
        LocalDate firstDate = themeDate.minusDays(daysBefore);
        LocalDate lastDate = themeDate.plusDays(daysAfter);

        // If we are before the first possible day, do not choose this theme.
        if (now.isBefore(firstDate))
            return false;

        // If we are after the last possible day, do not choose this theme.
        if (now.isAfter(lastDate))
            return false;


        // If it is the day listed, always attempt to choose the theme.
        if (now.equals(themeDate))
            return true;


        int roll;

        if (now.isBefore(themeDate)) {
            roll = random.nextInt(0, daysBefore);
            return roll <= firstDate.until(now).getDays();
        } else {
            roll = random.nextInt(0, daysAfter);
            return roll >= now.until(lastDate).getDays();
        }
    }

    /**
     * A simple rollOdds function that chooses the theme half the time.
     * @return True if the theme should be selected, false otherwise. Usable in {@link MenuTheme#rollOdds()}
     */
    public static boolean rollOddsFlipCoin() {
        RandomSource random = RandomSource.create();

        return random.nextBoolean();
    }

    /**
     * Connect to the specified server, defaulting the server name to the server address.
     * @param address The address of the server
     */
    public static void joinServer(String address) {
        joinServer(address, address);
    }

    /**
     * Connect to the specified server.
     * @param name Name of the server, used for mod data storage
     * @param address Address of the server
     */
    public static void joinServer(String name, String address) {
        if (ServerAddress.isValidAddress(address)) {
            ServerData serverData = new ServerData(name, address, ServerData.Type.OTHER);
            serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
            joinServer(serverData);
        }
    }

    /**
     * Directs Minecraft to connect to the specified server.
     * @param server The ServerInfo of the server to join.
     */
    public static void joinServer(ServerData server) {
        Minecraft.getInstance().execute(() -> {
            leaveIfNeeded();

            SimpleMainMenuLibClient.LOGGER.info("Connecting to " + server.ip);

            // Connect to server

            ConnectScreen.startConnecting(new TitleScreen(), Minecraft.getInstance(), ServerAddress.parseString(server.ip), server, false, null);
        });
    }

    public static void loadWorld(String worldName) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().getLevelSource().levelExists(worldName)) {
                leaveIfNeeded();

                SimpleMainMenuLibClient.LOGGER.info("Loading world...");
                Minecraft.getInstance().createWorldOpenFlows().openWorld(worldName, () -> {
                    SimpleMainMenuLibClient.LOGGER.info("World load cancelled.");
                    Minecraft.getInstance().setScreenAndShow(new TitleScreen());
                });
            } else {
                SimpleMainMenuLibClient.LOGGER.warn("World " + worldName + " does not exist!");
                if (Minecraft.getInstance().level == null)
                    Minecraft.getInstance().setScreenAndShow(new AlertScreen(() -> Minecraft.getInstance().setScreenAndShow(new TitleScreen()), Component.translatable("text.smm-lib.error.worldnotfound.title"), Component.translatable("text.smm-lib.error.worldnotfound.body", worldName), Component.translatable("gui.toTitle"), true));
            }
        });
    }

    /**
     * Checks if in a world and leaves it.
     */
    private static void leaveIfNeeded() {
        if (Minecraft.getInstance().level != null) {
            SimpleMainMenuLibClient.LOGGER.info("Disconnecting from world...");

            Minecraft.getInstance().level.disconnect(Component.translatable("menu.disconnect"));
            Minecraft.getInstance().disconnectWithProgressScreen();
        }
    }

    public static final Style SPLASH_TEXT_STYLE = Style.EMPTY.withColor(-256);
}
