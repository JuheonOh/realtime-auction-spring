import { signUp } from "@apis/AuthAPI";
import InValidAlert from "@components/common/alerts/InValidAlert";
import { Eye, EyeOff, Lock, Mail, Phone, User } from "lucide-react";
import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

export default function SignUpPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    phone: "",
    nickname: "",
    agreeTerms: false,
  });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [inValid, setInValid] = useState({});

  const validateForm = () => {
    const inValid = {};

    if (!formData.name.trim()) {
      inValid.name = "이름은 필수 입력 사항입니다.";
    }

    if (!formData.email.trim()) {
      inValid.email = "이메일은 필수 입력 사항입니다.";
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      inValid.email = "유효한 이메일 주소를 입력하세요.";
    }

    if (!formData.password) {
      inValid.password = "비밀번호는 필수 입력 사항입니다.";
    }

    if (!formData.confirmPassword) {
      inValid.confirmPassword = "비밀번호 확인은 필수 입력 사항입니다.";
    }

    if (formData.password !== formData.confirmPassword) {
      inValid.confirmPassword = "비밀번호가 일치하지 않습니다.";
    }

    if (!formData.phone.trim()) {
      inValid.phone = "전화번호는 필수 입력 사항입니다.";
    } else if (!/^\d{3}-\d{3,4}-\d{4}$/.test(formData.phone)) {
      inValid.phone = "유효한 전화번호를 입력하세요.";
    }

    if (!formData.nickname.trim()) {
      inValid.nickname = "닉네임은 필수 입력 사항입니다.";
    }

    if (!formData.agreeTerms) {
      inValid.agreeTerms = "이용약관과 개인정보처리방침에 동의해야 합니다.";
    }

    setInValid(inValid);

    // inValid 첫번째 추가된 name에 focus
    if (Object.keys(inValid).length > 0) {
      document.getElementById(Object.keys(inValid)[0]).focus();
    }

    return Object.keys(inValid).length === 0;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;

    setInValid((prevInValid) => ({
      ...prevInValid,
      [name]: "",
    }));

    setFormData((prevData) => {
      let newValue = value;

      if (name === "phone") {
        // 숫자만 추출
        newValue = value.replace(/\D/g, "");

        // 최대 11자리로 제한
        newValue = newValue.slice(0, 11);

        // 하이픈 추가
        if (newValue.length > 3) {
          newValue = newValue.replace(/(\d{3})(\d{0,4})(\d{0,4})/, "$1-$2-$3");
          newValue = newValue.replace(/-{1,2}$/, ""); // 끝에 불필요한 하이픈 제거
        }
      }

      if (name === "agreeTerms") {
        newValue = e.target.checked;
      }

      return {
        ...prevData,
        [name]: newValue,
      };
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    try {
      await signUp(formData);
      navigate("/auth/login");
    } catch (err) {
      setInValid(err.response.data);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div className="max-w-md w-full mx-auto">
        <div className="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900 mb-6">회원가입</h2>
          <form className="space-y-6" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-gray-700">
                이름
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <User className="h-5 w-5 text-gray-400" aria-hidden="true" />
                </div>
                <input id="name" name="name" type="text" autoComplete="name" className={`focus:ring-blue-500 focus:border-blue-500 block w-full pl-10 sm:text-sm border-gray-300 rounded-md ${inValid.name ? "border-red-500" : ""}`} placeholder="홍길동" value={formData.name} onChange={handleChange} />
              </div>
              <InValidAlert inValid={inValid.name} message={inValid.name} className="mt-3" />
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                이메일 주소
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Mail className="h-5 w-5 text-gray-400" aria-hidden="true" />
                </div>
                <input id="email" name="email" type="text" autoComplete="email" className={`focus:ring-blue-500 focus:border-blue-500 block w-full pl-10 sm:text-sm border-gray-300 rounded-md ${inValid.email ? "border-red-500" : ""}`} placeholder="you@example.com" value={formData.email} onChange={handleChange} />
              </div>
              <InValidAlert inValid={inValid.email} message={inValid.email} className="mt-3" />
            </div>
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                비밀번호
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-gray-400" aria-hidden="true" />
                </div>
                <input id="password" name="password" type={showPassword ? "text" : "password"} autoComplete="new-password" className="focus:ring-blue-500 focus:border-blue-500 block w-full pl-10 pr-10 sm:text-sm border-gray-300 rounded-md" placeholder="••••••••" value={formData.password} onChange={handleChange} />
                <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                  <button type="button" className="text-gray-400 hover:text-gray-500 focus:outline-none focus:text-gray-500" onClick={() => setShowPassword(!showPassword)}>
                    {showPassword ? <EyeOff className="h-5 w-5" aria-hidden="true" /> : <Eye className="h-5 w-5" aria-hidden="true" />}
                  </button>
                </div>
              </div>
              <InValidAlert inValid={inValid.password} message={inValid.password} className="mt-3" />
            </div>
            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
                비밀번호 확인
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Lock className="h-5 w-5 text-gray-400" aria-hidden="true" />
                </div>
                <input id="confirmPassword" name="confirmPassword" type={showConfirmPassword ? "text" : "password"} autoComplete="new-password" className="focus:ring-blue-500 focus:border-blue-500 block w-full pl-10 pr-10 sm:text-sm border-gray-300 rounded-md" placeholder="••••••••" value={formData.confirmPassword} onChange={handleChange} />
                <div className="absolute inset-y-0 right-0 pr-3 flex items-center">
                  <button type="button" className="text-gray-400 hover:text-gray-500 focus:outline-none focus:text-gray-500" onClick={() => setShowConfirmPassword(!showConfirmPassword)}>
                    {showConfirmPassword ? <EyeOff className="h-5 w-5" aria-hidden="true" /> : <Eye className="h-5 w-5" aria-hidden="true" />}
                  </button>
                </div>
              </div>
              <InValidAlert inValid={inValid.confirmPassword} message={inValid.confirmPassword} className="mt-3" />
            </div>

            <div>
              <label htmlFor="phone" className="block text-sm font-medium text-gray-700">
                전화번호
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <Phone className="h-5 w-5 text-gray-400" aria-hidden="true" />
                </div>
                <input id="phone" name="phone" type="tel" autoComplete="tel" className="focus:ring-blue-500 focus:border-blue-500 block w-full pl-10 sm:text-sm border-gray-300 rounded-md" placeholder="010-1234-5678" value={formData.phone} onChange={handleChange} />
              </div>
              <InValidAlert inValid={inValid.phone} message={inValid.phone} className="mt-3" />
            </div>
            <div>
              <label htmlFor="nickname" className="block text-sm font-medium text-gray-700">
                닉네임
              </label>
              <div className="mt-1 relative rounded-md shadow-sm">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                  <User className="h-5 w-5 text-gray-400" aria-hidden="true" />
                </div>
                <input id="nickname" name="nickname" type="text" autoComplete="nickname" className={`focus:ring-blue-500 focus:border-blue-500 block w-full pl-10 sm:text-sm border-gray-300 rounded-md ${inValid.nickname ? "border-red-500" : ""}`} placeholder="닉네임" value={formData.nickname} onChange={handleChange} />
              </div>
              <InValidAlert inValid={inValid.nickname} message={inValid.nickname} className="mt-3" />
            </div>
            <div>
              <div className="flex items-center pt-3">
                <input id="agreeTerms" name="agreeTerms" type="checkbox" className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded" onChange={handleChange} />
                <label htmlFor="agreeTerms" className="ml-2 block text-sm text-gray-900">
                  <span>
                    <Link to="/terms" className="font-medium text-blue-600 hover:text-blue-500">
                      이용약관
                    </Link>
                    과{" "}
                    <Link to="/privacy" className="font-medium text-blue-600 hover:text-blue-500">
                      개인정보처리방침
                    </Link>
                    에 동의합니다.
                  </span>
                </label>
              </div>
              <InValidAlert inValid={inValid.agreeTerms} message={inValid.agreeTerms} className="mt-3" />
            </div>
            <div>
              <button type="submit" className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                회원가입
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
                  <span className="sr-only">이로 회원가입</span>
                  <svg className="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M15.8 9.7l-4.2 6.1H7.2V4.2h4.4v6.1l4.2-6.1h4.4l-4.4 6.3 4.4 5.3h-4.4z" />
                  </svg>
                </Link>
              </div>

              <div>
                <Link to="#" className="w-full inline-flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                  <span className="sr-only">카카오로 회원가입</span>
                  <svg className="w-5 h-5" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M10 0C4.5 0 0 3.6 0 8.1c0 2.9 1.9 5.4 4.7 6.9-.2.7-.7 2.6-.8 3-.1.5.2.5.4.3.2-.1 2.5-1.7 3.5-2.4.7.1 1.4.2 2.2.2 5.5 0 10-3.6 10-8.1S15.5 0 10 0z" />
                  </svg>
                </Link>
              </div>
            </div>
          </div>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600">
              이미 계정이 있으신가요?{" "}
              <Link to="/auth/login" className="font-medium text-blue-600 hover:text-blue-500">
                로그인
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
