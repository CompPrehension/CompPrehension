package com.example.demo;

import com.example.demo.models.businesslogic.Core;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApplication {

	
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
		
		
	}
	
	public void testOntology() {
		Core core = new Core();
		
		
	}

}
