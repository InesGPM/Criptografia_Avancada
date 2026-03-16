/*
* hjStreamServer.java 
* Streaming server: streams video frames in UDP packets
* for clients to play in real time the transmitted movies
*/

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.Properties;

class hjStreamServer {

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

			ByteArrayOutputStream seedBaos = new ByteArrayOutputStream(); // cria um buffer de bytes para construir a seed
			seedBaos.write(keyBytes);// escreve a chave secreta no buffer
			seedBaos.write(count); // escreve o número do frame no buffer (garante seed única por frame)
			byte[] seed = seedBaos.toByteArray(); // converte o buffer para array de bytes — esta é a seed final


			// Gerar keystream
			SecureRandom prng = new SecureRandom(seed);  // cria gerador pseudoaleatório determinístico com a seed (mesma seed = mesmo keystream)
			byte[] keystream = new byte[size];
			prng.nextBytes(keystream);  // preenche o keystream com bytes pseudoaleatórios derivados da seed

			// XOR frame com keystream
			byte[] encrypted = new byte[size];
			for (int i = 0; i < size; i++) {
				encrypted[i] = (byte) (buff[i] ^ keystream[i]);
			}

			// Enviar [count | encrypted]
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(count);
			baos.write(encrypted);
			byte[] msg = baos.toByteArray();
			p = new DatagramPacket(msg, msg.length, addr);
			s.send(p);
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
