import React from 'react';
import { Modal } from './Modal';
import { Button } from './Button';

interface ConfirmationDialogProps {
    isOpen: boolean;
    title: string;
    message: string;
    confirmLabel?: string;
    confirmVariant?: 'primary' | 'danger';
    onConfirm: () => void;
    onCancel: () => void;
    isLoading?: boolean;
}

export const ConfirmationDialog: React.FC<ConfirmationDialogProps> = ({
    isOpen,
    title,
    message,
    confirmLabel = 'Confirm',
    confirmVariant = 'danger',
    onConfirm,
    onCancel,
    isLoading,
}) => {
    return (
        <Modal
            isOpen={isOpen}
            onClose={onCancel}
            title={title}
            size="sm"
            footer={
                <div className="flex justify-end space-x-3">
                    <Button variant="ghost" onClick={onCancel} disabled={isLoading}>
                        Cancel
                    </Button>
                    <Button variant={confirmVariant} onClick={onConfirm} loading={isLoading}>
                        {confirmLabel}
                    </Button>
                </div>
            }
        >
            <div className="text-sm text-gray-600">{message}</div>
        </Modal>
    );
};
