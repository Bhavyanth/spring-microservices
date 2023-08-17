package com.java.inventoryservice;

import com.java.inventoryservice.model.Inventory;
import com.java.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableEurekaClient
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner loadData(InventoryRepository inventoryRepository){
		return args -> {
			Inventory inventory1 = Inventory.builder()
					.skuCode("airpods pro")
					.quantity(3)
					.build();

			Inventory inventory2 = Inventory.builder()
					.skuCode("airpods pro gen2")
					.quantity(5)
					.build();

			inventoryRepository.save(inventory1);
			inventoryRepository.save(inventory2);
		};
	}
}
