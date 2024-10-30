import { Clock, User } from "lucide-react";
import { Link } from "react-router-dom";
import { addCommas } from "../utils/formatNumber";
import formatTime from "../utils/formatTime";
import { IMAGE_URL } from "../utils/constant";
export default function AuctionCard({ auction }) {
  return (
    <div className="flex flex-col justify-between h-full bg-white rounded-lg shadow-md overflow-hidden transition-all duration-300 transform hover:shadow-lg hover:-translate-y-1 hover:scale-105">
      {/* 상품 이미지 */}
      <Link to={`/auctions/${auction.id}`}>
        <div className="relative w-full h-48">
          <img src={`${IMAGE_URL}/${auction.image}`} alt={auction.title} className="w-full h-full object-cover" onError={(e) => (e.target.src = `${IMAGE_URL}/placeholder.svg`)} loading="lazy" />
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
  );
}
