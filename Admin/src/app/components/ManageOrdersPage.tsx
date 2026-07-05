import React, { useState, useEffect } from "react";
import { StatusBadge, StatusType } from "./StatusBadge";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow, FormField, SaveCancel } from "./Modal";
import { exportCSV } from "../utils/csv";

const PER_PAGE = 10;

interface Order { id:string; customer:string; cook:string; product:string; amount:string; status:StatusType; date:string; }

const SEED: Order[] = [
  { id:"#ORD-1234", customer:"Rahul Sharma",  cook:"Anita's Kitchen", product:"Dal Makhani",    amount:"₹360", status:"delivered",       date:"May 18, 2025" },
  { id:"#ORD-1233", customer:"Priya Patel",   cook:"Spice Route",     product:"Paneer Tikka",   amount:"₹380", status:"processing",       date:"May 18, 2025" },
  { id:"#ORD-1232", customer:"Vikram Singh",  cook:"Healthy Meals",   product:"Veg Thali",      amount:"₹460", status:"out_for_delivery", date:"May 17, 2025" },
  { id:"#ORD-1231", customer:"Neha Gupta",    cook:"Tasty Tiffins",   product:"Chole Bhature",  amount:"₹320", status:"accepted",         date:"May 17, 2025" },
  { id:"#ORD-1230", customer:"Amit Kumar",    cook:"Anita's Kitchen", product:"Butter Chicken", amount:"₹380", status:"delivered",        date:"May 16, 2025" },
  { id:"#ORD-1229", customer:"Sunita Mehta",  cook:"Mumbai Spice",    product:"Biryani",        amount:"₹420", status:"pending",          date:"May 16, 2025" },
  { id:"#ORD-1228", customer:"Ravi Nair",     cook:"South Indian",    product:"Masala Dosa",    amount:"₹240", status:"cancelled",        date:"May 15, 2025" },
  { id:"#ORD-1227", customer:"Kavya Reddy",   cook:"Bengal Bites",    product:"Fish Curry",     amount:"₹350", status:"delivered",        date:"May 15, 2025" },
  { id:"#ORD-1226", customer:"Arjun Sharma",  cook:"Delhi Delights",  product:"Rajma Chawal",   amount:"₹290", status:"processing",       date:"May 14, 2025" },
  { id:"#ORD-1225", customer:"Pooja Verma",   cook:"Punjab Kitchen",  product:"Makki Roti",     amount:"₹310", status:"delivered",        date:"May 14, 2025" },
  { id:"#ORD-1224", customer:"Karan Malhotra",cook:"Anita's Kitchen", product:"Shahi Paneer",   amount:"₹390", status:"pending",          date:"May 13, 2025" },
  { id:"#ORD-1223", customer:"Sita Ram",      cook:"Spice Route",     product:"Aloo Paratha",   amount:"₹180", status:"delivered",        date:"May 13, 2025" },
];

const STATUS_OPTS: StatusType[] = ["pending","accepted","processing","out_for_delivery","delivered","cancelled"];
const blankOrder: Omit<Order,"id"> = { customer:"", cook:"", product:"", amount:"", status:"pending", date:"" };

function validate(f: Omit<Order,"id">): Record<string,string> {
  const e: Record<string,string> = {};
  if (!f.customer.trim()) e.customer = "Customer name is required";
  if (!f.cook.trim())     e.cook     = "Cook / Kitchen is required";
  if (!f.product.trim())  e.product  = "Product is required";
  if (!f.amount.trim())   e.amount   = "Amount is required";
  return e;
}

export function ManageOrdersPage() {
  const [rows,    setRows]    = useState<Order[]>(SEED);
  const [tab,     setTab]     = useState("all");
  const [search,  setSearch]  = useState("");
  const [page,    setPage]    = useState(1);
  const [viewing, setViewing] = useState<Order|null>(null);
  const [editing, setEditing] = useState<Order|null>(null);
  const [draft,   setDraft]   = useState<Order|null>(null);
  const [editErrs,setEditErrs]= useState<Record<string,string>>({});
  const [del,     setDel]     = useState<Order|null>(null);
  const [adding,  setAdding]  = useState(false);
  const [addForm, setAddForm] = useState<Omit<Order,"id">>(blankOrder);
  const [addErrs, setAddErrs] = useState<Record<string,string>>({});

  // Dynamic tab counts
  const counts = {
    pending:    rows.filter(o => o.status==="pending").length,
    processing: rows.filter(o => o.status==="processing").length,
    completed:  rows.filter(o => o.status==="delivered"||o.status==="completed").length,
    cancelled:  rows.filter(o => o.status==="cancelled").length,
  };

  const TABS = [
    { id:"all",        label:"All Orders"                         },
    { id:"pending",    label:`Pending (${counts.pending})`        },
    { id:"processing", label:`Processing (${counts.processing})`  },
    { id:"completed",  label:`Completed (${counts.completed})`    },
    { id:"cancelled",  label:`Cancelled (${counts.cancelled})`    },
  ];

  const filtered = rows.filter(o => {
    const s = !search || o.id.toLowerCase().includes(search.toLowerCase()) || o.customer.toLowerCase().includes(search.toLowerCase());
    const t = tab==="all" || (tab==="pending"&&o.status==="pending") || (tab==="processing"&&o.status==="processing")
           || (tab==="completed"&&(o.status==="delivered"||o.status==="completed")) || (tab==="cancelled"&&o.status==="cancelled");
    return s && t;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible    = filtered.slice((page-1)*PER_PAGE, page*PER_PAGE);

  useEffect(() => { setPage(1); }, [tab, search]);

  const startEdit = (o: Order) => { setEditing(o); setDraft({...o}); setEditErrs({}); };

  const saveEdit = () => {
    if (!draft) return;
    const errs = validate(draft);
    if (Object.keys(errs).length) { setEditErrs(errs); return; }
    setRows(p => p.map(o => o.id===draft.id ? draft : o));
    setEditing(null); setDraft(null); setEditErrs({});
  };

  const doDelete = () => { if(!del) return; setRows(p=>p.filter(o=>o.id!==del.id)); setDel(null); };

  const submitAdd = () => {
    const errs = validate(addForm);
    if (Object.keys(errs).length) { setAddErrs(errs); return; }
    const today = new Date().toLocaleDateString("en-US",{month:"short",day:"2-digit",year:"numeric"});
    const newId = `#ORD-${Math.floor(1000+Math.random()*9000)}`;
    setRows(p => [{ ...addForm, id: newId, date: addForm.date||today }, ...p]);
    setAdding(false); setAddForm(blankOrder); setAddErrs({});
  };

  const handleExport = () =>
    exportCSV("orders.csv", filtered.map(o => ({
      "Order ID": o.id, Customer: o.customer, Cook: o.cook,
      Product: o.product, Amount: o.amount, Status: o.status, Date: o.date,
    })));

  const ErrMsg = ({ field, errs }: { field:string; errs:Record<string,string> }) =>
    errs[field] ? <p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:2}}>{errs[field]}</p> : null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Manage Orders</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>View and track all order information.</p>
        </div>
        <div className="flex gap-2">
          <button onClick={handleExport}
            style={{padding:"10px 16px",borderRadius:8,border:"1px solid #e5e8ed",background:"white",fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6",cursor:"pointer"}}>
            Export CSV
          </button>
          <button onClick={()=>{setAdding(true);setAddForm(blankOrder);setAddErrs({});}}
            style={{padding:"10px 16px",borderRadius:8,border:"none",background:"#57b869",fontFamily:"Inter",fontWeight:600,fontSize:13,color:"white",cursor:"pointer"}}>
            + Add Order
          </button>
        </div>
      </div>

      <div className="flex gap-2 flex-wrap">
        {TABS.map(t=>(
          <button key={t.id} onClick={()=>setTab(t.id)}
            style={{padding:"10px 16px",borderRadius:8,fontSize:13,cursor:"pointer",border:"none",fontFamily:"Inter",fontWeight:500,
              background:tab===t.id?"#57b869":"#f2f5f7",color:tab===t.id?"#fff":"#9499a6"}}>
            {t.label}
          </button>
        ))}
      </div>

      <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white" style={{border:"1px solid #e5e8ed"}}>
        <span>🔍</span>
        <input className="flex-1 outline-none bg-transparent" style={{border:"none",fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
          placeholder="Search by order ID or customer..." value={search} onChange={e=>setSearch(e.target.value)}/>
      </div>

      <div className="bg-white rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div className="p-6">
          <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
            <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>S.N</p>
            {["Order ID","Customer","Cook","Product","Amount","Status","Date","Actions"].map((h,i)=>(
              <p key={h} style={{width:[100,130,130,130,75,120,100,110][i],flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>{h}</p>
            ))}
          </div>
          {visible.length===0 && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>No orders found.</p>
          )}
          {visible.map((o,idx)=>(
            <div key={o.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6"}}>{(page-1)*PER_PAGE+idx+1}</p>
                <p style={{width:100,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#7887fa"}}>{o.id}</p>
                <p style={{width:130,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{o.customer}</p>
                <p style={{width:130,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{o.cook}</p>
                <p style={{width:130,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{o.product}</p>
                <p style={{width:75, flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>{o.amount}</p>
                <div style={{width:120,flexShrink:0}}><StatusBadge status={o.status}/></div>
                <p style={{width:100,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{o.date}</p>
                <div style={{width:110,flexShrink:0}}>
                  <ActionButtons onView={()=>setViewing(o)} onEdit={()=>startEdit(o)} onDelete={()=>setDel(o)}/>
                </div>
              </div>
              {idx<visible.length-1 && <div style={{height:1,background:"#f2f5f7"}}/>}
            </div>
          ))}
        </div>
      </div>

      <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage}/>

      {/* View */}
      {viewing && (
        <Modal title="Order Details" onClose={()=>setViewing(null)}>
          <DetailRow label="Order ID"       value={<span style={{color:"#7887fa",fontWeight:600}}>{viewing.id}</span>}/>
          <DetailRow label="Customer"       value={viewing.customer}/>
          <DetailRow label="Cook / Kitchen" value={viewing.cook}/>
          <DetailRow label="Product"        value={viewing.product}/>
          <DetailRow label="Amount"         value={<span style={{fontWeight:700}}>{viewing.amount}</span>}/>
          <DetailRow label="Status"         value={<StatusBadge status={viewing.status}/>}/>
          <DetailRow label="Date"           value={viewing.date}/>
        </Modal>
      )}

      {/* Edit */}
      {editing && draft && (
        <Modal title="Update Order" onClose={()=>{setEditing(null);setDraft(null);setEditErrs({});}}>
          <FormField label="Customer" value={draft.customer} onChange={v=>setDraft({...draft,customer:v})}/>
          <ErrMsg field="customer" errs={editErrs}/>
          <FormField label="Product"  value={draft.product}  onChange={v=>setDraft({...draft,product:v})}/>
          <ErrMsg field="product"  errs={editErrs}/>
          <FormField label="Amount"   value={draft.amount}   onChange={v=>setDraft({...draft,amount:v})}/>
          <ErrMsg field="amount"   errs={editErrs}/>
          <FormField label="Status"   value={draft.status}   onChange={v=>setDraft({...draft,status:v as StatusType})} options={STATUS_OPTS}/>
          <SaveCancel onCancel={()=>{setEditing(null);setDraft(null);setEditErrs({});}} onSave={saveEdit}/>
        </Modal>
      )}

      {/* Add */}
      {adding && (
        <Modal title="Add New Order" onClose={()=>{setAdding(false);setAddErrs({});}}>
          <FormField label="Customer Name"  value={addForm.customer} onChange={v=>setAddForm({...addForm,customer:v})}/>
          <ErrMsg field="customer" errs={addErrs}/>
          <FormField label="Cook / Kitchen" value={addForm.cook}     onChange={v=>setAddForm({...addForm,cook:v})}/>
          <ErrMsg field="cook"     errs={addErrs}/>
          <FormField label="Product"        value={addForm.product}  onChange={v=>setAddForm({...addForm,product:v})}/>
          <ErrMsg field="product"  errs={addErrs}/>
          <FormField label="Amount (e.g. ₹360)" value={addForm.amount} onChange={v=>setAddForm({...addForm,amount:v})}/>
          <ErrMsg field="amount"   errs={addErrs}/>
          <FormField label="Status"         value={addForm.status}   onChange={v=>setAddForm({...addForm,status:v as StatusType})} options={STATUS_OPTS}/>
          <SaveCancel onCancel={()=>{setAdding(false);setAddErrs({});}} onSave={submitAdd}/>
        </Modal>
      )}

      {del && <ConfirmDelete name={`Order ${del.id}`} onConfirm={doDelete} onCancel={()=>setDel(null)}/>}
    </div>
  );
}
