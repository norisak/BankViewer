package com.norisak.bankviewer.network;

import com.norisak.bankviewer.Main;
import org.pcap4j.core.*;
import org.pcap4j.packet.Packet;
import org.pcap4j.util.NifSelector;

import java.util.List;
import java.util.Stack;
import java.util.concurrent.*;

public class PacketCapturer {

	private boolean isCapturing = false;
	private PacketHandler packetHandler;

	//TODO: Don't use a stack
	private Stack<PcapHandle> handles = new Stack<>();

	/**
	 * ThreadPool for capturing from network interfaces
	 */
	private ExecutorService executorService;

	public PacketCapturer(PacketHandler packetHandler) throws PcapNativeException{
		this.packetHandler = packetHandler;
		this.executorService = Executors.newFixedThreadPool(Pcaps.findAllDevs().size());

		Main.runOnShutdown.add(new Runnable() {
			@Override
			public void run() {
				executorService.shutdownNow();
			}
		});
	}


	public void start() throws PcapNativeException {

		List<PcapNetworkInterface> interfaces = Pcaps.findAllDevs();
		System.out.println("There are " + interfaces.size() + " network interfaces.");


		// Create a thread listening to each interface.
		// TODO: Don't listen to useless network interfaces to save system resources (i.e. create fewer threads)

		packetHandler.reset();

		PacketListener listener
				= new PacketListener() {
			@Override
			public void gotPacket(Packet packet) {
				if (packet != null) {
					packetHandler.addToQueue(packet);
				}
			}
		};

		for (int i = 0; i < interfaces.size(); ++i){
			PcapHandle.Builder phb =
					new PcapHandle.Builder(interfaces.get(i).getName())
							.snaplen(65536)
							.promiscuousMode(PcapNetworkInterface.PromiscuousMode.PROMISCUOUS)
							.timeoutMillis(100)
							.bufferSize(1024*1024);

			PcapHandle newHandle = null;
			try {
				newHandle = phb.build();
			} catch (PcapNativeException e) {
				e.printStackTrace();
				System.out.println("You done fucked up");  // TODO: Remove unprofessional print messages
			}

			final PcapHandle handle = newHandle;

			handles.push(handle);

			Future f = executorService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						handle.loop(-1, listener);
					} catch (PcapNativeException e) {
						e.printStackTrace();
					} catch (InterruptedException e) {
					} catch (NotOpenException e) {
						e.printStackTrace();
					}
				}
			});

			isCapturing = true;


			Main.runOnShutdown.add(new Runnable() {
				@Override
				public void run() {
					stop();
				}
			});
		}
	}

	public boolean isCapturing(){
		return isCapturing;
	}

	public void stop(){
		if (executorService == null)
			return;

		for (PcapHandle h : handles){
			try {
				h.breakLoop();
			} catch (NotOpenException e) {
				e.printStackTrace();
				System.out.println("notopenexception");
			}
		}
		handles.clear();
		isCapturing = false;
	}

}
