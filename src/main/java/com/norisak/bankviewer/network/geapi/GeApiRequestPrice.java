package com.norisak.bankviewer.network.geapi;

import com.norisak.bankviewer.BankController;
import com.norisak.bankviewer.ItemDataOracle;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class GeApiRequestPrice implements GeApiRequest {

	private int id;
	private ItemDataOracle itemDataOracle;
	private BankController bankController;

	public GeApiRequestPrice(int itemId, ItemDataOracle itemDataOracle, BankController bankController) {
		this.id = itemId;
		this.itemDataOracle = itemDataOracle;
		this.bankController = bankController;

		itemDataOracle.getItemDataFromId(itemId).setQueued(true);
	}

	@Override
	public void run() throws ItemNotFoundException, RateLimitedException {
		try {
			// Getting the price from the graph api will give us data that is one day old, but we get exact prices.
			URL url = new URL("http://services.runescape.com/m=itemdb_rs/api/graph/" + id + ".json");
			InputStream is = url.openStream();
			byte[] bytes = new byte[32768];
			int len = is.read(bytes);

			if (len == -1){
				throw new RateLimitedException();
			}

			String str = new String(bytes, 0, len);

			int pos = str.indexOf("},\"average");
			String newStr = str.substring(0, pos);
			int beginPos = newStr.lastIndexOf(",\"");

			String[] tokens = newStr.substring(beginPos+2).split("\":");
			System.out.println(tokens[0] +  " price is " + tokens[1]);

			int price = Integer.parseInt(tokens[1]);
			itemDataOracle.updateItemPrice(id, price);
			bankController.updatePrice(id, price);

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			throw new ItemNotFoundException(e);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public String getDescription() {
		return "price request";
	}
}
