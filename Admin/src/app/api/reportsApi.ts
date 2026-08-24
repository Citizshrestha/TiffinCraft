import { apiGet } from "./client";

export interface ReportTotals {
  total_orders: number;
  total_revenue: number | string | null;
  avg_order_value: number | string | null;
}

export interface ReportPeriods {
  cur_orders: number | string | null;
  prev_orders: number | string | null;
  cur_revenue: number | string | null;
  prev_revenue: number | string | null;
  cur_active_users: number | string | null;
  prev_active_users: number | string | null;
}

export interface StatusCount {
  status: string;
  count: number | string;
}

export interface DayMetric {
  /** "YYYY-MM-DD" */
  date: string;
  orders: number | string;
  revenue: number | string;
}

export interface ReportsResponse {
  totals: ReportTotals;
  periods: ReportPeriods;
  statusBreakdown: StatusCount[];
  revenueByDay: DayMetric[];
}

export async function fetchReports(): Promise<ReportsResponse> {
  const data = await apiGet<{ success: boolean } & ReportsResponse>("/admin/reports");
  return {
    totals: data.totals,
    periods: data.periods,
    statusBreakdown: data.statusBreakdown || [],
    revenueByDay: data.revenueByDay || [],
  };
}
