package com.norisak.bankviewer.network.geapi;

public interface GeApiRequest {

	public void run() throws ItemNotFoundException, RateLimitedException;
	public String getDescription();
}
