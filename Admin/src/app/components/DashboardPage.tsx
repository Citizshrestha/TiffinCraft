import React, { useEffect, useState, useCallback } from "react";
import { StatusBadge, StatusType } from "./StatusBadge";
import {
  AreaChart, Area, XAxis, YAxis, ResponsiveContainer, Tooltip,
} from "recharts";
import { fetchDashboard, DashboardResponse } from "../api/dashboardApi";

function formatCurrency(n: number | string | null | undefined): string {
  const num = Number(n) || 0;
  return `₹${num.toLocaleString("en-IN")}`;
}

function formatNumber(n: number | string | null | undefined): string {
  return (Number(n) || 0).toLocaleString("en-IN");
}

function formatChartDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

function pctChange(first: number, last: number): string {
  if (!first) return last > 0 ? "+100%" : "0%";
  const pct = ((last - first) / first) * 100;
  const sign = pct >= 0 ? "+" : "";
  return `${sign}${pct.toFixed(1)}%`;
}

function KpiCard({ label, value, sub }: { label:string; value:string; sub:string }) {
  return (
    <div className="bg-white flex flex-col gap-2 p-5 rounded-[12px] flex-1" style={{boxShadow:"0px 1px 3px rgba(0,0,0,0.06)"}}>
      <p style={{fontFamily:"Inter",fontWeight:400,fontSize:13,color:"#9499a6"}}>{label}</p>
      <p style={{fontFamily:"Inter",fontWeight:700,fontSize:32,color:"#1c1f29"}}>{value}</p>
      <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#10b981"}}>{sub}</p>
    </div>
  );
}

function ChartCard({ title, value, change, data, color, gradientId, dataKey }: {
  title:string; value:string; change:string; data:Record<string,string|number>[];
  color:string; gradientId:string; dataKey:string;
}) {
  return (
    <div className="bg-white flex flex-col gap-4 p-4 sm:p-6 rounded-[12px] lg:flex-1 min-w-0" style={{boxShadow:"0px 1px 3px rgba(0,0,0,0.06)"}}>
      <div>
        <p style={{fontFamily:"Inter",fontWeight:600,fontSize:16,color:"#1c1f29",marginBottom:4}}>{title}</p>
        <div className="flex items-center gap-2">
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>{value}</p>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:14,color:"#10b981"}}>{change}</p>
        </div>
      </div>
      <div className="h-[140px] rounded-[8px] overflow-hidden bg-[#fafafc]">
        {data.length === 0 ? (
          <div className="w-full h-full flex items-center justify-center">
            <p style={{fontFamily:"Inter",fontSize:12,color:"#b2b8bf"}}>No data for the last 7 days.</p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{top:10,right:10,left:-20,bottom:0}}>
              <defs>
                <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor={color} stopOpacity={0.15}/>
                  <stop offset="95%" stopColor={color} stopOpacity={0}/>
                </linearGradient>
              </defs>
              <XAxis dataKey="day" tick={{fontSize:11,fill:"#b2b8bf",fontFamily:"Inter"}} axisLine={false} tickLine={false}/>
              <YAxis hide/>
              <Tooltip contentStyle={{background:"white",border:"1px solid #e5e8ed",borderRadius:8,fontSize:12,fontFamily:"Inter"}}/>
              <Area type="monotone" dataKey={dataKey} stroke={color} strokeWidth={3} fill={`url(#${gradientId})`}
                dot={{r:3,fill:color,stroke:"white",strokeWidth:2}}
                activeDot={{r:5,fill:color,stroke:"white",strokeWidth:2}}/>
            </AreaChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
}

export function DashboardPage({ onNavigate }: { onNavigate:(page:string)=>void }) {
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
      setLoadError(err instanceof Error ? err.message : "Failed to load dashboard.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) {
    return (
      <div className="flex flex-col gap-6">
        <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6"}}>Loading dashboard...</p>
      </div>
    );
  }

  if (loadError || !data) {
    return (
      <div className="flex flex-col gap-6">
        <p style={{fontFamily:"Inter",fontSize:14,color:"#f25959",marginBottom:12}}>{loadError || "Failed to load dashboard."}</p>
        <button onClick={load} style={{width:160,background:"#f97316",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:13,padding:"10px 16px",borderRadius:8,cursor:"pointer"}}>Retry</button>
      </div>
    );
  }

  const { dashboard, recentOrders, topCooks, last7Days } = data;

  const chartData = last7Days.map(d => ({
    day: formatChartDate(d.date),
    orders: Number(d.orders) || 0,
    revenue: Number(d.revenue) || 0,
  }));

  const ordersTotal = chartData.reduce((s, d) => s + d.orders, 0);
  const revenueTotal = chartData.reduce((s, d) => s + d.revenue, 0);
  const ordersChange = chartData.length >= 2 ? pctChange(chartData[0].orders, chartData[chartData.length-1].orders) : "—";
  const revenueChange = chartData.length >= 2 ? pctChange(chartData[0].revenue, chartData[chartData.length-1].revenue) : "—";

  return (
    <div className="flex flex-col gap-6">
      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <KpiCard label="Total Users"   value={formatNumber(dashboard.total_users)}   sub={`${formatNumber(dashboard.total_customers)} customers`}/>
        <KpiCard label="Total Cooks"   value={formatNumber(dashboard.total_cooks)}   sub={`${formatNumber(dashboard.pending_approvals)} pending approval`}/>
        <KpiCard label="Total Orders"  value={formatNumber(dashboard.total_orders)}  sub={`${formatNumber(dashboard.today_orders)} today`}/>
        <KpiCard label="Total Revenue" value={formatCurrency(dashboard.total_revenue)} sub={`${formatNumber(dashboard.pending_orders)} pending orders`}/>
      </div>

      {/* Charts */}
      <div className="flex flex-col lg:flex-row gap-4">
        <ChartCard title="Orders Overview"  value={formatNumber(ordersTotal)}    change={ordersChange}  data={chartData} color="#7887FA" gradientId="ordersGrad"  dataKey="orders"/>
        <ChartCard title="Revenue Overview" value={formatCurrency(revenueTotal)} change={revenueChange} data={chartData} color="#57B869" gradientId="revenueGrad" dataKey="revenue"/>
      </div>

      {/* Bottom Section */}
      <div className="flex flex-col xl:flex-row gap-4">
        {/* Recent Orders */}
        <div className="bg-white flex flex-col p-4 sm:p-6 rounded-[12px] xl:flex-[1.7] min-w-0" style={{boxShadow:"0px 1px 3px rgba(0,0,0,0.06)"}}>
          <div className="flex items-center justify-between mb-4">
            <p style={{fontFamily:"Inter",fontWeight:600,fontSize:16,color:"#1c1f29"}}>Recent Orders</p>
            <button onClick={()=>onNavigate("orders")}
              style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#7887fa",background:"none",border:"none",cursor:"pointer"}}
              onMouseEnter={e=>((e.currentTarget as HTMLElement).style.textDecoration="underline")}
              onMouseLeave={e=>((e.currentTarget as HTMLElement).style.textDecoration="none")}>
              View All Orders →
            </button>
          </div>
          <div className="overflow-x-auto">
            <div className="min-w-[660px]">
              <div className="flex gap-3 pb-3" style={{borderBottom:"1px solid #edf0f2"}}>
                {["Order ID","Customer","Cook","Amount","Status"].map((h,i)=>(
                  <p key={h} style={{fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#b2b8bf",width:[100,140,140,100,120][i],flexShrink:0}}>{h}</p>
                ))}
              </div>
              {recentOrders.length === 0 && (
                <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6",textAlign:"center",padding:"24px 0"}}>No orders yet.</p>
              )}
              {recentOrders.map((order,idx)=>(
                <div key={order.id}>
                  <div className="flex gap-3 py-3 items-center hover:bg-[#f7f8fa] cursor-pointer -mx-2 px-2 rounded transition-colors"
                    onClick={()=>onNavigate("orders")}>
                    <p style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#7887fa",width:100,flexShrink:0}}>#ORD-{order.id}</p>
                    <p style={{fontFamily:"Inter",fontSize:13,color:"#1c1f29",width:140,flexShrink:0}}>{order.customer_name}</p>
                    <p style={{fontFamily:"Inter",fontSize:13,color:"#1c1f29",width:140,flexShrink:0}}>{order.kitchen_name || "-"}</p>
                    <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29",width:100,flexShrink:0}}>{formatCurrency(order.total_amount)}</p>
                    <div style={{width:120,flexShrink:0}}><StatusBadge status={order.status as StatusType}/></div>
                  </div>
                  {idx<recentOrders.length-1 && <div style={{height:1,background:"#f5f7fa"}}/>}
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Top Performing Cooks */}
        <div className="bg-white flex flex-col gap-4 p-4 sm:p-6 rounded-[12px] xl:flex-1 min-w-0" style={{boxShadow:"0px 1px 3px rgba(0,0,0,0.06)"}}>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:16,color:"#1c1f29"}}>Top Performing Cooks</p>
          {topCooks.length === 0 && (
            <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6",textAlign:"center",padding:"24px 0"}}>No cooks yet.</p>
          )}
          {topCooks.map((cook,idx)=>(
            <div key={cook.user_id}>
              <div className="flex items-center justify-between py-1 cursor-pointer hover:bg-[#f7f8fa] -mx-2 px-2 rounded transition-colors"
                onClick={()=>onNavigate("cooks")}>
                <div className="flex items-center gap-3">
                  <div className="relative w-10 h-10 shrink-0">
                    <div className="w-10 h-10 rounded-full bg-[#D9DEE6]"/>
                    {cook.profile_image && (
                      <img src={cook.profile_image} alt={cook.kitchen_name || "Cook"}
                        className="w-10 h-10 rounded-full object-cover absolute inset-0"
                        onError={e=>((e.currentTarget as HTMLImageElement).style.display="none")}/>
                    )}
                  </div>
                  <div>
                    <p style={{fontFamily:"Inter",fontWeight:500,fontSize:14,color:"#1c1f29"}}>{cook.kitchen_name || "Unnamed Kitchen"}</p>
                    <p style={{fontFamily:"Inter",fontSize:12,color:"#b2b8bf"}}>{formatNumber(cook.total_orders)}+ orders</p>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <span>⭐</span>
                  <p style={{fontFamily:"Inter",fontWeight:600,fontSize:14,color:"#1c1f29"}}>{(Number(cook.rating)||0).toFixed(1)}</p>
                </div>
              </div>
              {idx<topCooks.length-1 && <div style={{height:1,background:"#f5f7fa"}}/>}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
