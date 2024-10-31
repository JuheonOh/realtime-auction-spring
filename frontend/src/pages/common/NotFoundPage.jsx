import { Home } from "lucide-react";
import { Link } from "react-router-dom";

export default function NotFoundPage() {
  return (
    <div className="min-h-screen bg-gray-100 flex flex-col justify-center items-center px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8 text-center">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">404 - 페이지를 찾을 수 없습니다</h2>
          <p className="mt-2 text-center text-sm text-gray-600">죄송합니다. 요청하신 페이지를 찾을 수 없습니다.</p>
        </div>
        <div className="mt-8 space-y-6">
          <div className="flex flex-col sm:flex-row justify-center space-y-4 sm:space-y-0 sm:space-x-4">
            <Link to="/" className="flex items-center justify-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
              <Home className="h-5 w-5 mr-2" />
              메인 페이지로 돌아가기
            </Link>
          </div>
        </div>
        <div className="mt-8">
          <p className="text-sm text-gray-500">
            문제가 지속되면{" "}
            <a href="/contact" className="font-medium text-blue-600 hover:text-blue-500">
              고객센터
            </a>
            로 문의해 주세요.
          </p>
        </div>
      </div>
    </div>
  );
}
