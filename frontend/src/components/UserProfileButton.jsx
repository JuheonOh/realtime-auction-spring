import { User } from "lucide-react";
import React, { memo } from "react";
import { Link } from "react-router-dom";

function UserProfileButton({ isAuthenticated }) {
  return (
    <Link to={isAuthenticated ? "/user/profile" : "/auth/login"}>
      <button className="p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors">
        <User className="w-6 h-6 text-gray-600" />
      </button>
    </Link>
  );
}

export default memo(UserProfileButton);
