package com.norisak.bankviewer;

import java.util.Arrays;

public class ItemStack {

	private int itemId;
	private int itemCount;
	private long[] extraData;
	private long value = Long.MIN_VALUE;

	public ItemStack(int itemId, int itemCount, long[] extraData){
		this.itemId = itemId;
		this.itemCount = itemCount;
		this.extraData = extraData;
	}

	public int getItemId() {
		return itemId;
	}

	public int getItemCount() {
		return itemCount;
	}

	public long getValue(ItemDataOracle oracle){
		if (value == Long.MIN_VALUE){
			value = oracle.getValueFromId(itemId) * itemCount;
		}
		return value;
	}

	public long[] getExtraData() {
		return extraData;
	}

	@Override
	public String toString() {
		return "ItemStack{" +
				"itemId=" + itemId +
				", itemCount=" + itemCount +
				", extraData=" + Arrays.toString(extraData) +
				'}';
	}
}
