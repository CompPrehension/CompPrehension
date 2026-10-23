import { makeAutoObservable } from 'mobx';
import * as E from 'fp-ts/lib/Either';
import { ltiRegistrationController } from '../controllers';
import {
    LtiPlatformKeyType, LtiRegistration, LtiRegistrationInvite, LtiToolConfiguration, NewLtiRegistration,
} from '../controllers/lti/lti-registration-controller';
import { RequestError } from '../types/request-error';

/** One-time registration link for the LMS administrator. */
export class LtiInviteForm {
    description = '';
    invite: LtiRegistrationInvite | null = null;
    creating = false;
    error: RequestError | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    get inviteUrl(): string | null {
        return this.invite ? `${window.location.origin}/lti/register/${this.invite.token}` : null;
    }

    setDescription(description: string) {
        this.description = description;
    }

    async createInvite() {
        this.creating = true;
        this.error = null;
        const res = await ltiRegistrationController.createInvite(this.description);
        if (E.isRight(res)) {
            this.invite = res.right;
        } else {
            this.error = res.left;
        }
        this.creating = false;
    }
}

/** What to enter in Moodle when the tool is registered there by hand. */
export class LtiToolSetupHint {
    configuration: LtiToolConfiguration | null = null;
    error: RequestError | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    async load() {
        const res = await ltiRegistrationController.getToolConfiguration();
        if (E.isRight(res)) {
            this.configuration = res.right;
        } else {
            this.error = res.left;
        }
    }
}

export type ManualRegistrationField = Exclude<keyof NewLtiRegistration, 'platformKeyType'>;

/** A tool registered in the LMS by hand; Moodle addresses are used for the address fields left empty. */
export class LtiManualRegistrationForm {
    fields: NewLtiRegistration = {
        issuer: '',
        clientId: '',
        description: '',
        deploymentId: '',
        authorizationEndpoint: '',
        tokenEndpoint: '',
        platformKeyType: 'JWKS',
        platformKey: '',
    };
    submitting = false;
    error: RequestError | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    get moodleDefaults() {
        const issuer = this.fields.issuer.trim() || '<Platform ID>';
        return {
            authorizationEndpoint: `${issuer}/mod/lti/auth.php`,
            tokenEndpoint: `${issuer}/mod/lti/token.php`,
            jwksUri: `${issuer}/mod/lti/certs.php`,
        };
    }

    get canSubmit(): boolean {
        return !this.submitting && this.fields.issuer.trim() !== '' && this.fields.clientId.trim() !== ''
            // Moodle's keyset address is known, a public key is not.
            && (this.fields.platformKeyType === 'JWKS' || this.fields.platformKey.trim() !== '');
    }

    setField(field: ManualRegistrationField, value: string) {
        this.fields[field] = value;
    }

    setPlatformKeyType(type: LtiPlatformKeyType) {
        this.fields.platformKeyType = type;
        this.fields.platformKey = '';
    }

    /** Returns whether the tool got connected. */
    async submit(): Promise<boolean> {
        this.submitting = true;
        this.error = null;
        const defaults = this.moodleDefaults;
        const res = await ltiRegistrationController.registerManually({
            ...this.fields,
            issuer: this.fields.issuer.trim(),
            authorizationEndpoint: this.fields.authorizationEndpoint.trim() || defaults.authorizationEndpoint,
            tokenEndpoint: this.fields.tokenEndpoint.trim() || defaults.tokenEndpoint,
            platformKey: this.fields.platformKey.trim()
                || (this.fields.platformKeyType === 'JWKS' ? defaults.jwksUri : ''),
        });
        this.submitting = false;
        if (E.isLeft(res)) {
            this.error = res.left;
            return false;
        }
        return true;
    }
}

export class LtiDescriptionForm {
    description: string;
    saving = false;
    error: RequestError | null = null;

    constructor(readonly registration: LtiRegistration) {
        this.description = registration.description ?? '';
        makeAutoObservable(this);
    }

    setDescription(description: string) {
        this.description = description;
    }

    /** Returns whether the description got saved. */
    async save(): Promise<boolean> {
        this.saving = true;
        this.error = null;
        const res = await ltiRegistrationController.updateDescription(this.registration.id, this.description);
        this.saving = false;
        if (E.isLeft(res)) {
            this.error = res.left;
            return false;
        }
        return true;
    }
}

/** LMS connections page: the connected tools and the dialogs that change them. */
export class LtiRegistrationsStore {
    registrations: LtiRegistration[] = [];
    loadStatus: 'NONE' | 'LOADING' | 'LOADED' | 'FAILED' = 'NONE';
    error: RequestError | null = null;

    inviteForm: LtiInviteForm | null = null;
    manualForm: LtiManualRegistrationForm | null = null;
    setupHint: LtiToolSetupHint | null = null;
    descriptionForm: LtiDescriptionForm | null = null;
    registrationToDelete: LtiRegistration | null = null;
    deleteError: RequestError | null = null;

    constructor() {
        makeAutoObservable(this);
    }

    async loadRegistrations() {
        this.loadStatus = 'LOADING';
        this.error = null;
        const res = await ltiRegistrationController.getRegistrations();
        if (E.isLeft(res)) {
            this.error = res.left;
            this.loadStatus = 'FAILED';
            return;
        }
        this.registrations = res.right;
        this.loadStatus = 'LOADED';
    }

    openInviteForm() {
        this.inviteForm = new LtiInviteForm();
    }

    closeInviteForm() {
        this.inviteForm = null;
    }

    openManualForm() {
        this.manualForm = new LtiManualRegistrationForm();
    }

    closeManualForm() {
        this.manualForm = null;
    }

    async registerManually(form: LtiManualRegistrationForm) {
        if (await form.submit()) {
            this.manualForm = null;
            await this.loadRegistrations();
        }
    }

    openSetupHint() {
        this.setupHint = new LtiToolSetupHint();
        this.setupHint.load();
    }

    closeSetupHint() {
        this.setupHint = null;
    }

    openDescriptionForm(registration: LtiRegistration) {
        this.descriptionForm = new LtiDescriptionForm(registration);
    }

    closeDescriptionForm() {
        this.descriptionForm = null;
    }

    async saveDescription(form: LtiDescriptionForm) {
        if (await form.save()) {
            this.descriptionForm = null;
            await this.loadRegistrations();
        }
    }

    askToDelete(registration: LtiRegistration) {
        this.deleteError = null;
        this.registrationToDelete = registration;
    }

    cancelDelete() {
        this.registrationToDelete = null;
    }

    async deleteRegistration(registration: LtiRegistration) {
        this.registrationToDelete = null;
        const res = await ltiRegistrationController.deleteRegistration(registration.id);
        if (E.isLeft(res)) {
            this.deleteError = res.left;
        }
        await this.loadRegistrations();
    }
}
