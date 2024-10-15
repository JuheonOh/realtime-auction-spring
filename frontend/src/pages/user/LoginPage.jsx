import { Eye, EyeOff, Lock, Mail } from "lucide-react";
import { useState } from "react";
import { useDispatch } from "react-redux";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../../apis/AuthAPI";
import { SET_ACCESS_TOKEN } from "../../redux/store/User";
import { setCookie } from "../../storage/Cookie";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const res = await login({ email, password });
      setCookie("tokenType", res.data.tokenType);
      setCookie("refreshToken", res.data.refreshToken);
      setCookie("accessToken", res.data.accessToken);

      dispatch(SET_ACCESS_TOKEN(res.data.accessToken));

      navigate("/");
    } catch (err) {
      if (err.response.data.message) {
        alert(err.response.data.message);
      }

      console.log(err);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="sm:mx-auto sm:w-full sm:max-w-md">
        <h2 className="text-center text-3xl font-extrabold text-gray-900">
          <Link to="/">
            <span className="font-bold text-blue-600 hover:text-blue-500 transition-colors">경매나라</span>
          </Link>
          에 오신 것을 환영합니다
        </h2>
        <p className="mt-2 text-center text-md text-gray-600">
          계정이 없으신가요?{" "}
          <Link to="/auth/signup" className="font-medium text-blue-600 hover:text-blue-500">
            회원가입
          </Link>
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <form className="space-y-6" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                이메일 주소
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-gray-400" aria-hidden="true" />
                </div>
                <input id="email" name="email" type="email" autoComplete="email" required className="focus:ring-blue-500 focus:border-blue-500 block w-full pl-10 sm:text-sm border-gray-300 rounded-md py-1" placeholder="you@example.com" value={email} onChange={(e) => setEmail(e.target.value)} />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                비밀번호
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-gray-400" aria-hidden="true" />
                </div>
                <input id="password" name="password" type={showPassword ? "text" : "password"} autoComplete="current-password" required className="focus:ring-blue-500 focus:border-blue-500 block w-full pl-10 pr-10 sm:text-sm border-gray-300 rounded-md py-1" placeholder="••••••••" value={password} onChange={(e) => setPassword(e.target.value)} />
                <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                  <button type="button" className="text-gray-400 hover:text-gray-500 focus:outline-none focus:text-gray-500" onClick={() => setShowPassword(!showPassword)}>
                    {showPassword ? <EyeOff className="h-5 w-5" aria-hidden="true" /> : <Eye className="h-5 w-5" aria-hidden="true" />}
                  </button>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input id="remember-me" name="remember-me" type="checkbox" className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded" />
                <label htmlFor="remember-me" className="ml-2 block text-sm text-gray-900">
                  로그인 상태 유지
                </label>
              </div>

              <div className="text-sm">
                <Link to="/forgot-password" className="font-medium text-blue-600 hover:text-blue-500">
                  비밀번호를 잊으셨나요?
                </Link>
              </div>
            </div>

            <div>
              <button type="submit" className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                로그인
              </button>
            </div>
          </form>

          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-300" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-white text-gray-500">또는</span>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-3">
              <div>
                <Link to="#" className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                  <span className="sr-only">Sign in with Naver</span>
                  <svg className="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M15.8 9.7l-4.2 6.1H7.2V4.2h4.4v6.1l4.2-6.1h4.4l-4.4 6.3 4.4 5.3h-4.4z" />
                  </svg>
                </Link>
              </div>

              <div>
                <Link to="#" className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                  <span className="sr-only">Sign in with Kakao</span>
                  <svg className="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 0C4.5 0 0 3.6 0 8.1c0 2.9 1.9 5.4 4.7 6.9-.2.7-.7 2.6-.8 3-.1.5.2.5.4.3.2-.1 2.5-1.7 3.5-2.4.7.1 1.4.2 2.2.2 5.5 0 10-3.6 10-8.1S15.5 0 10 0z" />
                  </svg>
                </Link>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
