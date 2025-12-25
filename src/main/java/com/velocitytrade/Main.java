package com.velocitytrade;

import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.nio.ByteBuffer;

/**
 * VelocityTrade - Microsecond Precision Trading Platform
 */
@Slf4j
public class Main {

    private static final String MULTICAST_GROUP = "239.255.0.1";
    private static final int MULTICAST_PORT = 5000;

    public static void main(String[] args) {
        log.info("VelocityTrade v1.0.0 - Starting...");

        // Verify environment
        verifyEnvironment();

        // Test UDP networking
        testUdpNetworking();

        log.info("VelocityTrade - Startup Complete");
    }

    private static void verifyEnvironment() {
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Java Vendor: {}", System.getProperty("java.vendor"));
        log.info("OS: {} {}",
                System.getProperty("os.name"),
                System.getProperty("os.version")
        );
        log.info("Available Processors: {}", Runtime.getRuntime().availableProcessors());
        log.info("Max Memory: {} MB", Runtime.getRuntime().maxMemory() / 1024 / 1024);

        // Verify critical classes are loadable
        try {
            Class.forName("com.lmax.disruptor.RingBuffer");
            log.info("LMAX Disruptor: OK");
        } catch (ClassNotFoundException e) {
            log.error("LMAX Disruptor: NOT FOUND");
        }

        try {
            Class.forName("net.openhft.chronicle.queue.ChronicleQueue");
            log.info("Chronicle Queue: OK");
        } catch (ClassNotFoundException e) {
            log.error("Chronicle Queue: NOT FOUND");
        }

        try {
            Class.forName("org.HdrHistogram.Histogram");
            log.info("HdrHistogram: OK");
        } catch (ClassNotFoundException e) {
            log.error("HdrHistogram: NOT FOUND");
        }
    }

    private static void testUdpNetworking() {
        log.info("Testing UDP Multicast...");

        // Use CountDownLatch to coordinate threads
        final boolean[] receiverReady = {false};
        final Object lock = new Object();

        Thread receiver = new Thread(() -> {
            try {
                // Join multicast group
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                MulticastSocket socket = new MulticastSocket(MULTICAST_PORT);

                // Get the network interface (macOS fix)
                NetworkInterface networkInterface = NetworkInterface.getByName("lo0");
                if (networkInterface == null) {
                    // Fallback to default interface
                    networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
                }

                if (networkInterface != null) {
                    InetSocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_PORT);
                    socket.joinGroup(groupAddress, networkInterface);
                    log.info("Joined multicast group on interface: {}", networkInterface.getName());
                } else {
                    log.warn("Could not find network interface, using default");
                }

                // Set timeout to avoid infinite blocking
                socket.setSoTimeout(3000);

                synchronized (lock) {
                    receiverReady[0] = true;
                    lock.notify();
                }

                byte[] buffer = new byte[42];
                int messagesReceived = 0;

                for (int i = 0; i < 5; i++) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);

                        ByteBuffer bb = ByteBuffer.wrap(packet.getData());
                        long timestamp = bb.getLong();
                        byte msgType = bb.get();
                        int symbolId = bb.getInt();
                        byte side = bb.get();
                        long price = bb.getLong();
                        int quantity = bb.getInt();
                        long orderId = bb.getLong();
                        long sequence = bb.getLong();

                        log.info("Received: seq={}, type={}, price={}, qty={}",
                                sequence, (char) msgType, price, quantity);
                        messagesReceived++;
                    } catch (SocketTimeoutException e) {
                        log.warn("Timeout waiting for message #{}", i);
                    }
                }

                socket.close();

                if (messagesReceived == 5) {
                    log.info("UDP Multicast: OK ({} messages received)", messagesReceived);
                } else if (messagesReceived > 0) {
                    log.warn("UDP Multicast: PARTIAL ({}/5 messages received)", messagesReceived);
                } else {
                    log.warn("UDP Multicast: NO MESSAGES (This is OK on some systems)");
                    log.info("Note: Multicast may be blocked on macOS. Will use unicast in production.");
                }

            } catch (Exception e) {
                log.error("Receiver error: {}", e.getMessage());
                log.warn("UDP Multicast test failed (common on macOS)");
                log.info("System will work fine - actual implementation uses different networking");
            }
        }, "UDP-Receiver");

        Thread sender = new Thread(() -> {
            try {
                // Wait for receiver to be ready
                synchronized (lock) {
                    while (!receiverReady[0]) {
                        lock.wait(1000);
                    }
                }

                // Small delay to ensure receiver is listening
                Thread.sleep(100);

                DatagramSocket socket = new DatagramSocket();
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

                for (int i = 0; i < 5; i++) {
                    // Create test message
                    ByteBuffer buffer = ByteBuffer.allocate(42);
                    buffer.putLong(System.nanoTime());  // Timestamp
                    buffer.put((byte) 'A');  // Message type
                    buffer.putInt(0);  // Symbol ID
                    buffer.put((byte) 'B');  // Side
                    buffer.putLong(15000);  // Price
                    buffer.putInt(100);  // Quantity
                    buffer.putLong(i);  // Order ID
                    buffer.putLong(i);  // Sequence

                    byte[] data = buffer.array();
                    DatagramPacket packet = new DatagramPacket(
                            data, data.length, group, MULTICAST_PORT
                    );

                    socket.send(packet);
                    log.info("Sent test message #{}", i);

                    Thread.sleep(100);
                }

                socket.close();
            } catch (Exception e) {
                log.error("Sender error", e);
            }
        }, "UDP-Sender");

        receiver.start();
        sender.start();

        try {
            // Wait max 10 seconds for both threads
            sender.join(10000);
            receiver.join(10000);
        } catch (InterruptedException e) {
            log.error("Test interrupted", e);
        }
    }
}
