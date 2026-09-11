package org.vstu.compprehension;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.Ordered;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods(order = Ordered.LOWEST_PRECEDENCE - 1)
public class CompPrehensionApplication {
	public static void main(String[] args) {
		SpringApplication.run(CompPrehensionApplication.class, args);
	}
}
