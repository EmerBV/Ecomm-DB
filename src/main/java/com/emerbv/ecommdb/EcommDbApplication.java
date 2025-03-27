package com.emerbv.ecommdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling // Habilita las tareas programadas como la actualización de estado de pagos
public class EcommDbApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcommDbApplication.class, args);
	}

}
