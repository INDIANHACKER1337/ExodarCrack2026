/////
///// THIS SHIT WAS LEAKED BY INDIAN HACKER 1337 & jsexp ImLegiitXD LOL!
/////
/////
/////
package io.github.exodar.friend;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the friends list with file persistence
 * Friends are saved to a separate file (not part of configs)
 */
public class FriendManager {
    private static FriendManager instance;
    private final Set<String> friends = new HashSet<>();
    private static final String FRIENDS_FILE = "exodar_friends.txt";

    private FriendManager() {
        load();
    }

    public static FriendManager getInstance() {
        if (instance == null) {
            instance = new FriendManager();
        }
        return instance;
    }

    public boolean isFriend(String name) {
        if (name == null) return false;
        return friends.contains(name.toLowerCase());
    }

    public void addFriend(String name) {
        if (name != null && !name.isEmpty()) {
            friends.add(name.toLowerCase());
            save();
        }
    }

    public void removeFriend(String name) {
        if (name != null) {
            friends.remove(name.toLowerCase());
            save();
        }
    }

    public boolean toggleFriend(String name) {
        if (name == null || name.isEmpty()) return false;

        String lowerName = name.toLowerCase();
        if (friends.contains(lowerName)) {
            friends.remove(lowerName);
            save();
            return false; // Removed
        } else {
            friends.add(lowerName);
            save();
            return true; // Added
        }
    }

    public Set<String> getFriends() {
        return new HashSet<>(friends);
    }

    public List<String> getFriendsList() {
        return new ArrayList<>(friends);
    }

    public void clearFriends() {
        friends.clear();
        save();
    }

    public int getFriendCount() {
        return friends.size();
    }

    /**
     * Save friends to file
     */
    public void save() {
        try {
            // Get minecraft directory
            File mcDir = new File(System.getProperty("user.home"), ".minecraft");
            if (!mcDir.exists()) {
                mcDir = new File(".");
            }

            File friendsFile = new File(mcDir, FRIENDS_FILE);

            try (PrintWriter writer = new PrintWriter(new FileWriter(friendsFile))) {
                for (String friend : friends) {
                    writer.println(friend);
                }
            }
        } catch (Exception e) {
            System.out.println("[FriendManager] Error saving friends: " + e.getMessage());
        }
    }

    /**
     * Load friends from file
     */
    public void load() {
        try {
            // Get minecraft directory
            File mcDir = new File(System.getProperty("user.home"), ".minecraft");
            if (!mcDir.exists()) {
                mcDir = new File(".");
            }

            File friendsFile = new File(mcDir, FRIENDS_FILE);

            if (!friendsFile.exists()) {
                return;
            }

            friends.clear();
            try (BufferedReader reader = new BufferedReader(new FileReader(friendsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    if (!line.isEmpty()) {
                        friends.add(line);
                    }
                }
            }
            System.out.println("[FriendManager] Loaded " + friends.size() + " friends");
        } catch (Exception e) {
            System.out.println("[FriendManager] Error loading friends: " + e.getMessage());
        }
    }
}
