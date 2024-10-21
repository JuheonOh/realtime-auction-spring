import React from "react";
import { Search } from "lucide-react";

export default function SearchInput({ searchTerm, setSearchTerm }) {
  return (
    <div className="relative hidden md:block">
      <input
        type="text"
        placeholder="경매 검색..."
        className="pl-10 pr-4 py-2 rounded-full border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
      />
      <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
    </div>
  );
}
