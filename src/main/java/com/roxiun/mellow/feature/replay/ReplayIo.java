package com.roxiun.mellow.feature.replay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

public class ReplayIo {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String META_FILE = "meta.json";
    private static final String PACKETS_FILE = "packets.bin";
    private static final String EVENTS_FILE = "events.bin";
    private static final String INDEX_FILE = "index.bin";
    private static final int FORMAT_VERSION_V2 = 2;
    private static final int PACKETS_MAGIC = 0x4D52504B; // MRPK
    private static final int EVENTS_MAGIC = 0x4D524556; // MREV
    private static final byte EVENT_CHAT = 1;
    private static final byte EVENT_SCOREBOARD = 2;
    private static final byte EVENT_LOCAL_PLAYER = 3;
    private static final byte[] XZ_HEADER = new byte[] {
        (byte) 0xFD,
        '7',
        'z',
        'X',
        'Z',
        0x00,
    };
    private static final int XZ_COMPRESSION_PRESET = 4;

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
        metadata.setFormatVersion(FORMAT_VERSION_V2);
        metadata.setPacketCount(packets.size());
        writeMetadata(new File(directory, META_FILE), metadata);
        writePackets(new File(directory, PACKETS_FILE), packets);
        writeEvents(new File(directory, EVENTS_FILE), chats, scoreboards, localSnapshots);
        writeIndex(new File(directory, INDEX_FILE), packets);
    }

    public ReplayLoadedData loadReplay(File directory) throws IOException {
        ReplayMetadata metadata = readMetadata(new File(directory, META_FILE));
        if (metadata.getFormatVersion() != FORMAT_VERSION_V2) {
            throw new IOException(
                "Replay format v" + metadata.getFormatVersion() +
                " is no longer supported. Please record a new replay."
            );
        }

        List<ReplayPacketFrame> packets = readPackets(new File(directory, PACKETS_FILE));
        List<ReplayChatEvent> chats = new ArrayList<>();
        List<ReplayScoreboardFrame> scoreboards = new ArrayList<>();
        List<ReplayLocalPlayerSnapshot> localSnapshots = new ArrayList<>();
        readEvents(new File(directory, EVENTS_FILE), chats, scoreboards, localSnapshots);
        sortChats(chats);
        sortScoreboards(scoreboards);
        sortLocalSnapshots(localSnapshots);
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
        Map<String, Integer> packetTypeIds = new LinkedHashMap<>();
        for (ReplayPacketFrame frame : packets) {
            if (!packetTypeIds.containsKey(frame.getClassName())) {
                packetTypeIds.put(frame.getClassName(), packetTypeIds.size());
            }
        }

        try (
            DataOutputStream out = openCompressedOutput(file)
        ) {
            out.writeInt(PACKETS_MAGIC);
            out.writeInt(FORMAT_VERSION_V2);
            writeVarInt(out, packetTypeIds.size());
            for (String className : packetTypeIds.keySet()) {
                writeString(out, className);
            }
            writeVarInt(out, packets.size());
            int previousTimestamp = 0;
            for (ReplayPacketFrame frame : packets) {
                writeVarInt(out, frame.getTimestampMs() - previousTimestamp);
                previousTimestamp = frame.getTimestampMs();
                writeVarInt(out, packetTypeIds.get(frame.getClassName()).intValue());
                writeByteArray(out, frame.getPayload());
            }
        }
    }

    private List<ReplayPacketFrame> readPackets(File file) throws IOException {
        List<ReplayPacketFrame> packets = new ArrayList<>();
        if (!file.exists()) {
            return packets;
        }

        try (
            DataInputStream in = openCompressedInput(file)
        ) {
            int magic = in.readInt();
            int version = in.readInt();
            if (magic != PACKETS_MAGIC || version != FORMAT_VERSION_V2) {
                throw new IOException("Invalid replay packet stream.");
            }

            int packetTypeCount = readVarInt(in);
            List<String> packetTypes = new ArrayList<>(packetTypeCount);
            for (int i = 0; i < packetTypeCount; i++) {
                packetTypes.add(readString(in));
            }

            int size = readVarInt(in);
            int timestamp = 0;
            for (int i = 0; i < size; i++) {
                timestamp += readVarInt(in);
                int packetTypeId = readVarInt(in);
                if (packetTypeId < 0 || packetTypeId >= packetTypes.size()) {
                    throw new IOException("Invalid replay packet type id: " + packetTypeId);
                }
                packets.add(
                    new ReplayPacketFrame(
                        timestamp,
                        packetTypes.get(packetTypeId),
                        readByteArray(in)
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
        try (
            DataOutputStream out = openCompressedOutput(file)
        ) {
            out.writeInt(EVENTS_MAGIC);
            out.writeInt(FORMAT_VERSION_V2);
            writeChatEvents(out, chats);
            writeScoreboardEvents(out, scoreboards);
            writeLocalPlayerEvents(out, localSnapshots);
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

        try (
            DataInputStream in = openCompressedInput(file)
        ) {
            int magic = in.readInt();
            int version = in.readInt();
            if (magic != EVENTS_MAGIC || version != FORMAT_VERSION_V2) {
                throw new IOException("Invalid replay event stream.");
            }
            readChatEvents(in, chats);
            readScoreboardEvents(in, scoreboards);
            readLocalPlayerEvents(in, localSnapshots);
        }
    }

    private void writeChatEvents(
        DataOutputStream out,
        List<ReplayChatEvent> chats
    ) throws IOException {
        writeVarInt(out, chats.size());
        int previousTimestamp = 0;
        for (ReplayChatEvent event : chats) {
            out.writeByte(EVENT_CHAT);
            writeVarInt(out, event.getTimestampMs() - previousTimestamp);
            previousTimestamp = event.getTimestampMs();
            writeString(out, event.getComponentJson());
            out.writeByte(event.getType() & 0xFF);
        }
    }

    private void readChatEvents(
        DataInputStream in,
        List<ReplayChatEvent> chats
    ) throws IOException {
        int count = readVarInt(in);
        int timestamp = 0;
        for (int i = 0; i < count; i++) {
            expectEventType(in, EVENT_CHAT);
            timestamp += readVarInt(in);
            chats.add(
                new ReplayChatEvent(
                    timestamp,
                    readString(in),
                    (byte) in.readUnsignedByte()
                )
            );
        }
    }

    private void writeScoreboardEvents(
        DataOutputStream out,
        List<ReplayScoreboardFrame> scoreboards
    ) throws IOException {
        writeVarInt(out, scoreboards.size());
        int previousTimestamp = 0;
        for (ReplayScoreboardFrame event : scoreboards) {
            out.writeByte(EVENT_SCOREBOARD);
            writeVarInt(out, event.getTimestampMs() - previousTimestamp);
            previousTimestamp = event.getTimestampMs();
            writeString(out, event.getTitle());
            List<String> lines = event.getLines();
            writeVarInt(out, lines.size());
            for (String line : lines) {
                writeString(out, line);
            }
        }
    }

    private void readScoreboardEvents(
        DataInputStream in,
        List<ReplayScoreboardFrame> scoreboards
    ) throws IOException {
        int count = readVarInt(in);
        int timestamp = 0;
        for (int i = 0; i < count; i++) {
            expectEventType(in, EVENT_SCOREBOARD);
            timestamp += readVarInt(in);
            String title = readString(in);
            int lineCount = readVarInt(in);
            List<String> lines = new ArrayList<>(lineCount);
            for (int lineIndex = 0; lineIndex < lineCount; lineIndex++) {
                lines.add(readString(in));
            }
            scoreboards.add(new ReplayScoreboardFrame(timestamp, title, lines));
        }
    }

    private void writeLocalPlayerEvents(
        DataOutputStream out,
        List<ReplayLocalPlayerSnapshot> localSnapshots
    ) throws IOException {
        writeVarInt(out, localSnapshots.size());
        int previousTimestamp = 0;
        for (ReplayLocalPlayerSnapshot event : localSnapshots) {
            out.writeByte(EVENT_LOCAL_PLAYER);
            writeVarInt(out, event.getTimestampMs() - previousTimestamp);
            previousTimestamp = event.getTimestampMs();
            out.writeDouble(event.getX());
            out.writeDouble(event.getY());
            out.writeDouble(event.getZ());
            out.writeFloat(event.getYaw());
            out.writeFloat(event.getPitch());
            out.writeBoolean(event.isSneaking());
            out.writeBoolean(event.isSprinting());
        }
    }

    private void readLocalPlayerEvents(
        DataInputStream in,
        List<ReplayLocalPlayerSnapshot> localSnapshots
    ) throws IOException {
        int count = readVarInt(in);
        int timestamp = 0;
        for (int i = 0; i < count; i++) {
            expectEventType(in, EVENT_LOCAL_PLAYER);
            timestamp += readVarInt(in);
            localSnapshots.add(
                new ReplayLocalPlayerSnapshot(
                    timestamp,
                    in.readDouble(),
                    in.readDouble(),
                    in.readDouble(),
                    in.readFloat(),
                    in.readFloat(),
                    in.readBoolean(),
                    in.readBoolean()
                )
            );
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

    private void expectEventType(DataInputStream in, byte expected) throws IOException {
        byte actual = in.readByte();
        if (actual != expected) {
            throw new IOException("Invalid replay event type: " + actual);
        }
    }

    private DataOutputStream openCompressedOutput(File file) throws IOException {
        return new DataOutputStream(
            new XZOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)),
                new LZMA2Options(XZ_COMPRESSION_PRESET)
            )
        );
    }

    private DataInputStream openCompressedInput(File file) throws IOException {
        BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
        input.mark(XZ_HEADER.length);
        byte[] header = new byte[XZ_HEADER.length];
        int read = input.read(header);
        input.reset();
        InputStream compressed = isXzHeader(header, read)
            ? new XZInputStream(input)
            : new GZIPInputStream(input);
        return new DataInputStream(compressed);
    }

    private boolean isXzHeader(byte[] header, int read) {
        if (read < XZ_HEADER.length) {
            return false;
        }
        for (int i = 0; i < XZ_HEADER.length; i++) {
            if (header[i] != XZ_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    private void writeByteArray(DataOutputStream out, byte[] bytes) throws IOException {
        byte[] value = bytes == null ? new byte[0] : bytes;
        writeVarInt(out, value.length);
        out.write(value);
    }

    private byte[] readByteArray(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return bytes;
    }

    private void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        int current = value;
        while ((current & ~0x7F) != 0) {
            out.writeByte((current & 0x7F) | 0x80);
            current >>>= 7;
        }
        out.writeByte(current);
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        int position = 0;
        while (position < 32) {
            int current = in.read();
            if (current == -1) {
                throw new EOFException("Unexpected end of replay stream.");
            }
            value |= (current & 0x7F) << position;
            if ((current & 0x80) == 0) {
                return value;
            }
            position += 7;
        }
        throw new IOException("Replay varint is too large.");
    }

    private void sortChats(List<ReplayChatEvent> chats) {
        Collections.sort(
            chats,
            new Comparator<ReplayChatEvent>() {
                @Override
                public int compare(ReplayChatEvent left, ReplayChatEvent right) {
                    return Integer.compare(left.getTimestampMs(), right.getTimestampMs());
                }
            }
        );
    }

    private void sortScoreboards(List<ReplayScoreboardFrame> scoreboards) {
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
    }

    private void sortLocalSnapshots(
        List<ReplayLocalPlayerSnapshot> localSnapshots
    ) {
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
