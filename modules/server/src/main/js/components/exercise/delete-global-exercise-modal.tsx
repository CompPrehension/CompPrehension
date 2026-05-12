import React, { useEffect, useState } from 'react';
import { container } from 'tsyringe';
import * as E from 'fp-ts/lib/Either';
import { CourseController } from '../../controllers/course/course-controller';
import { CourseDto } from '../../types/course';

type Props = {
    exerciseId: number;
    onConfirm: () => void;
    onCancel: () => void;
};

export const DeleteGlobalExerciseModal: React.FC<Props> = ({ exerciseId, onConfirm, onCancel }) => {
    const [memberships, setMemberships] = useState<CourseDto[] | null>(null);
    const [courseController] = useState(() => container.resolve(CourseController));

    useEffect(() => {
        (async () => {
            const r = await courseController.getExerciseMemberships(exerciseId);
            if (E.isRight(r)) setMemberships(r.right);
        })();
    }, [exerciseId]);

    // Group memberships by educationResource for display.
    const byLms = (memberships ?? []).reduce<Record<string, CourseDto[]>>((acc, m) => {
        (acc[m.educationResourceName] ??= []).push(m);
        return acc;
    }, {});

    return (
        <div className="modal d-block" tabIndex={-1} role="dialog" style={{ background: 'rgba(0,0,0,0.5)' }}>
            <div className="modal-dialog" role="document">
                <div className="modal-content">
                    <div className="modal-header">
                        <h5 className="modal-title">Удалить упражнение из глобального пула</h5>
                        <button type="button" className="close" onClick={onCancel}>&times;</button>
                    </div>
                    <div className="modal-body">
                        {memberships === null
                            ? <div>Загрузка…</div>
                            : memberships.length === 0
                                ? <div>Это упражнение никем не используется. Будет удалено из пула.</div>
                                : (
                                    <>
                                        <p className="text-warning">
                                            <strong>Внимание:</strong> во всех курсах ниже будут созданы независимые копии этого упражнения. Связь с оригиналом разорвётся, оригинал будет удалён из пула.
                                        </p>
                                        {Object.entries(byLms).map(([lms, courses]) => (
                                            <div key={lms} className="mb-2">
                                                <div className="font-weight-bold">{lms}</div>
                                                <ul>
                                                    {courses.map(c => <li key={c.id}>{c.name}</li>)}
                                                </ul>
                                            </div>
                                        ))}
                                    </>
                                )}
                    </div>
                    <div className="modal-footer">
                        <button type="button" className="btn btn-secondary" onClick={onCancel}>Отмена</button>
                        <button type="button" className="btn btn-danger" onClick={onConfirm}>Удалить</button>
                    </div>
                </div>
            </div>
        </div>
    );
};
