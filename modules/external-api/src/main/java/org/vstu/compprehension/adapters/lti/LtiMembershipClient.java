package org.vstu.compprehension.adapters.lti;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.vstu.compprehension.data.lti.LtiCourseMemberData;
import org.vstu.compprehension.data.lti.LtiRegistrationData;
import org.vstu.compprehension.services.LtiMembershipProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Участники курса LMS по LTI Names and Role Provisioning Service.
 */
@Log4j2
@Component
public class LtiMembershipClient implements LtiMembershipProvider {

    public static final String SCOPE = "https://purl.imsglobal.org/spec/lti-nrps/scope/contextmembership.readonly";

    private static final MediaType MEMBERSHIP_CONTAINER =
            MediaType.parseMediaType("application/vnd.ims.lti-nrps.v2.membershipcontainer+json");
    private static final Pattern NEXT_PAGE_LINK = Pattern.compile("<([^>]+)>\\s*;\\s*rel=\"?next\"?");
    private static final int MAX_PAGES = 1000;
    private static final String ACTIVE_STATUS = "Active";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final LtiServiceTokenClient tokenClient;

    public LtiMembershipClient(@NotNull RestTemplate restTemplate, @NotNull LtiServiceTokenClient tokenClient) {
        this.restTemplate = restTemplate;
        this.tokenClient = tokenClient;
    }

    @Override
    public @NotNull List<LtiCourseMemberData> fetchMembers(@NotNull LtiRegistrationData tool, @NotNull String membershipsUrl) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(tokenClient.obtainAccessToken(tool.tokenEndpoint(), tool.clientId(), SCOPE));
        headers.setAccept(List.of(MEMBERSHIP_CONTAINER));
        var request = new HttpEntity<Void>(headers);

        var members = new ArrayList<LtiCourseMemberData>();
        int pages = 0;
        URI page = URI.create(membershipsUrl);
        while (page != null) {
            // Ссылка на следующую страницу, зациклившаяся у LMS, иначе крутила бы задачу бесконечно.
            if (++pages > MAX_PAGES) {
                throw new IllegalStateException("NRPS list at " + membershipsUrl + " has more than " + MAX_PAGES + " pages");
            }
            var response = restTemplate.exchange(page, HttpMethod.GET, request, String.class);
            if (response.getBody() == null) {
                throw new IllegalStateException("Empty NRPS response from " + page);
            }
            for (JsonNode member : JSON.readTree(response.getBody()).path("members")) {
                members.add(toMember(member));
            }
            page = findNextPage(response.getHeaders(), page);
        }
        log.debug("Fetched {} members from {} in {} pages", members.size(), membershipsUrl, pages);
        return members;
    }

    /** Без {@code status} участник активен: так по спецификации NRPS. */
    private static @NotNull LtiCourseMemberData toMember(@NotNull JsonNode member) {
        var roles = new ArrayList<String>();
        member.path("roles").forEach(role -> roles.add(role.asText()));
        return new LtiCourseMemberData(
                member.path("user_id").asText(),
                roles,
                ACTIVE_STATUS.equals(member.path("status").asText(ACTIVE_STATUS)));
    }

    /** Следующая страница берётся только с того же адреса LMS: к ней уходит токен доступа к списку. */
    private static @Nullable URI findNextPage(@NotNull HttpHeaders headers, @NotNull URI page) {
        for (String link : headers.getOrEmpty(HttpHeaders.LINK)) {
            var matcher = NEXT_PAGE_LINK.matcher(link);
            if (matcher.find()) {
                URI next = page.resolve(matcher.group(1));
                if (!isSameOrigin(next, page)) {
                    throw new IllegalStateException("NRPS page " + page + " links to next page on another host: " + next);
                }
                return next;
            }
        }
        return null;
    }

    private static boolean isSameOrigin(@NotNull URI first, @NotNull URI second) {
        return Objects.equals(first.getScheme(), second.getScheme())
                && Objects.equals(first.getHost(), second.getHost())
                && first.getPort() == second.getPort();
    }
}
