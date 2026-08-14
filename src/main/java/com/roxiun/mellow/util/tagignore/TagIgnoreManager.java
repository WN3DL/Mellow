package com.roxiun.mellow.util.tagignore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;

public class TagIgnoreManager {

    private final File tagIgnoreFile;
    private final File tagIgnoreTxtFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<UUID, TagIgnoredPlayer> tagIgnoreList = new ConcurrentHashMap<>();

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    public TagIgnoreManager() {
        File configDir = new File(
            Minecraft.getMinecraft().mcDataDir,
            "config/mellow"
        );
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.tagIgnoreFile = new File(configDir, "tagignore.json");
        this.tagIgnoreTxtFile = new File(configDir, "tagignore.txt");
        loadTagIgnore();
        syncWithTxtFile();
    }

    public void loadTagIgnore() {
        if (tagIgnoreFile.exists()) {
            try (FileReader reader = new FileReader(tagIgnoreFile)) {
                Type type = new TypeToken<
                    ConcurrentHashMap<UUID, TagIgnoredPlayer>
                >() {}.getType();
                tagIgnoreList = gson.fromJson(reader, type);
                if (tagIgnoreList == null) {
                    tagIgnoreList = new ConcurrentHashMap<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveTagIgnore() {
        try (FileWriter writer = new FileWriter(tagIgnoreFile)) {
            gson.toJson(tagIgnoreList, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFromTxtFile() {
        if (!tagIgnoreTxtFile.exists()) {
            return;
        }

        try (
            BufferedReader reader = new BufferedReader(
                new FileReader(tagIgnoreTxtFile)
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (UUID_PATTERN.matcher(line).matches()) {
                    try {
                        UUID uuid = UUID.fromString(line);
                        if (!tagIgnoreList.containsKey(uuid)) {
                            tagIgnoreList.put(
                                uuid,
                                new TagIgnoredPlayer(
                                    uuid.toString(),
                                    "Added from TXT file"
                                )
                            );
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println(
                            "Invalid UUID format in tagignore.txt: " + line
                        );
                    }
                } else {
                    System.err.println(
                        "Skipping invalid UUID in tagignore.txt: " + line
                    );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToTxtFile() {
        try (FileWriter writer = new FileWriter(tagIgnoreTxtFile)) {
            for (UUID uuid : tagIgnoreList.keySet()) {
                writer.write(uuid.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void syncWithTxtFile() {
        if (tagIgnoreTxtFile.exists()) {
            loadFromTxtFile();
        }
        saveToTxtFile();
    }

    public int syncWithExternalFile(File externalFile) {
        if (!externalFile.exists()) {
            return 0;
        }

        int newEntriesCount = 0;
        try (
            BufferedReader reader = new BufferedReader(
                new FileReader(externalFile)
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (UUID_PATTERN.matcher(line).matches()) {
                    try {
                        UUID uuid = UUID.fromString(line);
                        if (!tagIgnoreList.containsKey(uuid)) {
                            tagIgnoreList.put(
                                uuid,
                                new TagIgnoredPlayer(
                                    uuid.toString(),
                                    "Added from external file"
                                )
                            );
                            newEntriesCount++;
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println(
                            "Invalid UUID format in external file: " + line
                        );
                    }
                } else {
                    System.err.println(
                        "Skipping invalid UUID in external file: " + line
                    );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        if (newEntriesCount > 0) {
            saveTagIgnore();
            saveToTxtFile();
        }

        return newEntriesCount;
    }

    public boolean addPlayer(UUID uuid, String name, String reason) {
        if (tagIgnoreList.containsKey(uuid)) {
            return false;
        }
        tagIgnoreList.put(uuid, new TagIgnoredPlayer(name, reason));
        saveTagIgnore();
        saveToTxtFile();
        return true;
    }

    public void removePlayer(UUID uuid) {
        if (tagIgnoreList.remove(uuid) != null) {
            saveTagIgnore();
            saveToTxtFile();
        }
    }

    public boolean isTagIgnored(UUID uuid) {
        return tagIgnoreList.containsKey(uuid);
    }

    public TagIgnoredPlayer getTagIgnoredPlayer(UUID uuid) {
        return tagIgnoreList.get(uuid);
    }

    public Map<UUID, TagIgnoredPlayer> getTagIgnoreList() {
        return tagIgnoreList;
    }
}
