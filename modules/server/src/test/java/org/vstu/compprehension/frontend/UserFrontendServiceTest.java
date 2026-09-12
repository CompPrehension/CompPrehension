package org.vstu.compprehension.frontend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.vstu.compprehension.authorization.TestUserService;
import org.vstu.compprehension.enums.Language;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;
import org.vstu.compprehension.infrastructure.TestData;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class UserFrontendServiceTest extends AbstractIntegrationTest {

    @Autowired private UserFrontendService service;

    @AfterEach
    void resetCurrentUser() {
        TestUserService.reset();
    }

    /** Id текущего пользователя. */
    @Test
    void getCurrentUserIdReturnsActingUser() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act & Assert.
        assertEquals(TestData.Users.GLOBAL_STUDENT_ID, service.getCurrentUserId());
    }

    /** Без пользователя id не получить. */
    @Test
    void getCurrentUserIdWithoutUserFails() {
        // Act & Assert.
        assertThrows(RuntimeException.class, () -> service.getCurrentUserId());
    }

    /** Язык из профиля. */
    @Test
    void getCurrentUserLanguageComesFromProfile() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act & Assert.
        assertEquals(Language.RUSSIAN, service.getCurrentUserLanguage());
        assertEquals(Optional.of(Language.RUSSIAN), service.tryGetCurrentUserLanguage());
    }

    /** Без пользователя языка нет, но и ошибки нет. */
    @Test
    void tryGetCurrentUserLanguageWithoutUserIsEmpty() {
        // Act & Assert.
        assertEquals(Optional.empty(), service.tryGetCurrentUserLanguage());
    }

    /** Смена языка видна везде. */
    @Test
    void setLanguageChangesCurrentUserLanguage() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        service.setLanguage(Language.ENGLISH);

        // Assert.
        assertEquals(Language.ENGLISH, service.getCurrentUserLanguage());
        assertEquals("EN", service.getCurrentUserInfo().getLanguage());
    }

    /** Смена языка не трогает других. */
    @Test
    void setLanguageDoesNotAffectOtherUsers() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        service.setLanguage(Language.ENGLISH);

        // Act.
        TestUserService.actAs(TestData.Users.MAIN_COURSE_STUDENT_ID);

        // Assert.
        assertEquals(Language.RUSSIAN, service.getCurrentUserLanguage());
    }

    /** Сведения о пользователе. */
    @Test
    void getCurrentUserInfoDescribesUser() {
        // Arrange.
        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);

        // Act.
        var info = service.getCurrentUserInfo();

        // Assert.
        assertEquals(TestData.Users.GLOBAL_STUDENT_ID, info.getId());
        assertEquals("global-student@test.local", info.getEmail());
        assertEquals("RU", info.getLanguage());
        assertTrue(info.getDisplayName().isEmpty());
    }

    /** Глобальный пул виден тем, у кого VIEW_EXERCISE в GLOBAL. */
    @Test
    void canViewGlobalPoolFollowsGlobalViewPermission() {
        // Act & Assert.
        TestUserService.actAs(TestData.Users.GLOBAL_EXERCISE_AUTHOR_ID);
        assertTrue(service.getCurrentUserInfo().getPermissions().canViewGlobalPool());

        TestUserService.actAs(TestData.Users.GLOBAL_ADMIN_ID);
        assertTrue(service.getCurrentUserInfo().getPermissions().canViewGlobalPool());

        TestUserService.actAs(TestData.Users.MAIN_COURSE_TEACHER_ID);
        assertFalse(service.getCurrentUserInfo().getPermissions().canViewGlobalPool());

        TestUserService.actAs(TestData.Users.GLOBAL_STUDENT_ID);
        assertFalse(service.getCurrentUserInfo().getPermissions().canViewGlobalPool());
    }
}
