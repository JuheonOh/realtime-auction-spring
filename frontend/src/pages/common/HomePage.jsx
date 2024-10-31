import { getFeaturedAuctions } from "@apis/AuctionAPI";
import { getCategoryList } from "@apis/CommonAPI";
import LoadingSpinner from "@components/common/loading/LoadingSpinner";
import AuctionCard from "@components/features/auction/AuctionCard";
import { SET_CATEGORY_LIST, SET_FEATURED_AUCTION_LIST } from "@data/redux/store/Auction";
import { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";

export default function HomePage() {
  const dispatch = useDispatch();
  const categoryList = useSelector((state) => state.auction.categoryList);
  const featuredAuctionList = useSelector((state) => state.auction.featuredAuctionList);

  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

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

    const fetchFeaturedAuctionList = async () => {
      try {
        const response = await getFeaturedAuctions();
        dispatch(SET_FEATURED_AUCTION_LIST(response.data));
        setIsLoading(false);
      } catch (error) {
        setIsError(true);
      } finally {
        setIsLoading(false);
      }
    };

    fetchCategoryList();
    fetchFeaturedAuctionList();
  }, [dispatch]);

  return (
    <main className="container mx-auto px-4 py-8">
      <section className="mb-12">
        <h2 className="text-2xl font-semibold mb-6">주목할 만한 경매</h2>
        <LoadingSpinner isLoading={isLoading} isError={isError} message="주목할 만한 경매" />
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {featuredAuctionList.map((auction) => (
            <AuctionCard key={auction.id} auction={auction} />
          ))}
        </div>
      </section>

      <section className="mb-12">
        <h2 className="text-2xl font-semibold mb-6">카테고리</h2>
        {isLoading || isError ? (
          <LoadingSpinner isLoading={isLoading} isError={isError} message="카테고리" />
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            {categoryList.map((category) => (
              <button key={category.id} className="bg-white hover:bg-gray-50 border border-gray-200 rounded-lg p-4 text-center transition-colors">
                {category.name}
              </button>
            ))}
          </div>
        )}
      </section>

      <section>
        <h2 className="text-2xl font-semibold mb-6">이용 방법</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="text-3xl font-bold text-blue-500 mb-4">1</div>
            <h3 className="text-lg font-semibold mb-2">회원가입</h3>
            <p className="text-gray-600">계정을 만들어 흥미로운 물품에 입찰을 시작하세요.</p>
          </div>
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="text-3xl font-bold text-blue-500 mb-4">2</div>
            <h3 className="text-lg font-semibold mb-2">입찰</h3>
            <p className="text-gray-600">구매하고 싶은 물품에 입찰해보세요.</p>
          </div>
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="text-3xl font-bold text-blue-500 mb-4">3</div>
            <h3 className="text-lg font-semibold mb-2">낙찰</h3>
            <p className="text-gray-600">경매 종료 시 최고 입찰자가 되면 낙찰됩니다!</p>
          </div>
        </div>
      </section>
    </main>
  );
}
