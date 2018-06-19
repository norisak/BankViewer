package com.norisak.bankviewer.network.geapi;

import com.norisak.bankviewer.BankController;
import com.norisak.bankviewer.IconManager;

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

public class GeApi {

	private static final String iconBaseUrl = "http://services.runescape.com/m=itemdb_rs/obj_sprite.gif?id=";
	private static final String largeIconBaseUrl = ""; // TODO: Add

	private static final ConcurrentLinkedQueue<GeApiRequest> geApiQueue = new ConcurrentLinkedQueue<>();



	public static void downloadIcon(int itemId, BankController bankController, IconManager iconManager){
		try { //TODO: Detect if we are rate limited, the icon doesnt exist etc..
			URL url = new URL(iconBaseUrl + itemId);

			InputStream is = url.openStream();

			int length;
			byte[] b = new byte[32768];  // Assumes the .gif will be no larger than this size.

			if ((length = is.read(b)) != -1){
				iconManager.addIcon(itemId, Arrays.copyOf(b, length));
			}

			is.close();

		} catch (IOException e) { // TODO: Handle
			e.printStackTrace();
		}
	}



	public static void queueRequest(GeApiRequest e) {
		geApiQueue.add(e);
	}

	public static void performRequest(){

		if (!geApiQueue.isEmpty()) {
			GeApiRequest r = geApiQueue.poll();
			System.out.println("Running GeApiRequest "+r.getDescription());
			try {
				r.run();
			} catch (ItemNotFoundException e) {
				e.printStackTrace();
			} catch (RateLimitedException e) {

			}
		}
	}
}
