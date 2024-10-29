import { Clock, Search, User } from "lucide-react";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Link } from "react-router-dom";
import { getAuctionList } from "../../apis/AuctionAPI";
import { getCategoryList } from "../../apis/CommonAPI";
import { SET_AUCTION_LIST, SET_CATEGORY_LIST } from "../../data/redux/store/Auction";
import useInterval from "../../hooks/useInterval";
import { addCommas } from "../../utils/formatNumber";
import formatTime from "../../utils/formatTime";

export default function AuctionListPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [sortBy, setSortBy] = useState("newAuction");

  const dispatch = useDispatch();
  const categoryList = useSelector((state) => state.auction.categoryList);
  const auctionList = useSelector((state) => state.auction.auctionList);

  useEffect(() => {
    const fetchCategoryList = async () => {
      const response = await getCategoryList();
      dispatch(SET_CATEGORY_LIST(response.data));
    };

    const fetchAuctionList = async () => {
      const response = await getAuctionList();
      dispatch(SET_AUCTION_LIST(response.data));
    };

    fetchCategoryList();
    fetchAuctionList();
  }, [dispatch]);

  useInterval(() => {
    dispatch(SET_AUCTION_LIST(auctionList.map((auction) => ({ ...auction, auctionLeftTime: auction.auctionLeftTime - 1 }))));
  }, 1000);

  const filteredAndSortedAuctions = auctionList
    .filter((auction) => auction.title.toLowerCase().includes(searchTerm.toLowerCase()) && (selectedCategory === "All" || auction.categoryName === selectedCategory))
    .sort((a, b) => {
      if (sortBy === "newAuction") return new Date(b.auctionStartTime) - new Date(a.auctionStartTime);
      if (sortBy === "highPrice") return b.currentPrice - a.currentPrice;
      if (sortBy === "lowPrice") return a.currentPrice - b.currentPrice;
      if (sortBy === "fastEnd") return new Date(a.auctionEndTime) - new Date(b.auctionEndTime);
      if (sortBy === "slowEnd") return new Date(b.auctionEndTime) - new Date(a.auctionEndTime);
      if (sortBy === "highBidsCount") return b.bidsCount - a.bidsCount;
      if (sortBy === "lowBidsCount") return a.bidsCount - b.bidsCount;
      return 0;
    });

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="mb-8 text-3xl font-bold">실시간 경매</h1>
      <div className="flex flex-col md:flex-row gap-4 mb-8">
        <div className="flex-grow relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
          <input type="text" className="w-full pl-10 pr-4 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="경매 상품 검색..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
        </div>
        <select className="px-4 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500" value={selectedCategory} onChange={(e) => setSelectedCategory(e.target.value)}>
          <option value="All">전체</option>
          {categoryList.map((category) => (
            <option key={category.id} value={category.name}>
              {category.name}
            </option>
          ))}
        </select>
        <select className="px-4 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500" value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
          <option value="newAuction">신상품순</option>
          <option value="highPrice">가격 높은순</option>
          <option value="lowPrice">가격 낮은순</option>
          <option value="fastEnd">종료 빠른순</option>
          <option value="slowEnd">종료 느린순</option>
          <option value="highBidsCount">입찰 많은순</option>
          <option value="lowBidsCount">입찰 적은순</option>
        </select>
      </div>

      <div className="grid grid-cols-4 gap-6">
        {filteredAndSortedAuctions.map((auction, index) => (
          <div key={index} className="flex flex-col justify-between h-full bg-white rounded-lg shadow-md overflow-hidden transition-all duration-300 transform hover:shadow-lg hover:-translate-y-1 hover:scale-105">
            {/* 상품 이미지 */}
            <Link to={`/auctions/${auction.id}`}>
              <div className="relative w-full h-48">
                <img src={`http://localhost:8080/auction/images/${auction.image}`} alt={auction.title} className="w-full h-48 object-cover" onError={(e) => (e.target.src = `http://localhost:8080/auction/images/placeholder.svg`)} loading="lazy" />
                <div className="absolute inset-0 flex items-center justify-center bg-black bg-opacity-0 group hover:bg-opacity-30 transition-opacity duration-300">
                  <span className="text-white text-xl font-bold opacity-0 group-hover:opacity-100 transition-opacity duration-300">자세히 보기</span>
                </div>
              </div>
            </Link>

            {/* 상품 정보 */}
            <div className="p-4 flex-grow flex flex-col gap-y-4">
              {/* 상품 제목 */}
              <h2 className="h-16 text-xl font-semibold line-clamp-2 text-ellipsis break-keep">
                <Link to={`/auctions/${auction.id}`}>{auction.title}</Link>
              </h2>

              {/* 상품 카테고리 및 판매자 정보 */}
              <div className="flex items-center justify-between">
                <span className="inline-block bg-gray-200 rounded-full px-3 py-1 text-sm font-semibold text-gray-700">{auction.categoryName}</span>
                <Link to={`/users/${auction.userId}`} className="flex items-center">
                  <User className="w-5 h-5 text-gray-500 mr-1" />
                  <span className="text-sm">{auction.nickname}</span>
                </Link>
              </div>

              {/* 상품 가격 및 즉시 구매가 */}
              <div className="flex flex-col gap-y-1 mt-auto">
                {auction.buyNowPrice > 0 && (
                  <div className="flex items-center justify-end gap-x-2">
                    <span className="font-semibold text-red-500">즉시 구매가</span>
                    <span className="text-lg font-bold">{addCommas(auction.buyNowPrice)}원</span>
                  </div>
                )}
                <div className="flex items-center justify-end gap-x-2">
                  <span className="font-semibold text-blue-500">현재 입찰가</span>
                  <span className="text-lg font-bold">{addCommas(auction.currentPrice)}원</span>
                </div>
              </div>
            </div>

            {/* 상품 남은 시간 */}
            <div className="px-4 py-3 bg-gray-100">
              <div className="flex gap-2 items-center justify-center">
                <Clock className="w-5 h-5 text-red-500" />
                {auction.auctionLeftTime > 0 ? <span>{formatTime(auction.auctionLeftTime, 2)}</span> : <span className="text-red-500 font-bold">경매 종료</span>}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
