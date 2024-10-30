import { Search } from "lucide-react";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { getAuctionList } from "../../apis/AuctionAPI";
import { getCategoryList } from "../../apis/CommonAPI";
import AuctionCard from "../../components/AuctionCard";
import LoadingSpinner from "../../components/LoadingSpinner";
import { SET_AUCTION_LIST, SET_CATEGORY_LIST } from "../../data/redux/store/Auction";
import useInterval from "../../hooks/useInterval";

export default function AuctionListPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [sortBy, setSortBy] = useState("newAuction");

  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const dispatch = useDispatch();
  const categoryList = useSelector((state) => state.auction.categoryList);
  const auctionList = useSelector((state) => state.auction.auctionList);

  useEffect(() => {
    const fetchCategoryList = async () => {
      try {
        const response = await getCategoryList();
        dispatch(SET_CATEGORY_LIST(response.data));
      } catch (error) {
        setIsError(true);
      } finally {
        setIsLoading(false);
      }
    };

    const fetchAuctionList = async () => {
      try {
        const response = await getAuctionList();
        dispatch(SET_AUCTION_LIST(response.data));
      } catch (error) {
        setIsError(true);
      } finally {
        setIsLoading(false);
      }
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

      {isLoading || isError ? (
        <LoadingSpinner isLoading={isLoading} isError={isError} message="경매 목록" />
      ) : (
        <div className="grid grid-cols-4 gap-6">
          {filteredAndSortedAuctions.map((auction) => (
            <AuctionCard key={auction.id} auction={auction} />
          ))}
        </div>
      )}
    </div>
  );
}
