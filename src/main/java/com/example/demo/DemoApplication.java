package com.example.demo;

import com.example.demo.Service.*;
import com.example.demo.models.entities.*;
import com.example.demo.models.entities.EnumData.CourseRole;
import com.example.demo.models.entities.EnumData.Language;
import com.example.demo.models.entities.EnumData.Role;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class DemoApplication {

	@Autowired
	public static UserService userService;
	
	@Autowired
	public static GroupService groupService;
	
	@Autowired
	public static CourseService courseService;
	
	@Autowired
	public static BackendService backendService;
	
	@Autowired
	public static ExerciseService exerciseService;
	
	@Autowired
	public static DomainService domainService;
	
	@Autowired
	public static LawService lawService;
	
	@Autowired
	public static ConceptService conceptService;
	
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
		
		/*
		* При первом запуске оставить не закомментированной эту строку. 
		* При последующих запусках держим эту строку закомментированной, т.к. 
		* в базе уже созданы все необходимые для запуска сущности
		*/
		initialDB();
	}


	
	public static void initialDB() {
		
		//Создаем первого студента
		User user = new User();
		user.setBirthdate(new Date());
		user.setEmail("dsadasda@dadsa.com");
		user.setPassword("1234");
		user.setLogin("user_1");
		user.setPreferred_language(Language.ENGLISH);
		List<Role> roles = new ArrayList<>();
		roles.add(Role.STUDENT);
		user.setRoles(roles);
		
		
		//Создаем первую группу
		Group group = new Group();
		group.setName("PrIn-366");
		List<User> users = new ArrayList<>();
		users.add(user);
		
		//Создаем второго студента
		User user_2 = new User();
		user_2.setBirthdate(new Date());
		user_2.setEmail("uyrtutyua@ytr.com");
		user_2.setPassword("123");
		user_2.setLogin("user_2");
		user_2.setPreferred_language(Language.ENGLISH);
		roles = new ArrayList<>();
		roles.add(Role.STUDENT);
		user_2.setRoles(roles);
		
		//Создаем вторую группу
		Group group_2 = new Group();
		group_2.setName("PrIn-367");
		users = new ArrayList<>();
		users.add(user_2);

		//Создаем преподавателя
		User user_3 = new User();
		user_3.setBirthdate(new Date());
		user_3.setEmail("qewqe@qeq.com");
		user_3.setPassword("12345");
		user_3.setLogin("user_3");
		user_3.setPreferred_language(Language.ENGLISH);
		roles = new ArrayList<>();
		roles.add(Role.TEACHER);
		user_3.setRoles(roles);
		
		//Создаем курс
		Course course_1 = new Course();
		course_1.setName("Курс_1");
		course_1.setDescription("Курс 1 о ...");
		//course_1.set;
				
		//Создаем привязку студентов и преподавателей к курсу
		List<UserCourseRole> userCourseRoles = new ArrayList<>();
		UserCourseRole userCourseRole = new UserCourseRole();
		userCourseRole.setCourse(course_1);
		userCourseRole.setCourseRole(CourseRole.AUTHOR);
		userCourseRole.setUser(user_3);
		userCourseRoles.add(userCourseRole);

		userCourseRole = new UserCourseRole();
		userCourseRole.setCourse(course_1);
		userCourseRole.setCourseRole(CourseRole.STUDENT);
		userCourseRole.setUser(user_2);
		userCourseRoles.add(userCourseRole);
		
		//userService
	}

}
