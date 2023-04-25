package com.xmxe.config;

public class IdempotencyException extends RuntimeException{

	public IdempotencyException(String message){
		super(message);
	}
}