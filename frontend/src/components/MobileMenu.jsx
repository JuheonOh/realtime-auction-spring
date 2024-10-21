import React from "react";
import { Link } from "react-router-dom";
import { Search } from "lucide-react";

export default function MobileMenu({ isMenuOpen, searchTerm, setSearchTerm }) {
  if (!isMenuOpen) return null;

  return (
    <div className="mt-4 md:hidden">
      <Link to="/" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">
        홈
      </Link>
      <Link to="/auctions" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">
        경매 목록
      </Link>
      <Link to="/auctions/new" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">
        판매하기
      </Link>
      <Link to="/user/profile" className="block py-2 text-gray-600 hover:text-blue-600 transition-colors">
        마이페이지
      </Link>
      <div className="relative mt-2">
        <input
          type="text"
          placeholder="경매 검색..."
          className="w-full pl-10 pr-4 py-2 rounded-full border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
        />
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
      </div>
    </div>
  );
}
