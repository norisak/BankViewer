package com.norisak.bankviewer;

import java.util.Arrays;

public class ByteArrayDecoder {


	private byte[] bytePattern = {0x00, 0x5f, 0x02};  // The bytepattern to search for to find the bank

	private final BankController bankController;

	public ByteArrayDecoder(BankController bankController){
		this.bankController = bankController;
	}


	/**
	 * Returns a Bank object containing the bank parsed from the TCP stream.
	 * @param tcpData The raw byte data from the TCP stream.
	 * @return A Bank object.
	 */
	public Bank parseBank(byte[] tcpData){
		int pos = tcpData.length - bytePattern.length - 1;  // Start at the end and work our way backwards to get the latest bank data.
		int attempts = 0;

		main:
		while (--pos >= 0){
			for (int i = 0; i < bytePattern.length; ++i){
				if (tcpData[pos + i] != bytePattern[i])
					continue main;
			}
			try{
				++attempts;
				return parseBankFromPosition(tcpData, pos + bytePattern.length);
			} catch (InvalidBankStructureException e){
				System.out.println("Failed to parse bank "+e.getArrayIndexOutOfBoundsException());
			}
		}

		return null;
	}

	/**
	 * Attempts to parse bank data from a given position in a byte array
	 * @param tcpData The byte array containing the bank
	 * @param pos The position to start parsing from
	 * @return a Bank object
	 * @throws InvalidBankStructureException if the parse fails due to invalid data structure
	 */
	private Bank parseBankFromPosition(byte[] tcpData, int pos) throws InvalidBankStructureException{
		try {
			System.out.println("Before bank: " + Arrays.toString(Arrays.copyOfRange(tcpData,pos-16,pos)));

			System.out.println(tcpData[pos]);
			System.out.println(tcpData[pos + 1]);
			int banksize = ((tcpData[pos] & 0xFF) << 8) + (tcpData[pos + 1] & 0xFF);
			System.out.println(banksize);
			pos += 2;
			Bank bank = new Bank(banksize);

			for (int i = 0; i < banksize; ++i) {
				int id = ((tcpData[pos] & 0xFF) << 8) + (tcpData[pos + 1] & 0xFF) - 1;
				int count = tcpData[pos + 2] & 0xFF;

				// The item count is encoded as 1 byte if the number of items less than 255, otherwise 4 extra bytes are used.
				if (count == 255) {
					count = ((tcpData[pos + 3] & 0xFF) << 24) + ((tcpData[pos + 4] & 0xFF) << 16) +
							((tcpData[pos + 5] & 0xFF) << 8) + (tcpData[pos + 6] & 0xFF);
					pos += 4;
				}
				int extraDataCount = tcpData[pos + 3] & 0xFF;  // Items can contain a number of 6 byte chunks of extra data
				long[] extraData = new long[extraDataCount];
				pos += 4;
				for (int j = 0; j < extraDataCount; ++j) {
					extraData[j] = ((tcpData[pos] & 0xFFL) << 40) + ((tcpData[pos + 1] & 0xFFL) << 32) +
							((tcpData[pos + 2] & 0xFFL) << 24) + ((tcpData[pos + 3] & 0xFFL) << 16) +
							((tcpData[pos + 4] & 0xFFL) << 8) + (tcpData[pos + 5] & 0xFFL);
					pos += 6;
				}
				bank.setItemStackAtPos(new ItemStack(id, count, extraData), i);
			}
			System.out.println("After bank: " + Arrays.toString(Arrays.copyOfRange(tcpData,pos+1,pos+17)));
			return bank;
		} catch (ArrayIndexOutOfBoundsException e){
			throw new InvalidBankStructureException(e);
		}
	}

	/**
	 * Checks the byte array for a bank and sends the bank to the BankController if it exists
	 * @param tcpData
	 * @return true if a bank was found, false otherwise
	 */
	public boolean checkByteStream(byte[] tcpData){
		Bank bank = parseBank(tcpData);
		if (bank == null)
			return false;
		bankController.handleBank(bank, false);
		return true;
	}


}
