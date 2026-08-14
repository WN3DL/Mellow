package com.roxiun.mellow.util.annoylist;

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

public class AnnoylistManager {

    private final File annoylistFile;
    private final File annoylistTxtFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Map<UUID, AnnoylistedPlayer> annoylist = new ConcurrentHashMap<>();

    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    public AnnoylistManager() {
        File configDir = new File(
            Minecraft.getMinecraft().mcDataDir,
            "config/mellow"
        );
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.annoylistFile = new File(configDir, "annoylist.json");
        this.annoylistTxtFile = new File(configDir, "annoylist.txt");
        loadAnnoylist();
        syncWithTxtFile();
    }

    public void loadAnnoylist() {
        if (annoylistFile.exists()) {
            try (FileReader reader = new FileReader(annoylistFile)) {
                Type type = new TypeToken<
                    ConcurrentHashMap<UUID, AnnoylistedPlayer>
                >() {}.getType();
                annoylist = gson.fromJson(reader, type);
                if (annoylist == null) {
                    annoylist = new ConcurrentHashMap<>();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void saveAnnoylist() {
        try (FileWriter writer = new FileWriter(annoylistFile)) {
            gson.toJson(annoylist, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFromTxtFile() {
        if (!annoylistTxtFile.exists()) {
            return;
        }

        try (
            BufferedReader reader = new BufferedReader(
                new FileReader(annoylistTxtFile)
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
                        if (!annoylist.containsKey(uuid)) {
                            annoylist.put(
                                uuid,
                                new AnnoylistedPlayer(
                                    uuid.toString(),
                                    "Added from TXT file"
                                )
                            );
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println(
                            "Invalid UUID format in annoylist.txt: " + line
                        );
                    }
                } else {
                    System.err.println(
                        "Skipping invalid UUID in annoylist.txt: " + line
                    );
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToTxtFile() {
        try (FileWriter writer = new FileWriter(annoylistTxtFile)) {
            for (UUID uuid : annoylist.keySet()) {
                writer.write(uuid.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void syncWithTxtFile() {
        if (annoylistTxtFile.exists()) {
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
                        if (!annoylist.containsKey(uuid)) {
                            annoylist.put(
                                uuid,
                                new AnnoylistedPlayer(
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
            saveAnnoylist();
            saveToTxtFile();
        }

        return newEntriesCount;
    }

    public boolean addPlayer(UUID uuid, String name, String reason) {
        if (annoylist.containsKey(uuid)) {
            return false;
        }
        annoylist.put(uuid, new AnnoylistedPlayer(name, reason));
        saveAnnoylist();
        saveToTxtFile();
        return true;
    }

    public void removePlayer(UUID uuid) {
        if (annoylist.remove(uuid) != null) {
            saveAnnoylist();
            saveToTxtFile();
        }
    }

    public boolean isAnnoylisted(UUID uuid) {
        return annoylist.containsKey(uuid);
    }

    public AnnoylistedPlayer getAnnoylistedPlayer(UUID uuid) {
        return annoylist.get(uuid);
    }

    public Map<UUID, AnnoylistedPlayer> getAnnoylist() {
        return annoylist;
    }
}
