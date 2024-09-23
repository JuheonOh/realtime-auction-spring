package com.inhatc.auction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class AuctionApplication {
	public static void main(String[] args) {
		SpringApplication.run(AuctionApplication.class, args);
	}
}