import { Menu, X } from "lucide-react";

export default function MenuToggleButton({ isMenuOpen, setIsMenuOpen }) {
  return (
    <button className="md:hidden p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors" onClick={() => setIsMenuOpen(!isMenuOpen)}>
      {isMenuOpen ? <X className="w-6 h-6 text-gray-600" /> : <Menu className="w-6 h-6 text-gray-600" />}
    </button>
  );
}
