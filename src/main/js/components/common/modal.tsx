import React from "react";
import { Button, Modal as RBModal } from "react-bootstrap";
import { notNulAndUndefinded } from "../../utils/helpers";
import { Optional } from "./optional";


export type ModalProps = {
    show?: boolean,
    title?: string,
    type?: 'MODAL' | 'DIALOG',
    size?: 'sm' | 'lg' | 'xl',
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
        size,
    } = props;

    return (
        <Optional isVisible={show ?? true}>
            <ModalWrapper type={type ?? 'MODAL'} show={show ?? true} onHide={handleClose} size={size}>
                <Optional isVisible={notNulAndUndefinded(title) && title.length > 0}>
                    <RBModal.Header closeButton={closeButton}>
                        <RBModal.Title>{title}</RBModal.Title>
                    </RBModal.Header>
                </Optional>      
                <RBModal.Body>
                    {children}
                </RBModal.Body>
                {(secondaryBtnTitle || primaryBtnTitle)
                    ? <RBModal.Footer>
                        {secondaryBtnTitle && <Button variant="secondary" onClick={handleSecondaryBtnClicked}>{secondaryBtnTitle}</Button>}
                        {primaryBtnTitle && <Button variant="primary" onClick={handlePrimaryBtnClicked}>{primaryBtnTitle}</Button>}
                      </RBModal.Footer>
                    : null}            
            </ModalWrapper>
        </Optional>        
    );
}

const ModalWrapper = (props: { type: 'MODAL' | 'DIALOG', size?: 'sm' | 'lg' | 'xl', show: boolean, onHide?: () => void, children: React.ReactNode[] | React.ReactNode, }) => {
    const { 
        type,
        show,
        onHide,
        children,
        size,
    } = props;
    if (type === 'DIALOG') {
        return (
            <RBModal.Dialog size={size}>{children}</RBModal.Dialog>
        );
    }
    return (
        <RBModal size={size} show={show} onHide={onHide}>{children}</RBModal>
    );
}
