import React from 'react';
import { AlertCircle } from 'lucide-react';

interface AlertErrorProps {
  message: string | null;
}

export const AlertError: React.FC<AlertErrorProps> = ({ message }) => {
  if (!message) return null;

  return (
    <div className="alert-error" role="alert">
      <AlertCircle size={18} />
      <span>{message}</span>
    </div>
  );
};
