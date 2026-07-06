import svgPaths from "./svg-j47mlv6oz0";

function Frame() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[24px]">Welcome back, Admin! 👋</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[14px]">{`Here's what's happening with TiffinCraft today.`}</p>
    </div>
  );
}

function Frame1() {
  return (
    <div className="relative rounded-[6px] shrink-0" data-name="Frame">
      <div className="content-stretch flex items-start overflow-clip px-[12px] py-[8px] relative rounded-[inherit] size-full">
        <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">📅 May 12 - May 18, 2025</p>
      </div>
      <div aria-hidden className="absolute border border-[#e5e8ed] border-solid inset-0 pointer-events-none rounded-[6px]" />
    </div>
  );
}

function WelcomeSection() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Welcome Section">
      <Frame />
      <Frame1 />
    </div>
  );
}

function KpiTotalUsers() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[8px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_1px_3px_0px_rgba(0,0,0,0.06)] shrink-0 w-[265px]" data-name="KPI - Total Users">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px]">Total Users</p>
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">2,345</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#57b869] text-[13px]">+12.8%</p>
    </div>
  );
}

function KpiTotalCooks() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[8px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_1px_3px_0px_rgba(0,0,0,0.06)] shrink-0 w-[265px]" data-name="KPI - Total Cooks">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px]">Total Cooks</p>
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">456</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#57b869] text-[13px]">+8.2%</p>
    </div>
  );
}

function KpiTotalOrders() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[8px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_1px_3px_0px_rgba(0,0,0,0.06)] shrink-0 w-[265px]" data-name="KPI - Total Orders">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px]">Total Orders</p>
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">6,709</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#57b869] text-[13px]">+18.3%</p>
    </div>
  );
}

function KpiTotalRevenue() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[8px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_1px_3px_0px_rgba(0,0,0,0.06)] shrink-0 w-[265px]" data-name="KPI - Total Revenue">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px]">Total Revenue</p>
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">₹1,45,230</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#57b869] text-[13px]">+24.4%</p>
    </div>
  );
}

function Component4KpiCards() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="4 KPI Cards">
      <KpiTotalUsers />
      <KpiTotalCooks />
      <KpiTotalOrders />
      <KpiTotalRevenue />
    </div>
  );
}

function Frame3() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[28px]">6,709</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#57b869] text-[14px]">+18.3%</p>
    </div>
  );
}

function Frame2() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[16px]">Orders Overview</p>
      <Frame3 />
    </div>
  );
}

function ChartArea() {
  return (
    <div className="h-[140px] relative shrink-0 w-[492px]" data-name="Chart Area">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 492 140">
        <g id="Chart Area">
          <rect fill="var(--fill-0, #FAFAFC)" height="140" rx="8" width="492" />
          <g id="Vector" />
          <path d={svgPaths.p31d756c0} id="Orders Trend Line" stroke="var(--stroke-0, #7887FA)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" />
          <circle cx="36" cy="120" fill="var(--fill-0, #7887FA)" id="Ellipse" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="106" cy="105" fill="var(--fill-0, #7887FA)" id="Ellipse_2" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="176" cy="110" fill="var(--fill-0, #7887FA)" id="Ellipse_3" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="246" cy="90" fill="var(--fill-0, #7887FA)" id="Ellipse_4" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="316" cy="80" fill="var(--fill-0, #7887FA)" id="Ellipse_5" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="386" cy="70" fill="var(--fill-0, #7887FA)" id="Ellipse_6" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="456" cy="50" fill="var(--fill-0, #7887FA)" id="Ellipse_7" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
        </g>
      </svg>
    </div>
  );
}

function Frame4() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex font-['Inter:Regular',sans-serif] font-normal items-start justify-between leading-[normal] not-italic overflow-clip relative shrink-0 text-[#b2b8bf] text-[11px] w-full whitespace-nowrap" data-name="Frame">
      <p className="relative shrink-0">May 12</p>
      <p className="relative shrink-0">May 13</p>
      <p className="relative shrink-0">May 14</p>
      <p className="relative shrink-0">May 15</p>
      <p className="relative shrink-0">May 16</p>
      <p className="relative shrink-0">May 17</p>
      <p className="relative shrink-0">May 18</p>
    </div>
  );
}

function OrdersOverview() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[16px] items-start overflow-clip p-[24px] relative rounded-[12px] shadow-[0px_1px_3px_0px_rgba(0,0,0,0.06)] shrink-0 w-[540px]" data-name="Orders Overview">
      <Frame2 />
      <ChartArea />
      <Frame4 />
    </div>
  );
}

function Frame6() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[28px]">₹1,45,230</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#57b869] text-[14px]">+24.4%</p>
    </div>
  );
}

function Frame5() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[16px]">Revenue Overview</p>
      <Frame6 />
    </div>
  );
}

function ChartArea1() {
  return (
    <div className="h-[140px] relative shrink-0 w-[492px]" data-name="Chart Area">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 492 140">
        <g id="Chart Area">
          <rect fill="var(--fill-0, #FAFCFA)" height="140" rx="8" width="492" />
          <g id="Vector" />
          <path d={svgPaths.p1d031680} id="Revenue Trend Line" stroke="var(--stroke-0, #57B869)" strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" />
          <circle cx="36" cy="110" fill="var(--fill-0, #57B869)" id="Ellipse" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="106" cy="105" fill="var(--fill-0, #57B869)" id="Ellipse_2" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="176" cy="90" fill="var(--fill-0, #57B869)" id="Ellipse_3" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="246" cy="85" fill="var(--fill-0, #57B869)" id="Ellipse_4" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="316" cy="70" fill="var(--fill-0, #57B869)" id="Ellipse_5" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="386" cy="60" fill="var(--fill-0, #57B869)" id="Ellipse_6" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
          <circle cx="456" cy="45" fill="var(--fill-0, #57B869)" id="Ellipse_7" r="3" stroke="var(--stroke-0, white)" strokeWidth="2" />
        </g>
      </svg>
    </div>
  );
}

function Frame7() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex font-['Inter:Regular',sans-serif] font-normal items-start justify-between leading-[normal] not-italic overflow-clip relative shrink-0 text-[#b2b8bf] text-[11px] w-full whitespace-nowrap" data-name="Frame">
      <p className="relative shrink-0">May 12</p>
      <p className="relative shrink-0">May 13</p>
      <p className="relative shrink-0">May 14</p>
      <p className="relative shrink-0">May 15</p>
      <p className="relative shrink-0">May 16</p>
      <p className="relative shrink-0">May 17</p>
      <p className="relative shrink-0">May 18</p>
    </div>
  );
}

function RevenueOverview() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[16px] items-start overflow-clip p-[24px] relative rounded-[12px] shadow-[0px_1px_3px_0px_rgba(0,0,0,0.06)] shrink-0 w-[540px]" data-name="Revenue Overview">
      <Frame5 />
      <ChartArea1 />
      <Frame7 />
    </div>
  );
}

function ChartsRow() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip relative shrink-0" data-name="Charts Row">
      <OrdersOverview />
      <RevenueOverview />
    </div>
  );
}

function Frame8() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex items-start justify-between leading-[normal] not-italic overflow-clip relative shrink-0 w-full whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[16px]">Recent Orders</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#7887fa] text-[13px]">View All Orders →</p>
    </div>
  );
}

function Frame9() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex font-['Inter:Semi_Bold',sans-serif] font-semibold gap-[12px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 text-[#b2b8bf] text-[12px] w-full" data-name="Frame">
      <p className="relative shrink-0 w-[100px]">Order ID</p>
      <p className="relative shrink-0 w-[140px]">Customer</p>
      <p className="relative shrink-0 w-[140px]">Cook</p>
      <p className="relative shrink-0 w-[100px]">Amount</p>
      <p className="relative shrink-0 w-[120px]">Status</p>
    </div>
  );
}

function Frame11() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.12)] content-stretch flex items-start left-0 overflow-clip px-[10px] py-[4px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Delivered</p>
    </div>
  );
}

function Frame10() {
  return (
    <div className="h-[24px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame11 />
    </div>
  );
}

function Order() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[12px] relative shrink-0 w-full" data-name="Order 1">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1234</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Rahul Sharma</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">{`Anita's Kitchen`}</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹360</p>
      <Frame10 />
    </div>
  );
}

function Frame13() {
  return (
    <div className="absolute bg-[rgba(242,199,64,0.12)] content-stretch flex items-start left-0 overflow-clip px-[10px] py-[4px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#f2c740] text-[12px] whitespace-nowrap">Processing</p>
    </div>
  );
}

function Frame12() {
  return (
    <div className="h-[24px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame13 />
    </div>
  );
}

function Order1() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[12px] relative shrink-0 w-full" data-name="Order 2">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1233</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Priya Patel</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Spice Route</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹380</p>
      <Frame12 />
    </div>
  );
}

function Frame15() {
  return (
    <div className="absolute bg-[rgba(242,140,64,0.12)] content-stretch flex items-start left-0 overflow-clip px-[10px] py-[4px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#f28c40] text-[12px] whitespace-nowrap">Out for Delivery</p>
    </div>
  );
}

function Frame14() {
  return (
    <div className="h-[24px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame15 />
    </div>
  );
}

function Order2() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[12px] relative shrink-0 w-full" data-name="Order 3">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1232</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Vikram Singh</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Healthy Meals</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹460</p>
      <Frame14 />
    </div>
  );
}

function Frame17() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.12)] content-stretch flex items-start left-0 overflow-clip px-[10px] py-[4px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Accepted</p>
    </div>
  );
}

function Frame16() {
  return (
    <div className="h-[24px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame17 />
    </div>
  );
}

function Order3() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[12px] relative shrink-0 w-full" data-name="Order 4">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1231</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Neha Gupta</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Tasty Tiffins</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹320</p>
      <Frame16 />
    </div>
  );
}

function Frame19() {
  return (
    <div className="absolute bg-[rgba(87,184,105,0.12)] content-stretch flex items-start left-0 overflow-clip px-[10px] py-[4px] rounded-[12px] top-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#57b869] text-[12px] whitespace-nowrap">Delivered</p>
    </div>
  );
}

function Frame18() {
  return (
    <div className="h-[24px] overflow-clip relative shrink-0 w-[120px]" data-name="Frame">
      <Frame19 />
    </div>
  );
}

function Order4() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip py-[12px] relative shrink-0 w-full" data-name="Order 5">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#7887fa] text-[13px] w-[100px]">#ORD-1230</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">Amit Kumar</p>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[140px]">{`Anita's Kitchen`}</p>
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] w-[100px]">₹380</p>
      <Frame18 />
    </div>
  );
}

function RecentOrders() {
  return (
    <div className="bg-white content-stretch flex flex-col items-start overflow-clip p-[24px] relative rounded-[12px] shadow-[0px_1px_3px_0px_rgba(0,0,0,0.06)] shrink-0 w-[680px]" data-name="Recent Orders">
      <Frame8 />
      <div className="h-[16px] relative shrink-0 w-[632px]" data-name="Rectangle" />
      <Frame9 />
      <div className="h-[12px] relative shrink-0 w-[632px]" data-name="Rectangle" />
      <div className="bg-[#edf0f2] h-px relative shrink-0 w-[632px]" data-name="Rectangle" />
      <Order />
      <div className="bg-[#f5f7fa] h-px relative shrink-0 w-[632px]" data-name="Rectangle" />
      <Order1 />
      <div className="bg-[#f5f7fa] h-px relative shrink-0 w-[632px]" data-name="Rectangle" />
      <Order2 />
      <div className="bg-[#f5f7fa] h-px relative shrink-0 w-[632px]" data-name="Rectangle" />
      <Order3 />
      <div className="bg-[#f5f7fa] h-px relative shrink-0 w-[632px]" data-name="Rectangle" />
      <Order4 />
    </div>
  );
}

function Frame21() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[14px]">{`Anita's Kitchen`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#b2b8bf] text-[12px]">123+ orders</p>
    </div>
  );
}

function Frame20() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame21 />
    </div>
  );
}

function Frame22() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 text-[14px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-black">⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29]">4.8</p>
    </div>
  );
}

function Cook() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Cook 1">
      <Frame20 />
      <Frame22 />
    </div>
  );
}

function Frame24() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[14px]">Spice Route</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#b2b8bf] text-[12px]">98+ orders</p>
    </div>
  );
}

function Frame23() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame24 />
    </div>
  );
}

function Frame25() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 text-[14px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-black">⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29]">4.7</p>
    </div>
  );
}

function Cook1() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Cook 2">
      <Frame23 />
      <Frame25 />
    </div>
  );
}

function Frame27() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[14px]">Healthy Meals</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#b2b8bf] text-[12px]">87+ orders</p>
    </div>
  );
}

function Frame26() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame27 />
    </div>
  );
}

function Frame28() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 text-[14px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-black">⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29]">4.6</p>
    </div>
  );
}

function Cook2() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Cook 3">
      <Frame26 />
      <Frame28 />
    </div>
  );
}

function Frame30() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[14px]">Tasty Tiffins</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#b2b8bf] text-[12px]">76+ orders</p>
    </div>
  );
}

function Frame29() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame30 />
    </div>
  );
}

function Frame31() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 text-[14px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-black">⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29]">4.5</p>
    </div>
  );
}

function Cook3() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Cook 4">
      <Frame29 />
      <Frame31 />
    </div>
  );
}

function Frame33() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[14px]">HomeBites</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#b2b8bf] text-[12px]">66+ orders</p>
    </div>
  );
}

function Frame32() {
  return (
    <div className="bg-white content-stretch flex gap-[12px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <div className="relative shrink-0 size-[40px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 40 40">
          <circle cx="20" cy="20" fill="var(--fill-0, #D9DEE6)" id="Ellipse" r="20" />
        </svg>
      </div>
      <Frame33 />
    </div>
  );
}

function Frame34() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 text-[14px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-black">⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29]">4.4</p>
    </div>
  );
}

function Cook4() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Cook 5">
      <Frame32 />
      <Frame34 />
    </div>
  );
}

function TopPerformingCooks() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[16px] items-start overflow-clip p-[24px] relative rounded-[12px] shadow-[0px_1px_3px_0px_rgba(0,0,0,0.06)] shrink-0 w-[400px]" data-name="Top Performing Cooks">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[16px] whitespace-nowrap">Top Performing Cooks</p>
      <Cook />
      <div className="bg-[#f5f7fa] h-px relative shrink-0 w-[352px]" data-name="Rectangle" />
      <Cook1 />
      <div className="bg-[#f5f7fa] h-px relative shrink-0 w-[352px]" data-name="Rectangle" />
      <Cook2 />
      <div className="bg-[#f5f7fa] h-px relative shrink-0 w-[352px]" data-name="Rectangle" />
      <Cook3 />
      <div className="bg-[#f5f7fa] h-px relative shrink-0 w-[352px]" data-name="Rectangle" />
      <Cook4 />
    </div>
  );
}

function BottomSection() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip relative shrink-0" data-name="Bottom Section">
      <RecentOrders />
      <TopPerformingCooks />
    </div>
  );
}

function MainContent() {
  return (
    <div className="absolute bg-[#f2f2f5] content-stretch flex flex-col gap-[24px] h-[900px] items-start left-[240px] overflow-clip px-[32px] py-[24px] top-0 w-[1160px]" data-name="Main Content">
      <WelcomeSection />
      <Component4KpiCards />
      <ChartsRow />
      <BottomSection />
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
    <div className="bg-[#58c66c] content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shadow-[0px_8px_24px_0px_rgba(88,198,108,0.25)] shrink-0 w-[228px]" data-name="Dashboard Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">🏠</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[14px] text-white">Dashboard</p>
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

export default function Component1DashboardOverview() {
  return (
    <div className="bg-[#f2f2f5] relative size-full" data-name="1. Dashboard Overview">
      <MainContent />
      <PremiumSidebar />
    </div>
  );
}