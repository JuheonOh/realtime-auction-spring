import { useState, useEffect } from 'react'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import { Clock, DollarSign, User, Eye, Heart, Share2 } from 'lucide-react'

// 실제 구현에서는 이 데이터를 서버에서 가져와야 합니다.
const initialAuctionData = {
  id: 1,
  name: "빈티지 롤렉스 시계",
  description: "1960년대 롤렉스 데이토나 모델. 희소성 높은 빈티지 시계입니다.",
  currentBid: 15000000,
  startingPrice: 10000000,
  timeLeft: 7200, // 2시간
  bidCount: 23,
  watchCount: 156,
  seller: "시계수집가A",
  images: [
    "/images/placeholder.svg?height=400&width=600",
    "/images/placeholder.svg?height=400&width=600",
    "/images/placeholder.svg?height=400&width=600"
  ]
}

const generateChartData = (startPrice, currentPrice, dataPoints = 20) => {
  const data = []
  const priceStep = Math.floor((currentPrice - startPrice) / ((dataPoints - 1) * 100000)) * 100000
  const now = new Date()
  
  for (let i = 0; i < dataPoints; i++) {
    const time = new Date(now.getTime() - (dataPoints - i - 1) * 5 * 60000) // 5분 간격
    data.push({
      시간: time.toLocaleTimeString(),
      가격: startPrice + priceStep * i
    })
  }
  
  return data
}

export default function AuctionDetail() {
  const [auctionData, setAuctionData] = useState(initialAuctionData)
  const [chartData, setChartData] = useState(generateChartData(auctionData.startingPrice, auctionData.currentBid))
  const [selectedImage, setSelectedImage] = useState(0)
  const [bidAmount, setBidAmount] = useState(auctionData.currentBid + 100000) // 10만원 단위로 입찰

  useEffect(() => {
    const timer = setInterval(() => {
      setAuctionData(prevData => ({
        ...prevData,
        timeLeft: prevData.timeLeft > 0 ? prevData.timeLeft - 1 : 0
      }))
    }, 1000)

    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    const dataUpdateInterval = setInterval(() => {
      const newPrice = auctionData.currentBid + Math.floor(Math.random() * 5 + 1) * 100000
      setAuctionData(prevData => ({
        ...prevData,
        currentBid: newPrice,
        bidCount: prevData.bidCount + 1
      }))
      setChartData(prevData => {
        const newData = [...prevData.slice(1), { time: new Date().toLocaleTimeString(), price: newPrice }]
        return newData
      })
    }, 5000) // 5초마다 새로운 입찰 시뮬레이션

    return () => clearInterval(dataUpdateInterval)
  }, [auctionData.currentBid])

  const formatTime = (seconds) => {
    const hours = Math.floor(seconds / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    const remainingSeconds = seconds % 60
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  }

  const handleBid = () => {
    if (bidAmount > auctionData.currentBid) {
      setAuctionData(prevData => ({
        ...prevData,
        currentBid: bidAmount,
        bidCount: prevData.bidCount + 1
      }))
      setChartData(prevData => {
        const newData = [...prevData.slice(1), { time: new Date().toLocaleTimeString(), price: bidAmount }]
        return newData
      })
      setBidAmount(bidAmount + 100000)
    } else {
      alert('입찰가는 현재 입찰가보다 높아야 합니다.')
    }
  }

  return (
    <div className="bg-gray-100 min-h-screen py-8">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8"> {/* 최대 너비를 1200px (max-w-6xl)로 설정 */}
        <div className="bg-white rounded-lg shadow-lg overflow-hidden">
          <div className="p-6 sm:p-10">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <div>
                <div className="mb-4">
                  <img src={auctionData.images[selectedImage]} alt={auctionData.name} className="w-full h-96 object-cover rounded-lg" />
                </div>
                <div className="flex space-x-2 overflow-x-auto">
                  {auctionData.images.map((image, index) => (
                    <img
                      key={index}
                      src={image}
                      alt={`${auctionData.name} - ${index + 1}`}
                      className={`w-24 h-24 object-cover rounded-md cursor-pointer ${selectedImage === index ? 'border-2 border-blue-500' : ''}`}
                      onClick={() => setSelectedImage(index)}
                    />
                  ))}
                </div>
              </div>
              <div>
                <h1 className="text-3xl font-bold mb-4">{auctionData.name}</h1>
                <p className="text-gray-600 mb-4">{auctionData.description}</p>
                <div className="flex items-center space-x-4 mb-4">
                  <div className="flex items-center">
                    <DollarSign className="w-5 h-5 text-green-500 mr-1" />
                    <span className="font-bold text-2xl">{auctionData.currentBid.toLocaleString()}원</span>
                  </div>
                  <div className="flex items-center">
                    <Clock className="w-5 h-5 text-red-500 mr-1" />
                    <span className="font-bold">{formatTime(auctionData.timeLeft)}</span>
                  </div>
                </div>
                <div className="flex items-center space-x-4 mb-4">
                  <div className="flex items-center">
                    <User className="w-5 h-5 text-gray-500 mr-1" />
                    <span>{auctionData.bidCount} 입찰</span>
                  </div>
                  <div className="flex items-center">
                    <Eye className="w-5 h-5 text-gray-500 mr-1" />
                    <span>{auctionData.watchCount} 관심</span>
                  </div>
                </div>
                <div className="mb-4">
                  <label htmlFor="bidAmount" className="block text-sm font-medium text-gray-700 mb-2">
                    입찰가
                  </label>
                  <div className="flex items-center">
                    <input
                      type="number"
                      id="bidAmount"
                      className="flex-grow px-3 py-2 border border-gray-300 rounded-l-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={bidAmount}
                      onChange={(e) => setBidAmount(Math.floor(Number(e.target.value) / 100000) * 100000)}
                      min={auctionData.currentBid + 100000}
                      step={100000}
                    />
                    <button
                      onClick={handleBid}
                      className="bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded-r-md transition-colors"
                    >
                      입찰하기
                    </button>
                  </div>
                </div>
                <div className="flex items-center space-x-4 mb-8">
                  <button className="flex items-center space-x-2 text-gray-600 hover:text-red-500">
                    <Heart className="w-5 h-5" />
                    <span>관심 등록</span>
                  </button>
                  <button className="flex items-center space-x-2 text-gray-600 hover:text-blue-500">
                    <Share2 className="w-5 h-5" />
                    <span>공유하기</span>
                  </button>
                </div>
                <div className="bg-gray-100 p-4 rounded-md">
                  <h2 className="text-lg font-semibold mb-2">판매자 정보</h2>
                  <p>{auctionData.seller}</p>
                </div>
              </div>
            </div>
            <div className="mt-12">
              <h2 className="text-2xl font-bold mb-4">실시간 입찰 현황</h2>
              <div className="h-80">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={chartData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="시간" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="가격" stroke="#8884d8" activeDot={{ r: 8 }} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}