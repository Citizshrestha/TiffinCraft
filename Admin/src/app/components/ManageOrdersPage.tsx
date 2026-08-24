import React, { useState, useEffect, useCallback } from "react";
import { StatusBadge, StatusType } from "./StatusBadge";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow, FormField, SaveCancel } from "./Modal";
import { exportCSV } from "../utils/csv";
import { Order, fetchOrders, updateOrderStatusApi, deleteOrderApi } from "../api/ordersApi";
import { useToast } from "./Toast";

const PER_PAGE = 10;

// Must match orders.status's actual DB enum — "completed" isn't a valid
// value there, selecting it always failed the update with a DB error.
const STATUS_OPTS: StatusType[] = ["pending","confirmed","preparing","ready","delivered","cancelled"];

export function ManageOrdersPage() {
  const { showToast } = useToast();

  const [rows, setRows] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [tab, setTab] = useState("all");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(1);

  const [viewing, setViewing] = useState<Order|null>(null);
  const [editing, setEditing] = useState<Order|null>(null);
  const [statusDraft, setStatusDraft] = useState<StatusType>("pending");
  const [savingEdit, setSavingEdit] = useState(false);

  const [del, setDel] = useState<Order|null>(null);
  const [deleting, setDeleting] = useState(false);

  const loadOrders = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      const data = await fetchOrders();
      setRows(data);
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load orders.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadOrders(); }, [loadOrders]);

  const counts = {
    pending:    rows.filter(o => o.status==="pending").length,
    preparing:  rows.filter(o => o.status==="confirmed"||o.status==="preparing"||o.status==="ready").length,
    delivered:  rows.filter(o => o.status==="delivered"||o.status==="completed").length,
    cancelled:  rows.filter(o => o.status==="cancelled").length,
  };

  const TABS = [
    { id:"all",        label:"All Orders"                         },
    { id:"pending",    label:`Pending (${counts.pending})`        },
    { id:"preparing",  label:`In Progress (${counts.preparing})`  },
    { id:"delivered",  label:`Delivered (${counts.delivered})`    },
    { id:"cancelled",  label:`Cancelled (${counts.cancelled})`    },
  ];

  const filtered = rows.filter(o => {
    const s = !search || o.displayId.toLowerCase().includes(search.toLowerCase()) || o.customer.toLowerCase().includes(search.toLowerCase());
    const t = tab==="all"
      || (tab==="pending"&&o.status==="pending")
      || (tab==="preparing"&&(o.status==="confirmed"||o.status==="preparing"||o.status==="ready"))
      || (tab==="delivered"&&(o.status==="delivered"||o.status==="completed"))
      || (tab==="cancelled"&&o.status==="cancelled");
    return s && t;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible = filtered.slice((page-1)*PER_PAGE, page*PER_PAGE);

  useEffect(() => { setPage(1); }, [tab, search]);

  const startEdit = (o: Order) => { setEditing(o); setStatusDraft(o.status); };

  const saveEdit = async () => {
    if (!editing || savingEdit) return;
    setSavingEdit(true);
    try {
      const updated = await updateOrderStatusApi(editing.id, statusDraft);
      setRows(p => p.map(o => o.id===updated.id ? updated : o));
      setEditing(null);
      showToast("Order status updated.", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to update order status.", "error");
    } finally {
      setSavingEdit(false);
    }
  };

  const doDelete = async () => {
    if (!del || deleting) return;
    setDeleting(true);
    try {
      await deleteOrderApi(del.id);
      setRows(p => p.filter(o => o.id !== del.id));
      setDel(null);
      showToast("Order deleted successfully.", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to delete order.", "error");
    } finally {
      setDeleting(false);
    }
  };

  const handleExport = () =>
    exportCSV("orders.csv", filtered.map(o => ({
      "Order ID": o.displayId, Customer: o.customer, Cook: o.cook,
      Product: o.product, Amount: `₹${o.amount}`, Status: o.status, Date: o.date,
    })));

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Manage Orders</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>View and track all order information.</p>
        </div>
        <button onClick={handleExport}
          className="self-start shrink-0"
          style={{padding:"10px 16px",borderRadius:8,border:"1px solid #e5e8ed",background:"white",fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6",cursor:"pointer"}}>
          Export CSV
        </button>
      </div>

      <div className="flex gap-2 flex-wrap">
        {TABS.map(t=>(
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
          placeholder="Search by order ID or customer..." value={search} onChange={e=>setSearch(e.target.value)}/>
      </div>

      <div className="bg-white rounded-[12px]" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
        <div className="p-4 sm:p-6 overflow-x-auto">
          <div className="min-w-[1080px]">
          <div className="flex gap-4 pb-4" style={{borderBottom:"1px solid #e5e8ed"}}>
            <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>S.N</p>
            {["Order ID","Customer","Cook","Product","Amount","Status","Date","Actions"].map((h,i)=>(
              <p key={h} style={{width:[100,130,130,130,75,120,100,110][i],flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:12,color:"#9499a6"}}>{h}</p>
            ))}
          </div>
          {loading && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>Loading orders...</p>
          )}
          {!loading && loadError && (
            <div style={{textAlign:"center",padding:"32px 0"}}>
              <p style={{fontFamily:"Inter",fontSize:14,color:"#f25959",marginBottom:12}}>{loadError}</p>
              <button onClick={loadOrders} style={{background:"#f97316",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:13,padding:"8px 16px",borderRadius:8,cursor:"pointer"}}>Retry</button>
            </div>
          )}
          {!loading && !loadError && visible.length===0 && (
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6",textAlign:"center",padding:"32px 0"}}>No orders found.</p>
          )}
          {!loading && !loadError && visible.map((o,idx)=>(
            <div key={o.id}>
              <div className="flex gap-4 items-center py-4 rounded hover:bg-[#f7f8fa] transition-colors -mx-2 px-2">
                <p style={{width:40,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#9499a6"}}>{(page-1)*PER_PAGE+idx+1}</p>
                <p style={{width:100,flexShrink:0,fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#7887fa"}}>{o.displayId}</p>
                <p style={{width:130,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{o.customer}</p>
                <p style={{width:130,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}}>{o.cook}</p>
                <p style={{width:130,flexShrink:0,fontFamily:"Inter",fontSize:13,color:"#1c1f29"}} title={o.product}>{o.product}</p>
                <p style={{width:75, flexShrink:0,fontFamily:"Inter",fontWeight:600,fontSize:13,color:"#1c1f29"}}>₹{o.amount}</p>
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
      </div>

      <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage}/>

      {/* View */}
      {viewing && (
        <Modal title="Order Details" onClose={()=>setViewing(null)}>
          <DetailRow label="Order ID"       value={<span style={{color:"#7887fa",fontWeight:600}}>{viewing.displayId}</span>}/>
          <DetailRow label="Customer"       value={viewing.customer}/>
          <DetailRow label="Cook / Kitchen" value={viewing.cook}/>
          <DetailRow label="Items"          value={viewing.product}/>
          <DetailRow label="Amount"         value={<span style={{fontWeight:700}}>₹{viewing.amount}</span>}/>
          <DetailRow label="Payment"        value={`${viewing.paymentMethod?.toUpperCase() || "-"} · ${viewing.paymentStatus || "-"}`}/>
          <DetailRow label="Status"         value={<StatusBadge status={viewing.status}/>}/>
          <DetailRow label="Date"           value={viewing.date}/>
        </Modal>
      )}

      {/* Edit (status only — the rest of an order is a historical fact, not editable) */}
      {editing && (
        <Modal title="Update Order Status" onClose={()=>setEditing(null)}>
          <DetailRow label="Order ID" value={<span style={{color:"#7887fa",fontWeight:600}}>{editing.displayId}</span>}/>
          <DetailRow label="Customer" value={editing.customer}/>
          <FormField label="Status" value={statusDraft} onChange={v=>setStatusDraft(v as StatusType)} options={STATUS_OPTS}/>
          <SaveCancel onCancel={()=>setEditing(null)} onSave={saveEdit} saving={savingEdit}/>
        </Modal>
      )}

      {del && <ConfirmDelete name={`Order ${del.displayId}`} onConfirm={doDelete} onCancel={()=>setDel(null)} loading={deleting}/>}
    </div>
  );
}
