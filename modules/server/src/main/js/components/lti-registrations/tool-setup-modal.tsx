import React from 'react';
import { observer } from 'mobx-react';
import { Button, Form, InputGroup, Table } from 'react-bootstrap';
import { useTranslation } from 'react-i18next';
import { Modal } from '../common/modal';
import { Loader } from '../common/loader';
import { LoadFailure } from '../common/errors';
import { LtiToolSetupHint } from '../../stores/lti-registrations-store';

type Props = {
    hint: LtiToolSetupHint;
    onClose: () => void;
};

const CopyableValue = ({ value, multiline }: { value: string, multiline?: boolean }) => {
    const { t } = useTranslation();
    return (
        <InputGroup size="sm">
            {multiline
                ? <Form.Control readOnly value={value} onFocus={e => e.target.select()}
                                as="textarea" rows={9} className="font-monospace" />
                : <Form.Control readOnly value={value} onFocus={e => e.target.select()} />}
            <Button variant="outline-secondary" onClick={() => navigator.clipboard.writeText(value)}>
                {t('ltiRegistrations_copyBtn')}
            </Button>
        </InputGroup>
    );
};

/** Moodle field labels and option names are given as Moodle shows them in English. */
export const ToolSetupModal = observer(({ hint, onClose }: Props) => {
    const { t } = useTranslation();
    const { configuration } = hint;

    const rows: [string, React.ReactNode][] = configuration ? [
        ['Tool URL', <CopyableValue value={configuration.launchUrl} />],
        ['LTI version', 'LTI 1.3'],
        ['Public key type', 'Keyset URL'],
        ['Public keyset', <CopyableValue value={configuration.jwksUrl} />],
        ['Initiate login URL', <CopyableValue value={configuration.loginUrl} />],
        ['Redirection URI(s)', <CopyableValue value={configuration.launchUrl} />],
        ['Tool configuration usage', 'Show in activity chooser and as a preconfigured tool'],
        ['Default launch container', 'New window'],
        ['Supports Deep Linking (Content-Item Message)', t('ltiRegistrations_setupEnabled')],
        ['Content Selection URL', <CopyableValue value={configuration.launchUrl} />],
        ['IMS LTI Assignment and Grade Services', 'Use this service for grade sync and column management'],
        ['IMS LTI Names and Role Provisioning', "Use this service to retrieve members' information as per privacy settings"],
        ["Share launcher's name with tool", 'Always'],
        ["Share launcher's email with tool", <>Always <Form.Text muted>{t('ltiRegistrations_setupEmailNote')}</Form.Text></>],
        ['Accept grades from the tool', 'Always'],
        ['Custom parameters', t('ltiRegistrations_setupEmpty')],
    ] : [];

    return (
        <Modal show={true}
               size="xl"
               title={t('ltiRegistrations_setupTitle')}
               closeButton={true}
               handleClose={onClose}
               secondaryBtnTitle={t('ltiRegistrations_closeBtn')}
               handleSecondaryBtnClicked={onClose}>
            {hint.error && <LoadFailure error={hint.error} />}
            {!hint.error && !configuration && <Loader />}
            {configuration && (
                <>
                    <p>{t('ltiRegistrations_setupOpen')}</p>
                    <Table size="sm" className="align-middle">
                        <tbody>
                            {rows.map(([label, value]) => (
                                <tr key={label}>
                                    <td className="text-nowrap">{label}</td>
                                    <td className="w-100">{value}</td>
                                </tr>
                            ))}
                        </tbody>
                    </Table>
                    <p className="mb-1">{t('ltiRegistrations_setupRsaKey')}</p>
                    <div className="mb-3"><CopyableValue value={configuration.publicKeyPem} multiline /></div>
                    <p className="mb-0">{t('ltiRegistrations_setupBack')}</p>
                </>
            )}
        </Modal>
    );
});
