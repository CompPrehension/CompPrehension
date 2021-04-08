package org.vstu.compprehension.Service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.vstu.compprehension.models.entities.*;
import org.vstu.compprehension.models.entities.EnumData.CourseRole;
import org.vstu.compprehension.models.entities.EnumData.Language;
import org.vstu.compprehension.models.entities.EnumData.Role;
import org.vstu.compprehension.Exceptions.NotFoundEx.UserNFException;
import org.vstu.compprehension.models.repository.UserRepository;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserCourseRoleService userCourseRoleService;

    public UserEntity createOrUpdateFromAuthentication() throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new Exception("Trying to create user within Anonymous access");
        }

        val currentPrincipalName = authentication.getName();
        val roles = authentication.getAuthorities().stream()
                .map(r -> r.getAuthority().split("ROLE_"))
                .flatMap(Arrays::stream)
                .filter(r -> r.length() > 0).distinct()
                .collect(Collectors.toList());
        authentication.getDetails();
        val externalId = "local_" + currentPrincipalName;

        val entity = userRepository.findByExternalId(externalId).orElseGet(UserEntity::new);
        entity.setFirstName(currentPrincipalName);
        entity.setLogin(currentPrincipalName);
        entity.setPassword("undefined");
        entity.setPreferred_language(Language.ENGLISH);
        entity.setRoles(fromLtiRoles(roles));
        entity.setExternalId(externalId);

        if (entity.getId() == null) {
            return userRepository.save(entity);
        }
        return entity;
    }


    /**
     * Creates or updates user entity from LTI launch params
     * @param params LTI launch params
     * @return user
     */
    public UserEntity createOrUpdateFromLti(Map<String, String> params) {
        val externalId = params.get("tool_consumer_instance_guid") + "_" + params.get("user_id");
        val email = params.get("lis_person_contact_email_primary");
        val firstName = params.get("lis_person_name_given");
        val lastName = params.get("lis_person_name_family");
        val roles = Stream.of(params.get("roles").split(","))
                .map(String::trim)
                .filter(s -> s.length() > 0 && s.matches("^\\w+$"))
                //.map(s -> s.substring(s.lastIndexOf('/') + 1))
                .distinct()
                .collect(Collectors.toList());
        val locale = params.get("launch_presentation_locale").split("-")[0].toUpperCase();

        val entity = userRepository.findByExternalId(externalId).orElseGet(UserEntity::new);
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setEmail(email);
        entity.setLogin(email);
        entity.setPassword("undefined");
        entity.setPreferred_language(locale.equals("EN") ? Language.ENGLISH : Language.RUSSIAN);
        entity.setRoles(fromLtiRoles(roles));
        entity.setExternalId(externalId);

        if (entity.getId() == null) {
            return userRepository.save(entity);
        }
        return entity;
    }

    private List<Role> fromLtiRoles(List<String> roles) {
        if (roles.contains("Administrator")) {
            return Arrays.asList(Role.values().clone());
        }

        val teacherRoles = Arrays.asList("Instructor", "TeachingAssistant", "ContentDeveloper", "Mentor");
        if (CollectionUtils.containsAny(roles, teacherRoles)) {
            return Arrays.asList(Role.TEACHER, Role.STUDENT);
        }

        return Arrays.asList(Role.STUDENT);
    }

    public UserEntity getUserByEmail(String email){
        try{
            return userRepository.findUserByEmail(email).orElseThrow(()->
                    new UserNFException("User with email: " + email + "Not Found"));
        }catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }
    
    public UserEntity getUserByLogin(String login) {
        try{
            return userRepository.findUserByLogin(login).orElseThrow(()->
                    new UserNFException("User with login: " + login + "Not Found"));
        } catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }

    public long checkUserData(String login, String password) {
        
        UserEntity user;
        try { user = getUser(login); } 
        catch (UserNFException e) { return -1; }
        //Если пользователь не найден или у найденного пользователя не тот пароль
        if (user == null || !user.getPassword().equals(password)) { return -1; }

        return user.getId();
    }

    public UserEntity getUser(long userId) {
        try {  
            return userRepository.findById(userId).orElseThrow(()->
                    new UserNFException("User with id: " + userId + "Not Found"));
        } catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }

    public UserEntity getUser(String login) {
        try {
            return userRepository.findUserByLogin(login).orElseThrow(()->
                    new UserNFException("User with login: " + login + "Not Found"));
        } catch (Exception e){
            throw new UserNFException("Failed translation DB-user to Model-user", e);
        }
    }


    /**
     * Удалить пользователя с курса
     * @param userId - id пользователя, которого будем удалять с курса
     * @param courseId - id курса, с которого будем удалять пользователя
     */
    public void removeFromCourse(long userId, long courseId) {

    }


    /**
     * Добавить пользователя на курс (по умолчанию добавляется пользователь
     * с ролью - студент)
     * @param userId - id пользователя, которого будем добавлять
     * @param courseId - id курса, куда будем добавлять пользователя
     */
    public void addToCourse(long userId, long courseId) {

    }

    /**
     * Добавить пользователя на курс
     * @param userId - id пользователя, которого будем добавлять
     * @param courseId - id курса, куда будем добавлять пользователя
     * @param role - роль пользователя в рамках курса
     */
    public void addToCourse(long userId, long courseId, CourseRole role) {

        UserEntity user = getUser(userId);
        CourseEntity course = courseService.getCourse(courseId);
        UserCourseRoleEntity userCourseRole = new UserCourseRoleEntity();
        userCourseRole.setUser(user);
        userCourseRole.setCourseRole(role);
        userCourseRole.setCourse(course);

        course.getUserCourseRoles().add(userCourseRole);
        user.getUserCourseRoles().add(userCourseRole);
        
        courseService.updateCourse(course);
        userRepository.save(user);
        userCourseRoleService.saveUserCourseRole(userCourseRole);
    }


    /**
     * @param userId - id пользователя, действия которого хотим получить
     * @return - список действий пользователя
     */
    public List<UserActionEntity> getUserActions(long userId) {

        return getUser(userId).getUserActions();
    }

    public List<Action> getUserActionsToFront(long userId) {
        
        List<Action> actions = new ArrayList<>();
        UserEntity user = getUser(userId);
        for (UserActionEntity ua : getUserActions(userId)) {
            Action tmp = new Action();
            tmp.setUserName(user.getFirstName() + user.getLastName());
            tmp.setActionType(ua.getActionType().toString());
            tmp.setActionTime(ua.getTime());
            if (ua.getUserActionExercise() != null) {
                tmp.setExerciseName(ua.getUserActionExercise().getExercise().getName());
            }            
            actions.add(tmp);
        }
        
        return actions;
    }

    public void saveUser(UserEntity user) {
        try {
            if (getUser(user.getLogin()) != null) {
                throw new DataIntegrityViolationException("Пользователь с таким логином " +
                        "уже существует");
            }
        } catch (UserNFException e) {
            userRepository.save(user);
        }        
    }
    
    /**
     * Обновить данные о пользователе
     * @param user - новые данные о пользователе
     */
    public void updateUserProfile(UserEntity user) {
        
        if (userRepository.existsById(user.getId())) {
            saveUser(user);
        } else {
            throw new UserNFException("User with id: " + user.getId() + "Not Found");
        }
    }

    /**
     * Получить курсы, на которые подписан пользователь (включая те, в которых он 
     * отмечен как автор)
     * @param userId - id пользователя, курсы которого мы хотим получить
     * @return - курсы, на которые подписан пользователь
     */
    public List<CourseEntity> getUserCourses(long userId) {

        ArrayList<CourseEntity> courses = new ArrayList<>();
        UserEntity user = getUser(userId);
       
        List<UserCourseRoleEntity> userCourseRoles = user.getUserCourseRoles();

        for (UserCourseRoleEntity ucr : userCourseRoles) {
            courses.add(ucr.getCourse());
        }
        
        return courses;
    }

    /**
     * Подписать пользователя на курсы
     * @param userId - id пользователя, которого надо подписать на курсы
     * @param courses - курсы, которые закрепим за пользователем
     */
    public void setUserCourses(long userId, List<CourseEntity> courses) {

    }


    /**
     * Получить все группы, в которых числится пользователь
     * @param userId - id пользователя, группы которого мы хотим получить
     * @return - список групп, в которых числится пользователь
     */
    public List<GroupEntity> getUserGroups(long userId) {

        UserEntity user = getUser(userId);
        
        return user.getGroups();
    }


    /**
     * Получить предпочитаемый пользователем язык
     * @param userId - id пользователя, предпочитаемый язык которого хотим узнать
     * @return - предпочитаемый пользователем язык
     */
    public Language getUserLanguage(long userId) {

        return null;
    }

    /**
     * Получить список всех глобальных ролей
     * @return
     */
    public ArrayList<Role> getGeneralRoles() {

        return new ArrayList<>();
    }



    public CourseRole getCourseRole(long userId, long courseId) {

        UserEntity user = getUser(userId);
        
        CourseRole cr = null;
        List<UserCourseRoleEntity> userCourseRoles = user.getUserCourseRoles();
        for (int i = 0; i < userCourseRoles.size(); i++) {
            
            if (userCourseRoles.get(i).getCourse().getId() == courseId) {

                cr = userCourseRoles.get(i).getCourseRole();
            }
        }
        
        return cr;
    }

    public void setCourseRole(long userId, long courseId, CourseRole role) {

    }

    public Role getRole(long userId) {

        return Role.STUDENT;
    }
}
