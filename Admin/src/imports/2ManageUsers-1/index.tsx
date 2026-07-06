function Frame() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[28px]">Manage Users</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[14px]">View and manage all user data.</p>
    </div>
  );
}

function Frame1() {
  return (
    <div className="bg-[#57b869] content-stretch flex items-start overflow-clip px-[20px] py-[12px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[14px] text-white whitespace-nowrap">+ Add User</p>
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

function Frame2() {
  return (
    <div className="bg-[#57b869] content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[13px] text-white whitespace-nowrap">All Users (2,345)</p>
    </div>
  );
}

function Frame3() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Customers (1,938)</p>
    </div>
  );
}

function Frame4() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Cooks (456)</p>
    </div>
  );
}

function Frame5() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-start overflow-clip px-[16px] py-[10px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Admins (5)</p>
    </div>
  );
}

function TabFilters() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Tab Filters">
      <Frame2 />
      <Frame3 />
      <Frame4 />
      <Frame5 />
    </div>
  );
}

function SearchBar() {
  return (
    <div className="bg-white relative rounded-[8px] shrink-0 w-[1096px]" data-name="Search Bar">
      <div className="[word-break:break-word] content-stretch flex font-['Inter:Regular',sans-serif] font-normal gap-[8px] items-start leading-[normal] not-italic overflow-clip px-[16px] py-[12px] relative rounded-[inherit] size-full text-[14px] whitespace-nowrap">
        <p className="relative shrink-0 text-black">🔍</p>
        <p className="relative shrink-0 text-[#b2b8bf]">Search users...</p>
      </div>
      <div aria-hidden className="absolute border border-[#e5e8ed] border-solid inset-0 pointer-events-none rounded-[8px]" />
    </div>
  );
}

function TableHeaders() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex font-semibold gap-[16px] items-start leading-[normal] not-italic overflow-clip pb-[16px] relative shrink-0 text-[#9499a6] text-[12px] w-full" data-name="Table Headers">
      <p className="font-['Inter:Semi_Bold','Noto_Sans_Symbols2:Regular',sans-serif] relative shrink-0 w-[40px]">☐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] relative shrink-0 w-[240px]">Name</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] relative shrink-0 w-[120px]">Role</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] relative shrink-0 w-[140px]">Phone</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] relative shrink-0 w-[100px]">Status</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] relative shrink-0 w-[120px]">Joined On</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] relative shrink-0 w-[80px]">Actions</p>
    </div>
  );
}

function Frame6() {
  return (
    <div className="h-[18px] overflow-clip relative shrink-0 w-[40px]" data-name="Frame">
      <div className="absolute border-[#ccd1d9] border-[1.5px] border-solid left-0 rounded-[4px] size-[18px] top-0" data-name="Rectangle" />
    </div>
  );
}

function Frame8() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[14px]">Maria Rosser</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">maria.rosser@gmail.com</p>
    </div>
  );
}

function Frame7() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] h-[44px] items-start overflow-clip relative shrink-0 w-[240px]" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame8 />
    </div>
  );
}

function Frame10() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Active</p>
    </div>
  );
}

function Frame9() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[100px]" data-name="Frame">
      <Frame10 />
    </div>
  );
}

function UserRow() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="User Row 1">
      <Frame6 />
      <Frame7 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Customer</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">9863201472</p>
      <Frame9 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 15, 2022</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame11() {
  return (
    <div className="h-[18px] overflow-clip relative shrink-0 w-[40px]" data-name="Frame">
      <div className="absolute border-[#ccd1d9] border-[1.5px] border-solid left-0 rounded-[4px] size-[18px] top-0" data-name="Rectangle" />
    </div>
  );
}

function Frame13() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[14px]">Rayna Carder</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">rayna.carder@yahoo.com</p>
    </div>
  );
}

function Frame12() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] h-[44px] items-start overflow-clip relative shrink-0 w-[240px]" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame13 />
    </div>
  );
}

function Frame15() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Active</p>
    </div>
  );
}

function Frame14() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[100px]" data-name="Frame">
      <Frame15 />
    </div>
  );
}

function UserRow1() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="User Row 2">
      <Frame11 />
      <Frame12 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Customer</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">9052134786</p>
      <Frame14 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 14, 2022</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame16() {
  return (
    <div className="h-[18px] overflow-clip relative shrink-0 w-[40px]" data-name="Frame">
      <div className="absolute border-[#ccd1d9] border-[1.5px] border-solid left-0 rounded-[4px] size-[18px] top-0" data-name="Rectangle" />
    </div>
  );
}

function Frame18() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[14px]">Talan Press</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">talan.press@outlook.com</p>
    </div>
  );
}

function Frame17() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] h-[44px] items-start overflow-clip relative shrink-0 w-[240px]" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame18 />
    </div>
  );
}

function Frame20() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Active</p>
    </div>
  );
}

function Frame19() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[100px]" data-name="Frame">
      <Frame20 />
    </div>
  );
}

function UserRow2() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="User Row 3">
      <Frame16 />
      <Frame17 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Cook</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">8765432190</p>
      <Frame19 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 13, 2022</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame21() {
  return (
    <div className="h-[18px] overflow-clip relative shrink-0 w-[40px]" data-name="Frame">
      <div className="absolute border-[#ccd1d9] border-[1.5px] border-solid left-0 rounded-[4px] size-[18px] top-0" data-name="Rectangle" />
    </div>
  );
}

function Frame23() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[14px]">Marley Dokidis</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">marley.dokidis@gmail.com</p>
    </div>
  );
}

function Frame22() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] h-[44px] items-start overflow-clip relative shrink-0 w-[240px]" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame23 />
    </div>
  );
}

function Frame25() {
  return (
    <div className="absolute bg-[rgba(242,89,89,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#f25959] text-[12px] whitespace-nowrap">Inactive</p>
    </div>
  );
}

function Frame24() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[100px]" data-name="Frame">
      <Frame25 />
    </div>
  );
}

function UserRow3() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="User Row 4">
      <Frame21 />
      <Frame22 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Customer</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">9123456780</p>
      <Frame24 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 12, 2022</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame26() {
  return (
    <div className="h-[18px] overflow-clip relative shrink-0 w-[40px]" data-name="Frame">
      <div className="absolute border-[#ccd1d9] border-[1.5px] border-solid left-0 rounded-[4px] size-[18px] top-0" data-name="Rectangle" />
    </div>
  );
}

function Frame28() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[14px]">Marcus Rosser</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">marcus.rosser@email.com</p>
    </div>
  );
}

function Frame27() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] h-[44px] items-start overflow-clip relative shrink-0 w-[240px]" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame28 />
    </div>
  );
}

function Frame30() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Active</p>
    </div>
  );
}

function Frame29() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[100px]" data-name="Frame">
      <Frame30 />
    </div>
  );
}

function UserRow4() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="User Row 5">
      <Frame26 />
      <Frame27 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Customer</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">8901234567</p>
      <Frame29 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 11, 2022</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame31() {
  return (
    <div className="h-[18px] overflow-clip relative shrink-0 w-[40px]" data-name="Frame">
      <div className="absolute border-[#ccd1d9] border-[1.5px] border-solid left-0 rounded-[4px] size-[18px] top-0" data-name="Rectangle" />
    </div>
  );
}

function Frame33() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[14px]">Zaire Bergson</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">zaire.bergson@mail.com</p>
    </div>
  );
}

function Frame32() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] h-[44px] items-start overflow-clip relative shrink-0 w-[240px]" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame33 />
    </div>
  );
}

function Frame35() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Active</p>
    </div>
  );
}

function Frame34() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[100px]" data-name="Frame">
      <Frame35 />
    </div>
  );
}

function UserRow5() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="User Row 6">
      <Frame31 />
      <Frame32 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Cook</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">9876543210</p>
      <Frame34 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 10, 2022</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame36() {
  return (
    <div className="h-[18px] overflow-clip relative shrink-0 w-[40px]" data-name="Frame">
      <div className="absolute border-[#ccd1d9] border-[1.5px] border-solid left-0 rounded-[4px] size-[18px] top-0" data-name="Rectangle" />
    </div>
  );
}

function Frame38() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[14px]">Lincoln Siphron</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">lincoln.s@company.com</p>
    </div>
  );
}

function Frame37() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] h-[44px] items-start overflow-clip relative shrink-0 w-[240px]" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame38 />
    </div>
  );
}

function Frame40() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.15)] content-stretch flex items-start left-0 overflow-clip px-[12px] py-[6px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Active</p>
    </div>
  );
}

function Frame39() {
  return (
    <div className="h-[27px] overflow-clip relative shrink-0 w-[100px]" data-name="Frame">
      <Frame40 />
    </div>
  );
}

function UserRow6() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip py-[16px] relative shrink-0 w-full" data-name="User Row 7">
      <Frame36 />
      <Frame37 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[120px]">Customer</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">8765409123</p>
      <Frame39 />
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 09, 2022</p>
      <p className="[word-break:break-word] font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function UserTable() {
  return (
    <div className="bg-white content-stretch flex flex-col h-[600px] items-start overflow-clip p-[24px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[1096px]" data-name="User Table">
      <TableHeaders />
      <div className="bg-[#e5e8ed] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <UserRow />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <UserRow1 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <UserRow2 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <UserRow3 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <UserRow4 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <UserRow5 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <UserRow6 />
    </div>
  );
}

function Frame42() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#b2b8bf] text-[16px] whitespace-nowrap">←</p>
    </div>
  );
}

function Frame43() {
  return (
    <div className="bg-[#57b869] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[13px] text-white whitespace-nowrap">1</p>
    </div>
  );
}

function Frame44() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">2</p>
    </div>
  );
}

function Frame45() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">3</p>
    </div>
  );
}

function Frame46() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">...</p>
    </div>
  );
}

function Frame47() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">235</p>
    </div>
  );
}

function Frame48() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[16px] whitespace-nowrap">→</p>
    </div>
  );
}

function Frame41() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <Frame42 />
      <Frame43 />
      <Frame44 />
      <Frame45 />
      <Frame46 />
      <Frame47 />
      <Frame48 />
    </div>
  );
}

function Pagination() {
  return (
    <div className="absolute bg-white content-stretch flex items-start justify-between left-[32px] overflow-clip top-[840px] w-[1096px]" data-name="Pagination">
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Showing 1 to 10 of 2,345 results</p>
      <Frame41 />
    </div>
  );
}

function MainContent() {
  return (
    <div className="absolute bg-[#f2f2f5] content-stretch flex flex-col gap-[24px] h-[900px] items-start left-[240px] overflow-clip px-[32px] py-[24px] top-0 w-[1160px]" data-name="Main Content">
      <Header />
      <TabFilters />
      <SearchBar />
      <UserTable />
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
    <div className="bg-[#58c66c] content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shadow-[0px_8px_24px_0px_rgba(88,198,108,0.25)] shrink-0 w-[228px]" data-name="Users Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">👥</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[14px] text-white">Users</p>
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
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Orders Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">📦</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Orders</p>
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

export default function Component2ManageUsers() {
  return (
    <div className="bg-[#f2f2f5] relative size-full" data-name="2. Manage Users">
      <MainContent />
      <PremiumSidebar />
    </div>
  );
}