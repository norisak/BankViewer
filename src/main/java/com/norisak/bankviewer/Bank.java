package com.norisak.bankviewer;

import com.norisak.bankviewer.network.geapi.GeApi;
import com.norisak.bankviewer.network.geapi.GeApiRequestPrice;
import javafx.application.Platform;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

public class Bank {

	private int size;
	private ItemStack[] itemStacks;
	private ItemStack[] unsortedItemStacks;
	private long timestamp;

	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

	public Bank(int size){
		this.size = size;
		this.itemStacks = new ItemStack[size];
		this.timestamp = System.currentTimeMillis() / 1000;
	}

	public void setItemStackAtPos(ItemStack itemStack, int pos){
		itemStacks[pos] = itemStack;
	}

	public void print(){
		for (ItemStack itemStack : itemStacks){
			System.out.println(itemStack);
		}

	}

	public void sort(ItemDataOracle oracle, Comparator<ItemStack> comparator, boolean reverse){
		if (unsortedItemStacks == null){
			unsortedItemStacks = itemStacks.clone();
		}

		if (comparator == null){

			if (reverse){
				for (int i = 0; i < itemStacks.length; ++i){
					itemStacks[i] = unsortedItemStacks[itemStacks.length-i-1];
				}
			}
			else{
				itemStacks = unsortedItemStacks.clone();
			}

			return;
		}
		else{
			Arrays.sort(itemStacks, reverse ? comparator.reversed() : comparator);
		}


	}

	@Override
	public String toString() {
		return "Bank{" +
				"size=" + size +
				'}';
	}

	public ItemStack[] getUnsortedItemStacks(){
		return unsortedItemStacks == null ? itemStacks : unsortedItemStacks;
	}

	/**
	 * Subtracts the contents of otherBankObj from this bank and returns a new bank containing the difference
	 * @param otherBankObj
	 * @return
	 */
	public Bank getDelta(Bank otherBankObj){
		long t = System.currentTimeMillis();
		LinkedList<ItemStack> thisBank = new LinkedList<>(Arrays.asList(getUnsortedItemStacks()));
		LinkedList<ItemStack> otherBank = new LinkedList<>(Arrays.asList(otherBankObj.getUnsortedItemStacks()));

		ArrayList<ItemStack> resultBank = new ArrayList<>();

		outerloop:
		for (ItemStack thisItemStack : thisBank){
			// TODO: Care about extradata?

			Iterator<ItemStack> otherBankIterator = otherBank.iterator();
			ItemStack otherItemStack;

			while(otherBankIterator.hasNext()){
				otherItemStack = otherBankIterator.next();
				if (thisItemStack.getItemId() == otherItemStack.getItemId()){
					int countDelta = thisItemStack.getItemCount() - otherItemStack.getItemCount();
					otherBankIterator.remove();

					if (countDelta != 0){
						resultBank.add(new ItemStack(thisItemStack.getItemId(), countDelta, thisItemStack.getExtraData()));
					}
					continue outerloop;
				}
			}
			// At this point there was no matching items
			resultBank.add(thisItemStack);

		}
		// Any remaining items in otherBank at this point needs to be added as lost
		for (ItemStack itemStack : otherBank){
			resultBank.add(new ItemStack(itemStack.getItemId(), -itemStack.getItemCount(), itemStack.getExtraData()));
		}

		int size = resultBank.size();

		System.out.println("Resultbank has " + size + " entries");

		Bank bank = new Bank(size);

		for (int i = 0; i < size; ++i){
			bank.setItemStackAtPos(resultBank.get(i), i);
		}


		long time = System.currentTimeMillis() - t;

		System.out.println("Bank compare took " + time + "ms");

		return bank;
	}


	/**
	 * Saves the bank to a file in the banks directory. Filename is automatically generated based on the timestamp.
	 * @return true if successful
	 */
	public boolean save(){
		/*
		.rsbank file format:
		First line contains a number indicating the version of the .rsbank format. Currently using value 1.
		Second line contains a unix timestamp of when the bank was captured.
		Third line contains a number indicating the number of itemstacks in the bank.
		The remaining lines contain one item stack per line, where each item stack consists of an itemId, count, and
		any number of extra data values, all separated by spaces
		 */

		// Check if the banks directory exists, and create it if it doesn't
		File dir = new File("banks");
		if (!dir.exists()){
			try {
				Files.createDirectory(dir.toPath());
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Failed to create banks directory");
				return false;
			}
		}


		String filename = "banks/bank_" + simpleDateFormat.format(new Date(timestamp * 1000)) + ".rsbank";

		try {
			PrintWriter pw = new PrintWriter(new FileOutputStream(filename));

			pw.println(1);
			pw.println(timestamp);
			pw.println(size);

			for (ItemStack itemStack : getUnsortedItemStacks()){
				StringBuilder line = new StringBuilder();
				line.append(itemStack.getItemId());
				line.append(' ');
				line.append(itemStack.getItemCount());
				for (long l : itemStack.getExtraData()){
					line.append(' ');
					line.append(l);
				}
				pw.println(line.toString());
			}

			pw.close();
			System.out.println("Bank was successfully saved to file "+filename);
			return true;

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * Attempts to parse a bank from a saved .rsbank file
	 * @param file the file to load
	 * @return A Bank object if successful, null otherwise
	 */
	public static Bank load(File file){
		// See save method for file format explanation
		try {
			Scanner scan = new Scanner(new FileInputStream(file));

			int fileFormatVersion = scan.nextInt();
			if (fileFormatVersion != 1){
				System.out.println("Invalid file format version "+fileFormatVersion);
				return null;
			}

			long timestamp = scan.nextLong();
			int size = scan.nextInt();
			scan.nextLine();
			Bank bank = new Bank(size);
			bank.timestamp = timestamp;


			for (int i = 0; i < size; ++i){
				String[] tokens = scan.nextLine().split(" ");
				int itemId = Integer.parseInt(tokens[0]);
				int count = Integer.parseInt(tokens[1]);
				long[] extraData = new long[tokens.length - 2];

				for (int j = 2; j < tokens.length; ++j){
					extraData[j-2] = Long.parseLong(tokens[j]);
				}

				bank.setItemStackAtPos(new ItemStack(itemId, count, extraData), i);
			}

			System.out.println("Bank " + file + " successfully parsed.");
			return bank;

		} catch (FileNotFoundException e) {
			System.out.println("Bankfile not found");
			return null;
		}
	}

	public long getValue(ItemDataOracle itemDataOracle){

		long value = 0;
		for (ItemStack itemStack : itemStacks){
			int count = itemStack.getItemCount();
			int id = itemStack.getItemId();
			long itemValue = itemDataOracle.getValueFromId(id);
			if (itemValue == 0)
				continue;
			value += count * itemValue;
		}
		return value;
	}

	public void queuePriceUpdates(ItemDataOracle oracle, BankController bankController){

		// TODO: Make this work on untradables

		double value = getValue(oracle);
		for (ItemStack itemStack : itemStacks){
			ItemData itemData = oracle.getItemDataFromId(itemStack.getItemId());

			if (itemData == null || itemData.isQueued()){
				continue;
			}
			double bankShare = ((double)itemStack.getValue(oracle)) / value;

			if (bankShare > 0.05 && itemData.getTimeSinceUpdate() > 86400){
				GeApi.queueRequest(new GeApiRequestPrice(itemStack.getItemId(), oracle, bankController));
			}
			else if (bankShare > 0.01 && itemData.getTimeSinceUpdate() > 86400 * 3){
				GeApi.queueRequest(new GeApiRequestPrice(itemStack.getItemId(), oracle, bankController));
			}
			else if (bankShare > 0.001 && itemData.getTimeSinceUpdate() > 86400 * 7){
				GeApi.queueRequest(new GeApiRequestPrice(itemStack.getItemId(), oracle, bankController));
			}
			else if (itemData.getTimeSinceUpdate() > 86400 * 30){
				GeApi.queueRequest(new GeApiRequestPrice(itemStack.getItemId(), oracle, bankController));
			}

		}
	}


	public void fillVbox(BankController bankController, boolean isCompare){
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				long wealth = 0;
				for (ItemStack itemStack : itemStacks){
					wealth += bankController.addItemStackToVBox(itemStack);

				}

				bankController.setBankValue(wealth);

				if (!isCompare){
					bankController.getTotalWealthLabel().setText("Total wealth:   " + String.format("%,d", wealth) + " gp");
				}
				else{
					bankController.getTotalWealthLabel().setText("Wealth difference:   " + String.format("%,d", wealth) + " gp");
				}
			}
		});
	}
}
