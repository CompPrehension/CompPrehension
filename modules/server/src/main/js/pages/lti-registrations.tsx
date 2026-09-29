import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react';
import { Button, Table } from 'react-bootstrap';
import { useTranslation } from 'react-i18next';
import { PageLayout } from '../components/common/page-layout';
import { Loader } from '../components/common/loader';
import { LoadFailure } from '../components/common/errors';
import { Modal } from '../components/common/modal';
import { InviteModal } from '../components/lti-registrations/invite-modal';
import { ManualRegistrationModal } from '../components/lti-registrations/manual-registration-modal';
import { DescriptionModal } from '../components/lti-registrations/description-modal';
import { ToolSetupModal } from '../components/lti-registrations/tool-setup-modal';
import { useCurrentUser } from '../hooks/session-context';
import { LtiRegistrationsStore } from '../stores/lti-registrations-store';

/** Admin page: the connected LMS tools and connecting new ones by a registration link or by hand. */
export const LtiRegistrationsPage = observer(() => {
    const { t } = useTranslation();
    const user = useCurrentUser();
    const [store] = useState(() => new LtiRegistrationsStore());

    useEffect(() => { store.loadRegistrations(); }, [store]);

    if (!user) return <Loader />;

    const { registrations, inviteForm, manualForm, setupHint, descriptionForm, registrationToDelete } = store;

    return (
        <PageLayout title={t('ltiRegistrations_page_title')}>
            <div className="mb-3 d-flex" style={{ gap: '0.5rem' }}>
                <Button variant="primary" onClick={() => store.openInviteForm()}>
                    {t('ltiRegistrations_connectTitle')}
                </Button>
                <Button variant="outline-primary" onClick={() => store.openManualForm()}>
                    {t('ltiRegistrations_manualTitle')}
                </Button>
                <Button variant="outline-secondary" onClick={() => store.openSetupHint()}>
                    {t('ltiRegistrations_setupTitle')}
                </Button>
            </div>

            {store.loadStatus === 'FAILED' && store.error && (
                <LoadFailure error={store.error} onRetry={() => store.loadRegistrations()} />
            )}
            {store.deleteError && <LoadFailure error={store.deleteError} />}
            {store.loadStatus === 'LOADING' && registrations.length === 0 && <Loader />}
            {store.loadStatus === 'LOADED' && registrations.length === 0 && (
                <div className="text-muted">{t('ltiRegistrations_empty')}</div>
            )}
            {registrations.length > 0 && (
                <Table size="sm" striped>
                    <thead>
                        <tr>
                            <th>{t('ltiRegistrations_lmsColumn')}</th>
                            <th>{t('ltiRegistrations_descriptionColumn')}</th>
                            <th>Client ID</th>
                            <th>{t('ltiRegistrations_methodColumn')}</th>
                            <th>{t('ltiRegistrations_createdColumn')}</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        {registrations.map(r => (
                            <tr key={r.id}>
                                <td>{r.lmsUrl}</td>
                                <td>{r.description ?? '—'}</td>
                                <td>{r.clientId}</td>
                                <td>{t(r.method === 'LINK' ? 'ltiRegistrations_methodLink' : 'ltiRegistrations_methodManual')}</td>
                                <td>{new Date(r.createdAt).toLocaleString()}</td>
                                <td className="text-end text-nowrap">
                                    <Button variant="outline-secondary" size="sm" className="me-2"
                                            onClick={() => store.openDescriptionForm(r)}>
                                        {t('ltiRegistrations_editBtn')}
                                    </Button>
                                    <Button variant="outline-danger" size="sm" onClick={() => store.askToDelete(r)}>
                                        {t('ltiRegistrations_deleteBtn')}
                                    </Button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </Table>
            )}

            {inviteForm && <InviteModal form={inviteForm} onClose={() => store.closeInviteForm()} />}
            {manualForm && (
                <ManualRegistrationModal form={manualForm}
                                         onSubmit={() => store.registerManually(manualForm)}
                                         onClose={() => store.closeManualForm()} />
            )}
            {setupHint && <ToolSetupModal hint={setupHint} onClose={() => store.closeSetupHint()} />}
            {descriptionForm && (
                <DescriptionModal form={descriptionForm}
                                  onSave={() => store.saveDescription(descriptionForm)}
                                  onClose={() => store.closeDescriptionForm()} />
            )}
            {registrationToDelete && (
                <Modal show={true}
                       title={t('ltiRegistrations_deleteTitle')}
                       closeButton={true}
                       handleClose={() => store.cancelDelete()}
                       primaryBtnTitle={t('ltiRegistrations_deleteBtn')}
                       primaryBtnVariant="danger"
                       handlePrimaryBtnClicked={() => store.deleteRegistration(registrationToDelete)}
                       secondaryBtnTitle={t('ltiRegistrations_cancelBtn')}
                       handleSecondaryBtnClicked={() => store.cancelDelete()}>
                    <p>{t('ltiRegistrations_deleteBody', { lms: registrationToDelete.lmsUrl, clientId: registrationToDelete.clientId })}</p>
                </Modal>
            )}
        </PageLayout>
    );
});
