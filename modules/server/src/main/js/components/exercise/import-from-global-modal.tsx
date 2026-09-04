import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react';
import { Button } from 'react-bootstrap';
import { useTranslation } from 'react-i18next';
import { GlobalPoolStore, ImportMode } from '../../stores/global-pool-store';
import { Modal } from '../common/modal';

type Props = {
    courseId: number;
    /** Each mode hits a different endpoint; the backend says which ones are open. */
    canInherit: boolean;
    canClone: boolean;
    onClose: () => void;
    onImported?: () => void;
};

export const ImportFromGlobalModal = observer(({ courseId, canInherit, canClone, onClose, onImported }: Props) => {
    const { t } = useTranslation();
    const [store] = useState(() => new GlobalPoolStore());
    const [mode, setMode] = useState<ImportMode>(canInherit ? 'INHERIT' : 'CLONE');
    const [busyId, setBusyId] = useState<number | null>(null);
    const modeAllowed = mode === 'INHERIT' ? canInherit : canClone;

    useEffect(() => { store.loadGlobalPool(); }, [store]);

    const onImportClick = async (exerciseId: number) => {
        setBusyId(exerciseId);
        const ok = await store.importToCourse(exerciseId, courseId, mode);
        setBusyId(null);
        if (ok) {
            onImported?.();
            onClose();
        }
    };

    const inheritWarning = (
        <div className="alert alert-warning py-1 px-2 mb-0 mt-2 small">
            <strong>{t('importModal_inherit_label')}</strong> {t('importModal_inherit_body')}
        </div>
    );
    const cloneHint = (
        <div className="alert alert-info py-1 px-2 mb-0 mt-2 small">
            <strong>{t('importModal_clone_label')}</strong> {t('importModal_clone_body')}
        </div>
    );

    return (
        <Modal show={true}
               size="lg"
               title={t('importModal_title')}
               closeButton={true}
               handleClose={onClose}
               secondaryBtnTitle={t('importModal_cancel')}
               handleSecondaryBtnClicked={onClose}>
            <div className="mb-3">
                <label className="fw-bold me-2">{t('importModal_modeLabel')}</label>
                <div className="btn-group" role="group">
                    <Button variant={mode === 'INHERIT' ? 'warning' : 'outline-warning'}
                            size="sm"
                            disabled={!canInherit}
                            onClick={() => setMode('INHERIT')}>
                        {t('importModal_inherit_btn')}
                    </Button>
                    <Button variant={mode === 'CLONE' ? 'success' : 'outline-success'}
                            size="sm"
                            disabled={!canClone}
                            onClick={() => setMode('CLONE')}>
                        {t('importModal_clone_btn')}
                    </Button>
                </div>
                {mode === 'INHERIT' ? inheritWarning : cloneHint}
            </div>
            {store.loadStatus === 'LOADING' && <div>{t('importModal_loading')}</div>}
            <ul className="list-group">
                {store.exercises.map(e =>
                    <li key={e.id} className="list-group-item d-flex justify-content-between align-items-center">
                        <span>{e.name}</span>
                        <Button variant="success"
                                size="sm"
                                disabled={busyId !== null || !modeAllowed}
                                onClick={() => onImportClick(e.id)}>
                            {busyId === e.id ? t('importModal_importing') : t('importModal_import')}
                        </Button>
                    </li>
                )}
            </ul>
        </Modal>
    );
});
