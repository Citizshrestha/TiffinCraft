import React, { useState, useEffect } from "react";
import { StatusBadge, StatusType } from "./StatusBadge";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow, FormField, SaveCancel } from "./Modal";
import { exportCSV } from "../utils/csv";

const PER_PAGE = 10;

interface Payment { id:string; orderId:string; customer:string; cook:string; amount:string; method:string; status:StatusType; date:string; }

const SEED: Payment[] = [
  { id:"#PAY-5001", orderId:"#ORD-1234", customer:"Rahul Sharma",  cook:"Anita's Kitchen", amount:"₹360", method:"UPI",         status:"paid",     date:"May 18, 2025" },
  { id:"#PAY-5002", orderId:"#ORD-1233", customer:"Priya Patel",   cook:"Spice Route",     amount:"₹380", method:"Card",        status:"paid",     date:"May 18, 2025" },
  { id:"#PAY-5003", orderId:"#ORD-1232", customer:"Vikram Singh",  cook:"Healthy Meals",   amount:"₹460", method:"Net Banking", status:"pending",  date:"May 17, 2025" },
  { id:"#PAY-5004", orderId:"#ORD-1231", customer:"Neha Gupta",    cook:"Tasty Tiffins",   amount:"₹320", method:"UPI",         status:"paid",     date:"May 17, 2025" },
  { id:"#PAY-5005", orderId:"#ORD-1230", customer:"Amit Kumar",    cook:"Anita's Kitchen", amount:"₹380", method:"Wallet",      status:"refunded", date:"May 16, 2025" },
  { id:"#PAY-5006", orderId:"#ORD-1229", customer:"Sunita Mehta",  cook:"Mumbai Spice",    amount:"₹420", method:"Card",        status:"pending",  date:"May 16, 2025" },
  { id:"#PAY-5007", orderId:"#ORD-1228", customer:"Ravi Nair",     cook:"South Indian",    amount:"₹240", method:"UPI",         status:"failed",   date:"May 15, 2025" },
  { id:"#PAY-5008", orderId:"#ORD-1227", customer:"Kavya Reddy",   cook:"Bengal Bites",    amount:"₹350", method:"Card",        status:"paid",     date:"May 15, 2025" },
  { id:"#PAY-5009", orderId:"#ORD-1226", customer:"Arjun Sharma",  cook:"Delhi Delights",  amount:"₹290", method:"UPI",         status:"paid",     date:"May 14, 2025" },
  { id:"#PAY-5010", orderId:"#ORD-1225", customer:"Pooja Verma",   cook:"Punjab Kitchen",  amount:"₹310", method:"Net Banking", status:"paid",     date:"May 14, 2025" },
  { id:"#PAY-5011", orderId:"#ORD-1224", customer:"Karan Malhotra",cook:"Anita's Kitchen", amount:"₹390", method:"Wallet",      status:"pending",  date:"May 13, 2025" },
];

const STAT_TABS = [
  { id:"all",      label:"All"      },
  { id:"paid",     label:"Paid"     },
  { id:"pending",  label:"Pending"  },
  { id:"refunded", label:"Refunded" },
  { id:"failed",   label:"Failed"   },
];

export function PaymentsPage() {
  const [rows,    setRows]    = useState<Payment[]>(SEED);
  const [search,  setSearch]  = useState("");
  const [tab,     setTab]     = useState("all");
  const [page,    setPage]    = useState(1);
  const [viewing, setViewing] = useState<Payment|null>(null);
  const [editing, setEditing] = useState<Payment|null>(null);
  const [draft,   setDraft]   = useState<Payment|null>(null);
  const [del,     setDel]     = useState<Payment|null>(null);

  const filtered = rows.filter(p => {
    const s = !search || p.id.toLowerCase().includes(search.toLowerCase()) || p.customer.toLowerCase().includes(search.toLowerCase());
    const t = tab==="all" || p.status===tab;
    return s && t;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible    = filtered.slice((page-1)*PER_PAGE, page*PER_PAGE);

  useEffect(() => { setPage(1); }, [tab, search]);

  const startEdit = (p: Payment) => { setEditing(p); setDraft({...p}); };
  const saveEdit  = () => { if(!draft) return; setRows(p=>p.map(x=>x.id===draft.id?draft:x)); setEditing(null); setDraft(null); };
  const doDelete  = () => { if(!del) return; setRows(p=>p.filter(x=>x.id!==del.id)); setDel(null); };

  const handleExport = () =>
    exportCSV("payments.csv", filtered.map(p => ({
      "Payment ID": p.id, "Order ID": p.orderId, Customer: p.customer,
      Cook: p.cook, Amount: p.amount, Method: p.method, Status: p.status, Date: p.date,
    })));

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Payments</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>View and manage all payment transactions.</p>
        </div>
        <button onClick={handleExport}
          className="self-start shrink-0"
          style={{padding:"10px 16px",borderRadius:8,border:"1px solid #e5e8ed",background:"white",fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6",cursor:"pointer"}}>
          Export CSV
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {[
          {label:"Total Collected",    value:"₹1,45,230", icon:"💰"},
          {label:"Pending Payouts",    value:"₹12,400",   icon:"⏳"},
          {label:"Refunds Issued",     value:"₹3,200",    icon:"↩️"},
          {label:"Failed Transactions",value:"7",         icon:"❌"},
        ].map(s=>(
          <div key={s.label} className="bg-white flex items-center gap-4 p-5 rounded-[12px] flex-1" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
            <span style={{fontSize:28}}>{s.icon}</span>
            <div>
              <p style={{fontFamily:"Inter",fontWeight:700,fontSize:20,color:"#1c1f29"}}>{s.value}</p>
              <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{s.label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Status filter tabs */}
      <div className="flex gap-2 flex-wrap">
        {STAT_TABS.map(t=>(
          <button key={t.id} onClick={()=>setTab(t.id)}
            style={{padding:"10px 16px",borderRadius:8,fontSize:13,cursor:"pointer",border:"none",fontFamily:"Inter",fontWeight:500,
              background:tab===t.id?"#3b82f6":"#f2f5f7",color:tab===t.id?"#fff":"#9499a6"}}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white" style={{border:"1px solid #e5e8ed"}}>
        <span>🔍</span>
        <input className="flex-1 outline-none bg-transparent" style={{border:"none",fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
          placeholder="Search payments..." value={search} onChange={e=>setSearch(e.target.value)}/>
      </div>

      <div className="bg-white rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div className="p-4 sm:p-6 overflow-x-auto">
          <div className="min-w-[1140px]">
          <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
            <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>S.N</p>
            {["Pay ID","Order ID","Customer","Cook","Amount","Method","Status","Date","Actions"].map((h,i)=>(
              <p key={h} style={{width:[100,100,130,130,75,100,100,100,110][i],flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>{h}</p>
            ))}
          </div>
          {visible.length===0 && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>No payments found.</p>
          )}
          {visible.map((p,idx)=>(
            <div key={p.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <p style={{width:40, flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6"}}>{(page-1)*PER_PAGE+idx+1}</p>
                <p style={{width:100,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#7887fa"}}>{p.id}</p>
                <p style={{width:100,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#7887fa"}}>{p.orderId}</p>
                <p style={{width:130,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{p.customer}</p>
                <p style={{width:130,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{p.cook}</p>
                <p style={{width:75, flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{p.amount}</p>
                <p style={{width:100,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{p.method}</p>
                <div style={{width:100,flexShrink:0}}><StatusBadge status={p.status}/></div>
                <p style={{width:100,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{p.date}</p>
                <div style={{width:110,flexShrink:0}}>
                  <ActionButtons onView={()=>setViewing(p)} onEdit={()=>startEdit(p)} onDelete={()=>setDel(p)}/>
                </div>
              </div>
              {idx<visible.length-1 && <div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          ))}
          </div>
        </div>
      </div>

      <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage}/>

      {viewing && (
        <Modal title="Payment Details" onClose={()=>setViewing(null)}>
          <DetailRow label="Payment ID" value={<span style={{color:"#7887fa",fontWeight:600}}>{viewing.id}</span>}/>
          <DetailRow label="Order ID"   value={<span style={{color:"#7887fa"}}>{viewing.orderId}</span>}/>
          <DetailRow label="Customer"   value={viewing.customer}/>
          <DetailRow label="Cook"       value={viewing.cook}/>
          <DetailRow label="Amount"     value={<span style={{fontWeight:700}}>{viewing.amount}</span>}/>
          <DetailRow label="Method"     value={viewing.method}/>
          <DetailRow label="Status"     value={<StatusBadge status={viewing.status}/>}/>
          <DetailRow label="Date"       value={viewing.date}/>
        </Modal>
      )}

      {editing && draft && (
        <Modal title="Update Payment" onClose={()=>{setEditing(null);setDraft(null);}}>
          <FormField label="Customer" value={draft.customer} onChange={v=>setDraft({...draft,customer:v})}/>
          <FormField label="Amount"   value={draft.amount}   onChange={v=>setDraft({...draft,amount:v})}/>
          <FormField label="Method"   value={draft.method}   onChange={v=>setDraft({...draft,method:v})} options={["UPI","Card","Net Banking","Wallet","Cash"]}/>
          <FormField label="Status"   value={draft.status}   onChange={v=>setDraft({...draft,status:v as StatusType})} options={["paid","pending","refunded","failed"]}/>
          <SaveCancel onCancel={()=>{setEditing(null);setDraft(null);}} onSave={saveEdit}/>
        </Modal>
      )}

      {del && <ConfirmDelete name={del.id} onConfirm={doDelete} onCancel={()=>setDel(null)}/>}
    </div>
  );
}
