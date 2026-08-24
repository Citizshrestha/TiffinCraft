import React from "react";
import { X } from "lucide-react";

export function Modal({ title, onClose, children, width = 500 }: { title: string; onClose: () => void; children: React.ReactNode; width?: number }) {
  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4" style={{ background: "rgba(0,0,0,0.5)" }} onClick={onClose}>
      <div className="bg-white rounded-[16px] w-full flex flex-col" style={{ maxWidth: width, maxHeight: "90vh", boxShadow: "0 24px 64px rgba(0,0,0,0.22)" }} onClick={e => e.stopPropagation()}>
        <div className="flex items-center justify-between px-6 py-4 shrink-0" style={{ borderBottom: "1px solid #f2f5f7" }}>
          <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 17, color: "#1c1f29" }}>{title}</p>
          <button onClick={onClose} className="w-8 h-8 rounded-[8px] flex items-center justify-center cursor-pointer hover:bg-[#f2f5f7]" style={{ border: "none", background: "none" }}>
            <X size={16} color="#9499a6" />
          </button>
        </div>
        <div className="overflow-y-auto px-6 py-5">{children}</div>
      </div>
    </div>
  );
}

export function ConfirmDelete({
  name,
  onConfirm,
  onCancel,
  loading = false,
}: {
  name: string;
  onConfirm: () => void;
  onCancel: () => void;
  loading?: boolean;
}) {
  return (
    <div className="fixed inset-0 z-[200] flex items-center justify-center p-4" style={{ background: "rgba(0,0,0,0.5)" }} onClick={onCancel}>
      <div className="bg-white rounded-[16px] w-full p-6" style={{ maxWidth: 400, boxShadow: "0 24px 64px rgba(0,0,0,0.22)" }} onClick={e => e.stopPropagation()}>
        <div className="w-14 h-14 rounded-full flex items-center justify-center mb-4 mx-auto" style={{ background: "rgba(242,89,89,0.1)" }}>
          <span style={{ fontSize: 26 }}>🗑️</span>
        </div>
        <p className="text-center mb-2" style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 17, color: "#1c1f29" }}>Confirm Delete</p>
        <p className="text-center mb-6" style={{ fontFamily: "Inter", fontSize: 14, color: "#9499a6" }}>
          Are you sure you want to delete <strong style={{ color: "#1c1f29" }}>{name}</strong>? This cannot be undone.
        </p>
        <div className="flex gap-3">
          <button
            onClick={onCancel}
            disabled={loading}
            style={{ flex: 1, padding: "12px 0", borderRadius: 8, border: "1px solid #e5e8ed", background: "white", fontFamily: "Inter", fontWeight: 500, fontSize: 14, color: "#9499a6", cursor: loading ? "not-allowed" : "pointer", opacity: loading ? 0.6 : 1 }}
          >
            Cancel
          </button>
          <button
            onClick={onConfirm}
            disabled={loading}
            style={{ flex: 1, padding: "12px 0", borderRadius: 8, border: "none", background: "#f25959", fontFamily: "Inter", fontWeight: 600, fontSize: 14, color: "white", cursor: loading ? "not-allowed" : "pointer", opacity: loading ? 0.7 : 1 }}
          >
            {loading ? "Deleting..." : "Delete"}
          </button>
        </div>
      </div>
    </div>
  );
}

export function DetailRow({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="py-3" style={{ borderBottom: "1px solid #f2f5f7" }}>
      <p style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 11, color: "#9499a6", textTransform: "uppercase", letterSpacing: "0.06em", marginBottom: 4 }}>{label}</p>
      <div style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 14, color: "#1c1f29" }}>{value}</div>
    </div>
  );
}

export function FormField({
  label,
  value,
  onChange,
  type = "text",
  options,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  options?: string[];
}) {
  const base: React.CSSProperties = {
    border: "1px solid #e5e8ed",
    fontFamily: "Inter",
    color: "#1c1f29",
    background: "white",
    width: "100%",
    padding: "12px 16px",
    borderRadius: 8,
    fontSize: 14,
    outline: "none",
  };
  return (
    <div style={{ marginBottom: 16 }}>
      <p style={{ fontFamily: "Inter", fontWeight: 500, fontSize: 13, color: "#1c1f29", marginBottom: 6 }}>{label}</p>
      {options ? (
        <select
          style={base}
          value={value}
          onChange={e => onChange(e.target.value)}
          onFocus={e => (e.target.style.borderColor = "#3b82f6")}
          onBlur={e => (e.target.style.borderColor = "#e5e8ed")}
        >
          {options.map(o => <option key={o} value={o}>{o}</option>)}
        </select>
      ) : (
        <input
          type={type}
          style={base}
          value={value}
          onChange={e => onChange(e.target.value)}
          onFocus={e => (e.target.style.borderColor = "#3b82f6")}
          onBlur={e => (e.target.style.borderColor = "#e5e8ed")}
        />
      )}
    </div>
  );
}

export function SaveCancel({
  onCancel,
  onSave,
  saving = false,
  saveLabel = "Save Changes",
}: {
  onCancel: () => void;
  onSave: () => void;
  saving?: boolean;
  saveLabel?: string;
}) {
  return (
    <div className="flex gap-3 mt-2">
      <button
        onClick={onCancel}
        disabled={saving}
        style={{ flex: 1, padding: "12px 0", borderRadius: 8, border: "1px solid #e5e8ed", background: "white", fontFamily: "Inter", fontWeight: 500, fontSize: 14, color: "#9499a6", cursor: saving ? "not-allowed" : "pointer", opacity: saving ? 0.6 : 1 }}
      >
        Cancel
      </button>
      <button
        onClick={onSave}
        disabled={saving}
        style={{ flex: 1, padding: "12px 0", borderRadius: 8, border: "none", background: "#3b82f6", fontFamily: "Inter", fontWeight: 600, fontSize: 14, color: "white", cursor: saving ? "not-allowed" : "pointer", opacity: saving ? 0.7 : 1 }}
      >
        {saving ? "Saving..." : saveLabel}
      </button>
    </div>
  );
}
