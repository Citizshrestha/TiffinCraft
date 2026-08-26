import React, { useEffect, useState, useCallback } from "react";
import { StatusBadge, StatusType } from "./StatusBadge";
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  BarChart,
  Bar,
  Cell,
} from "recharts";
import { fetchDashboard, DashboardResponse } from "../api/dashboardApi";

/* ─── Formatters ──────────────────────────────────────────────── */
function formatCurrency(n: number | string | null | undefined): string {
  const num = Number(n) || 0;
  if (num >= 100000)
    return `₹${(num / 100000).toFixed(1)}L`;
  if (num >= 1000)
    return `₹${(num / 1000).toFixed(1)}K`;
  return `₹${num.toLocaleString("en-IN")}`;
}

function formatCurrencyFull(n: number | string | null | undefined): string {
  const num = Number(n) || 0;
  return `₹${num.toLocaleString("en-IN")}`;
}

function formatNumber(n: number | string | null | undefined): string {
  return (Number(n) || 0).toLocaleString("en-IN");
}

function formatChartLabel(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

function pctChange(values: number[]): { text: string; up: boolean } {
  const nonZero = values.filter((v) => v > 0);
  if (nonZero.length < 2) return { text: "—", up: true };
  const first = values[0];
  const last = values[values.length - 1];
  if (!first) return { text: last > 0 ? "+100%" : "0%", up: last >= 0 };
  const pct = ((last - first) / first) * 100;
  return {
    text: `${pct >= 0 ? "+" : ""}${pct.toFixed(1)}%`,
    up: pct >= 0,
  };
}

/* ─── Build full 7-day skeleton so chart always shows 7 points ── */
function buildChartData(
  last7Days: { date: string; orders: number; revenue: number | string }[]
): { day: string; orders: number; revenue: number }[] {
  const result: { day: string; fullDate: string; orders: number; revenue: number }[] = [];

  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const iso = d.toISOString().slice(0, 10); // YYYY-MM-DD
    result.push({
      day: formatChartLabel(iso),
      fullDate: iso,
      orders: 0,
      revenue: 0,
    });
  }

  // Merge real data
  for (const item of last7Days) {
    const iso = item.date.slice(0, 10);
    const slot = result.find((r) => r.fullDate === iso);
    if (slot) {
      slot.orders = Number(item.orders) || 0;
      slot.revenue = Number(item.revenue) || 0;
    }
  }

  return result.map(({ day, orders, revenue }) => ({ day, orders, revenue }));
}

/* ─── KPI Card ─────────────────────────────────────────────────── */
function KpiCard({
  label,
  value,
  sub,
  icon,
  accent,
}: {
  label: string;
  value: string;
  sub: string;
  icon: string;
  accent: string;
}) {
  return (
    <div
      className="bg-white flex flex-col gap-3 p-5 rounded-[14px] flex-1"
      style={{ boxShadow: "0px 2px 12px rgba(0,0,0,0.07)" }}
    >
      <div className="flex items-center justify-between">
        <p
          style={{
            fontFamily: "Inter",
            fontWeight: 500,
            fontSize: 13,
            color: "#9499a6",
          }}
        >
          {label}
        </p>
        <div
          style={{
            width: 36,
            height: 36,
            borderRadius: 10,
            background: accent,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            fontSize: 16,
          }}
        >
          {icon}
        </div>
      </div>
      <p
        style={{
          fontFamily: "Inter",
          fontWeight: 700,
          fontSize: 28,
          color: "#1c1f29",
          lineHeight: 1,
        }}
      >
        {value}
      </p>
      <p
        style={{
          fontFamily: "Inter",
          fontWeight: 500,
          fontSize: 12,
          color: "#10b981",
        }}
      >
        {sub}
      </p>
    </div>
  );
}

/* ─── Custom Tooltip ───────────────────────────────────────────── */
function CustomTooltip({
  active,
  payload,
  label,
  isCurrency,
}: {
  active?: boolean;
  payload?: any[];
  label?: string;
  isCurrency?: boolean;
}) {
  if (!active || !payload || !payload.length) return null;
  const val = payload[0]?.value ?? 0;
  const color = payload[0]?.color ?? "#7887FA";
  return (
    <div
      style={{
        background: "white",
        border: "1px solid #e5e8ed",
        borderRadius: 10,
        padding: "10px 14px",
        boxShadow: "0 4px 16px rgba(0,0,0,0.1)",
      }}
    >
      <p
        style={{
          fontFamily: "Inter",
          fontSize: 11,
          color: "#9499a6",
          marginBottom: 4,
        }}
      >
        {label}
      </p>
      <p
        style={{
          fontFamily: "Inter",
          fontWeight: 700,
          fontSize: 15,
          color,
        }}
      >
        {isCurrency ? formatCurrencyFull(val) : formatNumber(val)}
      </p>
    </div>
  );
}

/* ─── Area Chart Card ──────────────────────────────────────────── */
function AreaChartCard({
  title,
  value,
  change,
  data,
  color,
  gradientId,
  dataKey,
  isCurrency,
}: {
  title: string;
  value: string;
  change: { text: string; up: boolean };
  data: Record<string, string | number>[];
  color: string;
  gradientId: string;
  dataKey: string;
  isCurrency?: boolean;
}) {
  return (
    <div
      className="bg-white flex flex-col gap-4 p-5 sm:p-6 rounded-[14px] lg:flex-1 min-w-0"
      style={{ boxShadow: "0px 2px 12px rgba(0,0,0,0.07)" }}
    >
      <div className="flex items-start justify-between">
        <div>
          <p
            style={{
              fontFamily: "Inter",
              fontWeight: 500,
              fontSize: 13,
              color: "#9499a6",
              marginBottom: 6,
            }}
          >
            {title}
          </p>
          <p
            style={{
              fontFamily: "Inter",
              fontWeight: 700,
              fontSize: 30,
              color: "#1c1f29",
              lineHeight: 1,
            }}
          >
            {value}
          </p>
        </div>
        <span
          style={{
            display: "inline-flex",
            alignItems: "center",
            gap: 3,
            padding: "4px 10px",
            borderRadius: 20,
            background: change.up
              ? "rgba(16,185,129,0.1)"
              : "rgba(239,68,68,0.1)",
            color: change.up ? "#059669" : "#dc2626",
            fontFamily: "Inter",
            fontWeight: 600,
            fontSize: 12,
          }}
        >
          {change.up ? "↑" : "↓"} {change.text}
        </span>
      </div>

      <div className="h-[180px]">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart
            data={data}
            margin={{ top: 8, right: 8, left: -20, bottom: 0 }}
          >
            <defs>
              <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor={color} stopOpacity={0.2} />
                <stop offset="100%" stopColor={color} stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid
              strokeDasharray="3 3"
              stroke="#f0f2f5"
              vertical={false}
            />
            <XAxis
              dataKey="day"
              tick={{
                fontSize: 11,
                fill: "#b2b8bf",
                fontFamily: "Inter",
              }}
              axisLine={false}
              tickLine={false}
            />
            <YAxis
              tick={{ fontSize: 11, fill: "#b2b8bf", fontFamily: "Inter" }}
              axisLine={false}
              tickLine={false}
              tickFormatter={isCurrency ? (v) => formatCurrency(v) : undefined}
            />
            <Tooltip
              content={
                <CustomTooltip isCurrency={isCurrency} />
              }
            />
            <Area
              type="monotone"
              dataKey={dataKey}
              stroke={color}
              strokeWidth={2.5}
              fill={`url(#${gradientId})`}
              dot={{ r: 3, fill: "white", stroke: color, strokeWidth: 2 }}
              activeDot={{ r: 5, fill: color, stroke: "white", strokeWidth: 2 }}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}

/* ─── Rating Bar Chart ─────────────────────────────────────────── */
function RatingBarChartCard({ topCooks }: { topCooks: { kitchen_name: string | null; rating: number | string; total_orders: number; profile_image?: string | null }[] }) {
  const data = topCooks.map((c) => ({
    name: (c.kitchen_name || "—").slice(0, 14),
    rating: parseFloat(String(c.rating)) || 0,
    orders: c.total_orders,
  }));

  const COLORS = ["#7887FA", "#57B869", "#f97316", "#a855f7", "#ec4899"];

  return (
    <div
      className="bg-white flex flex-col gap-4 p-5 sm:p-6 rounded-[14px] xl:flex-1 min-w-0"
      style={{ boxShadow: "0px 2px 12px rgba(0,0,0,0.07)" }}
    >
      <div className="flex items-center justify-between">
        <div>
          <p
            style={{
              fontFamily: "Inter",
              fontWeight: 600,
              fontSize: 16,
              color: "#1c1f29",
            }}
          >
            Top Performing Cooks
          </p>
          <p style={{ fontFamily: "Inter", fontSize: 12, color: "#9499a6" }}>
            By rating score
          </p>
        </div>
      </div>
      {data.length === 0 ? (
        <p
          style={{
            fontFamily: "Inter",
            fontSize: 13,
            color: "#9499a6",
            textAlign: "center",
            padding: "32px 0",
          }}
        >
          No cooks yet.
        </p>
      ) : (
        <div className="h-[180px]">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={data}
              margin={{ top: 4, right: 8, left: -20, bottom: 0 }}
              barCategoryGap="30%"
            >
              <CartesianGrid
                strokeDasharray="3 3"
                stroke="#f0f2f5"
                vertical={false}
              />
              <XAxis
                dataKey="name"
                tick={{ fontSize: 11, fill: "#b2b8bf", fontFamily: "Inter" }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                domain={[0, 5]}
                ticks={[0, 1, 2, 3, 4, 5]}
                tick={{ fontSize: 11, fill: "#b2b8bf", fontFamily: "Inter" }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                cursor={false}
                content={({ active, payload, label }) => {
                  if (!active || !payload?.length) return null;
                  return (
                    <div
                      style={{
                        background: "white",
                        border: "1px solid #e5e8ed",
                        borderRadius: 10,
                        padding: "10px 14px",
                        boxShadow: "0 4px 16px rgba(0,0,0,0.1)",
                      }}
                    >
                      <p style={{ fontFamily: "Inter", fontWeight: 600, fontSize: 13, color: "#1c1f29", marginBottom: 4 }}>
                        {label}
                      </p>
                      <p style={{ fontFamily: "Inter", fontSize: 12, color: "#f59e0b" }}>
                        ⭐ {payload[0]?.value?.toFixed(1)} rating
                      </p>
                      <p style={{ fontFamily: "Inter", fontSize: 12, color: "#9499a6" }}>
                        {formatNumber(payload[0]?.payload?.orders)} orders
                      </p>
                    </div>
                  );
                }}
              />
              <Bar dataKey="rating" radius={[6, 6, 0, 0]}>
                {data.map((_, index) => (
                  <Cell key={index} fill={COLORS[index % COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Legend */}
      <div className="flex flex-col gap-2">
        {data.map((cook, i) => (
          <div key={i} className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div
                style={{
                  width: 8,
                  height: 8,
                  borderRadius: "50%",
                  background: COLORS[i % COLORS.length],
                  flexShrink: 0,
                }}
              />
              <p
                style={{
                  fontFamily: "Inter",
                  fontSize: 13,
                  color: "#1c1f29",
                  fontWeight: 500,
                }}
              >
                {cook.name}
              </p>
            </div>
            <div className="flex items-center gap-3">
              <p style={{ fontFamily: "Inter", fontSize: 12, color: "#9499a6" }}>
                {formatNumber(cook.orders)} orders
              </p>
              <span
                style={{
                  padding: "2px 8px",
                  borderRadius: 20,
                  background: "#fff7ed",
                  fontFamily: "Inter",
                  fontWeight: 600,
                  fontSize: 12,
                  color: "#f59e0b",
                }}
              >
                ⭐ {cook.rating.toFixed(1)}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ─── Main Dashboard ───────────────────────────────────────────── */
export function DashboardPage({
  onNavigate,
}: {
  onNavigate: (page: string) => void;
}) {
  const [data, setData] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const res = await fetchDashboard();
      setData(res);
    } catch (err) {
      setLoadError(
        err instanceof Error ? err.message : "Failed to load dashboard."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (loading) {
    return (
      <div className="flex flex-col gap-6 animate-pulse">
        {/* KPI skeleton */}
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
          {[...Array(3)].map((_, i) => (
            <div
              key={i}
              className="bg-white rounded-[14px] p-5"
              style={{
                boxShadow: "0px 2px 12px rgba(0,0,0,0.07)",
                height: 120,
              }}
            >
              <div
                style={{
                  height: 12,
                  borderRadius: 6,
                  background: "#f0f2f5",
                  width: "60%",
                  marginBottom: 12,
                }}
              />
              <div
                style={{
                  height: 28,
                  borderRadius: 6,
                  background: "#f0f2f5",
                  width: "80%",
                }}
              />
            </div>
          ))}
        </div>
        {/* Chart skeletons */}
        <div className="flex flex-col lg:flex-row gap-4">
          {[...Array(2)].map((_, i) => (
            <div
              key={i}
              className="bg-white rounded-[14px] p-5 lg:flex-1"
              style={{
                boxShadow: "0px 2px 12px rgba(0,0,0,0.07)",
                height: 280,
              }}
            />
          ))}
        </div>
      </div>
    );
  }

  if (loadError || !data) {
    return (
      <div
        className="flex flex-col gap-3 p-6 rounded-[14px] bg-white"
        style={{ boxShadow: "0px 2px 12px rgba(0,0,0,0.07)" }}
      >
        <p
          style={{
            fontFamily: "Inter",
            fontSize: 14,
            color: "#dc2626",
          }}
        >
          ⚠️ {loadError || "Failed to load dashboard."}
        </p>
        <button
          onClick={load}
          style={{
            width: 120,
            background: "#f97316",
            border: "none",
            fontFamily: "Inter",
            fontWeight: 600,
            color: "white",
            fontSize: 13,
            padding: "10px 16px",
            borderRadius: 8,
            cursor: "pointer",
          }}
        >
          Retry
        </button>
      </div>
    );
  }

  const { dashboard, recentOrders, topCooks, last7Days } = data;
  const chartData = buildChartData(last7Days);
  const ordersTotal = chartData.reduce((s, d) => s + d.orders, 0);
  const revenueTotal = chartData.reduce((s, d) => s + d.revenue, 0);
  const ordersChange = pctChange(chartData.map((d) => d.orders));
  const revenueChange = pctChange(chartData.map((d) => d.revenue));

  return (
    <div className="flex flex-col gap-6">
      {/* ── KPI Cards ──────────────────────────────────────────── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        <KpiCard
          label="Total Users"
          value={formatNumber(dashboard.total_users)}
          sub={`${formatNumber(dashboard.total_customers)} customers`}
          icon="👥"
          accent="#eff6ff"
        />
        <KpiCard
          label="Total Orders"
          value={formatNumber(dashboard.total_orders)}
          sub={`${formatNumber(dashboard.today_orders)} today • ${formatNumber(dashboard.pending_orders)} pending`}
          icon="📦"
          accent="#fff7ed"
        />
        <KpiCard
          label="Total Revenue"
          value={formatCurrencyFull(dashboard.month_commission)}
          sub={`this month • ${formatCurrencyFull(dashboard.all_time_commission)} all time`}
          icon="💰"
          accent="#fdf4ff"
        />
      </div>

      {/* ── Charts ─────────────────────────────────────────────── */}
      <div className="flex flex-col lg:flex-row gap-4">
        <AreaChartCard
          title="Orders (Last 7 Days)"
          value={formatNumber(ordersTotal)}
          change={ordersChange}
          data={chartData}
          color="#7887FA"
          gradientId="ordersGrad"
          dataKey="orders"
        />
        <AreaChartCard
          title="Revenue (Last 7 Days)"
          value={formatCurrencyFull(revenueTotal)}
          change={revenueChange}
          data={chartData}
          color="#57B869"
          gradientId="revenueGrad"
          dataKey="revenue"
          isCurrency
        />
      </div>

      {/* ── Bottom Section ─────────────────────────────────────── */}
      <div className="flex flex-col xl:flex-row gap-4">
        {/* Recent Orders */}
        <div
          className="bg-white flex flex-col p-5 sm:p-6 rounded-[14px] xl:flex-[1.7] min-w-0"
          style={{ boxShadow: "0px 2px 12px rgba(0,0,0,0.07)" }}
        >
          <div className="flex items-center justify-between mb-5">
            <div>
              <p
                style={{
                  fontFamily: "Inter",
                  fontWeight: 600,
                  fontSize: 16,
                  color: "#1c1f29",
                }}
              >
                Recent Orders
              </p>
              <p
                style={{
                  fontFamily: "Inter",
                  fontSize: 12,
                  color: "#9499a6",
                }}
              >
                Latest 5 transactions
              </p>
            </div>
            <button
              onClick={() => onNavigate("orders")}
              style={{
                fontFamily: "Inter",
                fontWeight: 500,
                fontSize: 13,
                color: "#7887fa",
                background: "none",
                border: "1px solid #e0e7ff",
                borderRadius: 8,
                padding: "6px 12px",
                cursor: "pointer",
              }}
              onMouseEnter={(e) =>
                ((e.currentTarget as HTMLElement).style.background = "#eff6ff")
              }
              onMouseLeave={(e) =>
                ((e.currentTarget as HTMLElement).style.background = "none")
              }
            >
              View All →
            </button>
          </div>

          <div className="overflow-x-auto">
            <div className="min-w-[620px]">
              <div
                className="flex gap-3 pb-3"
                style={{ borderBottom: "1px solid #f0f2f5" }}
              >
                {["Order ID", "Customer", "Cook", "Amount", "Status"].map(
                  (h, i) => (
                    <p
                      key={h}
                      style={{
                        fontFamily: "Inter",
                        fontWeight: 600,
                        fontSize: 11,
                        color: "#b2b8bf",
                        width: [100, 140, 130, 100, 120][i],
                        flexShrink: 0,
                        textTransform: "uppercase",
                        letterSpacing: "0.05em",
                      }}
                    >
                      {h}
                    </p>
                  )
                )}
              </div>

              {recentOrders.length === 0 && (
                <p
                  style={{
                    fontFamily: "Inter",
                    fontSize: 13,
                    color: "#9499a6",
                    textAlign: "center",
                    padding: "24px 0",
                  }}
                >
                  No orders yet.
                </p>
              )}

              {recentOrders.map((order, idx) => (
                <div key={order.id}>
                  <div
                    className="flex gap-3 py-3 items-center hover:bg-[#f7f8fa] cursor-pointer -mx-2 px-2 rounded transition-colors"
                    onClick={() => onNavigate("orders")}
                  >
                    <p
                      style={{
                        fontFamily: "Inter",
                        fontWeight: 500,
                        fontSize: 13,
                        color: "#7887fa",
                        width: 100,
                        flexShrink: 0,
                      }}
                    >
                      #ORD-{order.id}
                    </p>
                    <p
                      style={{
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#1c1f29",
                        width: 140,
                        flexShrink: 0,
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {order.customer_name}
                    </p>
                    <p
                      style={{
                        fontFamily: "Inter",
                        fontSize: 13,
                        color: "#6b7280",
                        width: 130,
                        flexShrink: 0,
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {order.kitchen_name || "—"}
                    </p>
                    <p
                      style={{
                        fontFamily: "Inter",
                        fontWeight: 600,
                        fontSize: 13,
                        color: "#1c1f29",
                        width: 100,
                        flexShrink: 0,
                      }}
                    >
                      {formatCurrencyFull(order.total_amount)}
                    </p>
                    <div style={{ width: 120, flexShrink: 0 }}>
                      <StatusBadge status={order.status as StatusType} />
                    </div>
                  </div>
                  {idx < recentOrders.length - 1 && (
                    <div style={{ height: 1, background: "#f5f7fa" }} />
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Top Cooks Bar Chart */}
        <RatingBarChartCard topCooks={topCooks} />
      </div>
    </div>
  );
}
