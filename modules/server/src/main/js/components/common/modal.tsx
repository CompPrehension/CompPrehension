import React from "react";
import { Button, Modal as RBModal } from "react-bootstrap";
import { isNullOrUndefined } from "../../utils/helpers";
import { Optional } from "./optional";


export type ModalProps = {
    show?: boolean | null,
    title?: string | null,
    type?: 'MODAL' | 'DIALOG',
    size?: 'sm' | 'lg' | 'xl',
    primaryBtnTitle?: string | null,
    handlePrimaryBtnClicked?: (() => void) | null,
    secondaryBtnTitle?: string | null,
    handleSecondaryBtnClicked?: (() => void) | null,
    children?: React.ReactNode[] | React.ReactNode | null,
    closeButton?: boolean | null,
    handleClose?: (() => void) | null,
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
            <ModalWrapper type={type ?? 'MODAL'} show={show ?? true} onHide={handleClose ?? undefined} size={size}>
                <Optional isVisible={!isNullOrUndefined(title) && title.length > 0}>
                    <RBModal.Header closeButton={closeButton ?? undefined} placeholder={null}>
                        <RBModal.Title>{title}</RBModal.Title>
                    </RBModal.Header>
                </Optional>      
                <RBModal.Body>
                    {children}
                </RBModal.Body>
                {(secondaryBtnTitle || primaryBtnTitle)
                    ? <RBModal.Footer>
                        {secondaryBtnTitle && <Button variant="secondary" onClick={handleSecondaryBtnClicked ?? undefined}>{secondaryBtnTitle}</Button>}
                        {primaryBtnTitle && <Button variant="primary" onClick={handlePrimaryBtnClicked ?? undefined}>{primaryBtnTitle}</Button>}
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
