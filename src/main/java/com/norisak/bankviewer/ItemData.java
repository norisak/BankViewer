package com.norisak.bankviewer;

public class ItemData {

	private int itemId;
	private long value;
	private String name;
	private long timestamp;

	// Whether this item is queued for a price update


	private boolean isQueued = false;

	public static final ItemData worthlessItem = new ItemData(-1, 0, "none", 0);

	@Override
	public String toString() {
		return "ItemData{" +
				"itemId=" + itemId +
				", value=" + value +
				", name='" + name + '\'' +
				", timestamp=" + timestamp +
				'}';
	}

	public int getItemId() {
		return itemId;
	}


	public boolean isQueued() {
		return isQueued;
	}

	public void setQueued(boolean queued) {
		isQueued = queued;
	}
	public long getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public long getTimeSinceUpdate(){
		return System.currentTimeMillis() / 1000 - timestamp;
	}

	/**
	 * Updates the value of this item, and sets the timestamp to the current time
	 * @param newValue
	 */
	public void updateValue(long newValue){
		this.value = newValue;
		this.timestamp = System.currentTimeMillis() / 1000;
	}

	public ItemData(int itemId, long value, String name, long timestamp){
		this.itemId = itemId;

		this.value = value;
		this.name = name;
		this.timestamp = timestamp;
	}

}
