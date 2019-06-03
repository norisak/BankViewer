package com.norisak.bankviewer.network;

import com.norisak.bankviewer.BankController;
import com.norisak.bankviewer.ByteArrayDecoder;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.namednumber.EtherType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacketHandler {

	// Keep track of IP addresses to ignore so we don't waste time reverse DNSing them multiple times
	private HashSet<InetAddress> ignoreIps = new HashSet<InetAddress>();

	private ConcurrentLinkedQueue<Packet> packetQueue = new ConcurrentLinkedQueue<>();

	// When the runescape address is identified it will be stored in this field
	private InetAddress rsAddress = null;

	// A collection of the IP address of all runescape worlds
	private Set<InetAddress> worldAddresses = null;

	private volatile boolean stop = false;

	private TcpReassembler reassembler;
	private ByteArrayDecoder decoder;
	private BankController bankController;

	public PacketHandler(ByteArrayDecoder decoder, BankController bankController){
		reassembler = null;
		this.decoder = decoder;
		this.bankController = bankController;
	}



	public void generateIpWhitelist(){
		if (worldAddresses != null){
			return;
		}
		long nano = System.nanoTime();
		worldAddresses = Collections.synchronizedSet(new HashSet<InetAddress>());

		ExecutorService ex = Executors.newFixedThreadPool(10);

		for (int i = 1; i < 142; ++i){
			final int world = i;
			ex.submit(new Runnable() {
				@Override
				public void run() {
					try {
						InetAddress addr = InetAddress.getByName("world"+world+".runescape.com");
						worldAddresses.add(addr);
					} catch (UnknownHostException e) {
						System.out.println("World "+ world + " didnt exist.");
					}
				}
			});
		}
		ex.shutdown();
	}


	/**
	 * Checks a packet to see if it's from a runescape server and deals with it accordingly
	 * @param packet
	 */
	public void handle(Packet packet){
		try {
			EthernetPacket ethernetPacket = EthernetPacket.newPacket(packet.getRawData(),0, packet.getRawData().length);


			byte[] ipData = ethernetPacket.getPayload().getRawData();

			int offset = 4 * (ipData[0] & 0xF);  // Length of the IPv4 header
			int protocol = ipData[9];
			if (protocol != 6){  // Only continue if its a TCP packet
				return;
			}

			TcpPacket tcpPacket = TcpPacket.newPacket(ipData, offset, ipData.length - offset);

			if (tcpPacket.getHeader().getSrcPort().valueAsInt() != 443){  // Runescape uses port 443, so exit if its not that port
				return;
			}

			InetAddress srcAddr = InetAddress.getByAddress(Arrays.copyOfRange(ipData, 12, 16));

			if (rsAddress != null){
				if (srcAddr.equals(rsAddress)) {// It's a valid runescape packet and we want to process it
					reassembler.handle(tcpPacket);
				}

				return;
			}

			// Check if this address is in the ignore list, and exit if it is
			if (ignoreIps.contains(srcAddr)){
				return;
			}

			// Make sure we only process packets with payload

			if (tcpPacket.getPayload() == null)
				return;

			// We haven't seen this address before. Check if it's from a runescape server
			//TODO: Find a more reliable way to do this
			if (worldAddresses.contains(srcAddr)){  // It's an actual runescape server
				rsAddress = srcAddr;
				System.out.println("We found our IP! "+ srcAddr);
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						bankController.getCaptureStatusLabel().setTextFill(Color.GREEN);
						// TODO: Write which world
						bankController.getCaptureStatusLabel().setText("Connection found to " + srcAddr + "\nOpen your bank.");
					}
				});
				reassembler = new TcpReassembler(tcpPacket, decoder);
			}
			else{
				ignoreIps.add(srcAddr);  // Don't check the same IP again
				System.out.println("Adding " + srcAddr + " to ignore list");
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("fuck something went wrong handling the packet");
		}
	}

	public void addToQueue(Packet packet) {
		packetQueue.add(packet);
	}

	/**
	 * Called to stop working on the packets
	 */
	public void stopWorking(){
		stop = true;
	}


	public void workOnPackets(){
		stop = false;
		while(!stop){
			if (!packetQueue.isEmpty()){
				Packet p = packetQueue.poll();
				handle(p);
			}
		}
	}

	public void reset(){
		packetQueue.clear();
		rsAddress = null;
		reassembler = null;
	}
}
