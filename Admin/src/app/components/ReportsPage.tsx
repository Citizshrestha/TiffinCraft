import React, { useCallback, useEffect, useState } from "react";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  Cell, PieChart, Pie,
} from "recharts";
import { exportCSV } from "../utils/csv";
import { fetchReports, ReportsResponse } from "../api/reportsApi";

const num = (v: unknown): number => Number(v) || 0;

function formatCurrency(v: unknown): string {
  return `₹${num(v).toLocaleString("en-IN", { maximumFractionDigits: 0 })}`;
}

/** ₹1.45L / ₹2.3Cr for the metric cards, full number below a lakh. */
function formatCompactCurrency(v: unknown): string {
  const n = num(v);
  if (n >= 1e7) return `₹${(n / 1e7).toFixed(2)}Cr`;
  if (n >= 1e5) return `₹${(n / 1e5).toFixed(2)}L`;
  return formatCurrency(n);
}

function pctChange(prev: unknown, cur: unknown): string {
  const p = num(prev), c = num(cur);
  if (!p) return c > 0 ? "+100%" : "0%";
  const pct = ((c - p) / p) * 100;
  return `${pct >= 0 ? "+" : ""}${pct.toFixed(1)}%`;
}

/** The 7 order statuses collapsed into the four buckets the chart shows. */
const STATUS_BUCKETS = [
  { name: "Delivered",  color: "#7887FA", statuses: ["delivered", "completed"] },
  { name: "Processing", color: "#57B869", statuses: ["confirmed", "preparing", "ready"] },
  { name: "Pending",    color: "#F28C40", statuses: ["pending"] },
  { name: "Cancelled",  color: "#F2C740", statuses: ["cancelled"] },
];

function buildBreakdown(rows: ReportsResponse["statusBreakdown"]) {
  const counts = new Map(rows.map(r => [String(r.status).toLowerCase(), num(r.count)]));
  const buckets = STATUS_BUCKETS.map(b => ({
    name: b.name,
    color: b.color,
    value: b.statuses.reduce((sum, s) => sum + (counts.get(s) ?? 0), 0),
  }));

  // Anything outside the known enum still gets counted rather than silently dropped.
  const known = new Set(STATUS_BUCKETS.flatMap(b => b.statuses));
  const other = rows
    .filter(r => !known.has(String(r.status).toLowerCase()))
    .reduce((sum, r) => sum + num(r.count), 0);
  if (other > 0) buckets.push({ name: "Other", color: "#9499A6", value: other });

  const total = buckets.reduce((s, b) => s + b.value, 0);
  return buckets.map(b => ({
    ...b,
    pct: total ? Math.round((b.value / total) * 100) : 0,
  }));
}

/** Last 7 calendar days, zero-filled, labelled Mon…Sun. */
function buildRevenueByDay(rows: ReportsResponse["revenueByDay"]) {
  const byDate = new Map(rows.map(r => [r.date, r]));
  const out: { day: string; date: string; value: number; orders: number }[] = [];
  const today = new Date();

  for (let i = 6; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(today.getDate() - i);
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    const row = byDate.get(key);
    out.push({
      day: d.toLocaleDateString("en-US", { weekday: "short" }),
      date: key,
      value: num(row?.revenue),
      orders: num(row?.orders),
    });
  }
  return out;
}

function MetricCard({ icon, value, label, change, changeColor }: {
  icon:string; value:string; label:string; change:string; changeColor:string;
}) {
  return (
    <div className="bg-white flex flex-col gap-2 p-5 rounded-[12px] flex-1" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
      <div className="flex items-start justify-between">
        <p className="text-[24px]">{icon}</p>
        <p style={{fontFamily:"Inter",fontWeight:600,fontSize:12,color:changeColor}}>{change}</p>
      </div>
      <p style={{fontFamily:"Inter",fontWeight:700,fontSize:32,color:"#1c1f29"}}>{value}</p>
      <p style={{fontFamily:"Inter",fontWeight:400,fontSize:13,color:"#9499a6"}}>{label}</p>
    </div>
  );
}

export function ReportsPage() {
  const [data, setData] = useState<ReportsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      setData(await fetchReports());
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load reports.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) {
    return <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6"}}>Loading reports...</p>;
  }

  if (loadError || !data) {
    return (
      <div className="flex flex-col gap-6">
        <p style={{fontFamily:"Inter",fontSize:14,color:"#f25959"}}>{loadError || "Failed to load reports."}</p>
        <button onClick={load} style={{width:160,background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:13,padding:"10px 16px",borderRadius:8,cursor:"pointer"}}>Retry</button>
      </div>
    );
  }

  const { totals, periods } = data;
  const orderBreakdown = buildBreakdown(data.statusBreakdown);
  const revenueByDay = buildRevenueByDay(data.revenueByDay);
  const hasOrders = num(totals.total_orders) > 0;

  const handleExport = () =>
    exportCSV("reports-revenue.csv", revenueByDay.map(d => ({
      Date: d.date,
      Day: d.day,
      Orders: d.orders,
      Revenue: formatCurrency(d.value),
    })));

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Reports & Analytics</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>View detailed analytics and performance reports.</p>
        </div>
        <button onClick={handleExport}
          className="flex items-center gap-2 px-5 py-3 rounded-[8px] text-white text-[14px] cursor-pointer transition-all duration-150 hover:brightness-95 self-start shrink-0"
          style={{background:"#57b869",fontFamily:"Inter",fontWeight:600,border:"none"}}>
          📊 Export Report
        </button>
      </div>

      {/* Key Metrics — totals are all-time; the badge compares the last 30 days with the 30 before. */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <MetricCard icon="📦" value={num(totals.total_orders).toLocaleString("en-IN")} label="Total Orders"
          change={pctChange(periods.prev_orders, periods.cur_orders)} changeColor="#7887fa"/>
        <MetricCard icon="💰" value={formatCompactCurrency(totals.total_revenue)} label="Total Revenue"
          change={pctChange(periods.prev_revenue, periods.cur_revenue)} changeColor="#57b869"/>
        <MetricCard icon="👥" value={num(periods.cur_active_users).toLocaleString("en-IN")} label="Active Users (30d)"
          change={pctChange(periods.prev_active_users, periods.cur_active_users)} changeColor="#f28c40"/>
        <MetricCard icon="💳" value={formatCurrency(totals.avg_order_value)} label="Avg Order Value"
          change={pctChange(
            num(periods.prev_orders) ? num(periods.prev_revenue) / num(periods.prev_orders) : 0,
            num(periods.cur_orders) ? num(periods.cur_revenue) / num(periods.cur_orders) : 0,
          )} changeColor="#f2c740"/>
      </div>

      {/* Charts */}
      <div className="flex flex-col lg:flex-row gap-4">
        {/* Orders Breakdown */}
        <div className="bg-white flex flex-col gap-4 p-4 sm:p-6 rounded-[12px] w-full lg:w-[530px] lg:shrink-0 min-w-0" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Orders Breakdown</p>
          <div className="h-[200px]">
            {hasOrders ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie key="orders-pie" data={orderBreakdown} cx="40%" cy="50%" outerRadius={90} innerRadius={40} dataKey="value" strokeWidth={2}>
                    {orderBreakdown.map((entry,index)=>(
                      <Cell key={`cell-${entry.name}-${index}`} fill={entry.color} fillOpacity={0.85}/>
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{background:"white",border:"1px solid #e5e8ed",borderRadius:8,fontSize:12,fontFamily:"Inter"}}
                    formatter={(v:number)=>[v.toLocaleString(),"Orders"]}/>
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <p style={{fontFamily:"Inter",fontSize:12,color:"#b2b8bf"}}>No orders yet.</p>
              </div>
            )}
          </div>
          <div className="flex flex-col gap-3">
            {orderBreakdown.map(item=>(
              <div key={item.name} className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="w-3 h-3 rounded-full shrink-0" style={{background:item.color}}/>
                  <p style={{fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{item.name}</p>
                </div>
                <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#9499a6"}}>
                  {item.value.toLocaleString()} ({item.pct}%)
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* Revenue by Day */}
        <div className="bg-white flex flex-col gap-4 p-4 sm:p-6 rounded-[12px] lg:flex-1 min-w-0" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Revenue by Day</p>
          <div className="h-[200px] rounded-[8px] overflow-hidden" style={{background:"#f7f7fa"}}>
            {revenueByDay.some(d => d.value > 0 || d.orders > 0) ? (
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={revenueByDay} margin={{top:20,right:20,left:-10,bottom:0}}>
                <XAxis dataKey="day" tick={{fontSize:12,fill:"#9499a6",fontFamily:"Inter"}} axisLine={false} tickLine={false}/>
                <YAxis hide/>
                <Tooltip cursor={{fill:"rgba(120,135,250,0.08)"}}
                  contentStyle={{background:"white",border:"1px solid #e5e8ed",borderRadius:8,fontSize:12,fontFamily:"Inter"}}
                  formatter={(v:number)=>[formatCurrency(v),"Revenue"]}/>
                <Bar key="revenue-bar" dataKey="value" fill="#7887FA" radius={[4,4,0,0]} maxBarSize={50}/>
              </BarChart>
            </ResponsiveContainer>
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <p style={{fontFamily:"Inter",fontSize:12,color:"#b2b8bf"}}>No orders in the last 7 days.</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
