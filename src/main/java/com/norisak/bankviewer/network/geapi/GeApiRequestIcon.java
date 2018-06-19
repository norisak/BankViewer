package com.norisak.bankviewer.network.geapi;

import com.norisak.bankviewer.BankController;
import com.norisak.bankviewer.IconManager;
import com.norisak.bankviewer.ItemDataOracle;

public class GeApiRequestIcon implements GeApiRequest {

	private final int itemId;
	private final boolean isLargeIcon;
	private final ItemDataOracle oracle;
	private final BankController bankController;


	public GeApiRequestIcon(int itemId, boolean isLargeIcon, ItemDataOracle itemDataOracle, BankController bankController){
		this.itemId = itemId;
		this.isLargeIcon = isLargeIcon;
		this.oracle = itemDataOracle;
		this.bankController = bankController;
	}



	@Override
	public void run() {
		GeApi.downloadIcon(itemId, bankController, IconManager.getInstance());

		bankController.updateIcon(itemId);
	}

	@Override
	public String getDescription() {
		return "Fetch " + (isLargeIcon ? "large" : "small") + " icon for item id "+itemId + " (" + oracle.getNameFromId(itemId)+ ")";
	}


}
