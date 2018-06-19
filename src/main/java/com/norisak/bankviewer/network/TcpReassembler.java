package com.norisak.bankviewer.network;

import com.norisak.bankviewer.ByteArrayDecoder;
import org.pcap4j.packet.TcpPacket;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class TcpReassembler {

	private int expectedSeq;
	private final ArrayList<TcpPacket> outOfOrderPackets;
	private ArrayList<TcpPacket> initialPackets;
	private ByteArrayOutputStream reassembledBytes;
	private int numPackets = 0;
	private ByteArrayDecoder decoder;

	// Start with a new initial packet when this many out of order packets are recieved
	private final int outOfOrderPacketResetThreshold = 10;

	public TcpReassembler(TcpPacket initialPacket, ByteArrayDecoder decoder){
		outOfOrderPackets = new ArrayList<TcpPacket>();
		initialPackets = new ArrayList<>();
		expectedSeq = initialPacket.getHeader().getSequenceNumber();
		reassembledBytes = new ByteArrayOutputStream();
		this.decoder = decoder;

		handle(initialPacket);
	}


	/**
	 * Takes in a tcpPacket and correctly reassembles it into a byte stream
	 * @param tcpPacket
	 */
	public void handle(TcpPacket tcpPacket){

		if (!tcpPacket.getHeader().getPsh() && tcpPacket.getPayload().getRawData().length < 1000){
			// This filters out ACK packets that look like they have payload due to ethernet padding
			return;
		}
		if (((tcpPacket.getHeader().getSequenceNumber() - expectedSeq) & 0x7FFFFFFF) > 1000000){
			System.out.println("Fucked packet:");
			System.out.println(tcpPacket);
		}


		if (tcpPacket.getHeader().getSequenceNumber() - expectedSeq == 0){  // We got the expected in-order packet
			addPacket(tcpPacket);

			// Go through our outOfOrderPackets and see if any of them can be added
			outerloop:
			while(true) {
				for (int i = 0; i < outOfOrderPackets.size(); ++i) {
					TcpPacket p = outOfOrderPackets.get(i);
					if (p.getHeader().getSequenceNumber() == expectedSeq) {
						//System.out.println("Found extra packet");
						addPacket(p);
						outOfOrderPackets.remove(i);

						continue outerloop;
					}
				}
				break;  // No more packets can be added
			}

		}
		else if (tcpPacket.getHeader().getSequenceNumber() - expectedSeq > 0){
			outOfOrderPackets.add(tcpPacket);
			//System.out.println("We got out of order packet. Expected "+ expectedSeq + ", got "+tcpPacket.getHeader().getSequenceNumber() + " len "+(tcpPacket.getRawData().length-20));
			if (outOfOrderPackets.size() >= outOfOrderPacketResetThreshold){
				resetInitialPacket();
			}

		}
		else{ // A packet was retransmitted needlessly, or a packet before we starting listening was lost. Ignore packet

		}
	}

	public void resetInitialPacket(){

		System.out.println("Too many out of order packets recieved. Resetting initial packet");
		reassembledBytes.reset();
		TcpPacket earliestPacket = outOfOrderPackets.get(0);
		int relativeSeq = earliestPacket.getHeader().getSequenceNumber() - expectedSeq;

		for (TcpPacket p : outOfOrderPackets){
			if (p.getHeader().getSequenceNumber() - expectedSeq < relativeSeq){
				earliestPacket = p;
				relativeSeq = p.getHeader().getSequenceNumber() - expectedSeq;
			}
		}
		expectedSeq = earliestPacket.getHeader().getSequenceNumber();
		//System.out.println("lowest seqnum was "+expectedSeq);
		numPackets = 0;
		outOfOrderPackets.remove(earliestPacket);
		handle(earliestPacket);
	}


	public void addPacket(TcpPacket p){
		byte[] payload = p.getPayload().getRawData();

		reassembledBytes.write(payload, 0 ,payload.length);
		expectedSeq += payload.length;
		numPackets += 1;
		//System.out.println("Packet added seq = " + expectedSeq + " - " + numPackets + " packets total,  length was " + payload.length);

		if (p.getHeader().getPsh()){  // Check if there is a bank in the payload
			decoder.checkByteStream(reassembledBytes.toByteArray());
			reassembledBytes.reset();
		}
	}
}
