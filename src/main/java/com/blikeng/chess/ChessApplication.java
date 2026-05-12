package com.blikeng.chess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ChessApplication {
	static void main(String[] args) {
		SpringApplication.run(ChessApplication.class, args);
	}
}
