import * as io from "io-ts";
import { API_URL } from "../../appconfig";
import { RequestError } from "../../types/request-error";
import { PromiseEither, ajaxGet, ajaxPost } from "../../utils/ajax";

export const TDeepLinkBuildResponse = io.type({
    jwt: io.string,
    returnUrl: io.string,
});
export type DeepLinkBuildResponse = io.TypeOf<typeof TDeepLinkBuildResponse>;

export const TDeepLinkExistingResponse = io.type({
    exerciseIds: io.array(io.number),
});
export type DeepLinkExistingResponse = io.TypeOf<typeof TDeepLinkExistingResponse>;

export class DeepLinkingController {
    /** Build a signed LtiDeepLinkingResponse for the selected course exercises. */
    build(exerciseIds: number[]): PromiseEither<RequestError, DeepLinkBuildResponse> {
        return ajaxPost(`${API_URL}/api/lti/deep-link/build`, { exerciseIds }, TDeepLinkBuildResponse);
    }

    /** exercise_id's already added to the Moodle course as activities (AGS-based dedup). */
    existing(): PromiseEither<RequestError, DeepLinkExistingResponse> {
        return ajaxGet(`${API_URL}/api/lti/deep-link/existing`, TDeepLinkExistingResponse);
    }
}
