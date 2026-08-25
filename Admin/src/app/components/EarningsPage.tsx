import React, { useState, useEffect, useCallback } from "react";
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, ResponsiveContainer, Tooltip
} from "recharts";
import { Modal, FormField, SaveCancel } from "./Modal";
import { useToast } from "./Toast";
import {
  fetchCommissionSummary,
  updateCommissionSettings,
  CommissionSummary,
} from "../api/commissionApi";

const fmt = (n: number) => `₹${Math.round(n).toLocaleString("en-IN")}`;

const compact = (n: number) => {
  if (n >= 10000000) return `₹${(n / 10000000).toFixed(1)}Cr`;
  if (n >= 100000)  return `₹${(n / 100000).toFixed(1)}L`;
  if (n >= 1000)    return `₹${(n / 1000).toFixed(1)}k`;
  return `₹${Math.round(n)}`;
};

const MONTH_NAMES = ["January","February","March","April","May","June","July","August","September","October","November","December"];

/* ─── Custom chart tooltip ───────────────────────────────────────────────── */
interface TipPayload { name?: string; dataKey?: string; value?: number; color?: string }
const ChartTooltip = ({ active, payload, label, pct }: {
  active?: boolean; payload?: TipPayload[]; label?: string; pct: number;
}) => {
  if (!active || !payload?.length) return null;
  const commission = payload.find(p => p.dataKey === "commission")?.value ?? 0;
  const cookNet    = payload.find(p => p.dataKey === "cookNet")?.value ?? 0;
  const gross      = commission + cookNet;
  return (
    <div style={{background:"white",border:"1px solid #e5e8ed",borderRadius:12,padding:"12px 16px",
      boxShadow:"0 8px 24px rgba(0,0,0,0.12)",fontFamily:"Inter",minWidth:200}}>
      <p style={{fontWeight:600,fontSize:13,color:"#1c1f29",marginBottom:8,paddingBottom:8,borderBottom:"1px solid #f2f5f7"}}>{label}</p>
      <div style={{display:"flex",alignItems:"center",gap:8,marginBottom:4}}>
        <span style={{width:8,height:8,borderRadius:"50%",background:"#7887FA",flexShrink:0}}/>
        <span style={{fontSize:12,color:"#9499a6",flex:1}}>TiffinCraft ({pct}%)</span>
        <span style={{fontSize:12,fontWeight:700,color:"#7887FA"}}>{fmt(commission)}</span>
      </div>
      <div style={{display:"flex",alignItems:"center",gap:8,marginBottom:4}}>
        <span style={{width:8,height:8,borderRadius:"50%",background:"#57B869",flexShrink:0}}/>
        <span style={{fontSize:12,color:"#9499a6",flex:1}}>Cook Net ({(100 - pct).toFixed(0)}%)</span>
        <span style={{fontSize:12,fontWeight:700,color:"#57B869"}}>{fmt(cookNet)}</span>
      </div>
      <div style={{display:"flex",alignItems:"center",gap:8,marginTop:8,paddingTop:8,borderTop:"1px dashed #e5e8ed"}}>
        <span style={{fontSize:12,color:"#9499a6",flex:1}}>Gross Order Value</span>
        <span style={{fontSize:12,fontWeight:700,color:"#1c1f29"}}>{fmt(gross)}</span>
      </div>
    </div>
  );
};

/* ─── Stat card ──────────────────────────────────────────────────────────── */
const StatCard = ({ icon, value, label, sub, change, changeTone }: {
  icon: string; value: string; label: string; sub: string;
  change?: string; changeTone?: "up"|"down";
}) => (
  <div className="bg-white flex flex-col gap-2 p-5 rounded-[12px] flex-1" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
    <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
      <span style={{fontSize:22}}>{icon}</span>
      {change && (
        <span style={{fontFamily:"Inter",fontWeight:600,fontSize:12,
          color:changeTone==="down"?"#f25959":"#10b981",
          background:changeTone==="down"?"rgba(242,89,89,0.10)":"rgba(16,185,129,0.10)",
          padding:"3px 10px",borderRadius:12}}>
          {change}
        </span>
      )}
    </div>
    <p style={{fontFamily:"Inter",fontWeight:700,fontSize:26,color:"#1c1f29"}}>{value}</p>
    <p style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#1c1f29"}}>{label}</p>
    <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6"}}>{sub}</p>
  </div>
);

const SkeletonCard = () => (
  <div className="bg-white flex flex-col gap-3 p-5 rounded-[12px] flex-1 animate-pulse" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
    <div style={{width:36,height:36,borderRadius:10,background:"#f2f5f7"}}/>
    <div style={{width:"60%",height:26,borderRadius:6,background:"#f2f5f7"}}/>
    <div style={{width:"80%",height:13,borderRadius:6,background:"#f2f5f7"}}/>
    <div style={{width:"50%",height:12,borderRadius:6,background:"#f2f5f7"}}/>
  </div>
);

/* ─── Main component ─────────────────────────────────────────────────────── */
export function EarningsPage() {
  const { showToast } = useToast();
  const [summary,   setSummary]   = useState<CommissionSummary|null>(null);
  const [loading,   setLoading]   = useState(true);
  const [error,     setError]     = useState<string|null>(null);

  const [settingRate,  setSettingRate]  = useState(false);
  const [rateInput,    setRateInput]    = useState("");
  const [savingRate,   setSavingRate]   = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchCommissionSummary();
      setSummary(data);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load earnings data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const pct = summary?.commission_pct ?? 0;
  const trend = summary?.trend ?? [];
  const netPct = 100 - pct;

  // Month-over-month deltas from the real trend series
  const lastTwo  = trend.slice(-2);
  const thisMonthVal = lastTwo[1]?.commission ?? 0;
  const prevMonthVal = lastTwo[0]?.commission ?? 0;
  const momDelta = prevMonthVal > 0 ? ((thisMonthVal - prevMonthVal) / prevMonthVal) * 100 : null;

  const saveRate = async () => {
    const value = Number(rateInput);
    if (rateInput === "" || Number.isNaN(value) || value < 0 || value > 100) {
      showToast("Commission % must be a number between 0 and 100.", "error");
      return;
    }
    setSavingRate(true);
    try {
      await updateCommissionSettings(value);
      showToast(`Commission rate updated to ${value}%.`, "success");
      setSettingRate(false);
      await load();
    } catch (e) {
      showToast(e instanceof Error ? e.message : "Failed to update commission rate.", "error");
    } finally {
      setSavingRate(false);
    }
  };

  const byCook = summary?.by_cook ?? [];
  const totalGross = byCook.reduce((s, r) => s + r.gross_total, 0);
  const totalComm  = byCook.reduce((s, r) => s + r.commission_total, 0);
  const totalOrders = byCook.reduce((s, r) => s + r.order_count, 0);

  return (
    <div className="flex flex-col gap-6">

      {/* Header */}
      <div>
        <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Earnings</p>
        {loading ? (
          <div style={{width:420,height:16,borderRadius:6,background:"#f2f5f7",marginTop:8}} className="animate-pulse"/>
        ) : (
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>
            TiffinCraft commission earnings — cooks pay a <strong style={{color:"#7887fa"}}>{pct}% commission</strong> on every successful delivery.
          </p>
        )}
      </div>

      {error && (
        <div className="bg-white p-6 rounded-[12px] flex flex-col items-center gap-3" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
          <span style={{fontSize:28}}>⚠️</span>
          <p style={{fontFamily:"Inter",fontSize:14,color:"#f25959"}}>{error}</p>
          <button onClick={load}
            style={{padding:"8px 20px",borderRadius:8,border:"none",background:"#7887FA",color:"white",fontFamily:"Inter",fontWeight:600,fontSize:13,cursor:"pointer"}}>
            Retry
          </button>
        </div>
      )}

      {/* Stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {loading || !summary ? (
          <><SkeletonCard/><SkeletonCard/><SkeletonCard/><SkeletonCard/></>
        ) : (
          <>
            <StatCard
              icon="💹" label="Total Commission Earned" sub="All time, all cooks"
              value={fmt(summary.all_time_commission)}
              change={momDelta !== null ? `${momDelta >= 0 ? "+" : ""}${momDelta.toFixed(1)}%` : undefined}
              changeTone={momDelta !== null && momDelta < 0 ? "down" : "up"}
            />
            <StatCard
              icon="📅" label="Commission This Month" sub={`${MONTH_NAMES[summary.month - 1]} ${summary.year}`}
              value={fmt(summary.total_commission)}
              change={momDelta !== null ? `${momDelta >= 0 ? "+" : ""}${momDelta.toFixed(1)}% MoM` : undefined}
              changeTone={momDelta !== null && momDelta < 0 ? "down" : "up"}
            />
            <StatCard
              icon="📊" label="Fixed Commission Rate" sub="Per successful order"
              value={`${pct}%`}
            />
            <StatCard
              icon="⏳" label="Pending Collection" sub="Awaiting delivery"
              value={fmt(summary.pending_commission)}
              change={`${summary.pending_order_count} orders`}
            />
          </>
        )}
      </div>

      {/* Revenue Trend chart */}
      <div className="bg-white p-4 sm:p-6 rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",flexWrap:"wrap",gap:12,marginBottom:20}}>
          <div>
            <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Revenue Trend</p>
            <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6",marginTop:2}}>Last 6 months — commission vs cook net earnings</p>
          </div>
          <div style={{display:"flex",gap:20,flexWrap:"wrap"}}>
            {[{c:"#7887FA",l:`TiffinCraft (${pct}% Commission)`},{c:"#57B869",l:`Cook Net Earnings (${netPct.toFixed(0)}%)`}].map(x=>(
              <div key={x.l} style={{display:"flex",alignItems:"center",gap:6}}>
                <div style={{width:10,height:10,borderRadius:"50%",background:x.c}}/>
                <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6"}}>{x.l}</p>
              </div>
            ))}
          </div>
        </div>
        <div style={{height:300}}>
          {loading || !summary ? (
            <div className="animate-pulse" style={{width:"100%",height:"100%",borderRadius:10,background:"#f7f8fa"}}/>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={trend} margin={{top:10,right:20,left:-6,bottom:0}}>
                <defs>
                  <linearGradient id="eG1" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%"  stopColor="#7887FA" stopOpacity={0.35}/>
                    <stop offset="100%" stopColor="#7887FA" stopOpacity={0.02}/>
                  </linearGradient>
                  <linearGradient id="eG2" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%"  stopColor="#57B869" stopOpacity={0.28}/>
                    <stop offset="100%" stopColor="#57B869" stopOpacity={0.02}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="4 6" stroke="#eef0f4" vertical={false}/>
                <XAxis dataKey="month" tick={{fontSize:12,fill:"#9499a6",fontFamily:"Inter"}}
                  axisLine={false} tickLine={false} dy={8} padding={{left:16,right:16}}/>
                <YAxis tickFormatter={(v:number)=>compact(v)} tick={{fontSize:11,fill:"#b6bac4",fontFamily:"Inter"}}
                  axisLine={false} tickLine={false} width={56}/>
                <Tooltip content={<ChartTooltip pct={pct}/>} cursor={{stroke:"#d9deE6",strokeDasharray:"4 4"}}/>
                <Area key="ek" type="monotone" dataKey="cookNet" name="cookNet" stroke="#57B869" strokeWidth={2.5}
                  fill="url(#eG2)" animationDuration={900}
                  dot={{r:3,fill:"#57B869",stroke:"white",strokeWidth:2}} activeDot={{r:5,stroke:"white",strokeWidth:2}}/>
                <Area key="ec" type="monotone" dataKey="commission" name="commission" stroke="#7887FA" strokeWidth={2.5}
                  fill="url(#eG1)" animationDuration={900}
                  dot={{r:3,fill:"#7887FA",stroke:"white",strokeWidth:2}} activeDot={{r:5,stroke:"white",strokeWidth:2}}/>
              </AreaChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* ── Per-cook commission (this month, live from DB) ── */}
      <div className="bg-white rounded-[12px] p-4 sm:p-6" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",flexWrap:"wrap",gap:12,marginBottom:20}}>
          <div>
            <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Commission by Cook</p>
            <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6",marginTop:2}}>
              {summary ? `${pct}% deducted from delivered orders in ${MONTH_NAMES[summary.month - 1]} ${summary.year}` : "Loading…"}
            </p>
          </div>
          <button onClick={()=>{ setRateInput(String(pct)); setSettingRate(true); }}
            style={{padding:"10px 18px",borderRadius:8,border:"none",background:"#f97316",fontFamily:"Inter",fontWeight:600,fontSize:13,color:"white",cursor:"pointer"}}>
            + Set Commission
          </button>
        </div>
        <div className="overflow-x-auto">
        <div className="min-w-[860px]">
        <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
          <TH w={40}>S.N</TH>
          {["Kitchen","Owner","Orders","Order Value",`Commission (${pct}%)`,"Cook Net"].map((h,i)=>(
            <TH key={h} w={[200,160,90,130,150,130][i]}>{h}</TH>
          ))}
        </div>
        {!loading && byCook.length === 0 && (
          <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6",padding:"24px 0",textAlign:"center"}}>
            No delivered orders with commission recorded for this month yet.
          </p>
        )}
        {byCook.map((r,idx)=>{
          const net = r.gross_total - r.commission_total;
          return (
            <div key={r.cook_id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <TD w={40} accent="#9499a6">{idx+1}</TD>
                <div style={{width:200,flexShrink:0,display:"flex",alignItems:"center",gap:10}}>
                  <div style={{width:36,height:36,borderRadius:"50%",background:"#D9DEE6",display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0}}>
                    <span style={{fontFamily:"Inter",fontWeight:700,fontSize:12,color:"#6b7280"}}>
                      {(r.kitchen_name || r.owner_name || "??").slice(0,2).toUpperCase()}
                    </span>
                  </div>
                  <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{r.kitchen_name || r.owner_name}</p>
                </div>
                <TD w={160}>{r.owner_name}</TD>
                <TD w={90}>{r.order_count}</TD>
                <TD w={130} bold>{fmt(r.gross_total)}</TD>
                <TD w={150} bold accent="#10b981">{fmt(r.commission_total)}</TD>
                <TD w={130} bold accent="#10b981">{fmt(net)}</TD>
              </div>
              {idx<byCook.length-1 && <div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          );
        })}
        {byCook.length > 0 && (
          <div className="flex gap-4 items-center pt-4 mt-2" style={{borderTop:"2px solid #e5e8ed"}}>
            <div style={{width:40,flexShrink:0}}/>
            <div style={{width:200,flexShrink:0}}><p style={{fontFamily:"Inter",fontWeight:700,fontSize:13,color:"#1c1f29"}}>Total</p></div>
            <div style={{width:160,flexShrink:0}}/>
            <TD w={90} bold>{totalOrders}</TD>
            <TD w={130} bold>{fmt(totalGross)}</TD>
            <TD w={150} bold accent="#10b981">{fmt(totalComm)}</TD>
            <TD w={130} bold accent="#10b981">{fmt(totalGross - totalComm)}</TD>
          </div>
        )}
        </div>
        </div>
      </div>

      {/* Set Commission modal */}
      {settingRate && (
        <Modal title="Set Commission Rate" onClose={()=>setSettingRate(false)}>
          <div className="mb-4 p-4 rounded-[10px]" style={{background:"rgba(120,135,250,0.06)",border:"1px solid rgba(120,135,250,0.15)"}}>
            <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#7887fa",marginBottom:4}}>ℹ️ How it works</p>
            <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6",lineHeight:1.6}}>
              On a <strong style={{color:"#1c1f29"}}>₹400 order</strong> at <strong style={{color:"#7887fa"}}>{rateInput || 0}%</strong>, TiffinCraft earns{" "}
              <strong style={{color:"#10b981"}}>₹{Math.round(400 * (Number(rateInput) || 0) / 100)}</strong> and the cook receives{" "}
              <strong style={{color:"#1c1f29"}}>₹{400 - Math.round(400 * (Number(rateInput) || 0) / 100)}</strong>.<br/>
              Applies to orders delivered from now on — already-delivered orders keep their original rate.
            </p>
          </div>
          <FormField label="Commission % (0–100)" value={rateInput}
            onChange={v=>setRateInput(v)} type="number"/>
          <SaveCancel onCancel={()=>setSettingRate(false)} onSave={saveRate}/>
        </Modal>
      )}
    </div>
  );
}

/* ─── Table helpers ──────────────────────────────────────────────────────── */
const TH = ({ children, w }: { children: React.ReactNode; w: number }) => (
  <p style={{ width:w, flexShrink:0, fontFamily:"Inter", fontWeight:600, fontSize:12, color:"#9499a6" }}>{children}</p>
);
const TD = ({ children, w, bold, accent }: { children: React.ReactNode; w: number; bold?: boolean; accent?: string }) => (
  <p style={{ width:w, flexShrink:0, fontFamily:"Inter", fontWeight: bold?700:400, fontSize:13, color: accent ?? "#1c1f29" }}>{children}</p>
);
