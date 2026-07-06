function Frame() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex flex-col gap-[4px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[28px]">{`Reviews & Ratings`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[14px]">Customer feedback and ratings overview.</p>
    </div>
  );
}

function Header() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-full" data-name="Header">
      <Frame />
    </div>
  );
}

function StatAverageRating() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[12px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[260px]" data-name="Stat - Average Rating">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[24px] text-black">⭐</p>
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">4.6</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[14px]">Average Rating</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">out of 5.0</p>
    </div>
  );
}

function StatTotalReviews() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[12px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[260px]" data-name="Stat - Total Reviews">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[24px] text-black">📝</p>
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">1,234</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[14px]">Total Reviews</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">+18% this month</p>
    </div>
  );
}

function Stat5StarReviews() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[12px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[260px]" data-name="Stat - 5 Star Reviews">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[24px] text-black">⭐</p>
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">789</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[14px]">5 Star Reviews</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">64% of total</p>
    </div>
  );
}

function StatPendingReviews() {
  return (
    <div className="bg-white content-stretch flex flex-col gap-[12px] items-start overflow-clip p-[20px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[260px]" data-name="Stat - Pending Reviews">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[24px] text-black">⏳</p>
      <p className="font-['Inter:Bold',sans-serif] font-bold relative shrink-0 text-[#1c1f29] text-[32px]">23</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[14px]">Pending Reviews</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[12px]">Need response</p>
    </div>
  );
}

function RatingStatistics() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip relative shrink-0 whitespace-nowrap" data-name="Rating Statistics">
      <StatAverageRating />
      <StatTotalReviews />
      <Stat5StarReviews />
      <StatPendingReviews />
    </div>
  );
}

function TableHeaders() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex font-['Inter:Semi_Bold',sans-serif] font-semibold gap-[16px] items-start leading-[normal] not-italic overflow-clip pb-[16px] relative shrink-0 text-[#9499a6] text-[12px] w-full" data-name="Table Headers">
      <p className="relative shrink-0 w-[180px]">Customer</p>
      <p className="relative shrink-0 w-[160px]">Cook</p>
      <p className="relative shrink-0 w-[140px]">Rating</p>
      <p className="relative shrink-0 w-[340px]">Comment</p>
      <p className="relative shrink-0 w-[120px]">Date</p>
      <p className="relative shrink-0 w-[80px]">Actions</p>
    </div>
  );
}

function Frame1() {
  return (
    <div className="bg-white content-stretch flex gap-[4px] h-[20px] items-start overflow-clip relative shrink-0 w-[140px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[14px] text-black">⭐⭐⭐⭐⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[13px]">5</p>
    </div>
  );
}

function ReviewRow() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip py-[16px] relative shrink-0 w-full" data-name="Review Row 1">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[13px] w-[180px]">Maria Rosser</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">{`Anita's Kitchen`}</p>
      <Frame1 />
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[340px]">{`"Excellent food quality! Very fresh ingredients."`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 18, 2025</p>
      <p className="font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame2() {
  return (
    <div className="bg-white content-stretch flex gap-[4px] h-[20px] items-start overflow-clip relative shrink-0 w-[140px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[14px] text-black">⭐⭐⭐⭐⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[13px]">5</p>
    </div>
  );
}

function ReviewRow1() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip py-[16px] relative shrink-0 w-full" data-name="Review Row 2">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[13px] w-[180px]">Rayna Carder</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Mumbai Spice</p>
      <Frame2 />
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[340px]">{`"Delicious authentic taste. Will order again!"`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 17, 2025</p>
      <p className="font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame3() {
  return (
    <div className="bg-white content-stretch flex gap-[4px] h-[20px] items-start overflow-clip relative shrink-0 w-[140px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[14px] text-black">⭐⭐⭐⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[13px]">4</p>
    </div>
  );
}

function ReviewRow2() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip py-[16px] relative shrink-0 w-full" data-name="Review Row 3">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[13px] w-[180px]">Talan Press</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">South Indian</p>
      <Frame3 />
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[340px]">{`"Good taste. Timely delivery. Slightly spicy."`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 17, 2025</p>
      <p className="font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame4() {
  return (
    <div className="bg-white content-stretch flex gap-[4px] h-[20px] items-start overflow-clip relative shrink-0 w-[140px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[14px] text-black">⭐⭐⭐⭐⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[13px]">5</p>
    </div>
  );
}

function ReviewRow3() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip py-[16px] relative shrink-0 w-full" data-name="Review Row 4">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[13px] w-[180px]">Marley Dokidis</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Delhi Delights</p>
      <Frame4 />
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[340px]">{`"Amazing flavors! Best chole bhature ever."`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 16, 2025</p>
      <p className="font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame5() {
  return (
    <div className="bg-white content-stretch flex gap-[4px] h-[20px] items-start overflow-clip relative shrink-0 w-[140px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[14px] text-black">⭐⭐⭐⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[13px]">4</p>
    </div>
  );
}

function ReviewRow4() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip py-[16px] relative shrink-0 w-full" data-name="Review Row 5">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[13px] w-[180px]">Marcus Rosser</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Punjab Kitchen</p>
      <Frame5 />
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[340px]">{`"Great food but portion size could be better."`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 16, 2025</p>
      <p className="font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame6() {
  return (
    <div className="bg-white content-stretch flex gap-[4px] h-[20px] items-start overflow-clip relative shrink-0 w-[140px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[14px] text-black">⭐⭐⭐⭐⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[13px]">5</p>
    </div>
  );
}

function ReviewRow5() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip py-[16px] relative shrink-0 w-full" data-name="Review Row 6">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[13px] w-[180px]">Zaire Bergson</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Bengal Bites</p>
      <Frame6 />
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[340px]">{`"Authentic Bengali cuisine. Highly recommend!"`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 15, 2025</p>
      <p className="font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame7() {
  return (
    <div className="bg-white content-stretch flex gap-[4px] h-[20px] items-start overflow-clip relative shrink-0 w-[140px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[14px] text-black">⭐⭐⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[13px]">3</p>
    </div>
  );
}

function ReviewRow6() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip py-[16px] relative shrink-0 w-full" data-name="Review Row 7">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[13px] w-[180px]">Lincoln Siphron</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">Coastal Treats</p>
      <Frame7 />
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[340px]">{`"Average taste. Expected better quality."`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 15, 2025</p>
      <p className="font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function Frame8() {
  return (
    <div className="bg-white content-stretch flex gap-[4px] h-[20px] items-start overflow-clip relative shrink-0 w-[140px] whitespace-nowrap" data-name="Frame">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[14px] text-black">⭐⭐⭐⭐⭐</p>
      <p className="font-['Inter:Semi_Bold',sans-serif] font-semibold relative shrink-0 text-[#1c1f29] text-[13px]">5</p>
    </div>
  );
}

function ReviewRow7() {
  return (
    <div className="[word-break:break-word] bg-white content-stretch flex gap-[16px] items-start leading-[normal] not-italic overflow-clip py-[16px] relative shrink-0 w-full" data-name="Review Row 8">
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#1c1f29] text-[13px] w-[180px]">Talan Dokidis</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[160px]">{`Anita's Kitchen`}</p>
      <Frame8 />
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#1c1f29] text-[13px] w-[340px]">{`"Perfect dal makhani! Creamy and delicious."`}</p>
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[13px] w-[120px]">May 14, 2025</p>
      <p className="font-['Inter:Regular','Noto_Sans_Math:Regular',sans-serif] font-normal relative shrink-0 text-[#9499a6] text-[20px] text-center w-[80px]">⋮</p>
    </div>
  );
}

function ReviewsTable() {
  return (
    <div className="bg-white content-stretch flex flex-col items-start overflow-clip p-[24px] relative rounded-[12px] shadow-[0px_2px_8px_0px_rgba(0,0,0,0.08)] shrink-0 w-[1096px]" data-name="Reviews Table">
      <TableHeaders />
      <div className="bg-[#e5e8ed] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <ReviewRow />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <ReviewRow1 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <ReviewRow2 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <ReviewRow3 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <ReviewRow4 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <ReviewRow5 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <ReviewRow6 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <ReviewRow7 />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
      <div className="bg-[#f2f5f7] h-px relative shrink-0 w-[1048px]" data-name="Rectangle" />
    </div>
  );
}

function Frame10() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#b2b8bf] text-[16px] whitespace-nowrap">←</p>
    </div>
  );
}

function Frame11() {
  return (
    <div className="bg-[#57b869] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[13px] text-white whitespace-nowrap">1</p>
    </div>
  );
}

function Frame12() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">2</p>
    </div>
  );
}

function Frame13() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Medium',sans-serif] font-medium leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">3</p>
    </div>
  );
}

function Frame14() {
  return (
    <div className="bg-[#f2f5f7] content-stretch flex items-center justify-center overflow-clip relative rounded-[6px] shrink-0 size-[36px]" data-name="Frame">
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[16px] whitespace-nowrap">→</p>
    </div>
  );
}

function Frame9() {
  return (
    <div className="bg-white content-stretch flex gap-[8px] items-start overflow-clip relative shrink-0" data-name="Frame">
      <Frame10 />
      <Frame11 />
      <Frame12 />
      <Frame13 />
      <Frame14 />
    </div>
  );
}

function Pagination() {
  return (
    <div className="bg-white content-stretch flex items-start justify-between overflow-clip relative shrink-0 w-[1096px]" data-name="Pagination">
      <p className="[word-break:break-word] font-['Inter:Regular',sans-serif] font-normal leading-[normal] not-italic relative shrink-0 text-[#9499a6] text-[13px] whitespace-nowrap">Showing 1 to 10 of 1,234 results</p>
      <Frame9 />
    </div>
  );
}

function MainContent() {
  return (
    <div className="absolute bg-[#f2f2f5] content-stretch flex flex-col gap-[24px] h-[900px] items-start left-[240px] overflow-clip px-[32px] py-[24px] top-0 w-[1160px]" data-name="Main Content">
      <Header />
      <RatingStatistics />
      <ReviewsTable />
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
    <div className="content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shrink-0 w-[228px]" data-name="Orders Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">📦</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[#d1d5db] text-[14px]">Orders</p>
    </div>
  );
}

function ReviewsItem() {
  return (
    <div className="bg-[#58c66c] content-stretch flex gap-[12px] h-[44px] items-center overflow-clip px-[14px] relative rounded-[10px] shadow-[0px_8px_24px_0px_rgba(88,198,108,0.25)] shrink-0 w-[228px]" data-name="Reviews Item">
      <p className="font-['Inter:Regular',sans-serif] font-normal relative shrink-0 text-[18px] text-black">⭐</p>
      <p className="font-['Inter:Medium',sans-serif] font-medium relative shrink-0 text-[14px] text-white">Reviews</p>
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

export default function Component6ReviewsRatings() {
  return (
    <div className="bg-[#f2f2f5] relative size-full" data-name="6. Reviews & Ratings">
      <MainContent />
      <PremiumSidebar />
    </div>
  );
}