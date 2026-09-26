import React, { useCallback, useEffect, useState } from 'react';
import { Button, Form, InputGroup, Table } from 'react-bootstrap';
import { useTranslation } from 'react-i18next';
import * as E from 'fp-ts/lib/Either';
import { PageLayout } from '../components/common/page-layout';
import { Loader } from '../components/common/loader';
import { LoadFailure } from '../components/common/errors';
import { Modal } from '../components/common/modal';
import { useCurrentUser } from '../hooks/session-context';
import { ltiRegistrationController } from '../controllers';
import { LtiRegistration, LtiRegistrationInvite } from '../controllers/lti/lti-registration-controller';
import { RequestError } from '../types/request-error';

/** Admin page: connect an LMS by a one-time dynamic registration link and see the connected ones. */
export const LtiRegistrationsPage = () => {
    const { t } = useTranslation();
    const user = useCurrentUser();
    const [registrations, setRegistrations] = useState<LtiRegistration[] | null>(null);
    const [loadError, setLoadError] = useState<RequestError | null>(null);
    const [invite, setInvite] = useState<LtiRegistrationInvite | null>(null);
    const [inviteError, setInviteError] = useState<RequestError | null>(null);
    const [creatingInvite, setCreatingInvite] = useState(false);
    const [registrationToDelete, setRegistrationToDelete] = useState<LtiRegistration | null>(null);
    const [deleteError, setDeleteError] = useState<RequestError | null>(null);

    const loadRegistrations = useCallback(async () => {
        setLoadError(null);
        const res = await ltiRegistrationController.getRegistrations();
        if (E.isRight(res)) {
            setRegistrations(res.right);
        } else {
            setLoadError(res.left);
        }
    }, []);

    useEffect(() => { loadRegistrations(); }, [loadRegistrations]);

    const createInvite = async () => {
        setCreatingInvite(true);
        setInviteError(null);
        const res = await ltiRegistrationController.createInvite();
        if (E.isRight(res)) {
            setInvite(res.right);
        } else {
            setInviteError(res.left);
        }
        setCreatingInvite(false);
    };

    const deleteRegistration = async (registration: LtiRegistration) => {
        setRegistrationToDelete(null);
        setDeleteError(null);
        const res = await ltiRegistrationController.deleteRegistration(registration.id);
        if (E.isLeft(res)) {
            setDeleteError(res.left);
        }
        await loadRegistrations();
    };

    if (!user) return <Loader />;

    const inviteUrl = invite ? `${window.location.origin}/lti/1_3/register/${invite.token}` : null;

    return (
        <PageLayout title={t('ltiRegistrations_page_title')}>
            <h5>{t('ltiRegistrations_connectTitle')}</h5>
            <p className="text-muted">{t('ltiRegistrations_connectHint')}</p>
            <Button variant="primary" className="mb-3" disabled={creatingInvite} onClick={createInvite}>
                {t('ltiRegistrations_createInviteBtn')}
            </Button>
            {inviteError && <LoadFailure error={inviteError} />}
            {inviteUrl && invite && (
                <div className="mb-4">
                    <InputGroup>
                        <Form.Control readOnly value={inviteUrl} onFocus={e => e.target.select()} />
                        <Button variant="outline-secondary" onClick={() => navigator.clipboard.writeText(inviteUrl)}>
                            {t('ltiRegistrations_copyBtn')}
                        </Button>
                    </InputGroup>
                    <Form.Text muted>
                        {t('ltiRegistrations_inviteExpires', { date: new Date(invite.expiresAt).toLocaleString() })}
                    </Form.Text>
                </div>
            )}

            <h5 className="mt-4">{t('ltiRegistrations_listTitle')}</h5>
            {loadError && <LoadFailure error={loadError} onRetry={loadRegistrations} />}
            {deleteError && <LoadFailure error={deleteError} />}
            {registrations == null && !loadError && <Loader />}
            {registrations?.length === 0 && <div className="text-muted">{t('ltiRegistrations_empty')}</div>}
            {registrations != null && registrations.length > 0 && (
                <Table size="sm" striped>
                    <thead>
                        <tr>
                            <th>{t('ltiRegistrations_lmsColumn')}</th>
                            <th>Client ID</th>
                            <th>{t('ltiRegistrations_createdColumn')}</th>
                            <th></th>
                        </tr>
                    </thead>
                    <tbody>
                        {registrations.map(r => (
                            <tr key={r.id}>
                                <td>{r.lmsUrl}</td>
                                <td>{r.clientId}</td>
                                <td>{new Date(r.createdAt).toLocaleString()}</td>
                                <td className="text-end">
                                    <Button variant="outline-danger" size="sm" onClick={() => setRegistrationToDelete(r)}>
                                        {t('ltiRegistrations_deleteBtn')}
                                    </Button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </Table>
            )}
            {registrationToDelete && (
                <Modal show={true}
                       title={t('ltiRegistrations_deleteTitle')}
                       closeButton={true}
                       handleClose={() => setRegistrationToDelete(null)}
                       primaryBtnTitle={t('ltiRegistrations_deleteBtn')}
                       primaryBtnVariant="danger"
                       handlePrimaryBtnClicked={() => deleteRegistration(registrationToDelete)}
                       secondaryBtnTitle={t('ltiRegistrations_cancelBtn')}
                       handleSecondaryBtnClicked={() => setRegistrationToDelete(null)}>
                    <p>{t('ltiRegistrations_deleteBody', { lms: registrationToDelete.lmsUrl })}</p>
                </Modal>
            )}
        </PageLayout>
    );
};
