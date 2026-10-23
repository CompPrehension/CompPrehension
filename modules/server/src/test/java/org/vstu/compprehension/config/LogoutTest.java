package org.vstu.compprehension.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.vstu.compprehension.infrastructure.AbstractIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogoutTest extends AbstractIntegrationTest {

    private static final String PAGE = "/pages/exercise-settings?courseId=1&exerciseId=2";

    @Autowired private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    /** Вышедший пользователь после повторного входа возвращается на страницу, с которой вышел. */
    @Test
    void logoutReturnsToPageAfterNextLogin() throws Exception {
        // Arrange.
        var session = new MockHttpSession();

        // Act.
        var pageAfterLogout = mockMvc.perform(get("/logout").param("returnTo", PAGE).with(oauth2Login()))
                .andReturn().getResponse().getRedirectedUrl();
        var result = mockMvc.perform(get(pageAfterLogout).accept(MediaType.TEXT_HTML).session(session));

        // Assert.
        assertEquals(PAGE, pageAfterLogout);
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
        var savedRequest = (SavedRequest) session.getAttribute("SPRING_SECURITY_SAVED_REQUEST");
        // Spring дописывает к адресу свой параметр continue.
        assertTrue(savedRequest.getRedirectUrl().startsWith("http://localhost" + PAGE));
    }

    /** Выход не уводит на чужой сайт, даже если адрес возврата подставлен в ссылку. */
    @Test
    void logoutIgnoresForeignReturnAddress() throws Exception {
        // Act.
        var result = mockMvc.perform(get("/logout").param("returnTo", "https://evil.example/pages/").with(oauth2Login()));

        // Assert.
        result.andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pages/courses"));
    }
}
