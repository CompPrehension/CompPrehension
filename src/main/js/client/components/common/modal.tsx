import React from "react";
import { Button, Modal as RBModal } from "react-bootstrap";


export type ModalProps = {
    show?: boolean,
    title?: string,
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
    } = props;

    return (
        <RBModal show={show ?? true} onHide={handleClose}>
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
        </RBModal>
    );
}
