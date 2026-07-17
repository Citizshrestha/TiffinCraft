import React, { useState, useEffect, useCallback, useMemo } from "react";
import { Pagination } from "./Pagination";
import { ActionButtons } from "./ActionButtons";
import { Modal, ConfirmDelete, DetailRow, FormField, SaveCancel } from "./Modal";
import { DbMeal, fetchMeals, createMealApi, updateMealApi, deleteMealApi } from "../api/mealsApi";
import { Cook, fetchCooks } from "../api/cooksApi";
import { useToast } from "./Toast";

const PER_PAGE = 8;

/** Unified shape both the demo cards and real DB meals render through. */
interface UnifiedMeal {
  source: "dummy" | "db";
  id: number;
  cookId?: number;
  name: string;
  cook: string;
  price: number;
  category: string;
  available: boolean;
  isVegetarian?: boolean;
  isVegan?: boolean;
  imageUrl?: string | null;
}

const SEED: UnifiedMeal[] = [
  { source:"dummy", id:1,  name:"Dal Makhani",       cook:"Anita's Kitchen", price:180, category:"North Indian", available:true  },
  { source:"dummy", id:2,  name:"Paneer Tikka",      cook:"Spice Route",     price:220, category:"North Indian", available:true  },
  { source:"dummy", id:3,  name:"Masala Dosa",       cook:"South Indian",    price:120, category:"South Indian", available:true  },
  { source:"dummy", id:4,  name:"Chole Bhature",     cook:"Delhi Delights",  price:160, category:"North Indian", available:false },
  { source:"dummy", id:5,  name:"Biryani",           cook:"Mumbai Spice",    price:260, category:"Mughlai",      available:true  },
  { source:"dummy", id:6,  name:"Butter Chicken",    cook:"Punjab Kitchen",  price:240, category:"North Indian", available:true  },
  { source:"dummy", id:7,  name:"Fish Curry",        cook:"Bengal Bites",    price:280, category:"Bengali",      available:true  },
  { source:"dummy", id:8,  name:"Rajma Chawal",      cook:"Healthy Meals",   price:140, category:"North Indian", available:false },
  { source:"dummy", id:9,  name:"Idli Sambar",       cook:"South Indian",    price:100, category:"South Indian", available:true  },
  { source:"dummy", id:10, name:"Shahi Paneer",      cook:"Anita's Kitchen", price:200, category:"North Indian", available:true  },
  { source:"dummy", id:11, name:"Matar Paneer",      cook:"Tasty Tiffins",   price:190, category:"North Indian", available:false },
  { source:"dummy", id:12, name:"Chicken Curry",     cook:"Mumbai Spice",    price:270, category:"Mughlai",      available:true  },
];

const EM: Record<string,string> = {"North Indian":"🍛","South Indian":"🥘","Mughlai":"🍖","Bengali":"🐟"};
const CATS = ["North Indian","South Indian","Mughlai","Bengali","Continental","Chinese"];
const VEG_FILTERS = [
  { id: "all",     label: "All" },
  { id: "veg",     label: "🟢 Veg" },
  { id: "non-veg", label: "🔴 Non-Veg" },
];

interface EditForm { name: string; price: string; category: string; vegStatus: string; available: boolean; }
interface AddForm { cookId: string; name: string; price: string; category: string; vegStatus: string; available: boolean; }

const blankAdd: AddForm = { cookId:"", name:"", price:"", category:"North Indian", vegStatus:"veg", available:true };

function validateEdit(f: EditForm): Record<string,string> {
  const e: Record<string,string> = {};
  if (!f.name.trim())  e.name  = "Meal name is required";
  if (!f.price.trim() || isNaN(Number(f.price)) || Number(f.price) <= 0) e.price = "Enter a valid price";
  if (!f.category.trim()) e.category = "Category is required";
  return e;
}

function validateAdd(f: AddForm): Record<string,string> {
  const e: Record<string,string> = {};
  if (!f.cookId) e.cookId = "Select a cook";
  if (!f.name.trim())  e.name  = "Meal name is required";
  if (!f.price.trim() || isNaN(Number(f.price)) || Number(f.price) <= 0) e.price = "Enter a valid price";
  if (!f.category.trim()) e.category = "Category is required";
  return e;
}

function formatPrice(n: number): string {
  return `₹${n.toLocaleString("en-IN")}`;
}

export function MealsPage() {
  const { showToast } = useToast();

  const [dummyMeals, setDummyMeals] = useState<UnifiedMeal[]>(SEED);
  const [dbMeals, setDbMeals] = useState<DbMeal[]>([]);
  const [loadingDb, setLoadingDb] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [cooks, setCooks] = useState<Cook[]>([]);

  const [search,  setSearch]  = useState("");
  const [catFilter, setCatFilter] = useState("all");
  const [vegFilter, setVegFilter] = useState("all");
  const [page, setPage] = useState(1);

  const [viewing, setViewing] = useState<UnifiedMeal|null>(null);
  const [editing, setEditing] = useState<UnifiedMeal|null>(null);
  const [editForm, setEditForm] = useState<EditForm|null>(null);
  const [editErrs, setEditErrs] = useState<Record<string,string>>({});
  const [savingEdit, setSavingEdit] = useState(false);

  const [del, setDel] = useState<UnifiedMeal|null>(null);
  const [deleting, setDeleting] = useState(false);

  const [adding, setAdding] = useState(false);
  const [addForm, setAddForm] = useState<AddForm>(blankAdd);
  const [addErrs, setAddErrs] = useState<Record<string,string>>({});
  const [savingAdd, setSavingAdd] = useState(false);

  const loadDbMeals = useCallback(async () => {
    setLoadingDb(true);
    setLoadError("");
    try {
      const data = await fetchMeals();
      setDbMeals(data);
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : "Failed to load meals.");
    } finally {
      setLoadingDb(false);
    }
  }, []);

  useEffect(() => { loadDbMeals(); }, [loadDbMeals]);
  useEffect(() => { fetchCooks().then(setCooks).catch(() => {}); }, []);

  const unifiedDb: UnifiedMeal[] = useMemo(() => dbMeals.map(m => ({
    source: "db" as const,
    id: m.id,
    cookId: m.cookId,
    name: m.name,
    cook: m.cook,
    price: m.price,
    category: m.category,
    available: m.available,
    isVegetarian: m.isVegetarian,
    isVegan: m.isVegan,
    imageUrl: m.imageUrl,
  })), [dbMeals]);

  const allMeals = useMemo(
    () => [...dummyMeals, ...unifiedDb].sort((a, b) => a.name.localeCompare(b.name)),
    [dummyMeals, unifiedDb]
  );

  const allCategories = useMemo(
    () => Array.from(new Set([...CATS, ...allMeals.map(m => m.category)])).sort(),
    [allMeals]
  );

  const filtered = allMeals.filter(m => {
    const s = !search || m.name.toLowerCase().includes(search.toLowerCase()) || m.cook.toLowerCase().includes(search.toLowerCase());
    const c = catFilter === "all" || m.category === catFilter;
    const v = vegFilter === "all"
      || (vegFilter === "veg" && m.isVegetarian === true)
      || (vegFilter === "non-veg" && m.isVegetarian === false);
    return s && c && v;
  });

  const totalPages = Math.max(1, Math.ceil(filtered.length / PER_PAGE));
  const visible = filtered.slice((page - 1) * PER_PAGE, page * PER_PAGE);

  useEffect(() => { setPage(1); }, [search, catFilter, vegFilter]);

  const toggleAvailability = async (m: UnifiedMeal) => {
    if (m.source === "dummy") {
      setDummyMeals(p => p.map(x => x.id === m.id ? { ...x, available: !x.available } : x));
      return;
    }
    const next = !m.available;
    setDbMeals(p => p.map(x => x.id === m.id ? { ...x, available: next } : x));
    try {
      await updateMealApi(m.id, { is_available: next });
    } catch (err) {
      setDbMeals(p => p.map(x => x.id === m.id ? { ...x, available: !next } : x));
      showToast(err instanceof Error ? err.message : "Failed to update availability.", "error");
    }
  };

  const startEdit = (m: UnifiedMeal) => {
    setEditing(m);
    setEditForm({
      name: m.name,
      price: String(m.price),
      category: m.category,
      vegStatus: m.isVegetarian === false ? "non-veg" : "veg",
      available: m.available,
    });
    setEditErrs({});
  };

  const saveEdit = async () => {
    if (!editing || !editForm || savingEdit) return;
    const errs = validateEdit(editForm);
    if (Object.keys(errs).length) { setEditErrs(errs); return; }

    if (editing.source === "dummy") {
      setDummyMeals(p => p.map(x => x.id === editing.id ? {
        ...x,
        name: editForm.name,
        price: Number(editForm.price),
        category: editForm.category,
        available: editForm.available,
      } : x));
      setEditing(null); setEditForm(null);
      showToast("Meal updated (demo card — not persisted).", "success");
      return;
    }

    setSavingEdit(true);
    try {
      const updated = await updateMealApi(editing.id, {
        name: editForm.name,
        price: Number(editForm.price),
        category: editForm.category,
        is_vegetarian: editForm.vegStatus === "veg",
        is_vegan: false,
        is_available: editForm.available,
      });
      setDbMeals(p => p.map(x => x.id === updated.id ? updated : x));
      setEditing(null); setEditForm(null);
      showToast("Meal updated successfully.", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to update meal.", "error");
    } finally {
      setSavingEdit(false);
    }
  };

  const doDelete = async () => {
    if (!del || deleting) return;
    if (del.source === "dummy") {
      setDummyMeals(p => p.filter(x => x.id !== del.id));
      setDel(null);
      return;
    }
    setDeleting(true);
    try {
      await deleteMealApi(del.id);
      setDbMeals(p => p.filter(x => x.id !== del.id));
      setDel(null);
      showToast("Meal deleted successfully.", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to delete meal.", "error");
    } finally {
      setDeleting(false);
    }
  };

  const submitAdd = async () => {
    if (savingAdd) return;
    const errs = validateAdd(addForm);
    if (Object.keys(errs).length) { setAddErrs(errs); return; }
    setSavingAdd(true);
    try {
      const created = await createMealApi({
        cook_id: Number(addForm.cookId),
        name: addForm.name,
        price: Number(addForm.price),
        category: addForm.category,
        is_vegetarian: addForm.vegStatus === "veg",
        is_vegan: false,
        is_available: addForm.available,
      });
      setDbMeals(p => [created, ...p]);
      setAdding(false);
      setAddForm(blankAdd);
      showToast("Meal created successfully.", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to create meal.", "error");
    } finally {
      setSavingAdd(false);
    }
  };

  const ErrMsg = ({field,errs}:{field:string;errs:Record<string,string>}) =>
    errs[field]?<p style={{fontFamily:"Inter",fontSize:11,color:"#f25959",marginTop:2}}>{errs[field]}</p>:null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p style={{fontFamily:"Inter",fontWeight:700,fontSize:28,color:"#1c1f29"}}>Manage Meals</p>
          <p style={{fontFamily:"Inter",fontWeight:400,fontSize:14,color:"#9499a6",marginTop:4}}>Browse and manage all available meals.</p>
        </div>
        <button onClick={()=>{setAdding(true);setAddForm(blankAdd);setAddErrs({});}}
          className="self-start shrink-0"
          style={{background:"#57b869",border:"none",fontFamily:"Inter",fontWeight:600,color:"white",fontSize:14,padding:"12px 20px",borderRadius:8,cursor:"pointer"}}>
          + Add Meal
        </button>
      </div>

      {loadError && (
        <div style={{background:"rgba(242,89,89,0.08)",border:"1px solid rgba(242,89,89,0.25)",borderRadius:8,padding:"10px 16px",display:"flex",alignItems:"center",justifyContent:"space-between"}}>
          <p style={{fontFamily:"Inter",fontSize:13,color:"#f25959"}}>{loadError} — showing demo meals only.</p>
          <button onClick={loadDbMeals} style={{background:"none",border:"1px solid #f25959",color:"#f25959",borderRadius:6,padding:"4px 10px",fontFamily:"Inter",fontSize:12,cursor:"pointer"}}>Retry</button>
        </div>
      )}
      {loadingDb && !loadError && (
        <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>Loading meals from the database…</p>
      )}

      <div className="flex gap-3 flex-wrap items-center">
        <div className="flex items-center gap-2 px-4 py-3 rounded-[8px] bg-white flex-1" style={{border:"1px solid #e5e8ed",minWidth:200}}>
          <span>🔍</span>
          <input className="flex-1 outline-none bg-transparent" style={{border:"none",fontFamily:"Inter",fontSize:14,color:"#1c1f29"}}
            placeholder="Search meals..." value={search} onChange={e=>setSearch(e.target.value)}/>
        </div>
        <div className="flex gap-2 flex-wrap">
          {VEG_FILTERS.map(v=>(
            <button key={v.id} onClick={()=>setVegFilter(v.id)}
              style={{padding:"8px 14px",borderRadius:8,fontSize:12,cursor:"pointer",border:"none",fontFamily:"Inter",fontWeight:500,
                background:vegFilter===v.id?"#1c1f29":"#f2f5f7",color:vegFilter===v.id?"#fff":"#9499a6"}}>
              {v.label}
            </button>
          ))}
        </div>
      </div>

      <div className="flex gap-2 flex-wrap">
        {["all",...allCategories].map(c=>(
          <button key={c} onClick={()=>setCatFilter(c)}
            style={{padding:"8px 14px",borderRadius:8,fontSize:12,cursor:"pointer",border:"none",fontFamily:"Inter",fontWeight:500,
              background:catFilter===c?"#57b869":"#f2f5f7",color:catFilter===c?"#fff":"#9499a6"}}>
            {c==="all"?"All":c}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {visible.map(m=>(
          <div key={`${m.source}-${m.id}`} className="bg-white rounded-[12px] overflow-hidden hover:shadow-md transition-shadow" style={{boxShadow:"0px 2px 8px rgba(0,0,0,0.08)"}}>
            <div className="h-[120px] flex items-center justify-center overflow-hidden" style={{background:"#f2f5f7"}}>
              {m.imageUrl ? (
                <img src={m.imageUrl} alt={m.name} className="w-full h-full object-cover" />
              ) : (
                <span style={{fontSize:48}}>{EM[m.category]||"🍛"}</span>
              )}
            </div>
            <div className="p-4">
              <div className="flex items-center gap-2 mb-1">
                {m.isVegetarian !== undefined && (
                  <span title={m.isVegetarian ? "Vegetarian" : "Non-Vegetarian"} style={{fontSize:11}}>
                    {m.isVegetarian ? "🟢" : "🔴"}
                  </span>
                )}
                <p style={{fontFamily:"Inter",fontWeight:600,fontSize:15,color:"#1c1f29"}}>{m.name}</p>
              </div>
              <p style={{fontFamily:"Inter",fontSize:12,color:"#9499a6",marginBottom:10}}>{m.cook}</p>
              <div className="flex items-center justify-between mb-3">
                <p style={{fontFamily:"Inter",fontWeight:700,fontSize:14,color:"#1c1f29"}}>{formatPrice(m.price)}</p>
                <span style={{background:"#f2f5f7",fontFamily:"Inter",fontWeight:500,fontSize:11,color:"#9499a6",padding:"3px 8px",borderRadius:6}}>{m.category}</span>
              </div>
              <div className="flex items-center justify-between mb-3">
                <p style={{fontFamily:"Inter",fontSize:12,color:m.available?"#57b869":"#9499a6"}}>{m.available?"Available":"Unavailable"}</p>
                <button onClick={()=>toggleAvailability(m)} style={{width:40,height:20,borderRadius:10,border:"none",background:m.available?"#57b869":"#e5e8ed",position:"relative",cursor:"pointer",transition:"background 150ms"}}>
                  <div style={{position:"absolute",top:2,left:m.available?"calc(100% - 18px)":2,width:16,height:16,borderRadius:8,background:"white",transition:"left 150ms"}}/>
                </button>
              </div>
              <div className="flex justify-end">
                <ActionButtons onView={()=>setViewing(m)} onEdit={()=>startEdit(m)} onDelete={()=>setDel(m)}/>
              </div>
            </div>
          </div>
        ))}
        {visible.length===0&&(
          <div className="col-span-full text-center py-12">
            <p style={{fontFamily:"Inter",fontSize:14,color:"#9499a6"}}>No meals found.</p>
          </div>
        )}
      </div>

      <Pagination current={page} total={totalPages} totalItems={filtered.length} onPageChange={setPage}/>

      {viewing&&(
        <Modal title="Meal Details" onClose={()=>setViewing(null)}>
          <div className="flex items-center gap-4 mb-5">
            <div style={{width:64,height:64,borderRadius:12,background:"#f2f5f7",display:"flex",alignItems:"center",justifyContent:"center",fontSize:36,overflow:"hidden"}}>
              {viewing.imageUrl ? <img src={viewing.imageUrl} alt={viewing.name} className="w-full h-full object-cover"/> : (EM[viewing.category]||"🍛")}
            </div>
            <div>
              <p style={{fontFamily:"Inter",fontWeight:700,fontSize:18,color:"#1c1f29"}}>{viewing.name}</p>
              <p style={{fontFamily:"Inter",fontSize:13,color:"#9499a6"}}>{viewing.cook}</p>
            </div>
          </div>
          <DetailRow label="Price"        value={formatPrice(viewing.price)}/>
          <DetailRow label="Category"     value={viewing.category}/>
          <DetailRow label="Diet"         value={viewing.isVegetarian === undefined ? "N/A" : (viewing.isVegetarian ? "🟢 Vegetarian" : "🔴 Non-Vegetarian")}/>
          <DetailRow label="Availability" value={<span style={{color:viewing.available?"#57b869":"#9499a6",fontWeight:600}}>{viewing.available?"✓ Available":"✗ Unavailable"}</span>}/>
        </Modal>
      )}

      {editing&&editForm&&(
        <Modal title="Edit Meal" onClose={()=>{setEditing(null);setEditForm(null);setEditErrs({});}}>
          <FormField label="Meal Name" value={editForm.name} onChange={v=>setEditForm({...editForm,name:v})}/>
          <ErrMsg field="name" errs={editErrs}/>
          <FormField label="Price (₹)" value={editForm.price} onChange={v=>setEditForm({...editForm,price:v})} type="number"/>
          <ErrMsg field="price" errs={editErrs}/>
          <FormField label="Category" value={editForm.category} onChange={v=>setEditForm({...editForm,category:v})}/>
          <ErrMsg field="category" errs={editErrs}/>
          <FormField label="Diet" value={editForm.vegStatus} onChange={v=>setEditForm({...editForm,vegStatus:v})} options={["veg","non-veg"]}/>
          <FormField label="Availability" value={editForm.available?"available":"unavailable"} onChange={v=>setEditForm({...editForm,available:v==="available"})} options={["available","unavailable"]}/>
          <SaveCancel onCancel={()=>{setEditing(null);setEditForm(null);setEditErrs({});}} onSave={saveEdit} saving={savingEdit}/>
        </Modal>
      )}

      {adding&&(
        <Modal title="Add New Meal" onClose={()=>{setAdding(false);setAddErrs({});}}>
          <div style={{marginBottom:16}}>
            <p style={{fontFamily:"Inter",fontWeight:500,fontSize:13,color:"#1c1f29",marginBottom:6}}>Cook / Kitchen</p>
            <select value={addForm.cookId} onChange={e=>setAddForm({...addForm,cookId:e.target.value})}
              style={{border:"1px solid #e5e8ed",fontFamily:"Inter",color:"#1c1f29",background:"white",width:"100%",padding:"12px 16px",borderRadius:8,fontSize:14,outline:"none"}}>
              <option value="">Select a cook...</option>
              {cooks.map(c=>(<option key={c.id} value={c.id}>{c.kitchen} ({c.owner})</option>))}
            </select>
          </div>
          <ErrMsg field="cookId" errs={addErrs}/>
          <FormField label="Meal Name" value={addForm.name} onChange={v=>setAddForm({...addForm,name:v})}/>
          <ErrMsg field="name" errs={addErrs}/>
          <FormField label="Price (₹)" value={addForm.price} onChange={v=>setAddForm({...addForm,price:v})} type="number"/>
          <ErrMsg field="price" errs={addErrs}/>
          <FormField label="Category" value={addForm.category} onChange={v=>setAddForm({...addForm,category:v})} options={CATS}/>
          <FormField label="Diet" value={addForm.vegStatus} onChange={v=>setAddForm({...addForm,vegStatus:v})} options={["veg","non-veg"]}/>
          <FormField label="Availability" value={addForm.available?"available":"unavailable"} onChange={v=>setAddForm({...addForm,available:v==="available"})} options={["available","unavailable"]}/>
          <SaveCancel onCancel={()=>{setAdding(false);setAddErrs({});}} onSave={submitAdd} saving={savingAdd} saveLabel="Create Meal"/>
        </Modal>
      )}

      {del&&<ConfirmDelete name={del.name} onConfirm={doDelete} onCancel={()=>setDel(null)} loading={deleting}/>}
    </div>
  );
}
