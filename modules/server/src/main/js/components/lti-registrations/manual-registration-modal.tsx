import React from 'react';
import { observer } from 'mobx-react';
import { Form, ToggleButton, ToggleButtonGroup } from 'react-bootstrap';
import { useTranslation } from 'react-i18next';
import { Modal } from '../common/modal';
import { LoadFailure } from '../common/errors';
import { LtiManualRegistrationForm, ManualRegistrationField } from '../../stores/lti-registrations-store';
import { LtiPlatformKeyType } from '../../controllers/lti/lti-registration-controller';

type Props = {
    form: LtiManualRegistrationForm;
    onSubmit: () => void;
    onClose: () => void;
};

export const ManualRegistrationModal = observer(({ form, onSubmit, onClose }: Props) => {
    const { t } = useTranslation();
    const defaults = form.moodleDefaults;

    const field = (name: ManualRegistrationField, label: string, options: { required?: boolean, placeholder?: string } = {}) => (
        <Form.Group className="mb-2">
            <Form.Label>{label}</Form.Label>
            <Form.Control value={form.fields[name]} required={options.required} placeholder={options.placeholder}
                          onChange={e => form.setField(name, e.target.value)} />
        </Form.Group>
    );

    return (
        <Modal show={true}
               size="lg"
               title={t('ltiRegistrations_manualTitle')}
               closeButton={true}
               handleClose={onClose}
               primaryBtnTitle={t('ltiRegistrations_manualBtn')}
               primaryBtnDisabled={!form.canSubmit}
               handlePrimaryBtnClicked={onSubmit}
               secondaryBtnTitle={t('ltiRegistrations_cancelBtn')}
               handleSecondaryBtnClicked={onClose}>
            {field('issuer', 'Platform ID', { required: true, placeholder: 'https://moodle.example.org' })}
            {field('clientId', 'Client ID', { required: true })}
            {field('description', t('ltiRegistrations_descriptionColumn'))}
            {field('deploymentId', 'Deployment ID')}
            <Form.Text muted className="d-block mb-2">{t('ltiRegistrations_manualDefaultsHint')}</Form.Text>
            <Form.Group className="mb-2">
                <Form.Label className="d-block">{t('ltiRegistrations_platformKeyLabel')}</Form.Label>
                <ToggleButtonGroup type="radio" name="platformKeyType" size="sm" className="mb-2"
                                   value={form.fields.platformKeyType}
                                   onChange={(type: LtiPlatformKeyType) => form.setPlatformKeyType(type)}>
                    <ToggleButton id="platform-key-jwks" value="JWKS" variant="outline-secondary">
                        {t('ltiRegistrations_platformKeyJwks')}
                    </ToggleButton>
                    <ToggleButton id="platform-key-public" value="PUBLIC_KEY" variant="outline-secondary">
                        {t('ltiRegistrations_platformKeyPublic')}
                    </ToggleButton>
                </ToggleButtonGroup>
                {form.fields.platformKeyType === 'JWKS' ? (
                    <Form.Control value={form.fields.platformKey} placeholder={defaults.jwksUri}
                                  onChange={e => form.setField('platformKey', e.target.value)} />
                ) : (
                    <Form.Control as="textarea" rows={8} className="font-monospace" required
                                  value={form.fields.platformKey} placeholder="-----BEGIN PUBLIC KEY-----"
                                  onChange={e => form.setField('platformKey', e.target.value)} />
                )}
            </Form.Group>
            {field('authorizationEndpoint', t('ltiRegistrations_authorizationLabel'), { placeholder: defaults.authorizationEndpoint })}
            {field('tokenEndpoint', t('ltiRegistrations_tokenLabel'), { placeholder: defaults.tokenEndpoint })}
            {form.error && <LoadFailure error={form.error} />}
        </Modal>
    );
});
