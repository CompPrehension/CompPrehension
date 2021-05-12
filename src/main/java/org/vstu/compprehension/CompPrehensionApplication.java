package org.vstu.compprehension;

import org.vstu.compprehension.models.entities.CourseEntity;
import org.vstu.compprehension.models.entities.EnumData.CourseRole;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.models.entities.GroupEntity;
import org.vstu.compprehension.models.entities.UserCourseRoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.vstu.compprehension.Service.*;
import org.vstu.compprehension.models.entities.UserEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootApplication
public class CompPrehensionApplication {

	@Autowired
	public UserService userService;
	
	@Autowired
	public GroupService groupService;
	
	@Autowired
	public CourseService courseService;
	
	@Autowired
	public BackendService backendService;
	
	@Autowired
	public ExerciseService exerciseService;
	
	@Autowired
	public DomainService domainService;
	
	public static void main(String[] args) {
		SpringApplication.run(CompPrehensionApplication.class, args);
		
		/*
		* При первом запуске оставить не закомментированной эту строку. 
		* При последующих запусках держим эту строку закомментированной, т.к. 
		* в базе уже созданы все необходимые для запуска сущности
		*/
		//initialDB();
	}


	
	public static void initialDB() {
		
		//Создаем первого студента
		UserEntity user = new UserEntity();
		user.setBirthdate(new Date());
		user.setEmail("dsadasda@dadsa.com");
		user.setPassword("1234");
		user.setLogin("user_1");
		user.setPreferred_language(Language.ENGLISH);
		List<Role> roles = new ArrayList<>();
		roles.add(Role.STUDENT);
		user.setRoles(roles);
		
		
		//Создаем первую группу
		GroupEntity group = new GroupEntity();
		group.setName("PrIn-366");
		List<UserEntity> users = new ArrayList<>();
		users.add(user);
		
		//Создаем второго студента
		UserEntity user_2 = new UserEntity();
		user_2.setBirthdate(new Date());
		user_2.setEmail("uyrtutyua@ytr.com");
		user_2.setPassword("123");
		user_2.setLogin("user_2");
		user_2.setPreferred_language(Language.ENGLISH);
		roles = new ArrayList<>();
		roles.add(Role.STUDENT);
		user_2.setRoles(roles);
		
		//Создаем вторую группу
		GroupEntity group_2 = new GroupEntity();
		group_2.setName("PrIn-367");
		users = new ArrayList<>();
		users.add(user_2);

		//Создаем преподавателя
		UserEntity user_3 = new UserEntity();
		user_3.setBirthdate(new Date());
		user_3.setEmail("qewqe@qeq.com");
		user_3.setPassword("12345");
		user_3.setLogin("user_3");
		user_3.setPreferred_language(Language.ENGLISH);
		roles = new ArrayList<>();
		roles.add(Role.TEACHER);
		user_3.setRoles(roles);
		
		//Создаем курс
		CourseEntity course_1 = new CourseEntity();
		course_1.setName("Курс_1");
		course_1.setDescription("Курс 1 о ...");
		//course_1.set;
				
		//Создаем привязку студентов и преподавателей к курсу
		List<UserCourseRoleEntity> userCourseRoles = new ArrayList<>();
		UserCourseRoleEntity userCourseRole = new UserCourseRoleEntity();
		userCourseRole.setCourse(course_1);
		userCourseRole.setCourseRole(CourseRole.AUTHOR);
		userCourseRole.setUser(user_3);
		userCourseRoles.add(userCourseRole);

		userCourseRole = new UserCourseRoleEntity();
		userCourseRole.setCourse(course_1);
		userCourseRole.setCourseRole(CourseRole.STUDENT);
		userCourseRole.setUser(user_2);
		userCourseRoles.add(userCourseRole);
		
		//userService
		
		System.out.println("initialDB() completed.");
	}

}
