import { Clock, DollarSign } from "lucide-react";

export default function HomePage() {
  // 실제 구현에서는 이 데이터를 서버에서 가져와야 합니다.
  const featuredAuctions = [
    { id: 1, name: "빈티지 롤렉스 시계", currentBid: 15000000, timeLeft: 7200, image: "images/placeholder.svg?height=300&width=400", category: "액세서리" },
    { id: 2, name: "현대 미술 걸작", currentBid: 50000000, timeLeft: 10800, image: "images/placeholder.svg?height=300&width=400", category: "예술" },
    { id: 3, name: "희귀 초판 도서", currentBid: 5000000, timeLeft: 3600, image: "images/placeholder.svg?height=300&width=400", category: "수집품" },
    { id: 4, name: "디올 가방", currentBid: 3000000, timeLeft: 14400, image: "images/placeholder.svg?height=300&width=400", category: "패션" },
  ];

  const categories = ["액세서리", "예술", "수집품", "패션", "홈 & 가든", "쥬얼리", "테크놀로지", "자동차"];

  return (
    <main className="container mx-auto px-4 py-8">
      <section className="mb-12">
        <h2 className="text-2xl font-semibold mb-6">주목할 만한 경매</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {featuredAuctions.map((auction) => (
            <div key={auction.id} className="bg-white rounded-lg shadow-md overflow-hidden">
              <img src={auction.image} alt={auction.name} className="w-full h-48 object-cover" />
              <div className="p-4">
                <h3 className="text-lg font-semibold mb-2">{auction.name}</h3>
                <div className="flex justify-between items-center mb-2">
                  <div className="flex items-center">
                    <DollarSign className="w-5 h-5 text-green-500 mr-1" />
                    <span className="font-bold">{auction.currentBid.toLocaleString()}원</span>
                  </div>
                  <div className="flex items-center">
                    <Clock className="w-5 h-5 text-red-500 mr-1" />
                    <span>
                      {Math.floor(auction.timeLeft / 3600)}시간 {Math.floor((auction.timeLeft % 3600) / 60)}분
                    </span>
                  </div>
                </div>
                <button className="w-full bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded transition-colors">입찰하기</button>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="mb-12">
        <h2 className="text-2xl font-semibold mb-6">카테고리</h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
          {categories.map((category) => (
            <button key={category} className="bg-white hover:bg-gray-50 border border-gray-200 rounded-lg p-4 text-center transition-colors">
              {category}
            </button>
          ))}
        </div>
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
