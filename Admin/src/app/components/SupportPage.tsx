import React, { useState, useEffect, useRef } from "react";
import { Pagination } from "./Pagination";
import { Modal, DetailRow } from "./Modal";

const PER_PAGE = 10;

type TicketStatus = "pending" | "processing" | "resolved";
interface Ticket {
  id: string; user: string; subject: string; priority: "High"|"Medium"|"Low";
  status: TicketStatus; date: string; message: string; reply?: string;
}

const SEED: Ticket[] = [
  { id:"#TKT-001", user:"Rahul Sharma",  subject:"Order not delivered",       priority:"High",   status:"pending",    date:"May 18, 2025", message:"My order #ORD-1234 was not delivered. Please resolve urgently." },
  { id:"#TKT-002", user:"Priya Patel",   subject:"Wrong item delivered",       priority:"High",   status:"processing", date:"May 18, 2025", message:"I received Veg Thali instead of Dal Makhani." },
  { id:"#TKT-003", user:"Vikram Singh",  subject:"Refund request",             priority:"Medium", status:"resolved",   date:"May 17, 2025", message:"I cancelled my order. Please process the refund." },
  { id:"#TKT-004", user:"Neha Gupta",    subject:"Cook not responding",        priority:"High",   status:"pending",    date:"May 17, 2025", message:"Anita's Kitchen is not responding to my messages." },
  { id:"#TKT-005", user:"Amit Kumar",    subject:"Payment deducted twice",     priority:"High",   status:"processing", date:"May 16, 2025", message:"My account was debited twice for order #ORD-1230." },
  { id:"#TKT-006", user:"Sunita Mehta",  subject:"App crash issue",            priority:"Low",    status:"resolved",   date:"May 15, 2025", message:"App crashes when I try to view order history." },
  { id:"#TKT-007", user:"Ravi Nair",     subject:"Cannot place order",         priority:"Medium", status:"pending",    date:"May 15, 2025", message:"Checkout keeps failing after payment." },
  { id:"#TKT-008", user:"Kavya Reddy",   subject:"Delivery address wrong",     priority:"Medium", status:"processing", date:"May 14, 2025", message:"The app shows my old address even after updating." },
  { id:"#TKT-009", user:"Arjun Sharma",  subject:"Missing item in order",      priority:"Low",    status:"resolved",   date:"May 14, 2025", message:"My order was missing the raita that was listed." },
  { id:"#TKT-010", user:"Pooja Verma",   subject:"Discount code not working",  priority:"Low",    status:"pending",    date:"May 13, 2025", message:"Coupon SUMMER25 says invalid but it was valid yesterday." },
  { id:"#TKT-011", user:"Karan Malhotra",subject:"Food was stale",             priority:"High",   status:"pending",    date:"May 13, 2025", message:"The biryani I received was clearly not fresh." },
];

const statusStyle = (s: TicketStatus): React.CSSProperties => ({
  padding:"4px 10px", borderRadius:12, fontSize:12, fontFamily:"Inter", fontWeight:600,
  background: s==="resolved"?"rgba(87,184,105,0.12)":s==="processing"?"rgba(242,199,64,0.12)":"rgba(242,140,64,0.12)",
  color:       s==="resolved"?"#57b869":s==="processing"?"#f2c740":"#f28c40",
});

const priorityStyle = (p: "High"|"Medium"|"Low"): React.CSSProperties => ({
  padding:"3px 8px", borderRadius:8, fontSize:11, fontFamily:"Inter", fontWeight:600,
  background: p==="High"?"rgba(242,89,89,0.12)":p==="Medium"?"rgba(242,199,64,0.12)":"rgba(87,184,105,0.12)",
  color:       p==="High"?"#f25959":p==="Medium"?"#f2c740":"#57b869",
});

interface KebabMenuProps {
  onView: () => void;
  onReply: () => void;
  onClose: () => void;
  onDelete: () => void;
}

function KebabMenu({ onView, onReply, onClose, onDelete }: KebabMenuProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key==="Escape") setOpen(false); };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, []);

  const actions = [
    { label:"View Details", fn: onView,   color:"#1c1f29" },
    { label:"Reply",        fn: onReply,  color:"#1c1f29" },
    { label:"Close Ticket", fn: onClose,  color:"#f28c40" },
    { label:"Delete",       fn: onDelete, color:"#f25959" },
  ];

  return (
    <div ref={ref} style={{position:"relative"}}>
      <button onClick={e=>{e.stopPropagation();setOpen(v=>!v);}}
        style={{width:32,height:32,borderRadius:6,border:"none",background:"#f2f5f7",cursor:"pointer",fontSize:18,color:"#9499a6",display:"flex",alignItems:"center",justifyContent:"center"}}
        onMouseEnter={e=>((e.currentTarget as HTMLElement).style.background="#e5e8ed")}
        onMouseLeave={e=>((e.currentTarget as HTMLElement).style.background="#f2f5f7")}>
        ⋮
      </button>
      {open && (
        <div style={{position:"absolute",right:0,top:36,zIndex:50,background:"white",borderRadius:8,padding:"4px 0",minWidth:150,
          boxShadow:"0 4px 16px rgba(0,0,0,0.14)",border:"1px solid #e5e8ed"}}>
          {actions.map(a=>(
            <button key={a.label} onClick={e=>{e.stopPropagation();a.fn();setOpen(false);}}
              style={{width:"100%",textAlign:"left",padding:"10px 16px",fontSize:13,fontFamily:"Inter",fontWeight:400,color:a.color,
                background:"none",border:"none",cursor:"pointer",display:"block"}}
              onMouseEnter={e=>((e.currentTarget as HTMLElement).style.background="#f7f8fa")}
              onMouseLeave={e=>((e.currentTarget as HTMLElement).style.background="none")}>
              {a.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export function SupportPage() {
  const [tickets,  setTickets]  = useState<Ticket[]>(SEED);
  const [search,   setSearch]   = useState("");
  const [tabStatus,setTabStatus]= useState<"all"|TicketStatus>("all");
  const [page,     setPage]     = useState(1);
  const [viewing,  setViewing]  = useState<Ticket|null>(null);
  const [replying, setReplying] = useState<Ticket|null>(null);
  const [reply,    setReply]    = useState("");
  const [replyErr, setReplyErr] = useState("");

  const filtered = tickets.filter(t => {
    const s = !search || t.id.toLowerCase().includes(search.toLowerCase())
           || t.user.toLowerCase().includes(search.toLowerCase())
           || t.subject.toLowerCase().includes(search.toLowerCase());
    const st = tabStatus==="all" || t.status===tabStatus;
    return s && st;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible    = filtered.slice((page-1)*PER_PAGE, page*PER_PAGE);

  useEffect(() => { setPage(1); }, [search, tabStatus]);

  const counts = {
    pending:    tickets.filter(t=>t.status==="pending").length,
    processing: tickets.filter(t=>t.status==="processing").length,
    resolved:   tickets.filter(t=>t.status==="resolved").length,
  };

  const closeTicket = (id: string) =>
    setTickets(p => p.map(t => t.id===id ? {...t, status:"resolved"} : t));

  const deleteTicket = (id: string) =>
    setTickets(p => p.filter(t => t.id!==id));

  const sendReply = () => {
    if (!reply.trim()) { setReplyErr("Reply cannot be empty"); return; }
    if (!replying) return;
    setTickets(p => p.map(t => t.id===replying.id ? {...t, reply, status:"processing"} : t));
    setReplying(null); setReply(""); setReplyErr("");
  };

  const TABS = [
    { id:"all",        label:`All (${tickets.length})`          },
    { id:"pending",    label:`Pending (${counts.pending})`      },
    { id:"processing", label:`Processing (${counts.processing})`},
    { id:"resolved",   label:`Resolved (${counts.resolved})`    },
  ];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Support</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>Manage user support tickets and queries.</p>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {[
          {label:"Open Tickets",     value:String(counts.pending),    icon:"🎫", color:"#f28c40"},
          {label:"In Progress",      value:String(counts.processing), icon:"⚙️", color:"#7887fa"},
          {label:"Resolved",         value:String(counts.resolved),   icon:"✅", color:"#57b869"},
          {label:"Avg Response Time",value:"2.4h",                    icon:"⏱️", color:"#1c1f29"},
        ].map(s=>(
          <div key={s.label} className="bg-white flex flex-col gap-2 p-5 rounded-[12px] flex-1" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
            <span style={{fontSize:22}}>{s.icon}</span>
            <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:s.color}}>{s.value}</p>
            <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{s.label}</p>
          </div>
        ))}
      </div>

      {/* Tabs */}
      <div className="flex gap-2 flex-wrap">
        {TABS.map(t=>(
          <button key={t.id} onClick={()=>setTabStatus(t.id as typeof tabStatus)}
            style={{padding:"10px 16px",borderRadius:8,fontSize:13,cursor:"pointer",border:"none",fontFamily:"Inter",fontWeight:500,
              background:tabStatus===t.id?"#57b869":"#f2f5f7",color:tabStatus===t.id?"#fff":"#9499a6"}}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white" style={{border:"1px solid #e5e8ed"}}>
        <span>🔍</span>
        <input className="flex-1 outline-none bg-transparent" style={{border:"none",fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
          placeholder="Search tickets..." value={search} onChange={e=>setSearch(e.target.value)}/>
      </div>

      <div className="bg-white rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div className="p-4 sm:p-6 overflow-x-auto">
          <div className="min-w-[1010px]">
          <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
            <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>S.N</p>
            {["Ticket ID","User","Subject","Priority","Status","Date","Actions"].map((h,i)=>(
              <p key={h} style={{width:[100,150,250,80,110,120,40][i],flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>{h}</p>
            ))}
          </div>
          {visible.length===0 && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>No tickets found.</p>
          )}
          {visible.map((t,idx)=>(
            <div key={t.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6"}}>{(page-1)*PER_PAGE+idx+1}</p>
                <p style={{width:100,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#7887fa"}}>{t.id}</p>
                <p style={{width:150,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{t.user}</p>
                <p style={{width:250,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29",overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap"}}>{t.subject}</p>
                <div style={{width:80,flexShrink:0}}><span style={priorityStyle(t.priority)}>{t.priority}</span></div>
                <div style={{width:110,flexShrink:0}}><span style={statusStyle(t.status)}>{t.status.charAt(0).toUpperCase()+t.status.slice(1)}</span></div>
                <p style={{width:120,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{t.date}</p>
                <div style={{width:40,flexShrink:0}}>
                  <KebabMenu
                    onView={()=>setViewing(t)}
                    onReply={()=>{ setReplying(t); setReply(t.reply||""); setReplyErr(""); }}
                    onClose={()=>closeTicket(t.id)}
                    onDelete={()=>deleteTicket(t.id)}
                  />
                </div>
              </div>
              {idx<visible.length-1 && <div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          ))}
          </div>
        </div>
      </div>

      <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage}/>

      {/* View */}
      {viewing && (
        <Modal title="Ticket Details" onClose={()=>setViewing(null)}>
          <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:16}}>
            <span style={{fontFamily:"Inter",fontWeight:700,fontSize:15,color:"#7887fa"}}>{viewing.id}</span>
            <div style={{display:"flex",gap:8}}>
              <span style={priorityStyle(viewing.priority)}>{viewing.priority} Priority</span>
              <span style={statusStyle(viewing.status)}>{viewing.status.charAt(0).toUpperCase()+viewing.status.slice(1)}</span>
            </div>
          </div>
          <DetailRow label="User"    value={viewing.user}/>
          <DetailRow label="Subject" value={viewing.subject}/>
          <DetailRow label="Message" value={<span style={{lineHeight:1.6}}>{viewing.message}</span>}/>
          <DetailRow label="Date"    value={viewing.date}/>
          {viewing.reply && (
            <DetailRow label="Admin Reply" value={
              <div style={{background:"rgba(87,184,105,0.06)",border:"1px solid rgba(87,184,105,0.2)",borderRadius:8,padding:"10px 12px"}}>
                <p style={{fontFamily:"Inter",fontSize:13,color:"#57b869"}}>{viewing.reply}</p>
              </div>
            }/>
          )}
        </Modal>
      )}

      {/* Reply */}
      {replying && (
        <Modal title={replying.reply?"Edit Reply":"Reply to Ticket"} onClose={()=>setReplying(null)}>
          <div className="mb-4 p-4 rounded-[10px]" style={{background:"#f7f8fa"}}>
            <div style={{display:"flex",alignItems:"center",justifyContent:"space-between",marginBottom:6}}>
              <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{replying.subject}</p>
              <span style={priorityStyle(replying.priority)}>{replying.priority}</span>
            </div>
            <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{replying.message}</p>
          </div>
          <p style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#1c1f29",marginBottom:6}}>Your Reply</p>
          <textarea rows={4} className="w-full px-4 py-3 rounded-[8px] outline-none resize-none"
            style={{border:`1px solid ${replyErr?"#f25959":"#e5e8ed"}`,fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
            placeholder="Write your reply..." value={reply}
            onChange={e=>{ setReply(e.target.value); setReplyErr(""); }}
            onFocus={e=>(e.target.style.borderColor="#57b869")} onBlur={e=>(e.target.style.borderColor=replyErr?"#f25959":"#e5e8ed")}/>
          {replyErr && <p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:4}}>{replyErr}</p>}
          <div className="flex gap-3 mt-3">
            <button onClick={()=>setReplying(null)} style={{flex:1,padding:"12px 0",borderRadius:8,border:"1px solid #e5e8ed",background:"white",fontFamily:"Inter",fontWeight:500,fontSize:14,color:"#9499a6",cursor:"pointer"}}>Cancel</button>
            <button onClick={sendReply} style={{flex:1,padding:"12px 0",borderRadius:8,border:"none",background:"#57b869",fontFamily:"Inter",fontWeight:600,fontSize:14,color:"white",cursor:"pointer"}}>
              {replying.reply?"Update Reply":"Send Reply"}
            </button>
          </div>
        </Modal>
      )}
    </div>
  );
}
