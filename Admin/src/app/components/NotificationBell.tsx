import React, { useEffect, useRef, useState } from "react";
import { Bell } from "lucide-react";
import { Page } from "./Sidebar";
import {
  AppNotification,
  fetchNotifications,
  fetchUnreadCount,
  markNotificationRead,
  markAllNotificationsRead,
  timeAgo,
} from "../api/notificationsApi";

const POLL_MS = 30000;

/** Which page a notification's type should open when clicked. */
function pageForType(type: string): Page | null {
  if (type.startsWith("commission_")) return "settlements";
  if (type.startsWith("refund_")) return "refunds";
  if (type === "new_order" || type === "order_status") return "orders";
  if (type === "review" || type === "review_reply") return "reviews";
  return null;
}

const TYPE_ICON: Record<string, string> = {
  commission_due: "🧾",
  commission_submitted: "📩",
  commission_verified: "✅",
  commission_rejected: "⚠️",
  refund_requested: "↩️",
  refund_feedback: "↩️",
  refund_status: "↩️",
  new_order: "🛍️",
  order_status: "📦",
  review: "⭐",
  cook_approved: "👨‍🍳",
  cook_rejected: "👨‍🍳",
};

export function NotificationBell({ onNavigate, dark = false }: { onNavigate: (page: Page) => void; dark?: boolean }) {
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<AppNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);

  async function refreshCount() {
    try {
      setUnreadCount(await fetchUnreadCount());
    } catch {
      // Silent — a failed poll shouldn't surface an error toast every 30s.
    }
  }

  async function loadList() {
    setLoading(true);
    try {
      const data = await fetchNotifications();
      setItems(data.slice(0, 20)); // recent 20 is plenty for a dropdown
    } catch {
      // Leave whatever was already showing.
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refreshCount();
    const interval = setInterval(refreshCount, POLL_MS);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (open) loadList();
  }, [open]);

  useEffect(() => {
    function onOutside(e: MouseEvent) {
      if (wrapRef.current && !wrapRef.current.contains(e.target as Node)) setOpen(false);
    }
    if (open) document.addEventListener("mousedown", onOutside);
    return () => document.removeEventListener("mousedown", onOutside);
  }, [open]);

  async function handleItemClick(n: AppNotification) {
    if (!n.isRead) {
      setItems(prev => prev.map(x => (x.id === n.id ? { ...x, isRead: true } : x)));
      setUnreadCount(c => Math.max(0, c - 1));
      markNotificationRead(n.id).catch(() => {});
    }
    const page = pageForType(n.type);
    if (page) {
      onNavigate(page);
      setOpen(false);
    }
  }

  async function handleMarkAllRead() {
    setItems(prev => prev.map(x => ({ ...x, isRead: true })));
    setUnreadCount(0);
    try {
      await markAllNotificationsRead();
    } catch {
      loadList();
      refreshCount();
    }
  }

  return (
    <div ref={wrapRef} className="relative">
      <button
        onClick={() => setOpen(v => !v)}
        aria-label="Notifications"
        className="relative w-10 h-10 rounded-[10px] flex items-center justify-center cursor-pointer transition-colors duration-150"
        style={{ background: open ? (dark ? "rgba(255,255,255,0.08)" : "#f2f5f7") : "transparent", border: "none" }}
        onMouseEnter={e => { if (!open) (e.currentTarget as HTMLElement).style.background = dark ? "rgba(255,255,255,0.08)" : "#f7f8fa"; }}
        onMouseLeave={e => { if (!open) (e.currentTarget as HTMLElement).style.background = "transparent"; }}
      >
        <Bell size={20} color={dark ? "#ffffff" : "#1c1f29"} />
        {unreadCount > 0 && (
          <span
            className="absolute flex items-center justify-center"
            style={{
              top: 4, right: 4, minWidth: 16, height: 16, borderRadius: 8, padding: "0 4px",
              background: "#f25959", color: "white", fontFamily: "Inter", fontWeight: 700, fontSize: 10,
              border: "2px solid white",
            }}
          >
            {unreadCount > 99 ? "99+" : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div
          className="absolute right-0 mt-2 rounded-[14px] overflow-hidden z-[100]"
          style={{ width: 360, maxWidth: "calc(100vw - 32px)", background: "white", boxShadow: "0 16px 48px rgba(0,0,0,0.18)", border: "1px solid #e5e8ed" }}
        >
          <div className="flex items-center justify-between px-4 py-3" style={{ borderBottom: "1px solid #f2f5f7" }}>
            <p style={{ fontFamily: "Inter", fontWeight: 700, fontSize: 15, color: "#1c1f29" }}>Notifications</p>
            {unreadCount > 0 && (
              <button
                onClick={handleMarkAllRead}
                style={{ background: "none", border: "none", cursor: "pointer", fontFamily: "Inter", fontWeight: 600, fontSize: 12, color: "#3b82f6" }}
              >
                Mark all read
              </button>
            )}
          </div>

          <div style={{ maxHeight: 420, overflowY: "auto" }}>
            {loading && items.length === 0 && (
              <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6", textAlign: "center", padding: "32px 0" }}>Loading…</p>
            )}
            {!loading && items.length === 0 && (
              <div className="flex flex-col items-center py-10 px-4">
                <span style={{ fontSize: 28, marginBottom: 8 }}>🔔</span>
                <p style={{ fontFamily: "Inter", fontSize: 13, color: "#9499a6" }}>You're all caught up.</p>
              </div>
            )}
            {items.map(n => {
              const page = pageForType(n.type);
              return (
                <button
                  key={n.id}
                  onClick={() => handleItemClick(n)}
                  className="w-full text-left flex gap-3 px-4 py-3 transition-colors duration-100"
                  style={{
                    border: "none", borderBottom: "1px solid #f7f8fa",
                    background: n.isRead ? "white" : "rgba(87,184,105,0.05)",
                    cursor: page ? "pointer" : "default",
                  }}
                  onMouseEnter={e => (e.currentTarget as HTMLElement).style.background = "#f7f8fa"}
                  onMouseLeave={e => (e.currentTarget as HTMLElement).style.background = n.isRead ? "white" : "rgba(87,184,105,0.05)"}
                >
                  <span style={{ fontSize: 18, lineHeight: "20px", flexShrink: 0 }}>{TYPE_ICON[n.type] || "🔔"}</span>
                  <div className="flex-1 min-w-0">
                    <p style={{ fontFamily: "Inter", fontWeight: n.isRead ? 500 : 700, fontSize: 13, color: "#1c1f29" }}>{n.title}</p>
                    <p className="line-clamp-2" style={{ fontFamily: "Inter", fontSize: 12, color: "#9499a6", marginTop: 2 }}>{n.message}</p>
                    <p style={{ fontFamily: "Inter", fontSize: 11, color: "#c3c7cf", marginTop: 4 }}>{timeAgo(n.createdAt)}</p>
                  </div>
                  {!n.isRead && (
                    <span style={{ width: 8, height: 8, borderRadius: 4, background: "#3b82f6", flexShrink: 0, marginTop: 6 }} />
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
