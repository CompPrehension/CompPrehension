import * as io from "io-ts";
import { API_URL } from "../../appconfig";
import { RequestError } from "../../types/request-error";
import { PromiseEither, ajaxDelete, ajaxGet, ajaxPost, ajaxPut } from "../../utils/ajax";

export const TLtiRegistration = io.type({
    id: io.number,
    lmsUrl: io.string,
    issuer: io.string,
    clientId: io.string,
    description: io.union([io.string, io.null]),
    method: io.keyof({ LINK: null, MANUAL: null }),
    createdAt: io.string,
});
export type LtiRegistration = io.TypeOf<typeof TLtiRegistration>;

/** How the LMS key is given: the address of its keyset or the key itself (PEM). */
export type LtiPlatformKeyType = 'JWKS' | 'PUBLIC_KEY';

export type NewLtiRegistration = {
    issuer: string,
    clientId: string,
    description: string,
    deploymentId: string,
    authorizationEndpoint: string,
    tokenEndpoint: string,
    platformKeyType: LtiPlatformKeyType,
    platformKey: string,
};

export const TLtiToolConfiguration = io.type({
    launchUrl: io.string,
    loginUrl: io.string,
    jwksUrl: io.string,
    publicKeyPem: io.string,
});
export type LtiToolConfiguration = io.TypeOf<typeof TLtiToolConfiguration>;

export const TLtiRegistrationInvite = io.type({
    token: io.string,
    expiresAt: io.string,
});
export type LtiRegistrationInvite = io.TypeOf<typeof TLtiRegistrationInvite>;

export class LtiRegistrationController {
    /** Tools of connected LMS, however they were connected. */
    getRegistrations(): PromiseEither<RequestError, LtiRegistration[]> {
        return ajaxGet(`${API_URL}/api/lti/registrations`, io.array(TLtiRegistration));
    }

    /** What to enter in the LMS when the tool is registered there by hand. */
    getToolConfiguration(): PromiseEither<RequestError, LtiToolConfiguration> {
        return ajaxGet(`${API_URL}/api/lti/tool-configuration`, TLtiToolConfiguration);
    }

    /** A tool registered in the LMS by hand, e.g. a Moodle course tool. */
    registerManually(registration: NewLtiRegistration): PromiseEither<RequestError, LtiRegistration> {
        return ajaxPost(`${API_URL}/api/lti/registrations`, registration, TLtiRegistration);
    }

    /** One-time link for the LMS administrator ("Add LTI Advantage" in Moodle). */
    createInvite(description: string): PromiseEither<RequestError, LtiRegistrationInvite> {
        return ajaxPost(`${API_URL}/api/lti/registrations/invites`, { description }, TLtiRegistrationInvite);
    }

    /** A description tells apart several tools of the same LMS; an empty one clears it. */
    updateDescription(registrationId: number, description: string): PromiseEither<RequestError, unknown> {
        return ajaxPut(`${API_URL}/api/lti/registrations/${registrationId}/description`, { description });
    }

    /** The tool stops being connected; launches through it are refused. */
    deleteRegistration(registrationId: number): PromiseEither<RequestError, unknown> {
        return ajaxDelete(`${API_URL}/api/lti/registrations/${registrationId}`);
    }
}
