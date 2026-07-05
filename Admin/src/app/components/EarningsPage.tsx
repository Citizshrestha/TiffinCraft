import React, { useState } from "react";
import { AreaChart, Area, XAxis, YAxis, ResponsiveContainer, Tooltip } from "recharts";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow, FormField, SaveCancel } from "./Modal";

/* ─── Chart ──────────────────────────────────────────────────────────────── */
const CHART = [
  { month:"Jan", commission:2680,  cookNet:64320  },
  { month:"Feb", commission:3220,  cookNet:77280  },
  { month:"Mar", commission:3060,  cookNet:73440  },
  { month:"Apr", commission:3590,  cookNet:86160  },
  { month:"May", commission:4190,  cookNet:100560 },
  { month:"Jun", commission:4860,  cookNet:116640 },
];

/* ─── Commission rates ───────────────────────────────────────────────────── */
interface CommRate {
  id: number; kitchen: string; owner: string; commissionPct: number;
  totalOrders: number; totalOrderValue: number; status: "Active"|"Inactive";
}
const fmt = (n: number) => `₹${n.toLocaleString("en-IN")}`;

const INIT_RATES: CommRate[] = [
  { id:1, kitchen:"Anita's Kitchen", owner:"Anita Sharma",  commissionPct:4, totalOrders:123, totalOrderValue:82500,  status:"Active"   },
  { id:2, kitchen:"Spice Route",     owner:"Ramesh Kumar",  commissionPct:4, totalOrders:98,  totalOrderValue:65700,  status:"Active"   },
  { id:3, kitchen:"Healthy Meals",   owner:"Priya Mehta",   commissionPct:4, totalOrders:87,  totalOrderValue:54800,  status:"Active"   },
  { id:4, kitchen:"Tasty Tiffins",   owner:"Suresh Patel",  commissionPct:4, totalOrders:76,  totalOrderValue:50600,  status:"Active"   },
  { id:5, kitchen:"HomeBites",       owner:"Kavitha Reddy", commissionPct:4, totalOrders:66,  totalOrderValue:40900,  status:"Inactive" },
  { id:6, kitchen:"Mumbai Spice",    owner:"Anil Desai",    commissionPct:4, totalOrders:54,  totalOrderValue:36200,  status:"Active"   },
  { id:7, kitchen:"South Indian",    owner:"Lakshmi Iyer",  commissionPct:4, totalOrders:48,  totalOrderValue:30400,  status:"Active"   },
];

/* ─── Cook earnings (net after commission) ───────────────────────────────── */
type PayStatus = "Paid"|"Pending";
interface CookEarning {
  id: number; kitchen: string; owner: string; totalOrders: number;
  grossEarnings: number; payoutStatus: PayStatus; dueDate: string;
}
const COOK_EARNINGS: CookEarning[] = [
  { id:1, kitchen:"Anita's Kitchen", owner:"Anita Sharma",  totalOrders:123, grossEarnings:82500,  payoutStatus:"Paid",    dueDate:"Jun 01, 2025" },
  { id:2, kitchen:"Spice Route",     owner:"Ramesh Kumar",  totalOrders:98,  grossEarnings:65700,  payoutStatus:"Pending", dueDate:"Jun 05, 2025" },
  { id:3, kitchen:"Healthy Meals",   owner:"Priya Mehta",   totalOrders:87,  grossEarnings:54800,  payoutStatus:"Paid",    dueDate:"Jun 01, 2025" },
  { id:4, kitchen:"Tasty Tiffins",   owner:"Suresh Patel",  totalOrders:76,  grossEarnings:50600,  payoutStatus:"Paid",    dueDate:"Jun 01, 2025" },
  { id:5, kitchen:"HomeBites",       owner:"Kavitha Reddy", totalOrders:66,  grossEarnings:40900,  payoutStatus:"Pending", dueDate:"Jun 07, 2025" },
  { id:6, kitchen:"Mumbai Spice",    owner:"Anil Desai",    totalOrders:54,  grossEarnings:36200,  payoutStatus:"Paid",    dueDate:"Jun 01, 2025" },
  { id:7, kitchen:"South Indian",    owner:"Lakshmi Iyer",  totalOrders:48,  grossEarnings:30400,  payoutStatus:"Paid",    dueDate:"Jun 01, 2025" },
];

/* ─── Recent commission transactions ────────────────────────────────────── */
interface CommTx {
  id: string; kitchen: string; orderAmt: number; pct: number;
  status: "Collected"|"Pending"|"Failed"; date: string;
}
const TRANSACTIONS: CommTx[] = [
  { id:"#ORD-1234", kitchen:"Anita's Kitchen", orderAmt:360, pct:4, status:"Collected", date:"May 18, 2025" },
  { id:"#ORD-1233", kitchen:"Spice Route",     orderAmt:380, pct:4, status:"Collected", date:"May 18, 2025" },
  { id:"#ORD-1232", kitchen:"Healthy Meals",   orderAmt:460, pct:4, status:"Pending",   date:"May 17, 2025" },
  { id:"#ORD-1231", kitchen:"Tasty Tiffins",   orderAmt:320, pct:4, status:"Collected", date:"May 17, 2025" },
  { id:"#ORD-1230", kitchen:"Anita's Kitchen", orderAmt:380, pct:4, status:"Collected", date:"May 16, 2025" },
  { id:"#ORD-1229", kitchen:"Mumbai Spice",    orderAmt:420, pct:4, status:"Pending",   date:"May 16, 2025" },
  { id:"#ORD-1228", kitchen:"South Indian",    orderAmt:240, pct:4, status:"Failed",    date:"May 15, 2025" },
];

/* ─── Style helpers ──────────────────────────────────────────────────────── */
const pill = (s: string): React.CSSProperties => {
  const map: Record<string,{bg:string;color:string}> = {
    Collected: {bg:"rgba(87,184,105,0.12)",  color:"#57b869"},
    Paid:      {bg:"rgba(87,184,105,0.12)",  color:"#57b869"},
    Active:    {bg:"rgba(87,184,105,0.12)",  color:"#57b869"},
    Pending:   {bg:"rgba(242,140,64,0.12)",  color:"#f28c40"},
    Inactive:  {bg:"rgba(242,89,89,0.12)",   color:"#f25959"},
    Failed:    {bg:"rgba(242,89,89,0.12)",   color:"#f25959"},
  };
  const c = map[s] ?? {bg:"#f2f5f7",color:"#9499a6"};
  return { padding:"4px 12px", borderRadius:12, fontSize:12, fontFamily:"Inter", fontWeight:600, background:c.bg, color:c.color };
};

const commPill: React.CSSProperties = {
  padding:"3px 10px", borderRadius:20, fontSize:12, fontFamily:"Inter", fontWeight:700,
  background:"rgba(120,135,250,0.10)", color:"#7887fa", border:"1px solid rgba(120,135,250,0.2)",
};

const TH = ({ children, w }: { children: React.ReactNode; w: number }) => (
  <p style={{ width:w, flexShrink:0, fontFamily:"Inter", fontWeight:600, fontSize:12, color:"#9499a6" }}>{children}</p>
);
const TD = ({ children, w, bold, accent }: { children: React.ReactNode; w: number; bold?: boolean; accent?: string }) => (
  <p style={{ width:w, flexShrink:0, fontFamily:"Inter", fontWeight: bold?700:400, fontSize:13, color: accent ?? "#1c1f29" }}>{children}</p>
);

/* ─── Main component ─────────────────────────────────────────────────────── */
const blankRate: Omit<CommRate,"id"> = { kitchen:"", owner:"", commissionPct:4, totalOrders:0, totalOrderValue:0, status:"Active" };

function validateRate(f: Omit<CommRate,"id">): Record<string,string> {
  const e: Record<string,string> = {};
  if (!f.kitchen.trim()) e.kitchen = "Kitchen name is required";
  if (!f.owner.trim())   e.owner   = "Owner name is required";
  if (f.commissionPct < 0 || f.commissionPct > 100) e.commissionPct = "Must be between 0 and 100";
  return e;
}

export function EarningsPage() {
  const [rates,   setRates]   = useState<CommRate[]>(INIT_RATES);
  const [viewing, setViewing] = useState<CommRate|null>(null);
  const [editing, setEditing] = useState<CommRate|null>(null);
  const [draft,   setDraft]   = useState<CommRate|null>(null);
  const [del,     setDel]     = useState<CommRate|null>(null);
  const [txTab,   setTxTab]   = useState<"all"|"Collected"|"Pending"|"Failed">("all");
  const [adding,  setAdding]  = useState(false);
  const [addForm, setAddForm] = useState<Omit<CommRate,"id">>(blankRate);
  const [addErrs, setAddErrs] = useState<Record<string,string>>({});

  const totalCommission = rates.reduce((s,r) => s + Math.round(r.totalOrderValue * r.commissionPct / 100), 0);
  const filteredTx = TRANSACTIONS.filter(t => txTab==="all" || t.status===txTab);

  const startEdit  = (r:CommRate) => { setEditing(r); setDraft({...r}); };
  const saveEdit   = () => { if(!draft) return; setRates(p=>p.map(r=>r.id===draft.id?draft:r)); setEditing(null); setDraft(null); };
  const doDelete   = () => { if(!del) return; setRates(p=>p.filter(r=>r.id!==del.id)); setDel(null); };
  const submitAdd  = () => {
    const errs = validateRate(addForm);
    if (Object.keys(errs).length) { setAddErrs(errs); return; }
    setRates(p => [...p, { ...addForm, id: Date.now() }]);
    setAdding(false); setAddForm(blankRate); setAddErrs({});
  };
  const ErrMsg = ({field}:{field:string}) =>
    addErrs[field] ? <p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:2}}>{addErrs[field]}</p> : null;

  return (
    <div className="flex flex-col gap-6">

      {/* Header */}
      <div>
        <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Earnings</p>
        <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>
          TiffinCraft commission earnings — cooks pay a <strong style={{color:"#7887fa"}}>4% commission</strong> on every successful delivery.
        </p>
      </div>

      {/* Stat cards */}
      <div className="flex gap-4">
        {[
          { label:"Total Commission Earned", value:fmt(totalCommission), icon:"💹", change:"+21.3%", sub:"All time, all cooks" },
          { label:"Commission This Month",   value:"₹4,860",             icon:"📅", change:"+16.2%", sub:"June 2025"          },
          { label:"Fixed Commission Rate",   value:"4%",                  icon:"📊", change:"",       sub:"Per successful order"},
          { label:"Pending Collection",      value:"₹51",                icon:"⏳", change:"3 orders",sub:"Awaiting delivery"  },
        ].map(s=>(
          <div key={s.label} className="bg-white flex flex-col gap-2 p-5 rounded-[12px] flex-1" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
            <div style={{display:"flex",alignItems:"center",justifyContent:"space-between"}}>
              <span style={{fontSize:22}}>{s.icon}</span>
              {s.change && <span style={{fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#57b869"}}>{s.change}</span>}
            </div>
            <p style={{fontFamily:"Inter",fontWeight:700,fontSize:26,color:"#1c1f29"}}>{s.value}</p>
            <p style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#1c1f29"}}>{s.label}</p>
            <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6"}}>{s.sub}</p>
          </div>
        ))}
      </div>

      {/* Chart */}
      <div className="bg-white p-6 rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:16}}>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Revenue Trend</p>
          <div style={{display:"flex",gap:20}}>
            {[{c:"#7887FA",l:"TiffinCraft (4% Commission)"},{c:"#57B869",l:"Cook Net Earnings (96%)"}].map(x=>(
              <div key={x.l} style={{display:"flex",alignItems:"center",gap:6}}>
                <div style={{width:10,height:10,borderRadius:"50%",background:x.c}}/>
                <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6"}}>{x.l}</p>
              </div>
            ))}
          </div>
        </div>
        <div style={{height:200}}>
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={CHART} margin={{top:10,right:20,left:-10,bottom:0}}>
              <defs>
                <linearGradient id="eG1" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#7887FA" stopOpacity={0.2}/>
                  <stop offset="95%" stopColor="#7887FA" stopOpacity={0}/>
                </linearGradient>
                <linearGradient id="eG2" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%"  stopColor="#57B869" stopOpacity={0.15}/>
                  <stop offset="95%" stopColor="#57B869" stopOpacity={0}/>
                </linearGradient>
              </defs>
              <XAxis dataKey="month" tick={{fontSize:12,fill:"#9499a6",fontFamily:"Inter"}} axisLine={false} tickLine={false}/>
              <YAxis hide/>
              <Tooltip contentStyle={{background:"white",border:"1px solid #e5e8ed",borderRadius:8,fontSize:12,fontFamily:"Inter"}}
                formatter={(v:number,name:string)=>[`₹${v.toLocaleString("en-IN")}`,name==="commission"?"TiffinCraft 4%":"Cook Net (96%)"]}/>
              <Area key="ec" type="monotone" dataKey="commission" stroke="#7887FA" strokeWidth={2} fill="url(#eG1)" dot={{r:3,fill:"#7887FA",stroke:"white",strokeWidth:2}}/>
              <Area key="ek" type="monotone" dataKey="cookNet"    stroke="#57B869" strokeWidth={2} fill="url(#eG2)" dot={{r:3,fill:"#57B869",stroke:"white",strokeWidth:2}}/>
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* ── Commission Rates table ── */}
      <div className="bg-white rounded-[12px] p-6" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:20}}>
          <div>
            <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Commission Rates</p>
            <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6",marginTop:2}}>4% fixed deducted from every successful delivery</p>
          </div>
          <button onClick={()=>{ setAdding(true); setAddForm(blankRate); setAddErrs({}); }}
            style={{padding:"10px 18px",borderRadius:8,border:"none",background:"#57b869",fontFamily:"Inter",fontWeight:600,fontSize:13,color:"white",cursor:"pointer"}}>
            + Set Commission
          </button>
        </div>
        <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
          <TH w={40}>S.N</TH>
          {["Kitchen","Owner","Rate","Total Orders","Order Value","Commission Earned","Status","Actions"].map((h,i)=>(
            <TH key={h} w={[170,140,80,110,110,140,90,110][i]}>{h}</TH>
          ))}
        </div>
        {rates.map((r,idx)=>{
          const comm = Math.round(r.totalOrderValue * r.commissionPct / 100);
          return (
            <div key={r.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <TD w={40} accent="#9499a6">{idx+1}</TD>
                <div style={{width:170,flexShrink:0,display:"flex",alignItems:"center",gap:10}}>
                  <div style={{width:36,height:36,borderRadius:"50%",background:"#D9DEE6",display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0}}>
                    <span style={{fontFamily:"Inter",fontWeight:700,fontSize:12,color:"#6b7280"}}>{r.kitchen.slice(0,2)}</span>
                  </div>
                  <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{r.kitchen}</p>
                </div>
                <TD w={140}>{r.owner}</TD>
                <div style={{width:80,flexShrink:0}}><span style={commPill}>{r.commissionPct}%</span></div>
                <TD w={110}>{r.totalOrders} orders</TD>
                <TD w={110}>{fmt(r.totalOrderValue)}</TD>
                <TD w={140} bold accent="#57b869">{fmt(comm)}</TD>
                <div style={{width:90,flexShrink:0}}><span style={pill(r.status)}>{r.status}</span></div>
                <div style={{width:110,flexShrink:0}}>
                  <ActionButtons onView={()=>setViewing(r)} onEdit={()=>startEdit(r)} onDelete={()=>setDel(r)}/>
                </div>
              </div>
              {idx<rates.length-1 && <div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          );
        })}
      </div>

      {/* ── Cook Earnings table ── */}
      <div className="bg-white rounded-[12px] p-6" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div style={{marginBottom:20}}>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Cook Earnings</p>
          <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6",marginTop:2}}>Net amount payable to each cook after 4% commission deduction</p>
        </div>
        <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
          <TH w={40}>S.N</TH>
          {["Kitchen","Owner","Total Orders","Gross Earnings","Commission (4%)","Net Earnings","Payout Status","Due Date"].map((h,i)=>(
            <TH key={h} w={[160,140,100,120,120,120,110,110][i]}>{h}</TH>
          ))}
        </div>
        {COOK_EARNINGS.map((c,idx)=>{
          const commAmt = Math.round(c.grossEarnings * 0.04);
          const netAmt  = c.grossEarnings - commAmt;
          return (
            <div key={c.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <TD w={40} accent="#9499a6">{idx+1}</TD>
                <div style={{width:160,flexShrink:0,display:"flex",alignItems:"center",gap:10}}>
                  <div style={{width:36,height:36,borderRadius:"50%",background:"#D9DEE6",display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0}}>
                    <span style={{fontFamily:"Inter",fontWeight:700,fontSize:12,color:"#6b7280"}}>{c.kitchen.slice(0,2)}</span>
                  </div>
                  <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{c.kitchen}</p>
                </div>
                <TD w={140}>{c.owner}</TD>
                <TD w={100}>{c.totalOrders}</TD>
                <TD w={120} bold>{fmt(c.grossEarnings)}</TD>
                <div style={{width:120,flexShrink:0}}>
                  <span style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#f25959"}}>− {fmt(commAmt)}</span>
                </div>
                <TD w={120} bold accent="#57b869">{fmt(netAmt)}</TD>
                <div style={{width:110,flexShrink:0}}><span style={pill(c.payoutStatus)}>{c.payoutStatus}</span></div>
                <TD w={110} accent="#9499a6">{c.dueDate}</TD>
              </div>
              {idx<COOK_EARNINGS.length-1 && <div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          );
        })}
        {/* Footer totals */}
        <div className="flex gap-4 items-center pt-4 mt-2" style={{borderTop:"2px solid #e5e8ed"}}>
          <div style={{width:40,flexShrink:0}}/>
          <div style={{width:160,flexShrink:0}}><p style={{fontFamily:"Inter",fontWeight:700,fontSize:13,color:"#1c1f29"}}>Total</p></div>
          <div style={{width:140,flexShrink:0}}/>
          <TD w={100} bold>{COOK_EARNINGS.reduce((s,c)=>s+c.totalOrders,0)}</TD>
          <TD w={120} bold>{fmt(COOK_EARNINGS.reduce((s,c)=>s+c.grossEarnings,0))}</TD>
          <div style={{width:120,flexShrink:0}}>
            <span style={{fontFamily:"Inter",fontWeight:700,fontSize:13,color:"#f25959"}}>
              − {fmt(COOK_EARNINGS.reduce((s,c)=>s+Math.round(c.grossEarnings*0.04),0))}
            </span>
          </div>
          <TD w={120} bold accent="#57b869">
            {fmt(COOK_EARNINGS.reduce((s,c)=>s+(c.grossEarnings-Math.round(c.grossEarnings*0.04)),0))}
          </TD>
          <div style={{width:110,flexShrink:0}}/>
          <div style={{width:110,flexShrink:0}}/>
        </div>
      </div>

      {/* ── Commission Transactions ── */}
      <div className="bg-white rounded-[12px] p-6" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:16}}>
          <p style={{fontFamily:"Inter",fontWeight:600,fontSize:18,color:"#1c1f29"}}>Commission Transactions</p>
          <div style={{display:"flex",gap:8}}>
            {(["all","Collected","Pending","Failed"] as const).map(t=>(
              <button key={t} onClick={()=>setTxTab(t)}
                style={{padding:"6px 14px",borderRadius:8,fontSize:12,cursor:"pointer",border:"none",fontFamily:"Inter",fontWeight:500,
                  background:txTab===t?"#57b869":"#f2f5f7",color:txTab===t?"#fff":"#9499a6"}}>
                {t==="all"?"All":t}
              </button>
            ))}
          </div>
        </div>
        <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
          <TH w={40}>S.N</TH>
          {["Order ID","Kitchen","Order Amt","Rate","Commission","Cook Gets","Status","Date"].map((h,i)=>(
            <TH key={h} w={[110,160,100,70,110,110,110,110][i]}>{h}</TH>
          ))}
        </div>
        {filteredTx.map((t,idx)=>{
          const comm    = Math.round(t.orderAmt * t.pct / 100);
          const cookGet = t.orderAmt - comm;
          return (
            <div key={t.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <TD w={40} accent="#9499a6">{idx+1}</TD>
                <p style={{width:110,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#7887fa"}}>{t.id}</p>
                <TD w={160}>{t.kitchen}</TD>
                <TD w={100} bold>{fmt(t.orderAmt)}</TD>
                <div style={{width:70,flexShrink:0}}><span style={commPill}>{t.pct}%</span></div>
                <TD w={110} bold accent="#57b869">{fmt(comm)}</TD>
                <TD w={110} bold accent="#1c1f29">{fmt(cookGet)}</TD>
                <div style={{width:110,flexShrink:0}}><span style={pill(t.status)}>{t.status}</span></div>
                <TD w={110} accent="#9499a6">{t.date}</TD>
              </div>
              {idx<filteredTx.length-1 && <div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          );
        })}
      </div>

      {/* ── Modals ── */}
      {viewing && (
        <Modal title="Commission Details" onClose={()=>setViewing(null)}>
          <div className="flex items-center gap-4 mb-5">
            <div style={{width:56,height:56,borderRadius:"50%",background:"#D9DEE6",display:"flex",alignItems:"center",justifyContent:"center"}}>
              <span style={{fontFamily:"Inter",fontWeight:700,fontSize:16,color:"#6b7280"}}>{viewing.kitchen.slice(0,2)}</span>
            </div>
            <div>
              <p style={{fontFamily:"Inter",fontWeight:700,fontSize:18,color:"#1c1f29"}}>{viewing.kitchen}</p>
              <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>by {viewing.owner}</p>
            </div>
          </div>
          <DetailRow label="Commission Rate"  value={<span style={{fontFamily:"Inter",fontWeight:700,fontSize:18,color:"#7887fa"}}>{viewing.commissionPct}%</span>}/>
          <DetailRow label="Total Orders"     value={`${viewing.totalOrders} orders`}/>
          <DetailRow label="Total Order Value" value={fmt(viewing.totalOrderValue)}/>
          <DetailRow label="Commission Earned" value={<span style={{fontWeight:700,color:"#57b869"}}>{fmt(Math.round(viewing.totalOrderValue*viewing.commissionPct/100))}</span>}/>
          <DetailRow label="Cook Net Earnings" value={fmt(viewing.totalOrderValue - Math.round(viewing.totalOrderValue*viewing.commissionPct/100))}/>
          <DetailRow label="Status"           value={<span style={pill(viewing.status)}>{viewing.status}</span>}/>
        </Modal>
      )}

      {editing && draft && (
        <Modal title="Update Commission Rate" onClose={()=>{setEditing(null);setDraft(null);}}>
          <div className="mb-5 p-4 rounded-[10px]" style={{background:"rgba(120,135,250,0.06)",border:"1px solid rgba(120,135,250,0.15)"}}>
            <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#7887fa",marginBottom:4}}>ℹ️ Live Preview</p>
            <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6",lineHeight:1.6}}>
              On a <strong style={{color:"#1c1f29"}}>₹400 order</strong> from <strong style={{color:"#1c1f29"}}>{draft.kitchen}</strong>:<br/>
              TiffinCraft earns <strong style={{color:"#7887fa"}}>₹{Math.round(400*draft.commissionPct/100)}</strong> ({draft.commissionPct}%) &nbsp;|&nbsp;
              Cook receives <strong style={{color:"#57b869"}}>₹{400-Math.round(400*draft.commissionPct/100)}</strong>
            </p>
          </div>
          <FormField label="Kitchen Name" value={draft.kitchen} onChange={v=>setDraft({...draft,kitchen:v})}/>
          <FormField label="Owner Name"   value={draft.owner}   onChange={v=>setDraft({...draft,owner:v})}/>
          <FormField label="Commission % (fixed per delivery)" value={String(draft.commissionPct)}
            onChange={v=>setDraft({...draft,commissionPct:Math.min(100,Math.max(0,parseFloat(v)||0))})} type="number"/>
          <FormField label="Status" value={draft.status} onChange={v=>setDraft({...draft,status:v as "Active"|"Inactive"})} options={["Active","Inactive"]}/>
          <SaveCancel onCancel={()=>{setEditing(null);setDraft(null);}} onSave={saveEdit}/>
        </Modal>
      )}

      {/* Add Commission modal */}
      {adding && (
        <Modal title="Set Commission Rate" onClose={()=>{ setAdding(false); setAddErrs({}); }}>
          <div className="mb-4 p-4 rounded-[10px]" style={{background:"rgba(120,135,250,0.06)",border:"1px solid rgba(120,135,250,0.15)"}}>
            <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#7887fa",marginBottom:4}}>ℹ️ How it works</p>
            <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6",lineHeight:1.6}}>
              On a <strong style={{color:"#1c1f29"}}>₹400 order</strong> at <strong style={{color:"#7887fa"}}>{addForm.commissionPct}%</strong>, TiffinCraft earns{" "}
              <strong style={{color:"#57b869"}}>₹{Math.round(400*addForm.commissionPct/100)}</strong> and the cook receives{" "}
              <strong style={{color:"#1c1f29"}}>₹{400-Math.round(400*addForm.commissionPct/100)}</strong>.
            </p>
          </div>
          <FormField label="Kitchen Name" value={addForm.kitchen} onChange={v=>setAddForm({...addForm,kitchen:v})}/>
          {addErrs.kitchen && <p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:-10,marginBottom:10}}>{addErrs.kitchen}</p>}
          <FormField label="Owner Name"   value={addForm.owner}   onChange={v=>setAddForm({...addForm,owner:v})}/>
          {addErrs.owner && <p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:-10,marginBottom:10}}>{addErrs.owner}</p>}
          <FormField label="Commission % (e.g. 4)" value={String(addForm.commissionPct)}
            onChange={v=>setAddForm({...addForm,commissionPct:Math.min(100,Math.max(0,parseFloat(v)||0))})} type="number"/>
          {addErrs.commissionPct && <p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:-10,marginBottom:10}}>{addErrs.commissionPct}</p>}
          <FormField label="Status" value={addForm.status} onChange={v=>setAddForm({...addForm,status:v as "Active"|"Inactive"})} options={["Active","Inactive"]}/>
          <SaveCancel onCancel={()=>{ setAdding(false); setAddErrs({}); }} onSave={submitAdd}/>
        </Modal>
      )}

      {del && <ConfirmDelete name={del.kitchen} onConfirm={doDelete} onCancel={()=>setDel(null)}/>}
    </div>
  );
}
