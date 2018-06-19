package com.norisak.bankviewer;

import javafx.scene.image.Image;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class IconManager {

	private static IconManager instance;

	private static String iconFile = "icons.bin";

	private final HashMap<Integer, byte[]> icons;

	private boolean iconDirtyFlag = false;

	private IconManager(){
		instance = this;
		icons = new HashMap();
		init();
	}

	private void init(){
		loadFromFile();
		Main.runOnShutdown.add(new Runnable() {
			@Override
			public void run() {
				if (iconDirtyFlag){
					saveToFile();
				}
			}
		});
	}

	public static IconManager getInstance(){
		return instance != null ? instance : new IconManager();
	}

	/**
	 * Loads an icon if it exists, or null otherwise
	 * @param itemId
	 * @return a javafx Image object
	 */
	public Image loadIconIfExists(int itemId){
		byte[] iconBytes = icons.get(itemId);
		if (iconBytes == null)
			return null;

		InputStream is = new ByteArrayInputStream(iconBytes);
		Image i = new Image(is);
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return i;
	}

	public void saveToFile(){

		System.out.println("Attempting to save icons");
		try {
			long startTime = System.currentTimeMillis();
			DataOutputStream os = new DataOutputStream(new FileOutputStream(iconFile));

			// Write the number of icons in the file first
			int numIcons = icons.size();

			os.writeShort(numIcons);

			for (Map.Entry<Integer, byte[]> entry : icons.entrySet()){
				// For each element, write the item id as a short, and then the length of the bytearray as a short.
				// The actual bytes then follow.
				os.writeShort(entry.getKey());
				os.writeShort(entry.getValue().length);
				os.write(entry.getValue());
			}

			os.close();

			long time = System.currentTimeMillis() - startTime;

			System.out.println(numIcons + " icons saved in " + time + "ms");

			iconDirtyFlag = false;
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Saving icons failed");
		}

	}

	public void loadFromFile(){
		try {
			// See saveToFile for file format explanation

			long startTime = System.currentTimeMillis();

			DataInputStream is = new DataInputStream(new FileInputStream(iconFile));

			int numIcons = is.readShort() & 0xFFFF;

			for (int i = 0; i < numIcons; ++i){
				int itemId = is.readShort() & 0xFFFF;
				int length = is.readShort() & 0xFFFF;
				byte[] bytes = new byte[length];
				is.read(bytes);
				icons.put(itemId, bytes);
			}
			is.close();

			long time = System.currentTimeMillis() - startTime;

			System.out.println(numIcons + " icons loaded in " + time + "ms");
		} catch (FileNotFoundException e) {
			System.out.println("icons.bin not found. New one will be created");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}



	public void addIcon(int itemId, byte[] bytes) {
		icons.put(itemId, bytes);
		iconDirtyFlag = true;
	}
}
