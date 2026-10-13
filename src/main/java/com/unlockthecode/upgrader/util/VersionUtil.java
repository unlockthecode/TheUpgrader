package com.unlockthecode.upgrader.util;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for handling version differences across Paper/Minecraft versions.
 * Provides safe access to particles, sounds, and materials that may not exist in all versions.
 */
public final class VersionUtil {

    private static final Logger LOGGER = Logger.getLogger("TheUpgrader.VersionUtil");

    // Parsed server version (e.g., 1.21.1 -> major=1, minor=21, patch=1)
    private static final int VERSION_MAJOR;
    private static final int VERSION_MINOR;
    private static final int VERSION_PATCH;

    static {
        String version = Bukkit.getVersion(); // e.g., "git-Paper-1.21.1-R0.1-SNAPSHOT (MC: 1.21.1)"
        int major = 1, minor = 21, patch = 0;

        // Extract version from "MC: X.Y.Z" or similar
        int mcIndex = version.indexOf("MC: ");
        if (mcIndex >= 0) {
            String mcVersion = version.substring(mcIndex + 4).split("[ )]")[0];
            String[] parts = mcVersion.split("\\.");
            try {
                if (parts.length >= 1) major = Integer.parseInt(parts[0]);
                if (parts.length >= 2) minor = Integer.parseInt(parts[1]);
                if (parts.length >= 3) patch = Integer.parseInt(parts[2].split("-")[0]); // Handle 1.21.1-R0.1
            } catch (NumberFormatException ignored) {
                // Use defaults
            }
        } else {
            // Fallback: try to parse from "git-Paper-X.Y.Z"
            String[] parts = version.split("-");
            for (String part : parts) {
                if (part.matches("\\d+\\.\\d+\\.\\d+.*")) {
                    String[] verParts = part.split("\\.");
                    try {
                        if (verParts.length >= 1) major = Integer.parseInt(verParts[0]);
                        if (verParts.length >= 2) minor = Integer.parseInt(verParts[1]);
                        if (verParts.length >= 3) patch = Integer.parseInt(verParts[2].split("-")[0]);
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }
        }

        VERSION_MAJOR = major;
        VERSION_MINOR = minor;
        VERSION_PATCH = patch;

        LOGGER.info(String.format("Detected Minecraft version: %d.%d.%d", major, minor, patch));
    }

    public static int getMajorVersion() { return VERSION_MAJOR; }
    public static int getMinorVersion() { return VERSION_MINOR; }
    public static int getPatchVersion() { return VERSION_PATCH; }

    public static boolean isAtLeast(int major, int minor) {
        if (VERSION_MAJOR != major) return VERSION_MAJOR > major;
        return VERSION_MINOR >= minor;
    }

    public static boolean isAtLeast(int major, int minor, int patch) {
        if (VERSION_MAJOR != major) return VERSION_MAJOR > major;
        if (VERSION_MINOR != minor) return VERSION_MINOR > minor;
        return VERSION_PATCH >= patch;
    }

    /**
     * Safely get a Particle by name, with fallback.
     */
    public static Particle getParticle(String name, Particle fallback) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Particle {0} not found in this version, using fallback", name);
            return fallback;
        }
    }

    /**
     * Safely get a Sound by name, with fallback.
     */
    public static Sound getSound(String name, Sound fallback) {
        try {
            return Sound.valueOf(name);
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Sound {0} not found in this version, using fallback", name);
            return fallback;
        }
    }

    /**
     * Spawn particle safely with version-compatible names.
     */
    public static void spawnParticleSafe(org.bukkit.Location loc, String primaryName, String fallbackName, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        Particle primary = getParticle(primaryName, null);
        if (primary != null) {
            loc.getWorld().spawnParticle(primary, loc, count, offsetX, offsetY, offsetZ, extra);
            return;
        }
        Particle fallback = getParticle(fallbackName, Particle.EXPLOSION);
        loc.getWorld().spawnParticle(fallback, loc, count, offsetX, offsetY, offsetZ, extra);
    }

    /**
     * Play sound safely with version-compatible names.
     */
    public static void playSoundSafe(org.bukkit.Location loc, String primaryName, String fallbackName, float volume, float pitch) {
        Sound primary = getSound(primaryName, null);
        if (primary != null) {
            loc.getWorld().playSound(loc, primary, volume, pitch);
            return;
        }
        Sound fallback = getSound(fallbackName, Sound.ENTITY_PLAYER_LEVELUP);
        loc.getWorld().playSound(loc, fallback, volume, pitch);
    }

    /**
     * Get the server version string for display.
     */
    public static String getVersionString() {
        return VERSION_MAJOR + "." + VERSION_MINOR + "." + VERSION_PATCH;
    }

    /**
     * Check if running on a modern version (1.20+)
     */
    public static boolean isModernVersion() {
        return isAtLeast(1, 20);
    }

    /**
     * Check if running on 1.21+
     */
    public static boolean isVersion21Plus() {
        return isAtLeast(1, 21);
    }
}