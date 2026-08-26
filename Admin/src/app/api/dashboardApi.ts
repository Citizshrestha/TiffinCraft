import { apiGet } from "./client";

export interface DashboardStats {
  total_users: number;
  total_customers: number;
  total_cooks: number;
  pending_approvals: number;
  total_orders: number;
  total_revenue: number | string | null;
  pending_orders: number;
  today_orders: number;
  cook_total_orders: number;
  month_commission: number | string | null;
  all_time_commission: number | string | null;
}

export interface RecentOrder {
  id: number;
  total_amount: number | string;
  status: string;
  created_at: string;
  customer_name: string;
  kitchen_name: string | null;
}

export interface TopCook {
  user_id: number;
  kitchen_name: string | null;
  rating: number | string;
  total_orders: number;
  profile_image?: string | null;
}

export interface DailyMetric {
  date: string;
  orders: number;
  revenue: number | string; // admin commission earned that day
}

export interface DashboardResponse {
  dashboard: DashboardStats;
  recentOrders: RecentOrder[];
  topCooks: TopCook[];
  last7Days: DailyMetric[];
}

export async function fetchDashboard(): Promise<DashboardResponse> {
  const data = await apiGet<{ success: boolean } & DashboardResponse>("/admin/dashboard");
  return {
    dashboard: data.dashboard,
    recentOrders: data.recentOrders || [],
    topCooks: data.topCooks || [],
    last7Days: data.last7Days || [],
  };
}
