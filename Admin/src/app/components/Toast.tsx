import React, { createContext, useContext, useState, ReactNode, useEffect } from "react";
import { CheckCircle2, XCircle, X } from "lucide-react";

type ToastType = "success" | "error";

interface ToastContextType {
  showToast: (message: string, type: ToastType) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<{ message: string; type: ToastType } | null>(null);

  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => setToast(null), 4000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  return (
    <ToastContext.Provider value={{ showToast: (msg, type) => setToast({ message: msg, type }) }}>
      {children}
      {toast && (
        <div 
          className="fixed top-6 right-6 z-[300] flex items-center p-4 rounded-[12px] min-w-[300px] shadow-lg animate-in slide-in-from-top-2 fade-in duration-200"
          style={{ 
            background: "white", 
            border: `1px solid ${toast.type === 'success' ? '#e2f5e5' : '#fcecec'}`,
            boxShadow: "0 12px 32px rgba(0,0,0,0.08)"
          }}
        >
          <div className="mr-3 shrink-0">
            {toast.type === "success" ? (
              <CheckCircle2 color="#57b869" size={20} />
            ) : (
              <XCircle color="#f25959" size={20} />
            )}
          </div>
          <div style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 14, color: "#1c1f29", flex: 1 }}>
            {toast.message}
          </div>
          <button 
            onClick={() => setToast(null)}
            className="ml-4 shrink-0 flex items-center justify-center w-6 h-6 rounded-full hover:bg-gray-100 transition-colors"
          >
            <X size={14} color="#9499a6" />
          </button>
        </div>
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error("useToast must be used within ToastProvider");
  return context;
}
