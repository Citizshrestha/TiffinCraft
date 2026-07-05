import React, { useState, useEffect } from "react";
import { StatusBadge } from "./StatusBadge";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow, FormField, SaveCancel } from "./Modal";

const PER_PAGE = 10;
type CStatus = "verified"|"unverified";
interface Cook { id:number; kitchen:string; owner:string; rating:number; orders:number; status:CStatus; joined:string; }

const SEED: Cook[] = [
  { id:1, kitchen:"Anita's Kitchen", owner:"Anita Sharma",  rating:4.8, orders:123, status:"verified",   joined:"Jan 12, 2022" },
  { id:2, kitchen:"Spice Route",     owner:"Ramesh Kumar",  rating:4.7, orders:98,  status:"verified",   joined:"Feb 03, 2022" },
  { id:3, kitchen:"Healthy Meals",   owner:"Priya Mehta",   rating:4.6, orders:87,  status:"verified",   joined:"Mar 15, 2022" },
  { id:4, kitchen:"Tasty Tiffins",   owner:"Suresh Patel",  rating:4.5, orders:76,  status:"verified",   joined:"Mar 28, 2022" },
  { id:5, kitchen:"HomeBites",       owner:"Kavitha Reddy", rating:4.4, orders:66,  status:"unverified", joined:"Apr 10, 2022" },
  { id:6, kitchen:"Mumbai Spice",    owner:"Anil Desai",    rating:4.3, orders:54,  status:"verified",   joined:"Apr 22, 2022" },
  { id:7, kitchen:"South Indian",    owner:"Lakshmi Iyer",  rating:4.2, orders:48,  status:"verified",   joined:"May 05, 2022" },
  { id:8, kitchen:"Punjab Kitchen",  owner:"Gurpreet Singh",rating:4.1, orders:42,  status:"verified",   joined:"May 15, 2022" },
  { id:9, kitchen:"Delhi Delights",  owner:"Ritu Sharma",   rating:4.0, orders:38,  status:"unverified", joined:"May 20, 2022" },
  { id:10,kitchen:"Bengal Bites",    owner:"Suman Roy",     rating:3.9, orders:34,  status:"verified",   joined:"Jun 01, 2022" },
  { id:11,kitchen:"Coastal Treats",  owner:"Meena Nair",    rating:3.8, orders:29,  status:"verified",   joined:"Jun 10, 2022" },
];

const blankCook: Omit<Cook,"id"> = { kitchen:"", owner:"", rating:4.5, orders:0, status:"unverified", joined:"" };

function validate(f: Omit<Cook,"id">): Record<string,string> {
  const e: Record<string,string> = {};
  if (!f.kitchen.trim()) e.kitchen = "Kitchen name is required";
  if (!f.owner.trim())   e.owner   = "Owner name is required";
  if (f.rating<0||f.rating>5) e.rating = "Rating must be between 0 and 5";
  return e;
}

export function CooksPage() {
  const [rows,    setRows]    = useState<Cook[]>(SEED);
  const [search,  setSearch]  = useState("");
  const [page,    setPage]    = useState(1);
  const [viewing, setViewing] = useState<Cook|null>(null);
  const [editing, setEditing] = useState<Cook|null>(null);
  const [draft,   setDraft]   = useState<Cook|null>(null);
  const [editErrs,setEditErrs]= useState<Record<string,string>>({});
  const [del,     setDel]     = useState<Cook|null>(null);
  const [adding,  setAdding]  = useState(false);
  const [addForm, setAddForm] = useState<Omit<Cook,"id">>(blankCook);
  const [addErrs, setAddErrs] = useState<Record<string,string>>({});

  const filtered   = rows.filter(c => !search || c.kitchen.toLowerCase().includes(search.toLowerCase()) || c.owner.toLowerCase().includes(search.toLowerCase()));
  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible    = filtered.slice((page-1)*PER_PAGE, page*PER_PAGE);

  useEffect(() => { setPage(1); }, [search]);

  const startEdit = (c: Cook) => { setEditing(c); setDraft({...c}); setEditErrs({}); };
  const saveEdit  = () => {
    if(!draft) return;
    const errs = validate(draft);
    if(Object.keys(errs).length){ setEditErrs(errs); return; }
    setRows(p=>p.map(c=>c.id===draft.id?draft:c)); setEditing(null); setDraft(null); setEditErrs({});
  };
  const doDelete  = () => { if(!del) return; setRows(p=>p.filter(c=>c.id!==del.id)); setDel(null); };
  const submitAdd = () => {
    const errs = validate(addForm);
    if(Object.keys(errs).length){ setAddErrs(errs); return; }
    const today = new Date().toLocaleDateString("en-US",{month:"short",day:"2-digit",year:"numeric"});
    setRows(p=>[...p,{...addForm,id:Date.now(),joined:today}]);
    setAdding(false); setAddForm(blankCook); setAddErrs({});
  };

  const ErrMsg = ({field,errs}:{field:string;errs:Record<string,string>}) =>
    errs[field]?<p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:2}}>{errs[field]}</p>:null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Manage Cooks</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>View and manage all cook profiles.</p>
        </div>
        <button onClick={()=>{setAdding(true);setAddForm(blankCook);setAddErrs({});}}
          style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:14,padding:"12px 20px",borderRadius:8,cursor:"pointer"}}>
          + Add Cook
        </button>
      </div>

      <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white" style={{border:"1px solid #e5e8ed"}}>
        <span>🔍</span>
        <input className="flex-1 outline-none bg-transparent" style={{border:"none",fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
          placeholder="Search cooks..." value={search} onChange={e=>setSearch(e.target.value)}/>
      </div>

      <div className="bg-white rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div className="p-6">
          <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
            <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>S.N</p>
            {["Kitchen Name","Owner Name","Rating","Total Orders","Verification","Joined Date","Actions"].map((h,i)=>(
              <p key={h} style={{width:[180,150,80,110,110,110,110][i],flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>{h}</p>
            ))}
          </div>
          {visible.length===0&&<p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>No cooks found.</p>}
          {visible.map((c,idx)=>(
            <div key={c.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6"}}>{(page-1)*PER_PAGE+idx+1}</p>
                <div style={{width:180,flexShrink:0,display:"flex",alignItems:"center",gap:10}}>
                  <div style={{width:36,height:36,borderRadius:"50%",background:"#D9DEE6",display:"flex",alignItems:"center",justifyContent:"center",flexShrink:0}}>
                    <span style={{fontFamily:"Inter",fontWeight:700,fontSize:12,color:"#6b7280"}}>{c.kitchen.slice(0,2)}</span>
                  </div>
                  <p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{c.kitchen}</p>
                </div>
                <p style={{width:150,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{c.owner}</p>
                <div style={{width:80,flexShrink:0,display:"flex",alignItems:"center",gap:4}}>
                  <span>⭐</span><p style={{fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{c.rating}</p>
                </div>
                <p style={{width:110,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{c.orders}+ orders</p>
                <div style={{width:110,flexShrink:0}}><StatusBadge status={c.status}/></div>
                <p style={{width:110,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{c.joined}</p>
                <div style={{width:110,flexShrink:0}}>
                  <ActionButtons onView={()=>setViewing(c)} onEdit={()=>startEdit(c)} onDelete={()=>setDel(c)}/>
                </div>
              </div>
              {idx<visible.length-1&&<div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          ))}
        </div>
      </div>

      <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage}/>

      {viewing&&(
        <Modal title="Cook Details" onClose={()=>setViewing(null)}>
          <div className="flex items-center gap-4 mb-5">
            <div style={{width:64,height:64,borderRadius:"50%",background:"#f2f5f7",display:"flex",alignItems:"center",justifyContent:"center",fontSize:32}}>👨‍🍳</div>
            <div>
              <p style={{fontFamily:"Inter",fontWeight:700,fontSize:18,color:"#1c1f29"}}>{viewing.kitchen}</p>
              <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>by {viewing.owner}</p>
            </div>
          </div>
          <DetailRow label="Owner"        value={viewing.owner}/>
          <DetailRow label="Rating"       value={`⭐ ${viewing.rating} / 5.0`}/>
          <DetailRow label="Total Orders" value={`${viewing.orders}+`}/>
          <DetailRow label="Verification" value={<StatusBadge status={viewing.status}/>}/>
          <DetailRow label="Joined"       value={viewing.joined}/>
        </Modal>
      )}

      {editing&&draft&&(
        <Modal title="Edit Cook" onClose={()=>{setEditing(null);setDraft(null);setEditErrs({});}}>
          <FormField label="Kitchen Name"  value={draft.kitchen} onChange={v=>setDraft({...draft,kitchen:v})}/>
          <ErrMsg field="kitchen" errs={editErrs}/>
          <FormField label="Owner Name"    value={draft.owner}   onChange={v=>setDraft({...draft,owner:v})}/>
          <ErrMsg field="owner"   errs={editErrs}/>
          <FormField label="Rating (0–5)"  value={String(draft.rating)} onChange={v=>setDraft({...draft,rating:parseFloat(v)||draft.rating})} type="number"/>
          <ErrMsg field="rating"  errs={editErrs}/>
          <FormField label="Verification"  value={draft.status}  onChange={v=>setDraft({...draft,status:v as CStatus})} options={["verified","unverified"]}/>
          <SaveCancel onCancel={()=>{setEditing(null);setDraft(null);setEditErrs({});}} onSave={saveEdit}/>
        </Modal>
      )}

      {adding&&(
        <Modal title="Add New Cook" onClose={()=>{setAdding(false);setAddErrs({});}}>
          <FormField label="Kitchen Name" value={addForm.kitchen} onChange={v=>setAddForm({...addForm,kitchen:v})}/>
          <ErrMsg field="kitchen" errs={addErrs}/>
          <FormField label="Owner Name"   value={addForm.owner}   onChange={v=>setAddForm({...addForm,owner:v})}/>
          <ErrMsg field="owner"   errs={addErrs}/>
          <FormField label="Initial Rating" value={String(addForm.rating)} onChange={v=>setAddForm({...addForm,rating:parseFloat(v)||4.0})} type="number"/>
          <ErrMsg field="rating"  errs={addErrs}/>
          <FormField label="Verification" value={addForm.status}  onChange={v=>setAddForm({...addForm,status:v as CStatus})} options={["verified","unverified"]}/>
          <SaveCancel onCancel={()=>{setAdding(false);setAddErrs({});}} onSave={submitAdd}/>
        </Modal>
      )}

      {del&&<ConfirmDelete name={del.kitchen} onConfirm={doDelete} onCancel={()=>setDel(null)}/>}
    </div>
  );
}
