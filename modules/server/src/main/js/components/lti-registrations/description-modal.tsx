import React from 'react';
import { observer } from 'mobx-react';
import { Form } from 'react-bootstrap';
import { useTranslation } from 'react-i18next';
import { Modal } from '../common/modal';
import { LoadFailure } from '../common/errors';
import { LtiDescriptionForm } from '../../stores/lti-registrations-store';

type Props = {
    form: LtiDescriptionForm;
    onSave: () => void;
    onClose: () => void;
};

export const DescriptionModal = observer(({ form, onSave, onClose }: Props) => {
    const { t } = useTranslation();
    const { registration } = form;

    return (
        <Modal show={true}
               title={t('ltiRegistrations_editTitle')}
               closeButton={true}
               handleClose={onClose}
               primaryBtnTitle={t('ltiRegistrations_saveBtn')}
               primaryBtnDisabled={form.saving}
               handlePrimaryBtnClicked={onSave}
               secondaryBtnTitle={t('ltiRegistrations_cancelBtn')}
               handleSecondaryBtnClicked={onClose}>
            <p className="text-muted">{registration.lmsUrl}, Client ID {registration.clientId}</p>
            <Form.Control value={form.description} maxLength={255} autoFocus
                          onChange={e => form.setDescription(e.target.value)} />
            {form.error && <LoadFailure error={form.error} />}
        </Modal>
    );
});
