package com.roxiun.mellow.feature.replay;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Constructor;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;

public final class ReplayPacketCodec {

    private ReplayPacketCodec() {}

    public static ReplayPacketFrame encode(int timestampMs, Packet<?> packet)
        throws Exception {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        packet.writePacketData(buffer);
        byte[] payload = new byte[buffer.readableBytes()];
        buffer.readBytes(payload);
        buffer.release();
        return new ReplayPacketFrame(timestampMs, packet.getClass().getName(), payload);
    }

    @SuppressWarnings("unchecked")
    public static Packet<?> decode(ReplayPacketFrame frame) throws Exception {
        Class<?> rawClass = Class.forName(frame.getClassName());
        Constructor<?> constructor = rawClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        if (!(instance instanceof Packet)) {
            throw new IllegalStateException(frame.getClassName() + " is not a packet");
        }
        Packet<?> packet = (Packet<?>) instance;
        PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(frame.getPayload()));
        packet.readPacketData(buffer);
        buffer.release();
        return packet;
    }
}
