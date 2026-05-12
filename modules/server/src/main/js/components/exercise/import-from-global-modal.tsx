import React, { useEffect, useState } from 'react';
import { observer } from 'mobx-react';
import { container } from 'tsyringe';
import { GlobalPoolStore, ImportMode } from '../../stores/global-pool-store';

type Props = {
    courseId: number;
    onClose: () => void;
    onImported?: () => void;
};

export const ImportFromGlobalModal = observer(({ courseId, onClose, onImported }: Props) => {
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
            <strong>⚠ Inherit:</strong> курс будет использовать <em>общую</em> запись из пула.
            Любое изменение автора в пуле сразу отразится здесь и поломает уже идущие attempt'ы студентов,
            если автор поменяет наполнение. Это удобно для синхронизации, но опасно при активном использовании.
            Если не уверен — выбери <strong>Clone</strong>.
        </div>
    );
    const cloneHint = (
        <div className="alert alert-info py-1 px-2 mb-0 mt-2 small">
            <strong>Clone:</strong> создаётся независимая копия. Дальше курс работает со своей версией;
            изменения автора в пуле никак не влияют на эту копию.
        </div>
    );

    return (
        <div className="modal d-block" tabIndex={-1} role="dialog"
             style={{ background: 'rgba(0,0,0,0.5)', overflowY: 'auto' }}>
            <div className="modal-dialog modal-lg my-3" role="document"
                 style={{ maxHeight: 'calc(100vh - 2rem)', display: 'flex', flexDirection: 'column' }}>
                <div className="modal-content" style={{ maxHeight: '100%', overflow: 'hidden' }}>
                    <div className="modal-header">
                        <h5 className="modal-title">Импорт из глобального пула</h5>
                        <button type="button" className="close" onClick={onClose}>&times;</button>
                    </div>
                    <div className="modal-body" style={{ overflowY: 'auto' }}>
                        <div className="mb-3">
                            <label className="font-weight-bold mr-2">Режим:</label>
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
                        {store.loadStatus === 'LOADING' && <div>Загрузка…</div>}
                        <ul className="list-group">
                            {store.exercises.map(e =>
                                <li key={e.id} className="list-group-item d-flex justify-content-between align-items-center">
                                    <span>{e.name}</span>
                                    <button type="button"
                                            className="btn btn-sm btn-success"
                                            disabled={busyId !== null}
                                            onClick={() => onImportClick(e.id)}>
                                        {busyId === e.id ? 'Импорт…' : 'Импортировать'}
                                    </button>
                                </li>
                            )}
                        </ul>
                    </div>
                    <div className="modal-footer">
                        <button type="button" className="btn btn-secondary" onClick={onClose}>Отмена</button>
                    </div>
                </div>
            </div>
        </div>
    );
});
