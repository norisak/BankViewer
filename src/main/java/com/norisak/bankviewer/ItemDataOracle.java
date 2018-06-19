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
	private HashMap<Integer, ItemValueCompound[]> compounds;

	private int wealthIncludeLevel = 3;

	// Changed to true if the cache is updated
	private boolean cacheDirtyFlag = false;

	public ItemDataOracle(){
		loadCacheJson();
		compounds = new HashMap();
		createItemValueCompounds();
	}

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

		if (name.equals("Crystal hatchet")){  //TODO: Find a better way to do this
			name = "Dragon hatchet";
		}
		if (name.equals("Crystal pickaxe")){
			name = "Dragon pickaxe";
		}

		if (includeRecoverableUntradableWealth && name.startsWith("Augmented ")){
			System.out.println("Found augmented item: "+name);
			String nonAugmentedName = Character.toUpperCase(name.charAt(10)) + name.substring(11);

			System.out.println("Looking up "+ nonAugmentedName);
			return getValueFromName(nonAugmentedName);
		}

		Integer id;
		if ((id = tradableNameLookup.get(name)) != null){
			return getValueFromId(id);
		}


		return 0;
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

	public void createItemValueCompounds(){

		// Overload (3)
		IVComponent[] overloadComponents = new IVComponent[6];

		overloadComponents[0] = new IVComponent(15309, 1.0); // Extreme attack (3)
		overloadComponents[1] = new IVComponent(15313, 1.0); // Extreme strength (3)
		overloadComponents[2] = new IVComponent(15317, 1.0); // Extreme defence (3)
		overloadComponents[3] = new IVComponent(15325, 1.0); // Extreme ranging (3)
		overloadComponents[4] = new IVComponent(15321, 1.0); // Extreme magic (3)
		overloadComponents[5] = new IVComponent(269, 1.0); // Clean torstol

		putCompound(15333, new ItemValueCompound(3, overloadComponents));

		// Extreme attack (3)
		//"Extreme attack (3)"|"Super attack (3)","Clean avantoe"
	}
/*
	public void loadCompounds(String filename){

		try {
			String text =  new Scanner(new File(filename)).useDelimiter("\\Z").next();

			Iterator<> i = Arrays.asList(text.toCharArray()).iterator();
			while (true){
				str = new StringBuilder();
				// Check if its an itemName or id
				str.append((char)is.read());

				if (str.toString().equals("\"")){ // Using item name
					char c;
					while (is.rea)

				}


			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}*/

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
			itemNames.put(Integer.parseInt(tokens[0]), tokens.length >= 2 ? tokens[1] : "");
		}


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
