package com.norisak.bankviewer;

public class ItemValueCompound {

	private IVComponent[] components;

	// Which level of wealth inclusion required for this wealth to be included.
	// 0 is no requirement, 1 is recoverable, 2 is semirecoverable and 3 is unrecoverable.
	private int wealthIncludeLevel;

	public ItemValueCompound(int wealthIncludeLevel, IVComponent... components){
		this.components = components;  // TODO: What about max(noxious scyte, bow, staff)?
		this.wealthIncludeLevel = wealthIncludeLevel;
	}

	public long getValue(ItemDataOracle itemDataOracle){
		long sum = 0;
		for (IVComponent component : components){
			sum += component.getValue(itemDataOracle);
		}
		return sum;
	}

	public int getWealthIncludeLevel(){
		return wealthIncludeLevel;
	}
}
