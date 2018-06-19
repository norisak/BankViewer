package com.norisak.bankviewer;

public class InvalidBankStructureException extends Exception {

	private ArrayIndexOutOfBoundsException arrayIndexOutOfBoundsException;

	public InvalidBankStructureException(ArrayIndexOutOfBoundsException e){
		arrayIndexOutOfBoundsException = e;
	}

	public ArrayIndexOutOfBoundsException getArrayIndexOutOfBoundsException() {
		return arrayIndexOutOfBoundsException;
	}
}
