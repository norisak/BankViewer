package com.norisak.bankviewer;

public class IVComponent {

	private int itemId;
	private double coefficient;

	public IVComponent(int itemId, double coefficient){
		this.coefficient = coefficient;
		this.itemId = itemId;
	}

	public IVComponent(int itemId){
		this.coefficient = 1.0;
		this.itemId = itemId;
	}

	/**
	 *
	 * @param itemDataOracle
	 * @return The value of this component
	 */
	public long getValue(ItemDataOracle itemDataOracle){
		return (long)(coefficient * itemDataOracle.getValueFromId(itemId));
	}
}
