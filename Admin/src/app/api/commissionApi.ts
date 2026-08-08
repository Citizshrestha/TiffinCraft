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
  order_count: number;
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
  orderCount: number;
  status: StatusType;
  screenshotUrl: string | null;
  adminNotes: string | null;
  verifiedBy: string | null;
  date: string;
}

const MONTH_NAMES = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];

export function mapBackendSettlement(s: BackendCommissionSettlement): CommissionSettlement {
  const displayStatus: StatusType = s.status === "pending" && s.is_overdue ? "overdue" : s.status;
  return {
    id: s.id,
    displayId: `#CS-${s.id}`,
    cookId: s.cook_id,
    cook: s.kitchen_name || s.cook_name,
    cookPhone: s.cook_phone,
    period: `${MONTH_NAMES[s.month - 1]} ${s.year}`,
    month: s.month,
    year: s.year,
    amountDue: Number(s.amount_due) || 0,
    orderCount: s.order_count,
    status: displayStatus,
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

export async function verifySettlementApi(
  id: number,
  status: "verified" | "rejected",
  adminNotes?: string
): Promise<void> {
  await apiPut<{ success: boolean; message: string }>(`/commission/settlements/${id}/verify`, {
    status,
    admin_notes: adminNotes,
  });
}

export async function generateSettlementsApi(month?: number, year?: number): Promise<{ created: number }> {
  const query = month && year ? `?month=${month}&year=${year}` : "";
  return apiPost<{ success: boolean; created: number }>(`/commission/settlements/generate${query}`);
}

export async function fetchCommissionSettings(): Promise<{ commission_pct: number; updated_at: string | null }> {
  return apiGet(`/commission/settings`);
}

export async function updateCommissionSettings(pct: number): Promise<void> {
  await apiPut(`/commission/settings`, { commission_pct: pct });
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
