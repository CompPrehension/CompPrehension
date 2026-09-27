import * as io from "io-ts";
import { API_URL } from "../../appconfig";
import { RequestError } from "../../types/request-error";
import { PromiseEither, ajaxDelete, ajaxGet, ajaxPost } from "../../utils/ajax";

export const TLtiRegistration = io.type({
    id: io.number,
    lmsUrl: io.string,
    issuer: io.string,
    clientId: io.string,
    createdAt: io.string,
});
export type LtiRegistration = io.TypeOf<typeof TLtiRegistration>;

export const TConfiguredLtiRegistration = io.type({
    name: io.string,
    issuer: io.string,
    clientId: io.string,
    platformJwksUrl: io.union([io.string, io.null]),
});
export type ConfiguredLtiRegistration = io.TypeOf<typeof TConfiguredLtiRegistration>;

export const TLtiRegistrationInvite = io.type({
    token: io.string,
    expiresAt: io.string,
});
export type LtiRegistrationInvite = io.TypeOf<typeof TLtiRegistrationInvite>;

export class LtiRegistrationController {
    /** LMS connected by dynamic registration. */
    getRegistrations(): PromiseEither<RequestError, LtiRegistration[]> {
        return ajaxGet(`${API_URL}/api/lti/registrations`, io.array(TLtiRegistration));
    }

    /** LMS connected in the server settings (environment variables); read-only here. */
    getConfiguredRegistrations(): PromiseEither<RequestError, ConfiguredLtiRegistration[]> {
        return ajaxGet(`${API_URL}/api/lti/registrations/configured`, io.array(TConfiguredLtiRegistration));
    }

    /** One-time link for the LMS administrator ("Add LTI Advantage" in Moodle). */
    createInvite(): PromiseEither<RequestError, LtiRegistrationInvite> {
        return ajaxPost(`${API_URL}/api/lti/registrations/invites`, {}, TLtiRegistrationInvite);
    }

    /** The LMS stops being connected; it can be connected again with a new link. */
    deleteRegistration(registrationId: number): PromiseEither<RequestError, unknown> {
        return ajaxDelete(`${API_URL}/api/lti/registrations/${registrationId}`);
    }
}
