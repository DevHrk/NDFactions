package me.nd.factions.api;

import java.lang.reflect.*;
import org.bukkit.*;
import org.bukkit.entity.*;

public class TitleAPI
{
    private static final String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    private static Method a;
    private static Object enumTIMES;
    private static Object enumTITLE;
    private static Object enumSUBTITLE;
    private static Constructor<?> timeTitleConstructor;
    private static Constructor<?> textTitleConstructor;
    private static Method getHandle;
    private static Method sendPacket;
    private static Field playerConnectionField;

    static {
        try {
            getHandle = TitleAPI.getOBClass("entity.CraftPlayer").getMethod("getHandle", new Class[0]);
            playerConnectionField = TitleAPI.getNMSClass("EntityPlayer").getField("playerConnection");
            sendPacket = TitleAPI.getNMSClass("PlayerConnection").getMethod("sendPacket", TitleAPI.getNMSClass("Packet"));
            Class<?> icbc = TitleAPI.getNMSClass("IChatBaseComponent");
            Class<?> ppot = TitleAPI.getNMSClass("PacketPlayOutTitle");
            Class<?> enumClass = ppot.getDeclaredClasses().length > 0 ? ppot.getDeclaredClasses()[0] : TitleAPI.getNMSClass("EnumTitleAction");
            a = icbc.getDeclaredClasses().length > 0 ? icbc.getDeclaredClasses()[0].getMethod("a", String.class) : TitleAPI.getNMSClass("ChatSerializer").getMethod("a", String.class);
            enumTIMES = enumClass.getField("TIMES").get(null);
            enumTITLE = enumClass.getField("TITLE").get(null);
            enumSUBTITLE = enumClass.getField("SUBTITLE").get(null);
            timeTitleConstructor = ppot.getConstructor(enumClass, icbc, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            textTitleConstructor = ppot.getConstructor(enumClass, icbc);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public static void sendTitle(Player player, Integer fadeIn, Integer stay, Integer fadeOut, String title, String subtitle) {
        try {
            Object chatTitle = a.invoke(null, "{\"text\":\"" + title + "\"}");
            Object chatSubtitle = a.invoke(null, "{\"text\":\"" + subtitle + "\"}");
            Object timeTitlePacket = timeTitleConstructor.newInstance(enumTIMES, null, fadeIn, stay, fadeOut);
            Object titlePacket = textTitleConstructor.newInstance(enumTITLE, chatTitle);
            Object subtitlePacket = textTitleConstructor.newInstance(enumSUBTITLE, chatSubtitle);
            TitleAPI.sendPacket(player, timeTitlePacket);
            TitleAPI.sendPacket(player, titlePacket);
            TitleAPI.sendPacket(player, subtitlePacket);
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void sendPacket(Player player, Object packet) {
        try {
            Object entityPlayer = getHandle.invoke(player, new Object[0]);
            Object playerConnection = playerConnectionField.get(entityPlayer);
            sendPacket.invoke(playerConnection, packet);
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + version + "." + name);
    }

    public static Class<?> getOBClass(String name) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + version + "." + name);
    }
}
