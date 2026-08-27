import { apiGet, apiPost, apiPut, ApiError, TOKEN_KEY } from "./client";
import { formatJoinedDate } from "../utils/format";
import { StatusType } from "../components/StatusBadge";

const API_BASE_URL: string =
  (import.meta as any).env?.VITE_API_BASE_URL || "http://localhost:5000/api";

export interface BankDetails {
  esewa_qr_url?: string | null;
  khalti_qr_url?: string | null;
  bank_qr_url?: string | null;
}

export interface BackendCommissionSettlement {
  id: number;
  cook_id: number;
  month: number;
  year: number;
  amount_due: number | string;
  /** Total received so far, accumulated across installments (EC3). Defaults to 0. */
  amount_paid: number | string | null;
  order_count: number;
  /** yyyy-MM-dd calendar day (1st of the month after the period + 15-day grace). Null on legacy rows. */
  due_date: string | null;
  status: "pending" | "submitted" | "verified" | "rejected";
  payment_screenshot_url: string | null;
  submitted_at: string | null;
  verified_at: string | null;
  admin_notes: string | null;
  created_at: string;
  cook_name: string;
  cook_phone: string;
  kitchen_name: string | null;
  verified_by_name: string | null;
  is_overdue: boolean;
  /** True when the cook deleted their account — the settlement row deliberately outlives them (EC6). */
  cook_deleted: boolean;
}

export interface CommissionSettlement {
  id: number;
  displayId: string;
  cookId: number;
  cook: string;
  cookPhone: string;
  period: string;
  month: number;
  year: number;
  amountDue: number;
  /** Received so far. Less than amountDue on a part-paid settlement (EC3). */
  amountPaid: number;
  /** amountDue - amountPaid, floored at 0. */
  amountRemaining: number;
  orderCount: number;
  status: StatusType;
  /** Backend status, unmapped — "overdue" is a display-only state, the row is still `pending`. */
  rawStatus: "pending" | "submitted" | "verified" | "rejected";
  /** yyyy-MM-dd, or null on rows generated before due_date existed. */
  dueDate: string | null;
  isOverdue: boolean;
  cookDeleted: boolean;
  screenshotUrl: string | null;
  adminNotes: string | null;
  verifiedBy: string | null;
  date: string;
}

const MONTH_NAMES = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

export function mapBackendSettlement(s: BackendCommissionSettlement): CommissionSettlement {
  const displayStatus: StatusType = s.status === "pending" && s.is_overdue ? "overdue" : s.status;
  const amountDue = Number(s.amount_due) || 0;
  const amountPaid = Number(s.amount_paid) || 0;
  return {
    id: s.id,
    displayId: `#CS-${s.id}`,
    cookId: s.cook_id,
    cook: s.kitchen_name || s.cook_name,
    cookPhone: s.cook_phone,
    period: `${MONTH_NAMES[s.month - 1]} ${s.year}`,
    month: s.month,
    year: s.year,
    amountDue,
    amountPaid,
    amountRemaining: Math.max(0, Math.round((amountDue - amountPaid) * 100) / 100),
    orderCount: s.order_count,
    status: displayStatus,
    rawStatus: s.status,
    dueDate: s.due_date ? s.due_date.slice(0, 10) : null,
    isOverdue: !!s.is_overdue,
    cookDeleted: !!s.cook_deleted,
    screenshotUrl: s.payment_screenshot_url,
    adminNotes: s.admin_notes,
    verifiedBy: s.verified_by_name,
    date: formatJoinedDate(s.created_at),
  };
}

export async function fetchSettlements(status?: string): Promise<CommissionSettlement[]> {
  const query = status && status !== "all" ? `?status=${encodeURIComponent(status)}` : "";
  const data = await apiGet<{ success: boolean; settlements: BackendCommissionSettlement[] }>(
    `/commission/settlements${query}`
  );
  return data.settlements.map(mapBackendSettlement);
}

/**
 * EC3: `amountPaid` is optional — omitting it means "paid in full", which is
 * what the old two-argument callers relied on. Supplying less than the
 * outstanding balance banks a part payment and the backend deliberately leaves
 * the settlement `pending`, so `status` in the response may not be what was
 * requested. Callers should read it rather than assume.
 */
export async function verifySettlementApi(
  id: number,
  status: "verified" | "rejected",
  adminNotes?: string,
  amountPaid?: number
): Promise<{ status: string; amount_paid: string; amount_remaining: string; message: string }> {
  return apiPut<{ success: boolean; message: string; status: string; amount_paid: string; amount_remaining: string }>(
    `/commission/settlements/${id}/verify`,
    {
      status,
      admin_notes: adminNotes,
      amount_paid: amountPaid,
    }
  );
}

export async function generateSettlementsApi(month?: number, year?: number): Promise<{ created: number }> {
  const query = month && year ? `?month=${month}&year=${year}` : "";
  return apiPost<{ success: boolean; created: number }>(`/commission/settlements/generate${query}`);
}

export async function fetchCommissionSettings(): Promise<{ commission_pct: number; updated_at: string | null }> {
  return apiGet(`/commission/settings`);
}

export async function updateCommissionSettings(pct: number, changeReason?: string): Promise<{
  old_rate: number;
  new_rate: number;
  notified_cooks: number;
  chats_sent: number;
  no_change?: boolean;
}> {
  return apiPut(`/commission/settings`, { 
    commission_pct: pct,
    change_reason: changeReason 
  });
}

export interface CommissionRateHistory {
  id: number;
  old_rate: number;
  new_rate: number;
  changed_by: number;
  change_reason: string | null;
  affected_cooks_count: number;
  created_at: string;
  admin_name: string | null;
}

export async function fetchCommissionRateHistory(limit?: number): Promise<CommissionRateHistory[]> {
  const params = limit ? `?limit=${limit}` : "";
  const data = await apiGet<{ success: boolean; history: CommissionRateHistory[] }>(`/commission/rate-history${params}`);
  return data.history;
}

export interface CommissionTrendPoint {
  month: string;
  year: number;
  commission: number;
  cookNet: number;
}

export interface CommissionSummary {
  month: number;
  year: number;
  commission_pct: number;
  total_commission: number;
  all_time_commission: number;
  total_gross: number;
  order_count: number;
  pending_commission: number;
  pending_order_count: number;
  by_cook: {
    cook_id: number;
    owner_name: string;
    kitchen_name: string | null;
    order_count: number;
    gross_total: number;
    commission_total: number;
  }[];
  trend: CommissionTrendPoint[];
}

export async function fetchCommissionSummary(month?: number, year?: number): Promise<CommissionSummary> {
  const params = new URLSearchParams();
  if (month) params.set("month", String(month));
  if (year) params.set("year", String(year));
  const qs = params.toString();
  return apiGet(`/commission/summary${qs ? `?${qs}` : ""}`);
}

export async function fetchAdminQr(): Promise<BankDetails | null> {
  const data = await apiGet<{ success: boolean; bank_details: BankDetails | null }>(`/commission/admin-qr`);
  return data.bank_details;
}

export async function updateAdminQr(bankDetails: BankDetails): Promise<void> {
  await apiPut(`/commission/admin-qr`, { bank_details: bankDetails });
}

/**
 * Uploads a QR image file for the admin's platform payment QR. A raw
 * multipart fetch — the shared apiPost/apiPut client always sends JSON, so
 * file uploads bypass it here (same reasoning as the Android app's
 * multipart QR-upload calls).
 */
export async function uploadAdminQrImage(file: File, qrType: "esewa" | "khalti" | "bank"): Promise<string> {
  const token = localStorage.getItem(TOKEN_KEY);
  const form = new FormData();
  form.append("document", file);
  form.append("qrType", qrType);

  const res = await fetch(`${API_BASE_URL}/upload/bank-qr`, {
    method: "POST",
    headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    body: form,
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(data?.message || "Failed to upload QR image.", res.status, data);
  }
  return data.data.url as string;
}
