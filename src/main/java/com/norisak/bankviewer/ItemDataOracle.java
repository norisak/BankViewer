package com.norisak.bankviewer;

import javafx.scene.image.Image;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

public class ItemDataOracle {

	/**
	 * E.G items that just need to be repaired/un-augmented
	 */
	private boolean includeRecoverableUntradableWealth = true;

	/**
	 * E.G araxxi spider leg pieces
	 */
	private boolean includeSemiRecoverableUntradableWealth = false;

	/**
	 * E.G Overloads or dye value of dyed weapons
	 */
	private boolean includeUnrecoverableUntradableWealth = false;

	private HashSet<Integer> untradableSet;
	private HashMap<Integer, ItemData> itemMap;
	private HashSet<Integer> nonExistingIcons;
	private HashMap<Integer, String> itemNames;
	private HashMap<String, Integer> tradableNameLookup;
	private HashMap<String, Integer> nameLookup;
	private HashMap<Integer, ItemValueCompound[]> compounds;

	private int wealthIncludeLevel = 3;

	// Changed to true if the cache is updated
	private boolean cacheDirtyFlag = false;

	public ItemDataOracle(){
		loadCacheJson();
	}

	/**
	 * Gets the value of an item when given its item ID
	 * @param id the item ID
	 * @return the value of a single unit of that item
	 */
	public long getValueFromId(int id){
		ItemData itemData = itemMap.get(id);

		if (itemData != null)
			return itemData.getValue();

		ItemValueCompound[] c = compounds.get(id);

		if (c != null){
			ItemValueCompound comp = getBestCompound(c);

			if (comp != null){
				return comp.getValue(this);
			}
		}

		// Lookup the item name to see if we can find a value then
		return getValueFromName(getNameFromId(id));
		}

	/**
	 * Loads the desired Icon if it exists, returns null otherwise
	 * @param id item id
	 * @return an Image object
	 */
	public Image getIconIfExists(int id){
		File f = new File("icons/" + id + ".gif");
		if (!f.exists())
			return null;

		return new Image(f.toURI().toString());
	}

	public int getIdFromName(String name){
		return nameLookup.getOrDefault(name, -1);
	}

	public ItemValueCompound getBestCompound(ItemValueCompound[] itemValueCompounds){
		long maxValue = -1;

		ItemValueCompound best = null;

		for (ItemValueCompound c : itemValueCompounds){
			if (c.getWealthIncludeLevel() <= wealthIncludeLevel && c.getValue(this) > maxValue){
				maxValue = c.getValue(this);
				best = c;
			}
		}
		return best;
	}

	public long getValueFromName(String name){


		if (includeRecoverableUntradableWealth && name.startsWith("Augmented ")){
			String nonAugmentedName = Character.toUpperCase(name.charAt(10)) + name.substring(11);

			return getValueFromId(getIdFromName(nonAugmentedName));
		}

		Integer id;
		if ((id = tradableNameLookup.get(name)) != null){
			return getValueFromId(id);
		}

		// Check if this is a potion with weird dose count and interpolate value
		// TODO: Potion flasks (or just add 6 dose potion flasks to untradables.txt)
		if (name.matches(".*[(][1-6][)]")){
			System.out.println("Weird dosage detected for "+name);
			int dosePosition = name.lastIndexOf(")") - 1;
			int dosage = Integer.parseInt(name.substring(dosePosition, dosePosition + 1));

			// Check if a 3 dose variant exists
			if (dosage != 3){
				String testName = name.substring(0,dosePosition) + '3' + name.substring(dosePosition + 1);

				if (itemHasData(testName)){
					double value = getValueFromId(getIdFromName(testName));
					//System.out.println("Found data for "+testName+" when " + name + " didnt exist with value " + value);
					return (long)((double)dosage / 3.0 * (double) getValueFromId(getIdFromName(testName)));
				}
			}
			// Check if a 6 dose variant exists
			if (dosage != 6){
				String testName = name.substring(0, dosePosition) + '6' + name.substring(dosePosition + 1);

				if (itemHasData(testName)){
					double value = getValueFromId(getIdFromName(testName));
					//System.out.println("Found data for "+testName+" when " + name + " didnt exist with value " + value);
					return (long)((double)dosage / 6.0 * (double) getValueFromId(getIdFromName(testName)));
				}
			}
		}


		return 0;
	}

	/**
	 * Returns true if this item either has tradable item data or has a compound directly associated with it
	 * @param itemname
	 * @return
	 */
	public boolean itemHasData(String itemname){
		int id = nameLookup.getOrDefault(itemname, -1);
		if (id == -1)
			return false;
		return itemMap.containsKey(id) || compounds.containsKey(id);
	}

	public void updateItemPrice(int id, long price){
		ItemData data = itemMap.get(id);

		if (data == null){
			System.out.println("Tried to update item that doesnt exist in our price list");
			return;
		}

		data.updateValue(price);
		cacheDirtyFlag = true;
	}

	public String getNameFromId(int id){
		return itemNames.getOrDefault(id, "Unknown item");
	}

	public ItemData getItemDataFromId(int id){
		return itemMap.get(id);
	}


	public void loadCompounds(String filename){

		try {
			Scanner scan = new Scanner(new FileInputStream(filename));
			while (scan.hasNextLine()){
				ItemValueCompound.fromString(scan.nextLine(), this);

			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	/**
	 * Adds the itemValueCompound to the the array at the specified item id, either creating or extending an array.
	 * @param itemId
	 * @param itemValueCompound
	 */
	public void putCompound(int itemId, ItemValueCompound itemValueCompound){
		ItemValueCompound[] c = compounds.get(itemId);
		if (c == null){
			c = new ItemValueCompound[1];
		}
		else{ // Extend the existing array
			c = Arrays.copyOf(c, c.length + 1);
		}
		c[c.length - 1] = itemValueCompound;
		compounds.put(itemId, c);

	}

	private void loadNonexistingItems(){
		File f = new File("iconignore");

		nonExistingIcons = new HashSet<>();
		if (!f.exists()){
			return;
		}

		try {
			Scanner scan = new Scanner(f);
			while (scan.hasNext()){
				nonExistingIcons.add(scan.nextInt());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void saveNonexistingItems(){
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("iconignore"));
			for (Integer i : nonExistingIcons){
				writer.write(i);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void loadCacheJson(){
		nameLookup = new HashMap();
		compounds = new HashMap();

		String json;
		try {
			json = new Scanner(new File("cache.json")).useDelimiter("\\Z").next();
		} catch (FileNotFoundException e) {  //TODO: Handle
			e.printStackTrace();
			return;
		}

		JSONObject cache = new JSONObject(json);
		JSONArray untradables = cache.getJSONArray("untradables");
		untradableSet = new HashSet<Integer>();

		for (int i = 0; i < untradables.length(); ++i){
			untradableSet.add(untradables.getInt(i));
		}
		JSONObject items = cache.getJSONObject("items");

		itemMap = new HashMap<Integer, ItemData>();
		tradableNameLookup = new HashMap<>();

		for (String key : items.keySet()){
			JSONObject obj = items.getJSONObject(key);
			long lastupdate = obj.getLong("last_update");
			String name = obj.getString("name");
			long value = obj.getLong("value");
			int id = Integer.parseInt(key);
			ItemData x = new ItemData(id, value, name, lastupdate);
			tradableNameLookup.put(name, id);
			itemMap.put(id, x);
		}

		// Load item names

		File file = new File("items.txt");
		itemNames = new HashMap<>();

		Scanner scan;
		try {
			scan = new Scanner(file);
		} catch (FileNotFoundException e) {
			System.out.println("Filenotfoundexception");
			return;
		}
		while (scan.hasNextLine()){
			String[] tokens = scan.nextLine().split(" - ");
			String name = tokens.length >= 2 ? tokens[1] : "";
			itemNames.put(Integer.parseInt(tokens[0]), name);

			if (!nameLookup.containsKey(name)){
				nameLookup.put(name,Integer.parseInt(tokens[0]));
			}
		}

		loadCompounds("untradables.txt");


		Main.runOnShutdown.add(new Runnable() {
			@Override
			public void run() {
				if (cacheDirtyFlag)
					saveCacheJson();
			}
		});
	}


	private void saveCacheJson(){
		JSONObject cache = new JSONObject();

		cache.put("untradables", untradableSet);

		JSONObject items = new JSONObject();

		for (Map.Entry<Integer, ItemData> entry : itemMap.entrySet()){
			JSONObject itemObj = new JSONObject();
			itemObj.put("last_update", entry.getValue().getTimestamp());
			itemObj.put("name", entry.getValue().getName());
			itemObj.put("value", entry.getValue().getValue());

			items.put(Integer.toString(entry.getKey()), itemObj);
		}
		cache.put("items", items);

		try {
			PrintWriter writer = new PrintWriter(new FileOutputStream("cache.json"));
			writer.println(cache.toString());
			writer.close();
			System.out.println("cache.json saved");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Failed to save cache");
		}
	}
}
