import React, { useState, useRef, useEffect } from "react";
import { StatusBadge } from "./StatusBadge";
import {
  AreaChart, Area, XAxis, YAxis, ResponsiveContainer, Tooltip,
} from "recharts";

const DATE_RANGES = [
  { label:"Last 7 days",  from:"May 12, 2025", to:"May 18, 2025" },
  { label:"Last 30 days", from:"Apr 18, 2025", to:"May 18, 2025" },
  { label:"Last 3 months",from:"Feb 18, 2025", to:"May 18, 2025" },
  { label:"This year",    from:"Jan 01, 2025", to:"May 18, 2025" },
];

const ordersDataByRange = {
  "Last 7 days":   [{ day:"May 12",orders:820},{ day:"May 13",orders:932},{ day:"May 14",orders:901},{ day:"May 15",orders:1074},{ day:"May 16",orders:1130},{ day:"May 17",orders:1200},{ day:"May 18",orders:1290}],
  "Last 30 days":  [{ day:"Apr 18",orders:600},{ day:"Apr 25",orders:750},{ day:"May 01",orders:900},{ day:"May 08",orders:1000},{ day:"May 12",orders:1100},{ day:"May 15",orders:1200},{ day:"May 18",orders:1290}],
  "Last 3 months": [{ day:"Feb",orders:5200},{ day:"Mar",orders:5800},{ day:"Apr",orders:6100},{ day:"May",orders:6709},{ day:"",orders:0},{ day:"",orders:0},{ day:"",orders:0}],
  "This year":     [{ day:"Jan",orders:4200},{ day:"Feb",orders:5200},{ day:"Mar",orders:5800},{ day:"Apr",orders:6100},{ day:"May",orders:6709},{ day:"",orders:0},{ day:"",orders:0}],
};

const revenueDataByRange = {
  "Last 7 days":   [{ day:"May 12",revenue:18200},{ day:"May 13",revenue:19500},{ day:"May 14",revenue:21000},{ day:"May 15",revenue:21800},{ day:"May 16",revenue:23100},{ day:"May 17",revenue:24000},{ day:"May 18",revenue:25100}],
  "Last 30 days":  [{ day:"Apr 18",revenue:12000},{ day:"Apr 25",revenue:15000},{ day:"May 01",revenue:18000},{ day:"May 08",revenue:20000},{ day:"May 12",revenue:22000},{ day:"May 15",revenue:24000},{ day:"May 18",revenue:25100}],
  "Last 3 months": [{ day:"Feb",revenue:38000},{ day:"Mar",revenue:42000},{ day:"Apr",revenue:46000},{ day:"May",revenue:52100},{ day:"",revenue:0},{ day:"",revenue:0},{ day:"",revenue:0}],
  "This year":     [{ day:"Jan",revenue:32000},{ day:"Feb",revenue:38000},{ day:"Mar",revenue:42000},{ day:"Apr",revenue:46000},{ day:"May",revenue:52100},{ day:"",revenue:0},{ day:"",revenue:0}],
};

const recentOrders = [
  { id:"#ORD-1234", customer:"Rahul Sharma",  cook:"Anita's Kitchen", amount:"₹360", status:"delivered"       as const },
  { id:"#ORD-1233", customer:"Priya Patel",   cook:"Spice Route",     amount:"₹380", status:"processing"      as const },
  { id:"#ORD-1232", customer:"Vikram Singh",  cook:"Healthy Meals",   amount:"₹460", status:"out_for_delivery"as const },
  { id:"#ORD-1231", customer:"Neha Gupta",    cook:"Tasty Tiffins",   amount:"₹320", status:"accepted"        as const },
  { id:"#ORD-1230", customer:"Amit Kumar",    cook:"Anita's Kitchen", amount:"₹380", status:"delivered"       as const },
];

const topCooks = [
  { name:"Anita's Kitchen", orders:"123+ orders", rating:"4.8" },
  { name:"Spice Route",     orders:"98+ orders",  rating:"4.7" },
  { name:"Healthy Meals",   orders:"87+ orders",  rating:"4.6" },
  { name:"Tasty Tiffins",   orders:"76+ orders",  rating:"4.5" },
  { name:"HomeBites",       orders:"66+ orders",  rating:"4.4" },
];

function KpiCard({ label, value, change }: { label:string; value:string; change:string }) {
  return (
    <div className="bg-white flex flex-col gap-2 p-5 rounded-[12px] flex-1" style={{boxShadow:"0px 1px 3px rgba(0,0,0,0.06)"}}>
      <p style={{fontFamily:"Inter",fontWeight:400,fontSize:13,color:"#9499a6"}}>{label}</p>
      <p style={{fontFamily:"Inter",fontWeight:700,fontSize:32,color:"#1c1f29"}}>{value}</p>
      <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#57b869"}}>{change}</p>
    </div>
  );
}

function ChartCard({ title, value, change, data, color, gradientId, dataKey }: {
  title:string; value:string; change:string; data:Record<string,string|number>[];
  color:string; gradientId:string; dataKey:string;
}) {
  return (
    <div className="bg-white flex flex-col gap-4 p-6 rounded-[12px] flex-1" style={{boxShadow:"0px 1px 3px rgba(0,0,0,0.06)"}}>
      <div>
        <p style={{fontFamily:"Inter",fontWeight:600,fontSize:16,color:"#1c1f29",marginBottom:4}}>{title}</p>
        <div className="flex items-center gap-2">
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>{value}</p>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:14,color:"#57b869"}}>{change}</p>
        </div>
      </div>
      <div className="h-[140px] rounded-[8px] overflow-hidden bg-[#fafafc]">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data.filter(d=>d.day!=="")} margin={{top:10,right:10,left:-20,bottom:0}}>
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
      </div>
    </div>
  );
}

export function DashboardPage({ onNavigate }: { onNavigate:(page:string)=>void }) {
  const [rangeIdx, setRangeIdx] = useState(0);
  const [pickerOpen, setPickerOpen] = useState(false);
  const pickerRef = useRef<HTMLDivElement>(null);

  const range = DATE_RANGES[rangeIdx];
  const ordersData  = ordersDataByRange[range.label  as keyof typeof ordersDataByRange];
  const revenueData = revenueDataByRange[range.label as keyof typeof revenueDataByRange];

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (pickerRef.current && !pickerRef.current.contains(e.target as Node)) setPickerOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:24,color:"#1c1f29"}}>Welcome back, Admin! 👋</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>Here's what's happening with TiffinCraft today.</p>
        </div>
        {/* Date range picker */}
        <div ref={pickerRef} style={{position:"relative"}}>
          <button onClick={()=>setPickerOpen(v=>!v)}
            className="flex items-center px-3 py-2 rounded-[6px] cursor-pointer transition-colors hover:border-gray-400"
            style={{border:"1px solid #e5e8ed",background:"white",fontFamily:"Inter",fontWeight:400,fontSize:13,color:"#9499a6"}}>
            📅 {range.from} - {range.to}
          </button>
          {pickerOpen && (
            <div style={{position:"absolute",right:0,top:40,zIndex:50,background:"white",borderRadius:10,padding:"6px",
              boxShadow:"0 8px 24px rgba(0,0,0,0.14)",border:"1px solid #e5e8ed",minWidth:200}}>
              {DATE_RANGES.map((r,i)=>(
                <button key={r.label} onClick={()=>{ setRangeIdx(i); setPickerOpen(false); }}
                  style={{width:"100%",textAlign:"left",padding:"10px 14px",fontSize:13,fontFamily:"Inter",fontWeight:i===rangeIdx?600:400,
                    color:i===rangeIdx?"#57b869":"#1c1f29",background:i===rangeIdx?"rgba(87,184,105,0.08)":"none",
                    border:"none",cursor:"pointer",borderRadius:6}}
                  onMouseEnter={e=>{ if(i!==rangeIdx) (e.currentTarget as HTMLElement).style.background="#f7f8fa"; }}
                  onMouseLeave={e=>{ if(i!==rangeIdx) (e.currentTarget as HTMLElement).style.background="none"; }}>
                  {r.label}
                  <span style={{display:"block",fontSize:11,color:"#9499a6",fontWeight:400}}>{r.from} — {r.to}</span>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* KPI Cards */}
      <div className="flex gap-4">
        <KpiCard label="Total Users"   value="2,345"      change="+12.8%"/>
        <KpiCard label="Total Cooks"   value="456"        change="+8.2%"/>
        <KpiCard label="Total Orders"  value="6,709"      change="+18.3%"/>
        <KpiCard label="Total Revenue" value="₹1,45,230" change="+24.4%"/>
      </div>

      {/* Charts */}
      <div className="flex gap-4">
        <ChartCard title="Orders Overview"  value="6,709"      change="+18.3%" data={ordersData}  color="#7887FA" gradientId="ordersGrad"  dataKey="orders"/>
        <ChartCard title="Revenue Overview" value="₹1,45,230" change="+24.4%" data={revenueData} color="#57B869" gradientId="revenueGrad" dataKey="revenue"/>
      </div>

      {/* Bottom Section */}
      <div className="flex gap-4">
        {/* Recent Orders */}
        <div className="bg-white flex flex-col p-6 rounded-[12px]" style={{boxShadow:"0px 1px 3px rgba(0,0,0,0.06)",flex:"1.7"}}>
          <div className="flex items-center justify-between mb-4">
            <p style={{fontFamily:"Inter",fontWeight:600,fontSize:16,color:"#1c1f29"}}>Recent Orders</p>
            <button onClick={()=>onNavigate("orders")}
              style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#7887fa",background:"none",border:"none",cursor:"pointer"}}
              onMouseEnter={e=>((e.currentTarget as HTMLElement).style.textDecoration="underline")}
              onMouseLeave={e=>((e.currentTarget as HTMLElement).style.textDecoration="none")}>
              View All Orders →
            </button>
          </div>
          <div className="flex gap-3 pb-3" style={{borderBottom:"1px solid #edf0f2"}}>
            {["Order ID","Customer","Cook","Amount","Status"].map((h,i)=>(
              <p key={h} style={{fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#b2b8bf",width:[100,140,140,100,120][i],flexShrink:0}}>{h}</p>
            ))}
          </div>
          {recentOrders.map((order,idx)=>(
            <div key={order.id}>
              <div className="flex gap-3 py-3 items-center hover:bg-[#f7f8fa] cursor-pointer -mx-2 px-2 rounded transition-colors"
                onClick={()=>onNavigate("orders")}>
                <p style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#7887fa",width:100,flexShrink:0}}>{order.id}</p>
                <p style={{fontFamily:"Inter",fontSize:13,color:"#1c1f29",width:140,flexShrink:0}}>{order.customer}</p>
                <p style={{fontFamily:"Inter",fontSize:13,color:"#1c1f29",width:140,flexShrink:0}}>{order.cook}</p>
                <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29",width:100,flexShrink:0}}>{order.amount}</p>
                <div style={{width:120,flexShrink:0}}><StatusBadge status={order.status}/></div>
              </div>
              {idx<recentOrders.length-1 && <div style={{height:1,background:"#f5f7fa"}}/>}
            </div>
          ))}
        </div>

        {/* Top Performing Cooks */}
        <div className="bg-white flex flex-col gap-4 p-6 rounded-[12px] flex-1" style={{boxShadow:"0px 1px 3px rgba(0,0,0,0.06)"}}>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:16,color:"#1c1f29"}}>Top Performing Cooks</p>
          {topCooks.map((cook,idx)=>(
            <div key={cook.name}>
              <div className="flex items-center justify-between py-1 cursor-pointer hover:bg-[#f7f8fa] -mx-2 px-2 rounded transition-colors"
                onClick={()=>onNavigate("cooks")}>
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-full bg-[#D9DEE6] shrink-0"/>
                  <div>
                    <p style={{fontFamily:"Inter",fontWeight:500,fontSize:14,color:"#1c1f29"}}>{cook.name}</p>
                    <p style={{fontFamily:"Inter",fontSize:12,color:"#b2b8bf"}}>{cook.orders}</p>
                  </div>
                </div>
                <div className="flex items-center gap-1">
                  <span>⭐</span>
                  <p style={{fontFamily:"Inter",fontWeight:600,fontSize:14,color:"#1c1f29"}}>{cook.rating}</p>
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
