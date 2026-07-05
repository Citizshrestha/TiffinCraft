import React from "react";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  Cell, PieChart, Pie,
} from "recharts";
import { exportCSV } from "../utils/csv";

const revenueByDay = [
  { day:"Mon", value:18500 },
  { day:"Tue", value:21200 },
  { day:"Wed", value:15800 },
  { day:"Thu", value:26500 },
  { day:"Fri", value:19200 },
  { day:"Sat", value:24800 },
  { day:"Sun", value:22100 },
];

const orderBreakdown = [
  { name:"Delivered",  value:4287, pct:64, color:"#7887FA" },
  { name:"Processing", value:1342, pct:20, color:"#57B869" },
  { name:"Pending",    value:804,  pct:12, color:"#F28C40" },
  { name:"Cancelled",  value:276,  pct:4,  color:"#F2C740" },
];

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
  const handleExport = () =>
    exportCSV("reports-revenue.csv", revenueByDay.map(d => ({ Day: d.day, Revenue: `₹${d.value.toLocaleString("en-IN")}` })));

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Reports & Analytics</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>View detailed analytics and performance reports.</p>
        </div>
        <button onClick={handleExport}
          className="flex items-center gap-2 px-5 py-3 rounded-[8px] text-white text-[14px] cursor-pointer transition-all duration-150 hover:brightness-95"
          style={{background:"#57b869",fontFamily:"Inter",fontWeight:600,border:"none"}}>
          📊 Export Report
        </button>
      </div>

      {/* Key Metrics */}
      <div className="flex gap-4">
        <MetricCard icon="📦" value="6,709" label="Total Orders"    change="+12.3%" changeColor="#7887fa"/>
        <MetricCard icon="💰" value="₹1.45L" label="Total Revenue"  change="+18.5%" changeColor="#57b869"/>
        <MetricCard icon="👥" value="334"    label="Active Users"   change="+8.2%"  changeColor="#f28c40"/>
        <MetricCard icon="💳" value="₹456"   label="Avg Order Value" change="+5.1%" changeColor="#f2c740"/>
      </div>

      {/* Charts */}
      <div className="flex gap-4">
        {/* Orders Breakdown */}
        <div className="bg-white flex flex-col gap-4 p-6 rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)",width:530}}>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Orders Breakdown</p>
          <div className="h-[200px]">
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
        <div className="bg-white flex flex-col gap-4 p-6 rounded-[12px] flex-1" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Revenue by Day</p>
          <div className="h-[200px] rounded-[8px] overflow-hidden" style={{background:"#f7f7fa"}}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={revenueByDay} margin={{top:20,right:20,left:-10,bottom:0}}>
                <XAxis dataKey="day" tick={{fontSize:12,fill:"#9499a6",fontFamily:"Inter"}} axisLine={false} tickLine={false}/>
                <YAxis hide/>
                <Tooltip cursor={{fill:"rgba(120,135,250,0.08)"}}
                  contentStyle={{background:"white",border:"1px solid #e5e8ed",borderRadius:8,fontSize:12,fontFamily:"Inter"}}
                  formatter={(v:number)=>[`₹${v.toLocaleString("en-IN")}`,"Revenue"]}/>
                <Bar key="revenue-bar" dataKey="value" fill="#7887FA" radius={[4,4,0,0]} maxBarSize={50}/>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
