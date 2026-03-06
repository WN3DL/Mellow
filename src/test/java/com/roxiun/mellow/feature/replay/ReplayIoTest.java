package com.roxiun.mellow.feature.replay;

import java.io.File;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.junit.Assert;
import org.junit.Test;

public class ReplayIoTest {

    private static final byte[] XZ_HEADER = new byte[] {
        (byte) 0xFD,
        '7',
        'z',
        'X',
        'Z',
        0x00,
    };
    private static final int PACKETS_MAGIC = 0x4D52504B;
    private static final int EVENTS_MAGIC = 0x4D524556;

    @Test
    public void saveAndLoadRoundTripsFormatV2Replay() throws Exception {
        File tempDir = Files.createTempDirectory("replay-io-roundtrip").toFile();
        try {
            ReplayIo io = new ReplayIo();
            ReplayMetadata metadata = new ReplayMetadata();
            metadata.setMap("Picnic");
            metadata.setMode("BEDWARS_TWO_FOUR");
            metadata.setServerName("mini74CS");
            metadata.setGameType("bedwars");
            metadata.setStartedAt(1_772_818_863_383L);
            metadata.setEndedAt(1_772_818_962_191L);
            metadata.setDurationMs(98_776);
            metadata.setViewerName("Roxiun");
            metadata.setFormatVersion(1);

            File directory = io.createReplayDirectory(tempDir, metadata);
            List<ReplayPacketFrame> packets = Arrays.asList(
                new ReplayPacketFrame(0, "net.minecraft.network.play.server.S21PacketChunkData", repeatedPayload(1024, 0x11)),
                new ReplayPacketFrame(50, "net.minecraft.network.play.server.S21PacketChunkData", repeatedPayload(1024, 0x11)),
                new ReplayPacketFrame(95, "net.minecraft.network.play.server.S14PacketEntity$S17PacketEntityLookMove", repeatedPayload(128, 0x22))
            );
            List<ReplayChatEvent> chats = Collections.singletonList(
                new ReplayChatEvent(75, "{\"text\":\"hi\"}", (byte) 0)
            );
            List<ReplayScoreboardFrame> scoreboards = Collections.singletonList(
                new ReplayScoreboardFrame(100, "Bed Wars", Arrays.asList("R Red", "B Blue"))
            );
            List<ReplayLocalPlayerSnapshot> localSnapshots = Arrays.asList(
                new ReplayLocalPlayerSnapshot(0, 1.5D, 64.0D, 1.5D, 90.0F, 10.0F, false, true),
                new ReplayLocalPlayerSnapshot(150, 2.5D, 64.0D, 1.5D, 95.0F, 12.0F, true, true)
            );

            io.saveReplay(
                directory,
                metadata,
                packets,
                chats,
                scoreboards,
                localSnapshots
            );

            Assert.assertTrue(new File(directory, "packets.bin").isFile());
            Assert.assertTrue(new File(directory, "events.bin").isFile());
            Assert.assertFalse(new File(directory, "events.jsonl").exists());
            assertStartsWithXzHeader(new File(directory, "packets.bin"));
            assertStartsWithXzHeader(new File(directory, "events.bin"));

            ReplayLoadedData loaded = io.loadReplay(directory);
            Assert.assertEquals(2, loaded.getMetadata().getFormatVersion());
            Assert.assertEquals("Picnic", loaded.getMetadata().getMap());
            Assert.assertEquals("BEDWARS_TWO_FOUR", loaded.getMetadata().getMode());
            Assert.assertEquals(3, loaded.getPackets().size());
            assertPacketEquals(packets.get(0), loaded.getPackets().get(0));
            assertPacketEquals(packets.get(1), loaded.getPackets().get(1));
            assertPacketEquals(packets.get(2), loaded.getPackets().get(2));
            Assert.assertEquals(1, loaded.getChats().size());
            Assert.assertEquals("{\"text\":\"hi\"}", loaded.getChats().get(0).getComponentJson());
            Assert.assertEquals(100, loaded.getScoreboards().get(0).getTimestampMs());
            Assert.assertEquals(Arrays.asList("R Red", "B Blue"), loaded.getScoreboards().get(0).getLines());
            Assert.assertEquals(2, loaded.getLocalSnapshots().size());
            Assert.assertEquals(95.0F, loaded.getLocalSnapshots().get(1).getYaw(), 0.0F);
            Assert.assertTrue(loaded.getLocalSnapshots().get(1).isSneaking());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void loadReplayRejectsLegacyFormat() throws Exception {
        File tempDir = Files.createTempDirectory("replay-io-legacy").toFile();
        try {
            File directory = new File(tempDir, "legacy");
            Assert.assertTrue(directory.mkdirs());
            Files.write(
                new File(directory, "meta.json").toPath(),
                (
                    "{\n" +
                    "  \"replayId\": \"legacy\",\n" +
                    "  \"formatVersion\": 1\n" +
                    "}\n"
                ).getBytes(StandardCharsets.UTF_8)
            );

            try {
                new ReplayIo().loadReplay(directory);
                Assert.fail("Expected legacy replay load to fail");
            } catch (IOException e) {
                Assert.assertTrue(e.getMessage().contains("no longer supported"));
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Test
    public void loadReplaySupportsGzipV2Streams() throws Exception {
        File tempDir = Files.createTempDirectory("replay-io-gzip-v2").toFile();
        try {
            File directory = new File(tempDir, "gzip-v2");
            Assert.assertTrue(directory.mkdirs());
            Files.write(
                new File(directory, "meta.json").toPath(),
                (
                    "{\n" +
                    "  \"replayId\": \"gzip-v2\",\n" +
                    "  \"map\": \"Nebuc\",\n" +
                    "  \"mode\": \"BEDWARS_EIGHT_TWO\",\n" +
                    "  \"serverName\": \"mini1007H\",\n" +
                    "  \"gameType\": \"bedwars\",\n" +
                    "  \"startedAt\": 1,\n" +
                    "  \"endedAt\": 2,\n" +
                    "  \"durationMs\": 50,\n" +
                    "  \"packetCount\": 1,\n" +
                    "  \"viewerName\": \"Roxiun\",\n" +
                    "  \"formatVersion\": 2\n" +
                    "}\n"
                ).getBytes(StandardCharsets.UTF_8)
            );

            List<ReplayPacketFrame> packets = Collections.singletonList(
                new ReplayPacketFrame(50, "net.minecraft.network.play.server.S14PacketEntity$S17PacketEntityLookMove", repeatedPayload(16, 0x33))
            );
            writeGzipPackets(new File(directory, "packets.bin"), packets);
            writeGzipEvents(
                new File(directory, "events.bin"),
                Collections.singletonList(new ReplayChatEvent(25, "{\"text\":\"gzip\"}", (byte) 0)),
                Collections.singletonList(
                    new ReplayScoreboardFrame(40, "Board", Collections.singletonList("Line"))
                ),
                Collections.singletonList(
                    new ReplayLocalPlayerSnapshot(50, 0.0D, 64.0D, 0.0D, 90.0F, 0.0F, false, false)
                )
            );

            ReplayLoadedData loaded = new ReplayIo().loadReplay(directory);
            Assert.assertEquals(1, loaded.getPackets().size());
            Assert.assertEquals("net.minecraft.network.play.server.S14PacketEntity$S17PacketEntityLookMove", loaded.getPackets().get(0).getClassName());
            Assert.assertEquals(1, loaded.getChats().size());
            Assert.assertEquals(1, loaded.getScoreboards().size());
            Assert.assertEquals(1, loaded.getLocalSnapshots().size());
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private static void assertPacketEquals(ReplayPacketFrame expected, ReplayPacketFrame actual) {
        Assert.assertEquals(expected.getTimestampMs(), actual.getTimestampMs());
        Assert.assertEquals(expected.getClassName(), actual.getClassName());
        Assert.assertArrayEquals(expected.getPayload(), actual.getPayload());
    }

    private static byte[] repeatedPayload(int length, int value) {
        byte[] payload = new byte[length];
        Arrays.fill(payload, (byte) value);
        return payload;
    }

    private static void writeGzipPackets(
        File file,
        List<ReplayPacketFrame> packets
    ) throws IOException {
        Map<String, Integer> packetTypeIds = new LinkedHashMap<>();
        for (ReplayPacketFrame frame : packets) {
            if (!packetTypeIds.containsKey(frame.getClassName())) {
                packetTypeIds.put(frame.getClassName(), packetTypeIds.size());
            }
        }
        try (
            DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(new FileOutputStream(file))
            )
        ) {
            out.writeInt(PACKETS_MAGIC);
            out.writeInt(2);
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

    private static void writeGzipEvents(
        File file,
        List<ReplayChatEvent> chats,
        List<ReplayScoreboardFrame> scoreboards,
        List<ReplayLocalPlayerSnapshot> localSnapshots
    ) throws IOException {
        try (
            DataOutputStream out = new DataOutputStream(
                new GZIPOutputStream(new FileOutputStream(file))
            )
        ) {
            out.writeInt(EVENTS_MAGIC);
            out.writeInt(2);

            writeVarInt(out, chats.size());
            int previousChatTimestamp = 0;
            for (ReplayChatEvent event : chats) {
                out.writeByte(1);
                writeVarInt(out, event.getTimestampMs() - previousChatTimestamp);
                previousChatTimestamp = event.getTimestampMs();
                writeString(out, event.getComponentJson());
                out.writeByte(event.getType() & 0xFF);
            }

            writeVarInt(out, scoreboards.size());
            int previousScoreboardTimestamp = 0;
            for (ReplayScoreboardFrame frame : scoreboards) {
                out.writeByte(2);
                writeVarInt(out, frame.getTimestampMs() - previousScoreboardTimestamp);
                previousScoreboardTimestamp = frame.getTimestampMs();
                writeString(out, frame.getTitle());
                writeVarInt(out, frame.getLines().size());
                for (String line : frame.getLines()) {
                    writeString(out, line);
                }
            }

            writeVarInt(out, localSnapshots.size());
            int previousLocalTimestamp = 0;
            for (ReplayLocalPlayerSnapshot snapshot : localSnapshots) {
                out.writeByte(3);
                writeVarInt(out, snapshot.getTimestampMs() - previousLocalTimestamp);
                previousLocalTimestamp = snapshot.getTimestampMs();
                out.writeDouble(snapshot.getX());
                out.writeDouble(snapshot.getY());
                out.writeDouble(snapshot.getZ());
                out.writeFloat(snapshot.getYaw());
                out.writeFloat(snapshot.getPitch());
                out.writeBoolean(snapshot.isSneaking());
                out.writeBoolean(snapshot.isSprinting());
            }
        }
    }

    private static void writeByteArray(DataOutputStream out, byte[] payload) throws IOException {
        writeVarInt(out, payload.length);
        out.write(payload);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static void writeVarInt(OutputStream out, int value) throws IOException {
        int current = value;
        while ((current & ~0x7F) != 0) {
            out.write((current & 0x7F) | 0x80);
            current >>>= 7;
        }
        out.write(current);
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private static void assertStartsWithXzHeader(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        Assert.assertTrue(bytes.length >= XZ_HEADER.length);
        for (int i = 0; i < XZ_HEADER.length; i++) {
            Assert.assertEquals(XZ_HEADER[i], bytes[i]);
        }
    }
}
