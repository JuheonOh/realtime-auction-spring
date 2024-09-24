import { useState, useEffect } from "react";
import { Bell, Clock, DollarSign, Heart, Search, Zap } from "lucide-react";

// 실제 구현에서는 이 부분을 서버로부터 받아오는 로직으로 대체해야 합니다.
const mockAuctions = [
  { id: 1, name: "Vintage Watch", currentBid: 1500, timeLeft: 120, image: "/images/placeholder.svg", category: "Accessories", immediatePurchasePrice: 3000 },
  { id: 2, name: "Modern Art Painting", currentBid: 5000, timeLeft: 300, image: "/images/placeholder.svg", category: "Art", immediatePurchasePrice: 10000 },
  { id: 3, name: "Rare Coin Collection", currentBid: 2000, timeLeft: 600, image: "/images/placeholder.svg", category: "Collectibles" },
  { id: 4, name: "Antique Furniture", currentBid: 3500, timeLeft: 450, image: "/images/placeholder.svg", category: "Home & Garden", immediatePurchasePrice: 7000 },
  { id: 5, name: "Luxury Handbag", currentBid: 2500, timeLeft: 180, image: "/images/placeholder.svg", category: "Fashion" },
  { id: 6, name: "Classic Car", currentBid: 15000, timeLeft: 900, image: "/images/placeholder.svg", category: "Vehicles", immediatePurchasePrice: 30000 },
];

const categories = ["All", "Accessories", "Art", "Collectibles", "Fashion", "Home & Garden", "Vehicles"];

export default function AuctionPage() {
  const [auctions, setAuctions] = useState(mockAuctions);
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [sortBy, setSortBy] = useState("timeLeft");

  useEffect(() => {
    // 실시간 업데이트를 시뮬레이션합니다.
    const timer = setInterval(() => {
      setAuctions((prevAuctions) =>
        prevAuctions.map((auction) => ({
          ...auction,
          currentBid: auction.currentBid + Math.floor(Math.random() * 100),
          timeLeft: Math.max(0, auction.timeLeft - 1),
        }))
      );
    }, 1000);

    return () => clearInterval(timer);
  }, []);

  const filteredAndSortedAuctions = auctions
    .filter((auction) => auction.name.toLowerCase().includes(searchTerm.toLowerCase()) && (selectedCategory === "All" || auction.category === selectedCategory))
    .sort((a, b) => {
      if (sortBy === "timeLeft") return a.timeLeft - b.timeLeft;
      if (sortBy === "currentBid") return b.currentBid - a.currentBid;
      return 0;
    });

  return (
    <div className="container mx-auto px-4 py-8">
      <header className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">실시간 경매</h1>
        <div className="flex space-x-2">
          <button className="p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors" title="알림">
            <Bell className="w-5 h-5" />
          </button>
          <button className="p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors" title="관심 목록">
            <Heart className="w-5 h-5" />
          </button>
        </div>
      </header>

      <div className="flex flex-col md:flex-row gap-4 mb-8">
        <div className="flex-grow relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input type="text" className="w-full pl-10 pr-4 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="경매 상품 검색..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
        </div>
        <select className="px-4 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500" value={selectedCategory} onChange={(e) => setSelectedCategory(e.target.value)}>
          {categories.map((category) => (
            <option key={category} value={category}>
              {category}
            </option>
          ))}
        </select>
        <select className="px-4 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500" value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
          <option value="timeLeft">남은 시간순</option>
          <option value="currentBid">현재 입찰가순</option>
        </select>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {filteredAndSortedAuctions.map((auction) => (
          <div key={auction.id} className="bg-white rounded-lg shadow-md overflow-hidden">
            <img src={auction.image} alt={auction.name} className="w-full h-48 object-cover" />
            <div className="p-4">
              <h2 className="text-xl font-semibold mb-2">{auction.name}</h2>
              <span className="inline-block bg-gray-200 rounded-full px-3 py-1 text-sm font-semibold text-gray-700 mb-2">{auction.category}</span>
              <div className="flex justify-between items-center mb-2">
                <div className="flex items-center">
                  <DollarSign className="w-5 h-5 text-green-500 mr-1" />
                  <span className="font-bold">{auction.currentBid.toLocaleString()}원</span>
                </div>
                <div className="flex items-center">
                  <Clock className="w-5 h-5 text-red-500 mr-1" />
                  {auction.timeLeft > 0 ? (
                    <span>
                      {Math.floor(auction.timeLeft / 60)}:{(auction.timeLeft % 60).toString().padStart(2, "0")}
                    </span>
                  ) : (
                    <span className="text-red-500 font-bold">경매 종료</span>
                  )}
                </div>
              </div>
              {auction.immediatePurchasePrice && auction.timeLeft > 0 && (
                <div className="flex justify-end items-center mt-2">
                  <Zap className="w-5 h-5 text-yellow-500 mr-1" />
                  <span className="text-sm font-semibold">즉시 구매가: {auction.immediatePurchasePrice.toLocaleString()}원</span>
                </div>
              )}
            </div>
            <div className="px-4 py-3 bg-gray-50">
              {auction.timeLeft > 0 ? (
                <div className="flex gap-2">
                  <button className="flex-1 bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded transition-colors">입찰하기</button>
                  {auction.immediatePurchasePrice && (
                    <button className="flex-1 bg-gray-500 hover:bg-gray-600 text-white font-bold py-2 px-4 rounded transition-colors" title={`${auction.immediatePurchasePrice.toLocaleString()}원에 즉시 구매`}>
                      즉시 구매
                    </button>
                  )}
                </div>
              ) : (
                <button className="w-full bg-gray-300 text-gray-500 font-bold py-2 px-4 rounded cursor-not-allowed" disabled>
                  경매 종료
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
