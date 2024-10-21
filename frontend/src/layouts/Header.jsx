import { Search, User } from "lucide-react";
import React, { useState } from "react";
import { useSelector } from "react-redux";
import { Link } from "react-router-dom";
import MobileMenu from "../components/MobileMenu";
import FavoriteButton from "../components/widgets/FavoriteButton";
import MenuToggleButton from "../components/widgets/MenuToggleButton";
import NotificationComponent from "../components/widgets/NotificationDropdown";

export default function Header() {
  const [searchTerm, setSearchTerm] = useState("");
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const isAuthenticated = useSelector((state) => state.user.authenticated);

  return (
    <header className="bg-white shadow">
      <div className="container mx-auto px-4 py-6">
        <div className="flex justify-between items-center">
          <h1 className="text-3xl font-bold text-blue-600 hover:text-blue-500 transition-colors">
            <Link to="/">경매나라</Link>
          </h1>
          <div className="hidden md:flex items-center space-x-6">
            <Link to="/" className="text-gray-600 hover:text-blue-600 transition-colors">
              홈
            </Link>
            <Link to="/auctions" className="text-gray-600 hover:text-blue-600 transition-colors">
              경매 목록
            </Link>
            <Link to="/auctions/new" className="text-gray-600 hover:text-blue-600 transition-colors">
              판매하기
            </Link>
            <Link to="/user/profile" className="text-gray-600 hover:text-blue-600 transition-colors">
              마이페이지
            </Link>
          </div>
          <div className="flex items-center space-x-4">
            <div className="relative hidden md:block">
              <input type="text" placeholder="경매 검색..." className="pl-10 pr-4 py-2 rounded-full border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            </div>
            <NotificationComponent />
            <FavoriteButton />
            {isAuthenticated ? (
              <Link to="/user/profile">
                <button className="p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors">
                  <User className="w-6 h-6 text-gray-600" />
                </button>
              </Link>
            ) : (
              <Link to="/auth/login">
                <button className="p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors">
                  <User className="w-6 h-6 text-gray-600" />
                </button>
              </Link>
            )}
            <MenuToggleButton isMenuOpen={isMenuOpen} setIsMenuOpen={setIsMenuOpen} />
          </div>
        </div>
        {isMenuOpen && <MobileMenu isMenuOpen={isMenuOpen} searchTerm={searchTerm} setSearchTerm={setSearchTerm} />}
      </div>
    </header>
  );
}
