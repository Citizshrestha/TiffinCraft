import { apiGet, apiPut } from "./client";

export interface BackendNotification {
  id: number;
  user_id: number;
  title: string;
  message: string;
  type: string;
  reference_id: number | null;
  reference_type: string | null;
  is_read: number;
  created_at: string;
}

export interface AppNotification {
  id: number;
  title: string;
  message: string;
  type: string;
  referenceId: number | null;
  referenceType: string | null;
  isRead: boolean;
  createdAt: string;
}

function mapNotification(n: BackendNotification): AppNotification {
  return {
    id: n.id,
    title: n.title,
    message: n.message,
    type: n.type,
    referenceId: n.reference_id,
    referenceType: n.reference_type,
    isRead: !!n.is_read,
    createdAt: n.created_at,
  };
}

export async function fetchNotifications(): Promise<AppNotification[]> {
  const data = await apiGet<{ success: boolean; notifications: BackendNotification[] }>("/notifications");
  return data.notifications.map(mapNotification);
}

export async function fetchUnreadCount(): Promise<number> {
  const data = await apiGet<{ success: boolean; unread_count: number }>("/notifications/unread-count");
  return data.unread_count;
}

export async function markNotificationRead(id: number): Promise<void> {
  await apiPut<{ success: boolean }>(`/notifications/${id}/read`);
}

export async function markAllNotificationsRead(): Promise<void> {
  await apiPut<{ success: boolean }>("/notifications/read-all");
}

/** Relative time — "2m ago", "3h ago", "5d ago" — same granularity used elsewhere in the panel. */
export function timeAgo(iso: string): string {
  const then = new Date(iso).getTime();
  const diffSec = Math.max(0, Math.floor((Date.now() - then) / 1000));
  if (diffSec < 60) return "just now";
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  const diffDay = Math.floor(diffHr / 24);
  if (diffDay < 30) return `${diffDay}d ago`;
  return new Date(iso).toLocaleDateString("en-US", { month: "short", day: "numeric" });
}
