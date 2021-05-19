import React from "react";
import { Button, Modal as RBModal } from "react-bootstrap";


export type ModalProps = {
    show?: boolean,
    title?: string,
    type?: 'MODAL' | 'DIALOG',
    primaryBtnTitle?: string,
    handlePrimaryBtnClicked?: () => void,
    secondaryBtnTitle?: string,
    handleSecondaryBtnClicked?: () => void,
    children?: React.ReactNode[] | React.ReactNode,
    closeButton?: boolean,
    handleClose?: () => void,
}

export const Modal = (props: ModalProps) => {
    const { title, primaryBtnTitle,
        handlePrimaryBtnClicked,
        secondaryBtnTitle,
        handleSecondaryBtnClicked,
        children,
        closeButton,
        show,
        handleClose,
        type,
    } = props;

    return (
        <ModalWrapper type={type ?? 'MODAL'} show={show ?? true} onHide={handleClose}>
            <RBModal.Header closeButton={closeButton}>
                <RBModal.Title>{title}</RBModal.Title>
            </RBModal.Header>

            <RBModal.Body>
                {children}
            </RBModal.Body>
            {(secondaryBtnTitle || primaryBtnTitle)
                ? <RBModal.Footer>
                    {secondaryBtnTitle ? <Button variant="secondary" onClick={handleSecondaryBtnClicked}>{secondaryBtnTitle}</Button> : null}
                    {primaryBtnTitle ? <Button variant="primary" onClick={handlePrimaryBtnClicked}>{primaryBtnTitle}</Button> : null}
                  </RBModal.Footer>
                : null}            
        </ModalWrapper>
    );
}

const ModalWrapper = (props: { type: 'MODAL' | 'DIALOG', show: boolean, onHide?: () => void, children: React.ReactNode[] | React.ReactNode, }) => {
    const { 
        type,
        show,
        onHide,
        children
    } = props;
    if (type === 'DIALOG') {
        return (
            <RBModal.Dialog>{children}</RBModal.Dialog>
        );
    }
    return (
        <RBModal show={show} onHide={onHide}>{children}</RBModal>
    );
}
