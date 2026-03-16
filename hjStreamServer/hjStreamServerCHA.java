
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.Properties;

class hjStreamServerCHA {

    static public void main( String []args ) throws Exception {
        if (args.length != 3)
        {
            System.out.println("Erro, usar: mySend <movie> <ip-multicast-address> <port>");
            System.out.println("        or: mySend <movie> <ip-unicast-address> <port>");
            System.exit(-1);
        }

        int size;
        int csize = 0;
        int count = 0;
        long time;
        DataInputStream g = new DataInputStream( new FileInputStream(args[0]) );
        byte[] buff = new byte[4096];

        DatagramSocket s = new DatagramSocket();
        InetSocketAddress addr = new InetSocketAddress( args[1], Integer.parseInt(args[2]));
        DatagramPacket p = new DatagramPacket(buff, buff.length, addr );
        long t0 = System.nanoTime(); // Ref. time
        long q0 = 0;

        InputStream inputStream = new FileInputStream("config.properties");
        if (inputStream == null) {
            System.err.println("Configuration file not found!");
            System.exit(1);
        }
        Properties properties = new Properties();
        properties.load(inputStream);
        String keyy = properties.getProperty("key");
        byte[] keyBytes = keyy.getBytes();

        // 1. A chave secreta
        //SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        SecretKeySpec key = new SecretKeySpec(keyBytes, "ChaCha20");



        while ( g.available() > 0 ) {

            size = g.readShort(); // size of the frame
            csize=csize+size;
            time = g.readLong();  // timestamp of the frame
            if ( count == 0 ) q0 = time; // ref. time in the stream
            count += 1;
            g.readFully(buff, 0, size );
            p.setData(buff, 0, size );
            p.setSocketAddress( addr );

            long t = System.nanoTime(); // what time is it?

            // Decision about the right time to transmit
            Thread.sleep( Math.max(0, ((time-q0)-(t-t0))/1000000));

            // send datagram (udp packet) w/ payload frame)
            // Frames sent in clear (no encryption)
            // 2. O IV aleatório (12 bytes é o tamanho recomendado para GCM)
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            //GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128 = tamanho da tag em bits
            IvParameterSpec chaSpec = new IvParameterSpec(iv);

            // 3. O Cipher
            //Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            //cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
            cipher.init(Cipher.ENCRYPT_MODE, key, chaSpec);
            byte[] encrypted = cipher.doFinal(buff, 0, size);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(iv);
            baos.write(encrypted);
            byte[] msg = baos.toByteArray();
            p = new DatagramPacket(msg, msg.length, addr);
            s.send(p);

            // Just for awareness ... (debug)

            System.out.print( ":" );
        }

        long tend = System.nanoTime(); // "The end" time
        System.out.println();
        System.out.println("DONE! all frames sent: "+ count);

        long duration=(tend-t0)/1000000000;
        System.out.println("Movie duration "+ duration + " s");
        System.out.println("Throughput "+ count/duration + " fps");
        System.out.println("Throughput "+ (8*(csize)/duration)/1000 + " Kbps");

    }
}