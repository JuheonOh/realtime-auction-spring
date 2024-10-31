import { Link } from "react-router-dom";

export default function NavLink({ path, label }) {
  return (
    <Link to={path} className="text-gray-600 hover:text-blue-600 transition-colors">
      {label}
    </Link>
  );
}
