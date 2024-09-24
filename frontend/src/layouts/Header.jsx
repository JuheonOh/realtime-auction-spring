import React from "react";
import { useState } from "react";
import { Link } from "react-router-dom";

import { Search, User, Menu, X } from "lucide-react";
import NotificationComponent from "../components/Notification";

export default function Header() {
  const [searchTerm, setSearchTerm] = useState("");
  const [isMenuOpen, setIsMenuOpen] = useState(false);

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
            <Link to="/auction" className="text-gray-600 hover:text-blue-600 transition-colors">
              경매 목록
            </Link>
            <Link to="#" className="text-gray-600 hover:text-blue-600 transition-colors">
              판매하기
            </Link>
            <Link to="/profile" className="text-gray-600 hover:text-blue-600 transition-colors">
              마이페이지
            </Link>
          </div>
          <div className="flex items-center space-x-4">
            <div className="relative hidden md:block">
              <input type="text" placeholder="경매 검색..." className="pl-10 pr-4 py-2 rounded-full border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            </div>
            <NotificationComponent />
            <Link to="/login">
              <button className="p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors">
                <User className="w-6 h-6 text-gray-600" />
              </button>
            </Link>
            <button className="md:hidden p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors" onClick={() => setIsMenuOpen(!isMenuOpen)}>
              {isMenuOpen ? <X className="w-6 h-6 text-gray-600" /> : <Menu className="w-6 h-6 text-gray-600" />}
            </button>
          </div>
        </div>
        {isMenuOpen && (
          <div className="mt-4 md:hidden">
            <Link to="/" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">
              홈
            </Link>
            <Link to="/auction" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">
              경매 목록
            </Link>
            <Link to="#" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">
              판매하기
            </Link>
            <Link to="/profile" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">
              마이페이지
            </Link>
            <div className="relative mt-2">
              <input type="text" placeholder="경매 검색..." className="w-full pl-10 pr-4 py-2 rounded-full border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500" value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} />
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            </div>
          </div>
        )}
      </div>
    </header>
  );
}
