import { apiGet, apiPut } from "./client";
import { formatJoinedDate } from "../utils/format";
import { StatusType } from "../components/StatusBadge";

export interface BackendRefundRequest {
  id: number;
  order_id: number;
  payment_id: number | null;
  requested_by: number;
  reason: "failed_delivery" | "cook_mistake" | "customer_cancelled" | "other";
  reason_notes: string | null;
  refund_amount: number | string;
  status: "requested" | "under_review" | "approved" | "rejected" | "processed";
  admin_notes: string | null;
  processed_by: number | null;
  processed_at: string | null;
  created_at: string;
  order_total_amount: number | string;
  order_status: string;
  customer_name: string;
  customer_phone: string;
  cook_name: string;
  kitchen_name: string | null;
  requested_by_name: string;
  requested_by_role: string;
  processed_by_name: string | null;
}

export interface RefundRequest {
  id: number;
  displayId: string;
  orderId: number;
  orderDisplayId: string;
  customer: string;
  cook: string;
  requestedBy: string;
  requestedByRole: string;
  reason: string;
  reasonNotes: string | null;
  amount: number;
  status: StatusType;
  adminNotes: string | null;
  processedBy: string | null;
  date: string;
}

const REASON_LABELS: Record<string, string> = {
  failed_delivery: "Failed delivery",
  cook_mistake: "Cook made a mistake",
  customer_cancelled: "Customer cancelled",
  other: "Other",
};

export function mapBackendRefund(r: BackendRefundRequest): RefundRequest {
  return {
    id: r.id,
    displayId: `#REF-${r.id}`,
    orderId: r.order_id,
    orderDisplayId: `#ORD-${r.order_id}`,
    customer: r.customer_name,
    cook: r.kitchen_name || r.cook_name,
    requestedBy: r.requested_by_name,
    requestedByRole: r.requested_by_role,
    reason: REASON_LABELS[r.reason] || r.reason,
    reasonNotes: r.reason_notes,
    amount: Number(r.refund_amount) || 0,
    status: r.status as StatusType,
    adminNotes: r.admin_notes,
    processedBy: r.processed_by_name,
    date: formatJoinedDate(r.created_at),
  };
}

export async function fetchRefunds(status?: string): Promise<RefundRequest[]> {
  const query = status && status !== "all" ? `?status=${encodeURIComponent(status)}` : "";
  const data = await apiGet<{ success: boolean; refunds: BackendRefundRequest[] }>(`/refunds${query}`);
  return data.refunds.map(mapBackendRefund);
}

export async function processRefundApi(
  id: number,
  status: "under_review" | "approved" | "rejected" | "processed",
  adminNotes?: string
): Promise<void> {
  await apiPut<{ success: boolean; message: string }>(`/refunds/${id}/process`, {
    status,
    admin_notes: adminNotes,
  });
}
