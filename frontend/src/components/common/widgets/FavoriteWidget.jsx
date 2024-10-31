import { Heart } from "lucide-react";
import { useNavigate } from "react-router-dom";

export default function FavoriteWidget() {
  const navigate = useNavigate();

  const handleFavoriteClick = () => {
    navigate("/user/favorites");
  };

  return (
    <button className="p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors" title="관심 목록" onClick={handleFavoriteClick}>
      <Heart className="w-6 h-6 text-gray-600" />
    </button>
  );
}
