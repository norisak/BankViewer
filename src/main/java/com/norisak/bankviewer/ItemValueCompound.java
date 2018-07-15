package com.norisak.bankviewer;

import java.util.ArrayList;
import java.util.HashMap;

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

	/**
	 * Parses as single line as an ItemValueCompound object
	 * @param str
	 * @return
	 */
	public static void fromString(String str, ItemDataOracle itemDataOracle){
		//"Extreme attack (3)"|"Super attack (3)","Clean avantoe"
		int index = str.indexOf("|");
		String item = str.substring(0,index);
		System.out.println("item was "+item);
		String[] tokens = str.substring(index + 1).split(",");
		int wealthIncludeLevel = 0;
		if (tokens[0].equals("RECOVERABLE")){
			wealthIncludeLevel = 1;
		} else if (tokens[0].equals("SEMIRECOVERABLE")){
			wealthIncludeLevel = 2;
		} else if (tokens[0].equals("UNRECOVERABLE")){
			wealthIncludeLevel = 3;
		}

		ArrayList<IVComponent> ivList = new ArrayList<>();

		for (int i = wealthIncludeLevel > 0 ? 1 : 0; i < tokens.length; ++i){
			String token = tokens[i];
			double multiplier = 1.0;
			if (token.contains("*")){  // It has a multiplier
				multiplier = Double.parseDouble(token.substring(0, token.indexOf("*")));
				token = token.substring(token.indexOf("*") + 1);
			}
			int itemId = parseItemId(token, itemDataOracle);
			//System.out.println("Adding IVComponent for item "+token + ", (id is " + itemId + ") value is "+itemDataOracle.getValueFromId(itemId));
			ivList.add(new IVComponent(itemId, multiplier));
		}
		int id = parseItemId(item, itemDataOracle);

		//System.out.println("Adding new ItemValueCompound");

		IVComponent[] componentList = new IVComponent[ivList.size()];

		for(int i = 0; i < ivList.size(); ++i){
			componentList[i] = ivList.get(i);
		}

		//System.out.println("Adding compound for item "+item + ", id is "+id);
		itemDataOracle.putCompound(id, new ItemValueCompound(wealthIncludeLevel, componentList));
	}

	/**
	 * Parses an item id from a string that can either be an ID or a item name string in quotes.
	 * @param str
	 * @param itemDataOracle
	 * @return The item id
	 */
	public static int parseItemId(String str, ItemDataOracle itemDataOracle){
		try{
			return Integer.parseInt(str);
		} catch (NumberFormatException e){
			return itemDataOracle.getIdFromName(str.substring(1,str.length() - 1));
		}
	}
}
