import { apiGet } from "./client";

export interface AdminPayment {
  id: string;
  orderId: string;
  customer: string;
  cook: string;
  amount: string;
  method: string;
  status: string;
  date: string;
  refundStatus?: string | null;
}

export interface PaymentStats {
  total_collected: number;
  pending_payouts: number;
  refunds_issued: number;
  failed_transactions: number;
}

export interface PaymentPagination {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

export interface AdminPaymentsResponse {
  success: boolean;
  stats: PaymentStats;
  payments: AdminPayment[];
  pagination: PaymentPagination;
}

export interface FetchPaymentsParams {
  page?: number;
  limit?: number;
  search?: string;
  status?: string;
}

export async function fetchAdminPayments(
  params: FetchPaymentsParams = {}
): Promise<AdminPaymentsResponse> {
  const qs = new URLSearchParams();
  if (params.page) qs.set("page", String(params.page));
  if (params.limit) qs.set("limit", String(params.limit));
  if (params.search) qs.set("search", params.search);
  if (params.status && params.status !== "all") qs.set("status", params.status);
  const query = qs.toString() ? `?${qs}` : "";
  return apiGet<AdminPaymentsResponse>(`/admin/payments${query}`);
}
