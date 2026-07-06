function Frame() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[28px]">Manage Orders</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[14px]">View and track all order information.</p>
    </div>
  );
}

function Frame2() {
  return (
    <div className="relative rounded-[8px] shrink-0" data-name="Frame">
      <div className="content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[inherit] size-full">
        <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Export CSV</p>
      </div>
      <div aria-hidden className="absolute border border-[#e5e8ed] border-solid inset-0 pointer-events-none rounded-[8px]" />
    </div>
  );
}

function Frame3() {
  return (
    <div className="relative rounded-[8px] shrink-0" data-name="Frame">
      <div className="content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[inherit] size-full">
        <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Filter</p>
      </div>
      <div aria-hidden className="absolute border border-[#e5e8ed] border-solid inset-0 pointer-events-none rounded-[8px]" />
    </div>
  );
}

function Frame1() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <Frame2 />
      <Frame3 />
    </div>
  );
}

function Header() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Header">
      <Frame />
      <Frame1 />
    </div>
  );
}

function Frame4() {
  return (
    <div className="bg-[#57b869] content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[13px] text-white whitespace-nowrap">All Orders</p>
    </div>
  );
}

function Frame5() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Pending (23)</p>
    </div>
  );
}

function Frame6() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Processing (45)</p>
    </div>
  );
}

function Frame7() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Completed (189)</p>
    </div>
  );
}

function Frame8() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Cancelled (12)</p>
    </div>
  );
}

function TabFilters() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Tab Filters">
      <Frame4 />
      <Frame5 />
      <Frame6 />
      <Frame7 />
      <Frame8 />
    </div>
  );
}

function SearchBar() {
  return (
    <div className="bg-white relative rounded-[8px] shrink-0 w-[1096px]" data-name="Search Bar">
      <div className="[word-break:break-word] content-stretch flex font-['Inter:Regular',sans-serif] font-normal gap-[8px] items-start leading-[normal] not-italic overflow-clip px-[16px] py-[12px] relative rounded-[inherit] size-full text-[14px] whitespace-nowrap">
        <p className="relative shrink-0 text-black">🔍</p>
        <p className="relative shrink-0 text-[#b2b8bf]">Search orders...</p>
      </div>
      <div aria-hidden className="absolute border border-[#e5e8ed] border-solid inset-0 pointer-events-none rounded-[8px]" />
    </div>
  );
}

function TableHeaders() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex font-['Inter:Semi_Bold',sans-serif] font-semibold gap-[12px] items-start leading-[normal] not-italic overflow-clip pb-[16px] relative shrink-0 text-[#9499a6] text-[12px] w-full" data-name="Table Headers">
      <p className="relative shrink-0 w-[100px]">Order ID</p>
      <p className="relative shrink-0 w-[160px]">Customer</p>
      <p className="relative shrink-0 w-[140px]">Cook</p>
      <p className="relative shrink-0 w-[120px]">Product</p>
      <p className="relative shrink-0 w-[100px]">Amount</p>
      <p className="relative shrink-0 w-[120px]">Status</p>
      <p className="relative shrink-0 w-[110px]">Date</p>
      <p className="relative shrink-0 w-[80px]">Actions</p>
    </div>
  );
}

function Frame10() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Delivered</p>
    </div>
  );
}

function Frame9() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame10 />
    </div>
  );
}

function OrderRow() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="Order Row 1">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1523</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Maria Rosser</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">{`Anita's Kitchen`}</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Paneer Tikka</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹450</p>
      <Frame9 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[110px]">May 18, 2025</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame12() {
  return (
    <div className="absolute bg-[rgba(242,140,64,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#f28c40] text-[12px] whitespace-nowrap">Processing</p>
    </div>
  );
}

function Frame11() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame12 />
    </div>
  );
}

function OrderRow1() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="Order Row 2">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1522</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Rayna Carder</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Mumbai Spice</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Vada Pav</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹120</p>
      <Frame11 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[110px]">May 18, 2025</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame14() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Delivered</p>
    </div>
  );
}

function Frame13() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame14 />
    </div>
  );
}

function OrderRow2() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="Order Row 3">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1521</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Talan Press</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">South Indian</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Masala Dosa</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹180</p>
      <Frame13 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[110px]">May 17, 2025</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame16() {
  return (
    <div className="absolute bg-[rgba(242,199,64,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#f2c740] text-[12px] whitespace-nowrap">Pending</p>
    </div>
  );
}

function Frame15() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame16 />
    </div>
  );
}

function OrderRow3() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="Order Row 4">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1520</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Marley Dokidis</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Delhi Delights</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Chole Bhature</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹250</p>
      <Frame15 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[110px]">May 17, 2025</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame18() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Delivered</p>
    </div>
  );
}

function Frame17() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame18 />
    </div>
  );
}

function OrderRow4() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="Order Row 5">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1519</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Marcus Rosser</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Punjab Kitchen</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Butter Chicken</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹380</p>
      <Frame17 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[110px]">May 17, 2025</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame20() {
  return (
    <div className="absolute bg-[rgba(242,89,89,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#f25959] text-[12px] whitespace-nowrap">Cancelled</p>
    </div>
  );
}

function Frame19() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame20 />
    </div>
  );
}

function OrderRow5() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="Order Row 6">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1518</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Zaire Bergson</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Bengal Bites</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Fish Curry</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹320</p>
      <Frame19 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[110px]">May 16, 2025</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame22() {
  return (
    <div className="absolute bg-[rgba(242,140,64,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#f28c40] text-[12px] whitespace-nowrap">Processing</p>
    </div>
  );
}

function Frame21() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame22 />
    </div>
  );
}

function OrderRow6() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="Order Row 7">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1517</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Lincoln Siphron</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Coastal Treats</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Prawn Fry</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹480</p>
      <Frame21 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[110px]">May 16, 2025</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame24() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Delivered</p>
    </div>
  );
}

function Frame23() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame24 />
    </div>
  );
}

function OrderRow7() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="Order Row 8">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1516</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Talan Dokidis</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">{`Anita's Kitchen`}</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Dal Makhani</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹220</p>
      <Frame23 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[110px]">May 16, 2025</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function OrdersTable() {
  return (
    <div className="bg-white content-stretch flex flex-col items-start overflow-clip p-[24px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[1096px]" data-name="Orders Table">
      <TableHeaders />
      <div className="bg-[#e5e8ed] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <OrderRow />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <OrderRow1 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <OrderRow2 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <OrderRow3 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <OrderRow4 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <OrderRow5 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <OrderRow6 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <OrderRow7 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
    </div>
  );
}

function Frame26() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#b2b8bf] text-[16px] whitespace-nowrap">←</p>
    </div>
  );
}

function Frame27() {
  return (
    <div className="bg-[#57b869] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[13px] text-white whitespace-nowrap">1</p>
    </div>
  );
}

function Frame28() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">2</p>
    </div>
  );
}

function Frame29() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">3</p>
    </div>
  );
}

function Frame30() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">...</p>
    </div>
  );
}

function Frame31() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">671</p>
    </div>
  );
}

function Frame32() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[16px] whitespace-nowrap">→</p>
    </div>
  );
}

function Frame25() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <Frame26 />
      <Frame27 />
      <Frame28 />
      <Frame29 />
      <Frame30 />
      <Frame31 />
      <Frame32 />
    </div>
  );
}

function Pagination() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-[1096px]" data-name="Pagination">
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Showing 1 to 10 of 6,709 results</p>
      <Frame25 />
    </div>
  );
}

function MainContent() {
  return (
    <div className="absolute bg-[#f2f2f5] content-stretch flex flex-col gap-[24px] h-[900px] items-start left-[240px] overflow-clip px-[32px] py-[24px] top-0 w-[1160px]" data-name="Main Content">
      <Header />
      <TabFilters />
      <SearchBar />
      <OrdersTable />
      <Pagination />
    </div>
  );
}

function LogoSection() {
  return (
    <div className="absolute h-[80px] left-[-1px] overflow-clip top-[27px] w-[260px]" data-name="Logo Section">
      <div className="absolute bg-[#58c66c] left-[24px] rounded-[4px] size-[20px] top-0" data-name="Menu Icon" />
      <p className="[word-break:break-word] absolute font-['Inter:Bold',sans-serif] font-bold leading-[normal] left-[24px] not-italic text-[24px] text-white top-[28px] tracking-[-0.4px] whitespace-nowrap">TiffinCraft</p>
      <p className="[word-break:break-word] absolute font-['Inter:Medium',sans-serif] font-medium leading-[normal] left-[24px] not-italic text-[12px] text-[rgba(255,255,255,0.55)] top-[56px] whitespace-nowrap">Admin Panel</p>
    </div>
  );
}

function DashboardItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Dashboard Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">🏠</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Dashboard</p>
    </div>
  );
}

function UsersItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Users Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">👥</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Users</p>
    </div>
  );
}

function CooksItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Cooks Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">👨‍🍳</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Cooks</p>
    </div>
  );
}

function MealsItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Meals Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">🍱</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Meals</p>
    </div>
  );
}

function OrdersItem() {
  return (
    <div className="bg-[#58c66c] content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shadow-[0px_8px_24px_0px_rgba(88,198,108,0.25)] shrink-0 w-[228px]" data-name="Orders Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">📦</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[14px] text-white">Orders</p>
    </div>
  );
}

function ReviewsItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Reviews Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">⭐</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Reviews</p>
    </div>
  );
}

function PaymentsItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Payments Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">💳</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Payments</p>
    </div>
  );
}

function EarningsItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Earnings Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">💰</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Earnings</p>
    </div>
  );
}

function ReportsItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Reports Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">📊</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Reports</p>
    </div>
  );
}

function SettingsItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Settings Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">⚙️</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Settings</p>
    </div>
  );
}

function SupportItem() {
  return (
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Support Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">❓</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Support</p>
    </div>
  );
}

function NavigationMenu() {
  return (
    <div className="[word-break:break-word] absolute content-stretch flex flex-col gap-[8px] items-start leading-[normal] left-[-1px] not-italic overflow-clip px-[16px] top-[127px] whitespace-nowrap" data-name="Navigation Menu">
      <DashboardItem />
      <UsersItem />
      <CooksItem />
      <MealsItem />
      <OrdersItem />
      <ReviewsItem />
      <PaymentsItem />
      <EarningsItem />
      <ReportsItem />
      <SettingsItem />
      <SupportItem />
    </div>
  );
}

function AvatarContainer() {
  return (
    <div className="relative shrink-0 size-[36px]" data-name="Avatar Container">
      <div className="absolute left-0 size-[36px] top-0" data-name="Avatar Circle">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 36 36">
          <circle cx="18" cy="18" fill="var(--fill-0, #58C66C)" id="Avatar Circle" r="18" />
        </svg>
      </div>
      <p className="-translate-x-1/2 [word-break:break-word] absolute font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] left-[18px] not-italic text-[13px] text-center text-white top-[10px] whitespace-nowrap">AU</p>
    </div>
  );
}

function UserDetails() {
  return (
    <div className="[word-break:break-word] content-stretch flex flex-col gap-[2px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="User Details">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[13px] text-white">Admin User</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[11px] text-[rgba(255,255,255,0.55)]">Super Admin</p>
    </div>
  );
}

function ProfileCard() {
  return (
    <div className="absolute bg-[rgba(255,255,255,0.03)] h-[64px] left-[16px] rounded-[12px] top-[15px] w-[228px]" data-name="Profile Card">
      <div className="content-stretch flex gap-[12px] items-center overflow-clip p-[12px] relative rounded-[inherit] size-full">
        <AvatarContainer />
        <UserDetails />
      </div>
      <div aria-hidden className="absolute border border-[rgba(255,255,255,0.05)] border-solid inset-0 pointer-events-none rounded-[12px]" />
    </div>
  );
}

function ProfileSection() {
  return (
    <div className="absolute border-[rgba(255,255,255,0.08)] border-solid border-t h-[96px] left-[-1px] overflow-clip top-[803px] w-[260px]" data-name="Profile Section">
      <ProfileCard />
    </div>
  );
}

function PremiumSidebar() {
  return (
    <div className="absolute bg-[#1e222d] border border-[rgba(255,255,255,0.08)] border-solid h-[900px] left-0 overflow-clip top-0 w-[260px]" data-name="Premium Sidebar">
      <LogoSection />
      <NavigationMenu />
      <ProfileSection />
    </div>
  );
}

export default function Component4ManageOrders() {
  return (
    <div className="bg-[#f2f2f5] relative size-full" data-name="4. Manage Orders">
      <MainContent />
      <PremiumSidebar />
    </div>
  );
}