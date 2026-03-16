

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

class hjUDProxyCHA {
    public static void main(String[] args) throws Exception {
        InputStream inputStream = new FileInputStream("config.properties");
        if (inputStream == null) {
            System.err.println("Configuration file not found!");
            System.exit(1);
        }
        Properties properties = new Properties();
        properties.load(inputStream);
        String remote = properties.getProperty("remote");
        String destinations = properties.getProperty("localdelivery");

        SocketAddress inSocketAddress = parseSocketAddress(remote);
        Set<SocketAddress> outSocketAddressSet = Arrays.stream(destinations.split(",")).map(s -> parseSocketAddress(s)).collect(Collectors.toSet());

        DatagramSocket inSocket = new DatagramSocket(inSocketAddress);
        DatagramSocket outSocket = new DatagramSocket();
        byte[] buffer = new byte[4 * 1024];
        String keyy = properties.getProperty("key");
        byte[] keyBytes = keyy.getBytes();
        //SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        SecretKeySpec key = new SecretKeySpec(keyBytes, "ChaCha20");

        while (true) {
            DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
            inSocket.receive(inPacket);  // if remote is unicast
            byte[] data = inPacket.getData();
            int length = inPacket.getLength();
            byte[] iv = Arrays.copyOfRange(data, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(data, 12, length);

            //GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128 = tamanho da tag em bits
            IvParameterSpec chaSpec = new IvParameterSpec(iv);
            // 3. O Cipher
            //Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            //cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
            cipher.init(Cipher.DECRYPT_MODE, key, chaSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            System.out.print(".");
            for (SocketAddress outSocketAddress : outSocketAddressSet)
            {
                outSocket.send(new DatagramPacket(decrypted, decrypted.length, outSocketAddress));
            }
        }
    }

    private static InetSocketAddress parseSocketAddress(String socketAddress)
    {
        String[] split = socketAddress.split(":");
        String host = split[0];
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }
}