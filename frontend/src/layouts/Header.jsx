import MenuToggleButton from "@components/common/buttons/MenuToggleButton";
import UserProfileButton from "@components/common/buttons/UserProfileButton";
import InputSearch from "@components/common/inputs/InputSearch";
import MobileMenu from "@components/common/navigation/MobileMenu";
import NavLink from "@components/common/navigation/NavLink";
import FavoriteWidget from "@components/common/widgets/FavoriteWidget";
import NotificationComponent from "@components/features/notification/NotificationDropdown";
import { useState } from "react";
import { useSelector } from "react-redux";
import { Link } from "react-router-dom";

export default function Header() {
  const [searchTerm, setSearchTerm] = useState("");
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const isAuthenticated = useSelector((state) => state.user.authenticated);

  const NAV_LINKS = [
    { path: "/", label: "홈" },
    { path: "/auctions", label: "경매 목록" },
    { path: "/auctions/new", label: "판매하기" },
    { path: "/user/profile", label: "마이페이지" },
  ];

  return (
    <header className="bg-white shadow">
      <div className="container mx-auto px-4 py-6">
        <div className="flex justify-between items-center">
          <h1 className="text-3xl font-bold text-blue-600 hover:text-blue-500 transition-colors">
            <Link to="/">경매나라</Link>
          </h1>
          <div className="hidden md:flex items-center space-x-6">
            {NAV_LINKS.map((link) => (
              <NavLink key={link.path} {...link} />
            ))}
          </div>
          <div className="flex items-center space-x-4">
            <InputSearch searchTerm={searchTerm} setSearchTerm={setSearchTerm} />
            <NotificationComponent />
            <FavoriteWidget />
            <UserProfileButton isAuthenticated={isAuthenticated} />
            <MenuToggleButton isMenuOpen={isMenuOpen} setIsMenuOpen={setIsMenuOpen} />
          </div>
        </div>
        <MobileMenu isMenuOpen={isMenuOpen} searchTerm={searchTerm} setSearchTerm={setSearchTerm} />
      </div>
    </header>
  );
}
