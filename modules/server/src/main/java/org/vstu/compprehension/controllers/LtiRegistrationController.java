package org.vstu.compprehension.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;
import org.vstu.compprehension.frontend.AuthFrontendService;
import org.vstu.compprehension.frontend.LtiRegistrationFrontendService;
import org.vstu.compprehension.frontend.UserFrontendService;
import org.vstu.compprehension.frontend.dto.LtiRegistrationDto;
import org.vstu.compprehension.frontend.dto.LtiRegistrationInviteDto;
import org.vstu.compprehension.service.lti.LtiDynamicRegistrationService;

import java.util.List;

@Controller
@Log4j2
@RequiredArgsConstructor
public class LtiRegistrationController {

    private final LtiDynamicRegistrationService dynamicRegistrationService;
    private final LtiRegistrationFrontendService ltiRegistrationService;
    private final UserFrontendService userService;
    private final AuthFrontendService authService;

    /**
     * Ссылка динамической регистрации: её открывает LMS ("Add LTI Advantage" в Moodle), добавив адрес своих
     * настроек и токен регистрации. Ответ — страница в окне LMS, поэтому и ошибки отдаются страницей.
     */
    @GetMapping(value = "lti/1_3/register/{inviteToken}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> register(@PathVariable String inviteToken,
                                           @RequestParam("openid_configuration") String openidConfigurationUrl,
                                           @RequestParam(value = "registration_token", required = false) String registrationToken) {
        try {
            var registration = dynamicRegistrationService.register(inviteToken, openidConfigurationUrl, registrationToken);
            return ResponseEntity.ok(registrationPage(
                    "CompPrehension подключён к " + registration.educationResourceUrl()
                            + ". Окно можно закрыть; в LMS инструмент может потребовать активации.",
                    true));
        } catch (SecurityException ex) {
            return registrationFailure(HttpStatus.FORBIDDEN, ex);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            return registrationFailure(HttpStatus.CONFLICT, ex);
        } catch (RestClientException ex) {
            return registrationFailure(HttpStatus.BAD_GATEWAY, ex);
        }
    }

    @GetMapping("api/lti/registrations")
    @ResponseBody
    public List<LtiRegistrationDto> getRegistrations() {
        authService.ensureCanRegisterLms(userService.getCurrentUserId());
        return ltiRegistrationService.getAll();
    }

    @PostMapping("api/lti/registrations/invites")
    @ResponseBody
    public LtiRegistrationInviteDto createInvite() {
        var userId = userService.getCurrentUserId();
        authService.ensureCanRegisterLms(userId);
        dynamicRegistrationService.ensureConfigured();
        return ltiRegistrationService.createInvite(userId);
    }

    @DeleteMapping("api/lti/registrations/{registrationId}")
    @ResponseBody
    public void deleteRegistration(@PathVariable long registrationId) {
        authService.ensureCanRegisterLms(userService.getCurrentUserId());
        ltiRegistrationService.deleteRegistration(registrationId);
    }

    private static ResponseEntity<String> registrationFailure(HttpStatus status, Exception ex) {
        log.warn("LTI dynamic registration failed: {}", ex.getMessage());
        return ResponseEntity.status(status).body(registrationPage("Регистрация не удалась: " + ex.getMessage(), false));
    }

    private static String registrationPage(String message, boolean closeWindow) {
        // По спецификации инструмент сообщает LMS об окончании регистрации сообщением org.imsglobal.lti.close.
        String closeScript = closeWindow
                ? "<script>(window.opener || window.parent).postMessage({subject: 'org.imsglobal.lti.close'}, '*');</script>"
                : "";
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>CompPrehension</title></head>"
                + "<body><p>" + HtmlUtils.htmlEscape(message) + "</p>" + closeScript + "</body></html>";
    }
}
