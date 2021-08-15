package org.vstu.compprehension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.vstu.compprehension.Service.*;

@SpringBootApplication
public class CompPrehensionApplication {
	@Autowired
	public UserService userService;

	@Autowired
	public BackendService backendService;
	
	@Autowired
	public ExerciseService exerciseService;
	
	@Autowired
	public DomainService domainService;
	
	public static void main(String[] args) {
		SpringApplication.run(CompPrehensionApplication.class, args);
	}
}
