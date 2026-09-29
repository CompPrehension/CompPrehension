import React from 'react';
import { observer } from 'mobx-react';
import { Button, Form, InputGroup } from 'react-bootstrap';
import { useTranslation } from 'react-i18next';
import { Modal } from '../common/modal';
import { LoadFailure } from '../common/errors';
import { LtiInviteForm } from '../../stores/lti-registrations-store';

type Props = {
    form: LtiInviteForm;
    onClose: () => void;
};

export const InviteModal = observer(({ form, onClose }: Props) => {
    const { t } = useTranslation();
    const { invite, inviteUrl } = form;

    return (
        <Modal show={true}
               size="lg"
               title={t('ltiRegistrations_connectTitle')}
               closeButton={true}
               handleClose={onClose}
               primaryBtnTitle={invite ? null : t('ltiRegistrations_createInviteBtn')}
               primaryBtnDisabled={form.creating}
               handlePrimaryBtnClicked={() => form.createInvite()}
               secondaryBtnTitle={invite ? t('ltiRegistrations_closeBtn') : t('ltiRegistrations_cancelBtn')}
               handleSecondaryBtnClicked={onClose}>
            <p className="text-muted">{t('ltiRegistrations_connectHint')}</p>
            {invite && inviteUrl ? (
                <>
                    <InputGroup>
                        <Form.Control readOnly value={inviteUrl} onFocus={e => e.target.select()} />
                        <Button variant="outline-secondary" onClick={() => navigator.clipboard.writeText(inviteUrl)}>
                            {t('ltiRegistrations_copyBtn')}
                        </Button>
                    </InputGroup>
                    <Form.Text muted>
                        {t('ltiRegistrations_inviteExpires', { date: new Date(invite.expiresAt).toLocaleString() })}
                    </Form.Text>
                </>
            ) : (
                <Form.Group>
                    <Form.Label>{t('ltiRegistrations_descriptionColumn')}</Form.Label>
                    <Form.Control value={form.description} maxLength={255} autoFocus
                                  onChange={e => form.setDescription(e.target.value)} />
                </Form.Group>
            )}
            {form.error && <LoadFailure error={form.error} />}
        </Modal>
    );
});
