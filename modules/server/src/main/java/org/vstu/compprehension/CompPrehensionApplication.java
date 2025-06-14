package org.vstu.compprehension;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class CompPrehensionApplication {
	public static void main(String[] args) {
		SpringApplication.run(CompPrehensionApplication.class, args);
	}
}
