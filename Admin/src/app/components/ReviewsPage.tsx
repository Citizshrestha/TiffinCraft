import React, { useState, useEffect } from "react";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow } from "./Modal";

const PER_PAGE = 10;

interface Review { id:number; customer:string; cook:string; rating:number; comment:string; date:string; reply?:string; }

const SEED: Review[] = [
  { id:1, customer:"Maria Rosser",    cook:"Anita's Kitchen", rating:5, comment:'"Excellent food quality! Very fresh ingredients."',  date:"May 18, 2025" },
  { id:2, customer:"Rayna Carder",    cook:"Mumbai Spice",    rating:5, comment:'"Delicious authentic taste. Will order again!"',      date:"May 17, 2025" },
  { id:3, customer:"Talan Press",     cook:"South Indian",    rating:4, comment:'"Good taste. Timely delivery. Slightly spicy."',      date:"May 17, 2025" },
  { id:4, customer:"Marley Dokidis",  cook:"Delhi Delights",  rating:5, comment:'"Amazing flavors! Best chole bhature ever."',         date:"May 16, 2025" },
  { id:5, customer:"Marcus Rosser",   cook:"Punjab Kitchen",  rating:4, comment:'"Great food but portion size could be better."',      date:"May 16, 2025" },
  { id:6, customer:"Zaire Bergson",   cook:"Bengal Bites",    rating:5, comment:'"Authentic Bengali cuisine. Highly recommend!"',      date:"May 15, 2025" },
  { id:7, customer:"Lincoln Siphron", cook:"Coastal Treats",  rating:3, comment:'"Average taste. Expected better quality."',           date:"May 15, 2025" },
  { id:8, customer:"Talan Dokidis",   cook:"Anita's Kitchen", rating:5, comment:'"Perfect dal makhani! Creamy and delicious."',        date:"May 14, 2025" },
  { id:9, customer:"Priya Sharma",    cook:"Spice Route",     rating:4, comment:'"Nice spices, will try again."',                      date:"May 14, 2025" },
  { id:10,customer:"Arun Mehta",      cook:"Healthy Meals",   rating:5, comment:'"Very healthy and tasty! Great packaging."',          date:"May 13, 2025" },
  { id:11,customer:"Kavitha Nair",    cook:"Mumbai Spice",    rating:2, comment:'"Delivery was late and food was cold."',               date:"May 13, 2025" },
];

const ini = (n:string) => n.split(" ").map(x=>x[0]).join("").toUpperCase();

function Stars({ n }:{ n:number }) {
  return (
    <div style={{display:"flex",alignItems:"center",gap:4}}>
      <span style={{fontSize:13}}>{"⭐".repeat(n)}</span>
      <span style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{n}</span>
    </div>
  );
}

export function ReviewsPage() {
  const [rows,     setRows]     = useState<Review[]>(SEED);
  const [search,   setSearch]   = useState("");
  const [ratingFilter, setRatingFilter] = useState("all");
  const [page,     setPage]     = useState(1);
  const [viewing,  setViewing]  = useState<Review|null>(null);
  const [replying, setReplying] = useState<Review|null>(null);
  const [reply,    setReply]    = useState("");
  const [replyErr, setReplyErr] = useState("");
  const [del,      setDel]      = useState<Review|null>(null);

  const filtered = rows.filter(r => {
    const s = !search || r.customer.toLowerCase().includes(search.toLowerCase()) || r.cook.toLowerCase().includes(search.toLowerCase());
    const rf = ratingFilter==="all" || r.rating===parseInt(ratingFilter);
    return s && rf;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible    = filtered.slice((page-1)*PER_PAGE, page*PER_PAGE);

  useEffect(() => { setPage(1); }, [search, ratingFilter]);

  const sendReply = () => {
    if (!reply.trim()) { setReplyErr("Reply cannot be empty"); return; }
    if (!replying) return;
    setRows(p => p.map(r => r.id===replying.id ? {...r, reply} : r));
    setReplying(null); setReply(""); setReplyErr("");
  };

  const doDelete = () => { if(!del) return; setRows(p=>p.filter(r=>r.id!==del.id)); setDel(null); };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Reviews & Ratings</p>
        <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>Customer feedback and ratings overview.</p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        {[
          {icon:"⭐",value:"4.6",  label:"Average Rating",  sub:"out of 5.0"      },
          {icon:"📝",value:`${rows.length}`,label:"Total Reviews",sub:"in database"},
          {icon:"⭐",value:`${rows.filter(r=>r.rating===5).length}`,label:"5 Star Reviews",sub:`${Math.round(rows.filter(r=>r.rating===5).length/rows.length*100)}% of total`},
          {icon:"⏳",value:`${rows.filter(r=>!r.reply).length}`, label:"Pending Replies", sub:"Need response"},
        ].map(s=>(
          <div key={s.label} className="bg-white flex flex-col gap-3 p-5 rounded-[12px] flex-1" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
            <p style={{fontSize:24}}>{s.icon}</p>
            <p style={{fontFamily:"Inter",fontWeight:700,fontSize:32,color:"#1c1f29"}}>{s.value}</p>
            <p style={{fontFamily:"Inter",fontWeight:500,fontSize:14,color:"#1c1f29"}}>{s.label}</p>
            <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6"}}>{s.sub}</p>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="flex gap-3 items-center flex-wrap">
        <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white flex-1" style={{border:"1px solid #e5e8ed",minWidth:200}}>
          <span>🔍</span>
          <input className="flex-1 outline-none bg-transparent" style={{border:"none",fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
            placeholder="Search customer or cook..." value={search} onChange={e=>setSearch(e.target.value)}/>
        </div>
        <div className="flex gap-2 flex-wrap">
          {["all","5","4","3","2","1"].map(r=>(
            <button key={r} onClick={()=>setRatingFilter(r)}
              style={{padding:"10px 14px",borderRadius:8,fontSize:13,cursor:"pointer",border:"none",fontFamily:"Inter",fontWeight:500,
                background:ratingFilter===r?"#57b869":"#f2f5f7",color:ratingFilter===r?"#fff":"#9499a6"}}>
              {r==="all" ? "All Stars" : `${"⭐".repeat(parseInt(r))} ${r}`}
            </button>
          ))}
        </div>
      </div>

      <div className="bg-white rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div className="p-4 sm:p-6 overflow-x-auto">
          <div className="min-w-[1100px]">
          <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
            <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>S.N</p>
            {["Customer","Cook","Rating","Comment","Replied","Date","Actions"].map((h,i)=>(
              <p key={h} style={{width:[150,140,120,240,70,110,110][i],flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>{h}</p>
            ))}
          </div>
          {visible.length===0 && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>No reviews found.</p>
          )}
          {visible.map((r,idx)=>(
            <div key={r.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6"}}>{(page-1)*PER_PAGE+idx+1}</p>
                <p style={{width:150,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#1c1f29"}}>{r.customer}</p>
                <p style={{width:140,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{r.cook}</p>
                <div style={{width:120,flexShrink:0}}><Stars n={r.rating}/></div>
                <p style={{width:240,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29",fontStyle:"italic",overflow:"hidden",textOverflow:"ellipsis",whiteSpace:"nowrap"}}>{r.comment}</p>
                <div style={{width:70,flexShrink:0}}>
                  <span style={{padding:"3px 8px",borderRadius:12,fontSize:11,fontFamily:"Inter",fontWeight:600,
                    background:r.reply?"rgba(87,184,105,0.12)":"rgba(242,140,64,0.12)",
                    color:r.reply?"#57b869":"#f28c40"}}>
                    {r.reply?"Yes":"No"}
                  </span>
                </div>
                <p style={{width:110,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{r.date}</p>
                <div style={{width:110,flexShrink:0}}>
                  <ActionButtons
                    onView={()=>setViewing(r)}
                    onEdit={()=>{ setReplying(r); setReply(r.reply||""); setReplyErr(""); }}
                    onDelete={()=>setDel(r)}
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
        <Modal title="Review Details" onClose={()=>setViewing(null)}>
          <div className="flex items-center gap-3 mb-5">
            <div style={{width:48,height:48,borderRadius:"50%",background:"#D9DEE6",display:"flex",alignItems:"center",justifyContent:"center"}}>
              <span style={{fontFamily:"Inter",fontWeight:700,fontSize:14,color:"#6b7280"}}>{ini(viewing.customer)}</span>
            </div>
            <div>
              <p style={{fontFamily:"Inter",fontWeight:700,fontSize:16,color:"#1c1f29"}}>{viewing.customer}</p>
              <Stars n={viewing.rating}/>
            </div>
          </div>
          <DetailRow label="Cook / Kitchen" value={viewing.cook}/>
          <DetailRow label="Comment"        value={<span style={{fontStyle:"italic"}}>{viewing.comment}</span>}/>
          <DetailRow label="Date"           value={viewing.date}/>
          {viewing.reply && <DetailRow label="Admin Reply" value={<span style={{color:"#57b869"}}>{viewing.reply}</span>}/>}
        </Modal>
      )}

      {/* Reply */}
      {replying && (
        <Modal title={replying.reply ? "Edit Reply" : "Reply to Review"} onClose={()=>setReplying(null)}>
          <div className="mb-4 p-4 rounded-[10px]" style={{background:"#f7f8fa"}}>
            <div style={{display:"flex",alignItems:"center",gap:8,marginBottom:4}}>
              <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{replying.customer}</p>
              <Stars n={replying.rating}/>
            </div>
            <p style={{fontFamily:"Inter",fontSize:13,fontStyle:"italic",color:"#9499a6"}}>{replying.comment}</p>
          </div>
          <p style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#1c1f29",marginBottom:6}}>Your Reply</p>
          <textarea rows={4} className="w-full px-4 py-3 rounded-[8px] outline-none resize-none"
            style={{border:`1px solid ${replyErr?"#f25959":"#e5e8ed"}`,fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
            placeholder="Write a reply..." value={reply}
            onChange={e=>{ setReply(e.target.value); setReplyErr(""); }}
            onFocus={e=>(e.target.style.borderColor="#57b869")} onBlur={e=>(e.target.style.borderColor=replyErr?"#f25959":"#e5e8ed")}/>
          {replyErr && <p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:4}}>{replyErr}</p>}
          <div className="flex gap-3 mt-3">
            <button onClick={()=>setReplying(null)} style={{flex:1,padding:"12px 0",borderRadius:8,border:"1px solid #e5e8ed",background:"white",fontFamily:"Inter",fontWeight:500,fontSize:14,color:"#9499a6",cursor:"pointer"}}>Cancel</button>
            <button onClick={sendReply} style={{flex:1,padding:"12px 0",borderRadius:8,border:"none",background:"#57b869",fontFamily:"Inter",fontWeight:600,fontSize:14,color:"white",cursor:"pointer"}}>
              {replying.reply ? "Update Reply" : "Send Reply"}
            </button>
          </div>
        </Modal>
      )}

      {del && <ConfirmDelete name={`review by ${del.customer}`} onConfirm={doDelete} onCancel={()=>setDel(null)}/>}
    </div>
  );
}
