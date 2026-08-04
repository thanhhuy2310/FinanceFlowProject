import { AlertCircle, CheckCircle2, Info, X } from "lucide-react";
import { useCallback, useMemo, useRef, useState, type ReactNode } from "react";

import { ToastContext, type ToastTone } from "./toast-context";

const TOAST_DURATION_MS = 4500;
const MAX_VISIBLE_TOASTS = 4;

interface ToastItem {
  id: number;
  message: string;
  tone: ToastTone;
}

const toastIcons = {
  success: CheckCircle2,
  error: AlertCircle,
  info: Info,
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const nextId = useRef(0);

  const dismiss = useCallback((id: number) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const toast = useCallback(
    (message: string, tone: ToastTone = "success") => {
      const id = ++nextId.current;

      setToasts((current) => [...current.slice(-(MAX_VISIBLE_TOASTS - 1)), { id, message, tone }]);
      window.setTimeout(() => dismiss(id), TOAST_DURATION_MS);
    },
    [dismiss],
  );

  const value = useMemo(() => ({ toast }), [toast]);

  return (
    <ToastContext.Provider value={value}>
      {children}

      <div className="toast-viewport" aria-live="polite" aria-atomic="false">
        {toasts.map((item) => {
          const Icon = toastIcons[item.tone];

          return (
            <div
              key={item.id}
              className={`toast ${item.tone}`}
              role={item.tone === "error" ? "alert" : "status"}
            >
              <Icon size={18} />
              <p>{item.message}</p>
              <button
                type="button"
                className="icon-button toast-close"
                aria-label="Dismiss notification"
                onClick={() => dismiss(item.id)}
              >
                <X size={15} />
              </button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}
