import { Bell, Clock, DollarSign, Lock, Mail, Phone, Settings, User } from "lucide-react";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { fetchUser } from "../../api/UserAPI";

export default function ProfilePage() {
  const [user, setUser] = useState({});
  const ACCESS_TOKEN = localStorage.getItem("accessToken");
  const [activeTab, setActiveTab] = useState("profile");

  const navigate = useNavigate();

  useEffect(() => {
    if (ACCESS_TOKEN) {
      fetchUser()
        .then((res) => {
          setUser(res);
        })
        .catch((err) => {
          console.log(err);
          navigate("/user/profile")
        });
    } else {
      navigate("/auth/login");
    }
  }, [ACCESS_TOKEN, navigate]);

  const handleLogout = () => {
    localStorage.clear();
    navigate("/");
  };

  const auctionHistory = [
    { id: 1, name: "Vintage Watch", bid: 1500000, status: "낙찰", date: "2023-06-15" },
    { id: 2, name: "Modern Art Painting", bid: 5000000, status: "패찰", date: "2023-06-10" },
    { id: 3, name: "Rare Coin Collection", bid: 2000000, status: "진행중", date: "2023-06-20" },
  ];

  const wishlist = [
    { id: 1, name: "Antique Furniture", currentBid: 3500000, endDate: "2023-06-25" },
    { id: 2, name: "Luxury Handbag", currentBid: 2500000, endDate: "2023-06-30" },
  ];

  return (
    <div className="container mx-auto px-4 py-8 min-h-screen">
      <h1 className="text-3xl font-bold mb-8">마이페이지</h1>

      <div className="flex flex-col md:flex-row gap-8">
        {/* 사이드바 */}
        <div className="w-full md:w-1/4">
          <div className="bg-white shadow rounded-lg p-4">
            <div className="flex items-center space-x-4 mb-6">
              <div className="w-16 h-16 bg-gray-300 rounded-full"></div>
              <div>
                <h2 className="text-xl font-semibold">{user.ame}</h2>
                <p className="text-gray-500">{user.email}</p>
              </div>
            </div>
            <nav>
              <button className={`w-full text-left py-2 px-4 rounded ${activeTab === "profile" ? "bg-blue-500 text-white" : "hover:bg-gray-100"}`} onClick={() => setActiveTab("profile")}>
                프로필
              </button>
              <button className={`w-full text-left py-2 px-4 rounded ${activeTab === "auctions" ? "bg-blue-500 text-white" : "hover:bg-gray-100"}`} onClick={() => setActiveTab("auctions")}>
                경매 내역
              </button>
              <button className={`w-full text-left py-2 px-4 rounded ${activeTab === "wishlist" ? "bg-blue-500 text-white" : "hover:bg-gray-100"}`} onClick={() => setActiveTab("wishlist")}>
                관심 상품
              </button>
              <button className={`w-full text-left py-2 px-4 rounded ${activeTab === "settings" ? "bg-blue-500 text-white" : "hover:bg-gray-100"}`} onClick={() => setActiveTab("settings")}>
                설정
              </button>
            </nav>
            <button className="w-full mt-4 py-2 px-4 bg-red-500 text-white rounded hover:bg-red-600 transition-colors" onClick={handleLogout}>
              로그아웃
            </button>
          </div>
        </div>

        {/* 메인 컨텐츠 */}
        <div className="w-full md:w-3/4">
          <div className="bg-white shadow rounded-lg p-6">
            {activeTab === "profile" && (
              <div>
                <h3 className="text-xl font-semibold mb-4">프로필 정보</h3>
                <div className="space-y-4">
                  <div className="flex items-center">
                    <User className="w-5 h-5 mr-2 text-gray-500" />
                    <span>{user.name}</span>
                  </div>
                  <div className="flex items-center">
                    <Mail className="w-5 h-5 mr-2 text-gray-500" />
                    <span>{user.email}</span>
                  </div>
                  <div className="flex items-center">
                    <Phone className="w-5 h-5 mr-2 text-gray-500" />
                    <span>{user.phone}</span>
                  </div>
                  <div className="flex items-center">
                    <Clock className="w-5 h-5 mr-2 text-gray-500" />
                    <span>가입일: {new Date(user.createdAt).toLocaleString()}</span>
                  </div>
                </div>
              </div>
            )}

            {activeTab === "auctions" && (
              <div>
                <h3 className="text-xl font-semibold mb-4">경매 참여 내역</h3>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm text-left">
                    <thead className="text-xs text-gray-700 uppercase bg-gray-50">
                      <tr>
                        <th className="px-6 py-3">상품명</th>
                        <th className="px-6 py-3">입찰가</th>
                        <th className="px-6 py-3">상태</th>
                        <th className="px-6 py-3">날짜</th>
                      </tr>
                    </thead>
                    <tbody>
                      {auctionHistory.map((item) => (
                        <tr key={item.id} className="bg-white border-b">
                          <td className="px-6 py-4">{item.name}</td>
                          <td className="px-6 py-4">{item.bid.toLocaleString()}원</td>
                          <td className="px-6 py-4">
                            <span
                              className={`px-2 py-1 rounded text-xs font-semibold
                              ${item.status === "낙찰" ? "bg-green-100 text-green-800" : item.status === "패찰" ? "bg-red-100 text-red-800" : "bg-yellow-100 text-yellow-800"}`}
                            >
                              {item.status}
                            </span>
                          </td>
                          <td className="px-6 py-4">{item.date}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {activeTab === "wishlist" && (
              <div>
                <h3 className="text-xl font-semibold mb-4">관심 상품</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {wishlist.map((item) => (
                    <div key={item.id} className="border rounded-lg p-4">
                      <h4 className="font-semibold">{item.name}</h4>
                      <div className="flex justify-between items-center mt-2">
                        <div className="flex items-center">
                          <DollarSign className="w-4 h-4 text-green-500 mr-1" />
                          <span>{item.currentBid.toLocaleString()}원</span>
                        </div>
                        <div className="flex items-center">
                          <Clock className="w-4 h-4 text-red-500 mr-1" />
                          <span>{item.endDate}</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {activeTab === "settings" && (
              <div>
                <h3 className="text-xl font-semibold mb-4">계정 설정</h3>
                <div className="space-y-4">
                  <button className="w-full py-2 px-4 bg-gray-100 text-gray-800 rounded hover:bg-gray-200 transition-colors flex items-center justify-center">
                    <Settings className="w-5 h-5 mr-2" />
                    프로필 수정
                  </button>
                  <button className="w-full py-2 px-4 bg-gray-100 text-gray-800 rounded hover:bg-gray-200 transition-colors flex items-center justify-center">
                    <Lock className="w-5 h-5 mr-2" />
                    비밀번호 변경
                  </button>
                  <button className="w-full py-2 px-4 bg-gray-100 text-gray-800 rounded hover:bg-gray-200 transition-colors flex items-center justify-center">
                    <Bell className="w-5 h-5 mr-2" />
                    알림 설정
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
