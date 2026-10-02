package core;

import client.io.Session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    public static final ConcurrentHashMap<String, Integer> BANDWIDTH_SIZES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> LAST_RESET_TIME = new ConcurrentHashMap<>();
    private static final long RESET_INTERVAL = 60000; // Reset bandwidth counter every 60 seconds

    /**
     * Adds bandwidth usage for a specific IP address
     * Accumulates the size and checks for DDoS threshold
     */
    public static synchronized void addBandWidth(String ipAddress, int size) {
        if (ipAddress == null || size <= 0) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Reset bandwidth counter if interval has passed
        Long lastReset = LAST_RESET_TIME.get(ipAddress);
        if (lastReset != null && (currentTime - lastReset) > RESET_INTERVAL) {
            BANDWIDTH_SIZES.put(ipAddress, 0);
            LAST_RESET_TIME.put(ipAddress, currentTime);
        } else if (lastReset == null) {
            LAST_RESET_TIME.put(ipAddress, currentTime);
        }

        // Accumulate bandwidth usage
        int currentBandwidth = BANDWIDTH_SIZES.getOrDefault(ipAddress, 0);
        int newBandwidth = currentBandwidth + size;

        // Check for DDoS threshold (2GB)
        if (newBandwidth > 2_000_000_000) {
            CheckDDOS.blockIP(ipAddress, "Max Size BandWidth");
            BANDWIDTH_SIZES.remove(ipAddress);
            LAST_RESET_TIME.remove(ipAddress);
            return;
        }

        BANDWIDTH_SIZES.put(ipAddress, newBandwidth);
    }

    /**
     * Checks and logs the highest bandwidth usage, then clears the map
     */
    public static void checkBandWidth() {
        try {
            String topIP = "";
            int maxSize = 0;

            // Thread-safe iteration using snapshot
            Map<String, Integer> snapshot = new ConcurrentHashMap<>(BANDWIDTH_SIZES);

            for (Map.Entry<String, Integer> entry : snapshot.entrySet()) {
                String key = entry.getKey();
                Integer value = entry.getValue();

                if (key != null && value != null && value > maxSize) {
                    topIP = key;
                    maxSize = value;
                }
            }

            // Clear the bandwidth tracking
            BANDWIDTH_SIZES.clear();
            LAST_RESET_TIME.clear();

            // Log the highest bandwidth usage
            if (maxSize > 0) {
                String result = formatBandwidth(maxSize);
                Log.gI().addLogServer("BandWidth", "IP : [" + topIP + "] " + result);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Formats bandwidth size into readable format (b, KB, MB, GB)
     */
    private static String formatBandwidth(int size) {
        double n = size;

        if (n < 1024) {
            return n + " b";
        } else if (n < 1048576) {
            double kb = n / 1024;
            return String.format("%.2f", kb) + " KB";
        } else if (n < 1073741824) {
            double mb = n / 1048576;
            return String.format("%.2f", mb) + " MB";
        } else {
            double gb = n / 1073741824;
            return String.format("%.2f", gb) + " GB";
        }
    }

    /**
     * Removes inactive clients that haven't completed connection within timeout
     */


    public static void removeClient() {
        long currentTime = System.currentTimeMillis();

        for (int i = Session.SESSION_LIST.size() - 1; i >= 0; i--) {
            try {
                Session session = Session.SESSION_LIST.get(i);

                if (session == null) {
                    continue;
                }

                // Close session if connection timeout exceeded and info not received
                if (currentTime - session.timeConnect > Manager.timeRemoveClient && !session.get_in4) {
                    session.close();
                }

            } catch (Exception e) {
                // Log exception but continue processing other sessions
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets current bandwidth usage for a specific IP
     */
    public static int getBandwidthUsage(String ipAddress) {
        return BANDWIDTH_SIZES.getOrDefault(ipAddress, 0);
    }

    /**
     * Clears all bandwidth tracking data
     */
    public static void clearAllBandwidth() {
        BANDWIDTH_SIZES.clear();
        LAST_RESET_TIME.clear();
    }
}