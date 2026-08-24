import React from "react";
import { Eye, Pencil, Trash2 } from "lucide-react";

export function ActionButtons({ onView, onEdit, onDelete }: { onView: () => void; onEdit: () => void; onDelete: () => void }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
      {[
        { fn: onView,   Icon: Eye,     bg: "rgba(120,135,250,0.10)", hover: "rgba(120,135,250,0.22)", color: "#7887fa", title: "View"   },
        { fn: onEdit,   Icon: Pencil,  bg: "rgba(59,130,246,0.10)",  hover: "rgba(59,130,246,0.22)",  color: "#3b82f6", title: "Edit"   },
        { fn: onDelete, Icon: Trash2,  bg: "rgba(242,89,89,0.10)",   hover: "rgba(242,89,89,0.22)",   color: "#f25959", title: "Delete" },
      ].map(({ fn, Icon, bg, hover, color, title }) => (
        <button key={title} title={title}
          onClick={e => { e.stopPropagation(); fn(); }}
          style={{ width: 32, height: 32, borderRadius: 6, border: "none", background: bg, display: "flex", alignItems: "center", justifyContent: "center", cursor: "pointer", transition: "background 150ms", flexShrink: 0 }}
          onMouseEnter={e => ((e.currentTarget as HTMLElement).style.background = hover)}
          onMouseLeave={e => ((e.currentTarget as HTMLElement).style.background = bg)}
        >
          <Icon size={14} color={color} />
        </button>
      ))}
    </div>
  );
}
