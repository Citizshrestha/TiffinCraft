function Frame() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[28px]">{`Reports & Analytics`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[14px]">View detailed analytics and performance reports.</p>
    </div>
  );
}

function Frame1() {
  return (
    <div className="bg-[#57b869] content-stretch flex items-start overflow-clip px-[20px] py-[12px] relative rounded-[8px] shrink-0" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[14px] text-white whitespace-nowrap">📊 Export Report</p>
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
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[24px] text-black">📦</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#7887fa] text-[12px]">+12.3%</p>
    </div>
  );
}

function MetricTotalOrders() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[8px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[260px]" data-name="Metric - Total Orders">
      <Frame2 />
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">6,709</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px]">Total Orders</p>
    </div>
  );
}

function Frame3() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[24px] text-black">💰</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#57b869] text-[12px]">+18.5%</p>
    </div>
  );
}

function MetricTotalRevenue() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[8px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[260px]" data-name="Metric - Total Revenue">
      <Frame3 />
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">₹1.45L</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px]">Total Revenue</p>
    </div>
  );
}

function Frame4() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[24px] text-black">👥</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#f28c40] text-[12px]">+8.2%</p>
    </div>
  );
}

function MetricActiveUsers() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[8px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[260px]" data-name="Metric - Active Users">
      <Frame4 />
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">334</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px]">Active Users</p>
    </div>
  );
}

function Frame5() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[24px] text-black">💳</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#f2c740] text-[12px]">+5.1%</p>
    </div>
  );
}

function MetricAvgOrderValue() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[8px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[260px]" data-name="Metric - Avg Order Value">
      <Frame5 />
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">₹456</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px]">Avg Order Value</p>
    </div>
  );
}

function KeyMetrics() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Key Metrics">
      <MetricTotalOrders />
      <MetricTotalRevenue />
      <MetricActiveUsers />
      <MetricAvgOrderValue />
    </div>
  );
}

function PieChart() {
  return (
    <div className="h-[200px] relative shrink-0 w-[480px]" data-name="Pie Chart">
      <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 480 200">
        <g id="Pie Chart">
          <circle cx="100" cy="100" fill="var(--fill-0, #7887FA)" fillOpacity="0.7" id="Ellipse" r="90" />
          <circle cx="100" cy="100" fill="var(--fill-0, #57B869)" fillOpacity="0.7" id="Ellipse_2" r="75" />
          <circle cx="100" cy="100" fill="var(--fill-0, #F28C40)" fillOpacity="0.7" id="Ellipse_3" r="60" />
          <circle cx="100" cy="100" fill="var(--fill-0, #F2C740)" fillOpacity="0.7" id="Ellipse_4" r="45" />
        </g>
      </svg>
    </div>
  );
}

function Frame7() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <div className="relative shrink-0 size-[12px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 12 12">
          <circle cx="6" cy="6" fill="var(--fill-0, #7887FA)" id="Ellipse" r="6" />
        </svg>
      </div>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] whitespace-nowrap">Delivered</p>
    </div>
  );
}

function Frame6() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Frame">
      <Frame7 />
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">4,287 (64%)</p>
    </div>
  );
}

function Frame9() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <div className="relative shrink-0 size-[12px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 12 12">
          <circle cx="6" cy="6" fill="var(--fill-0, #57B869)" id="Ellipse" r="6" />
        </svg>
      </div>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] whitespace-nowrap">Processing</p>
    </div>
  );
}

function Frame8() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Frame">
      <Frame9 />
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">1,342 (20%)</p>
    </div>
  );
}

function Frame11() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <div className="relative shrink-0 size-[12px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 12 12">
          <circle cx="6" cy="6" fill="var(--fill-0, #F28C40)" id="Ellipse" r="6" />
        </svg>
      </div>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] whitespace-nowrap">Pending</p>
    </div>
  );
}

function Frame10() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Frame">
      <Frame11 />
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">804 (12%)</p>
    </div>
  );
}

function Frame13() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <div className="relative shrink-0 size-[12px]" data-name="Ellipse">
        <svg className="absolute block inset-0 size-full" fill="none" preserveAspectRatio="none" viewBox="0 0 12 12">
          <circle cx="6" cy="6" fill="var(--fill-0, #F2C740)" id="Ellipse" r="6" />
        </svg>
      </div>
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[13px] whitespace-nowrap">Cancelled</p>
    </div>
  );
}

function Frame12() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Frame">
      <Frame13 />
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">276 (4%)</p>
    </div>
  );
}

function Legend() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[12px] items-start overflow-clip relative shrink-0 w-[164px]" data-name="Legend">
      <Frame6 />
      <Frame8 />
      <Frame10 />
      <Frame12 />
    </div>
  );
}

function OrdersBreakdown() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[16px] items-start overflow-clip p-[24px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[530px]" data-name="Orders Breakdown">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[18px] whitespace-nowrap">Orders Breakdown</p>
      <PieChart />
      <Legend />
    </div>
  );
}

function BarChart() {
  return (
    <div className="bg-[#f7f7fa] h-[200px] overflow-clip relative rounded-[8px] shrink-0 w-[502px]" data-name="Bar Chart">
      <div className="absolute bg-[#7887fa] h-[120px] left-[20px] rounded-[4px] top-[70px] w-[50px]" data-name="Bar 1" />
      <div className="absolute bg-[#7887fa] h-[140px] left-[88px] rounded-[4px] top-[50px] w-[50px]" data-name="Bar 2" />
      <div className="absolute bg-[#7887fa] h-[95px] left-[156px] rounded-[4px] top-[95px] w-[50px]" data-name="Bar 3" />
      <div className="absolute bg-[#7887fa] h-[160px] left-[224px] rounded-[4px] top-[30px] w-[50px]" data-name="Bar 4" />
      <div className="absolute bg-[#7887fa] h-[110px] left-[292px] rounded-[4px] top-[80px] w-[50px]" data-name="Bar 5" />
      <div className="absolute bg-[#7887fa] h-[150px] left-[360px] rounded-[4px] top-[40px] w-[50px]" data-name="Bar 6" />
      <div className="absolute bg-[#7887fa] h-[130px] left-[428px] rounded-[4px] top-[60px] w-[50px]" data-name="Bar 7" />
    </div>
  );
}

function XLabels() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex font-['Inter:Regular',sans-serif] font-normal items-start justify-between leading-[normal] not-italic overflow-clip relative shrink-0 text-[#9499a6] text-[12px] w-full whitespace-nowrap" data-name="X Labels">
      <p className="relative shrink-0">Mon</p>
      <p className="relative shrink-0">Tue</p>
      <p className="relative shrink-0">Wed</p>
      <p className="relative shrink-0">Thu</p>
      <p className="relative shrink-0">Fri</p>
      <p className="relative shrink-0">Sat</p>
      <p className="relative shrink-0">Sun</p>
    </div>
  );
}

function RevenueByDay() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[16px] items-start overflow-clip p-[24px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[550px]" data-name="Revenue by Day">
      <p className="[word-break:break-word] font-['Inter:Semi_Bold',sans-serif] font-semibold leading-[normal] not-italic relative shrink-0 text-[#1c1f29] text-[18px] whitespace-nowrap">Revenue by Day</p>
      <BarChart />
      <XLabels />
    </div>
  );
}

function ChartsSection() {
  return (
    <div className="bg-white content-stretch flex gap-[16px] items-start overflow-clip relative shrink-0" data-name="Charts Section">
      <OrdersBreakdown />
      <RevenueByDay />
    </div>
  );
}

function MainContent() {
  return (
    <div className="absolute bg-[#f2f2f5] content-stretch flex flex-col gap-[24px] h-[714px] items-start left-[240px] overflow-clip px-[32px] py-[24px] top-0 w-[1160px]" data-name="Main Content">
      <Header />
      <KeyMetrics />
      <ChartsSection />
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
    <div className="bg-[#58c66c] content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shadow-[0px_8px_24px_0px_rgba(88,198,108,0.25)] shrink-0 w-[228px]" data-name="Reports Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">📊</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[14px] text-white">Reports</p>
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

export default function Component7ReportsAnalytics() {
  return (
    <div className="bg-[#f2f2f5] relative size-full" data-name="7. Reports & Analytics">
      <MainContent />
      <PremiumSidebar />
    </div>
  );
}