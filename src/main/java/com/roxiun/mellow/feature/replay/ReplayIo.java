package com.roxiun.mellow.feature.replay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonStreamParser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReplayIo {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT_GSON = new Gson();
    private static final String META_FILE = "meta.json";
    private static final String PACKETS_FILE = "packets.bin";
    private static final String EVENTS_FILE = "events.jsonl";
    private static final String INDEX_FILE = "index.bin";

    public File getReplayRoot(File mcDataDir) {
        File root = new File(mcDataDir, "mellow-replays");
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public File createReplayDirectory(File mcDataDir, ReplayMetadata metadata) {
        File root = getReplayRoot(mcDataDir);
        String stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)
            .format(new Date(metadata.getStartedAt()));
        String suffix = sanitize(metadata.getMap());
        String id = stamp + "_" + (suffix.isEmpty() ? "bedwars" : suffix);
        File directory = new File(root, id);
        int counter = 2;
        while (directory.exists()) {
            directory = new File(root, id + "_" + counter);
            counter++;
        }
        directory.mkdirs();
        metadata.setReplayId(directory.getName());
        return directory;
    }

    public void saveReplay(
        File directory,
        ReplayMetadata metadata,
        List<ReplayPacketFrame> packets,
        List<ReplayChatEvent> chats,
        List<ReplayScoreboardFrame> scoreboards,
        List<ReplayLocalPlayerSnapshot> localSnapshots
    ) throws IOException {
        metadata.setPacketCount(packets.size());
        writeMetadata(new File(directory, META_FILE), metadata);
        writePackets(new File(directory, PACKETS_FILE), packets);
        writeEvents(new File(directory, EVENTS_FILE), chats, scoreboards, localSnapshots);
        writeIndex(new File(directory, INDEX_FILE), packets);
    }

    public ReplayLoadedData loadReplay(File directory) throws IOException {
        ReplayMetadata metadata = readMetadata(new File(directory, META_FILE));
        List<ReplayPacketFrame> packets = readPackets(new File(directory, PACKETS_FILE));
        List<ReplayChatEvent> chats = new ArrayList<>();
        List<ReplayScoreboardFrame> scoreboards = new ArrayList<>();
        List<ReplayLocalPlayerSnapshot> localSnapshots = new ArrayList<>();
        readEvents(new File(directory, EVENTS_FILE), chats, scoreboards, localSnapshots);
        Collections.sort(
            chats,
            new Comparator<ReplayChatEvent>() {
                @Override
                public int compare(ReplayChatEvent left, ReplayChatEvent right) {
                    return Integer.compare(left.getTimestampMs(), right.getTimestampMs());
                }
            }
        );
        Collections.sort(
            scoreboards,
            new Comparator<ReplayScoreboardFrame>() {
                @Override
                public int compare(
                    ReplayScoreboardFrame left,
                    ReplayScoreboardFrame right
                ) {
                    return Integer.compare(left.getTimestampMs(), right.getTimestampMs());
                }
            }
        );
        Collections.sort(
            localSnapshots,
            new Comparator<ReplayLocalPlayerSnapshot>() {
                @Override
                public int compare(
                    ReplayLocalPlayerSnapshot left,
                    ReplayLocalPlayerSnapshot right
                ) {
                    return Integer.compare(left.getTimestampMs(), right.getTimestampMs());
                }
            }
        );
        return new ReplayLoadedData(
            directory,
            metadata,
            packets,
            chats,
            scoreboards,
            localSnapshots
        );
    }

    public List<ReplayCatalogEntry> listReplays(File mcDataDir) {
        File root = getReplayRoot(mcDataDir);
        File[] directories = root.listFiles();
        if (directories == null || directories.length == 0) {
            return Collections.emptyList();
        }

        List<ReplayCatalogEntry> entries = new ArrayList<>();
        Arrays.sort(directories);
        for (File directory : directories) {
            if (!directory.isDirectory()) {
                continue;
            }
            File meta = new File(directory, META_FILE);
            if (!meta.exists()) {
                continue;
            }
            try {
                entries.add(new ReplayCatalogEntry(directory, readMetadata(meta)));
            } catch (Exception ignored) {}
        }

        Collections.sort(
            entries,
            new Comparator<ReplayCatalogEntry>() {
                @Override
                public int compare(
                    ReplayCatalogEntry left,
                    ReplayCatalogEntry right
                ) {
                    return Long.compare(
                        right.getMetadata().getStartedAt(),
                        left.getMetadata().getStartedAt()
                    );
                }
            }
        );
        return entries;
    }

    public boolean deleteReplay(File directory) {
        if (directory == null || !directory.exists()) {
            return false;
        }
        return deleteRecursively(directory);
    }

    public void pruneOldest(File mcDataDir, int maxStoredReplays) {
        if (maxStoredReplays <= 0) {
            return;
        }

        List<ReplayCatalogEntry> entries = listReplays(mcDataDir);
        for (int i = maxStoredReplays; i < entries.size(); i++) {
            deleteReplay(entries.get(i).getDirectory());
        }
    }

    private void writeMetadata(File file, ReplayMetadata metadata) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            GSON.toJson(metadata, writer);
        }
    }

    private ReplayMetadata readMetadata(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return GSON.fromJson(reader, ReplayMetadata.class);
        }
    }

    private void writePackets(File file, List<ReplayPacketFrame> packets) throws IOException {
        try (
            DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file))
            )
        ) {
            out.writeInt(packets.size());
            for (ReplayPacketFrame frame : packets) {
                byte[] nameBytes = frame.getClassName().getBytes(StandardCharsets.UTF_8);
                out.writeInt(frame.getTimestampMs());
                out.writeInt(nameBytes.length);
                out.write(nameBytes);
                out.writeInt(frame.getPayload().length);
                out.write(frame.getPayload());
            }
        }
    }

    private List<ReplayPacketFrame> readPackets(File file) throws IOException {
        List<ReplayPacketFrame> packets = new ArrayList<>();
        if (!file.exists()) {
            return packets;
        }
        try (
            DataInputStream in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file))
            )
        ) {
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                int timestamp = in.readInt();
                int classNameLength = in.readInt();
                byte[] classNameBytes = new byte[classNameLength];
                in.readFully(classNameBytes);
                int payloadLength = in.readInt();
                byte[] payload = new byte[payloadLength];
                in.readFully(payload);
                packets.add(
                    new ReplayPacketFrame(
                        timestamp,
                        new String(classNameBytes, StandardCharsets.UTF_8),
                        payload
                    )
                );
            }
        }
        return packets;
    }

    private void writeEvents(
        File file,
        List<ReplayChatEvent> chats,
        List<ReplayScoreboardFrame> scoreboards,
        List<ReplayLocalPlayerSnapshot> localSnapshots
    ) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (ReplayChatEvent event : chats) {
                JsonObject object = new JsonObject();
                object.addProperty("type", "chat");
                object.add("payload", GSON.toJsonTree(event));
                writer.write(COMPACT_GSON.toJson(object));
                writer.newLine();
            }
            for (ReplayScoreboardFrame event : scoreboards) {
                JsonObject object = new JsonObject();
                object.addProperty("type", "scoreboard");
                object.add("payload", GSON.toJsonTree(event));
                writer.write(COMPACT_GSON.toJson(object));
                writer.newLine();
            }
            for (ReplayLocalPlayerSnapshot event : localSnapshots) {
                JsonObject object = new JsonObject();
                object.addProperty("type", "local_player");
                object.add("payload", GSON.toJsonTree(event));
                writer.write(COMPACT_GSON.toJson(object));
                writer.newLine();
            }
        }
    }

    private void readEvents(
        File file,
        List<ReplayChatEvent> chats,
        List<ReplayScoreboardFrame> scoreboards,
        List<ReplayLocalPlayerSnapshot> localSnapshots
    ) throws IOException {
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            JsonStreamParser parser = new JsonStreamParser(reader);
            while (parser.hasNext()) {
                JsonObject object = parser.next().getAsJsonObject();
                String type = object.get("type").getAsString();
                JsonElement payload = object.get("payload");
                if ("chat".equals(type)) {
                    chats.add(GSON.fromJson(payload, ReplayChatEvent.class));
                } else if ("scoreboard".equals(type)) {
                    scoreboards.add(
                        GSON.fromJson(payload, ReplayScoreboardFrame.class)
                    );
                } else if ("local_player".equals(type)) {
                    localSnapshots.add(
                        GSON.fromJson(payload, ReplayLocalPlayerSnapshot.class)
                    );
                }
            }
        }
    }

    private void writeIndex(File file, List<ReplayPacketFrame> packets) throws IOException {
        try (
            DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(file))
            )
        ) {
            int nextSecondMark = 0;
            int count = 0;
            for (int i = 0; i < packets.size(); i++) {
                ReplayPacketFrame frame = packets.get(i);
                if (frame.getTimestampMs() >= nextSecondMark) {
                    count++;
                    nextSecondMark += 1000;
                }
            }
            out.writeInt(count);
            nextSecondMark = 0;
            for (int i = 0; i < packets.size(); i++) {
                ReplayPacketFrame frame = packets.get(i);
                if (frame.getTimestampMs() >= nextSecondMark) {
                    out.writeInt(nextSecondMark);
                    out.writeInt(i);
                    nextSecondMark += 1000;
                }
            }
        }
    }

    private boolean deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
    }
}
