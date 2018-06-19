package com.norisak.bankviewer.network.geapi;

public class ItemNotFoundException extends GeApiException{

	private Exception e;

	public ItemNotFoundException(Exception e){
		this.e = e;
	}
}
