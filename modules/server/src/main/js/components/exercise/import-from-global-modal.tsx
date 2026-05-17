import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react';
import { container } from 'tsyringe';
import { useTranslation } from 'react-i18next';
import { GlobalPoolStore, ImportMode } from '../../stores/global-pool-store';

type Props = {
    courseId: number;
    onClose: () => void;
    onImported?: () => void;
};

export const ImportFromGlobalModal = observer(({ courseId, onClose, onImported }: Props) => {
    const { t } = useTranslation();
    const [store] = useState(() => container.resolve(GlobalPoolStore));
    const [mode, setMode] = useState<ImportMode>('INHERIT');
    const [busyId, setBusyId] = useState<number | null>(null);

    useEffect(() => { store.loadGlobalPool(); }, []);

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
            <strong>⚠ Inherit:</strong> {t('importModal_inherit_body')}
        </div>
    );
    const cloneHint = (
        <div className="alert alert-info py-1 px-2 mb-0 mt-2 small">
            <strong>Clone:</strong> {t('importModal_clone_body')}
        </div>
    );

    return (
        <div className="modal d-block" tabIndex={-1} role="dialog"
             style={{ background: 'rgba(0,0,0,0.5)', overflowY: 'auto' }}>
            <div className="modal-dialog modal-lg my-3" role="document"
                 style={{ maxHeight: 'calc(100vh - 2rem)', display: 'flex', flexDirection: 'column' }}>
                <div className="modal-content" style={{ maxHeight: '100%', overflow: 'hidden' }}>
                    <div className="modal-header">
                        <h5 className="modal-title">{t('importModal_title')}</h5>
                        <button type="button" className="close" onClick={onClose}>&times;</button>
                    </div>
                    <div className="modal-body" style={{ overflowY: 'auto' }}>
                        <div className="mb-3">
                            <label className="font-weight-bold mr-2">{t('importModal_modeLabel')}</label>
                            <div className="btn-group" role="group">
                                <button type="button"
                                        className={`btn btn-sm ${mode === 'INHERIT' ? 'btn-warning' : 'btn-outline-warning'}`}
                                        onClick={() => setMode('INHERIT')}>
                                    ⚠ Inherit
                                </button>
                                <button type="button"
                                        className={`btn btn-sm ${mode === 'CLONE' ? 'btn-success' : 'btn-outline-success'}`}
                                        onClick={() => setMode('CLONE')}>
                                    Clone
                                </button>
                            </div>
                            {mode === 'INHERIT' ? inheritWarning : cloneHint}
                        </div>
                        {store.loadStatus === 'LOADING' && <div>{t('importModal_loading')}</div>}
                        <ul className="list-group">
                            {store.exercises.map(e =>
                                <li key={e.id} className="list-group-item d-flex justify-content-between align-items-center">
                                    <span>{e.name}</span>
                                    <button type="button"
                                            className="btn btn-sm btn-success"
                                            disabled={busyId !== null}
                                            onClick={() => onImportClick(e.id)}>
                                        {busyId === e.id ? t('importModal_importing') : t('importModal_import')}
                                    </button>
                                </li>
                            )}
                        </ul>
                    </div>
                    <div className="modal-footer">
                        <button type="button" className="btn btn-secondary" onClick={onClose}>{t('importModal_cancel')}</button>
                    </div>
                </div>
            </div>
        </div>
    );
});
