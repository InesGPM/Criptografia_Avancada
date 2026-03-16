/* hjUDPproxy, 20/Mar/18
 *
 * This is a very simple (transparent) UDP proxy
 * The proxy can listening on a remote source (server) UDP sender
 * and transparently forward received datagram packets in the
 * delivering endpoint
 *
 * Possible Remote listening endpoints:
 *    Unicast IP address and port: configurable in the file config.properties
 *    Multicast IP address and port: configurable in the code
 *  
 * Possible local listening endpoints:
 *    Unicast IP address and port
 *    Multicast IP address and port
 *       Both configurable in the file config.properties
 */

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

class hjUDPproxy {
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

        while (true) {
          DatagramPacket inPacket = new DatagramPacket(buffer, buffer.length);
          inSocket.receive(inPacket);  // if remote is unicast
            // Extrair número do frame e dados encriptados
            byte[] data = inPacket.getData();
            int length = inPacket.getLength();
            int frameCount = data[0]; // primeiro byte = número do frame
            byte[] encrypted = Arrays.copyOfRange(data, 1, length);

            // Criar seed = chave + número do frame
            ByteArrayOutputStream seedBaos = new ByteArrayOutputStream();
            seedBaos.write(keyBytes);
            seedBaos.write(frameCount);
            byte[] seed = seedBaos.toByteArray();

            // Gerar o mesmo keystream
            SecureRandom prng = new SecureRandom(seed);
            byte[] keystream = new byte[encrypted.length];
            prng.nextBytes(keystream);

            // XOR para recuperar o frame original
            byte[] decrypted = new byte[encrypted.length];
            for (int i = 0; i < encrypted.length; i++) {
                decrypted[i] = (byte) (encrypted[i] ^ keystream[i]);
            }

            // Reencaminhar para o VLC
            for (SocketAddress outSocketAddress : outSocketAddressSet) {
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




/*

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

class hjUDPproxy {
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
*/

