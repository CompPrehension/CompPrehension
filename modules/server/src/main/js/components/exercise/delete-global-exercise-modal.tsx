import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { container } from 'tsyringe';
import * as E from 'fp-ts/lib/Either';
import { CourseController } from '../../controllers/course/course-controller';
import { CourseDto } from '../../types/course';
import { Modal } from '../common/modal';

type Props = {
    exerciseId: number;
    onConfirm: () => void;
    onCancel: () => void;
};

export const DeleteGlobalExerciseModal: React.FC<Props> = ({ exerciseId, onConfirm, onCancel }) => {
    const { t } = useTranslation();
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
        <Modal
            show={true}
            title={t('deleteModal_title')}
            handleClose={onCancel}
            closeButton={true}
            primaryBtnTitle={t('deleteModal_confirm')}
            primaryBtnVariant="danger"
            handlePrimaryBtnClicked={onConfirm}
            secondaryBtnTitle={t('deleteModal_cancel')}
            handleSecondaryBtnClicked={onCancel}
        >
            {memberships === null
                ? <div>{t('deleteModal_loading')}</div>
                : memberships.length === 0
                    ? <div>{t('deleteModal_noUsages')}</div>
                    : (
                        <>
                            <p className="text-warning">
                                <strong>{t('deleteModal_warning')}</strong> {t('deleteModal_warningBody')}
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
        </Modal>
    );
};
