import { useState } from "react";
import { Mail, Phone, MapPin, ChevronDown, ChevronUp } from "lucide-react";

const faqs = [
  {
    question: "경매에 어떻게 참여하나요?",
    answer: "원하는 상품 페이지에서 '입찰하기' 버튼을 클릭하고 금액을 입력하세요. 최고 입찰자가 되면 경매 종료 시 낙찰됩니다.",
  },
  {
    question: "결제는 어떻게 이루어지나요?",
    answer: "낙찰 후 24시간 이내에 등록된 결제 수단으로 자동 결제됩니다. 결제 수단은 마이페이지에서 관리할 수 있습니다.",
  },
  {
    question: "배송은 얼마나 걸리나요?",
    answer: "결제 완료 후 평균 3-5일 내에 배송이 시작됩니다. 정확한 배송 일정은 상품 상세 페이지에서 확인할 수 있습니다.",
  },
  {
    question: "경매 물품에 문제가 있을 경우 어떻게 하나요?",
    answer: "수령 후 24시간 이내에 고객센터로 문의해 주세요. 검수 후 환불 또는 교환 처리해 드립니다.",
  },
];

export default function ContactPage() {
  const [openFaq, setOpenFaq] = useState(null);
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    subject: "",
    message: "",
  });

  const toggleFaq = (index) => {
    if (openFaq === index) {
      setOpenFaq(null);
    } else {
      setOpenFaq(index);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    // 여기에 문의 제출 로직을 구현합니다.
    console.log("문의 제출:", formData);
  };

  return (
    <div className="min-h-screen bg-gray-100 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-extrabold text-gray-900 text-center mb-8">고객센터</h1>

        <div className="bg-white shadow overflow-hidden sm:rounded-lg mb-8">
          <div className="px-4 py-5 sm:p-6">
            <h2 className="text-lg leading-6 font-medium text-gray-900 mb-4">자주 묻는 질문 (FAQ)</h2>
            <div className="space-y-4">
              {faqs.map((faq, index) => (
                <div key={index} className="border-b border-gray-200 pb-4">
                  <button className="flex justify-between items-center w-full text-left" onClick={() => toggleFaq(index)}>
                    <span className="text-lg font-medium text-gray-900">{faq.question}</span>
                    {openFaq === index ? <ChevronUp className="h-5 w-5 text-gray-500" /> : <ChevronDown className="h-5 w-5 text-gray-500" />}
                  </button>
                  {openFaq === index && <p className="mt-2 text-gray-600">{faq.answer}</p>}
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="bg-white shadow overflow-hidden sm:rounded-lg mb-8">
          <div className="px-4 py-5 sm:p-6">
            <h2 className="text-lg leading-6 font-medium text-gray-900 mb-4">문의하기</h2>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label htmlFor="name" className="block text-sm font-medium text-gray-700">
                  이름
                </label>
                <input type="text" name="name" id="name" required className="mt-1 focus:ring-blue-500 focus:border-blue-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" value={formData.name} onChange={handleChange} />
              </div>
              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                  이메일
                </label>
                <input type="email" name="email" id="email" required className="mt-1 focus:ring-blue-500 focus:border-blue-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" value={formData.email} onChange={handleChange} />
              </div>
              <div>
                <label htmlFor="subject" className="block text-sm font-medium text-gray-700">
                  제목
                </label>
                <input type="text" name="subject" id="subject" required className="mt-1 focus:ring-blue-500 focus:border-blue-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" value={formData.subject} onChange={handleChange} />
              </div>
              <div>
                <label htmlFor="message" className="block text-sm font-medium text-gray-700">
                  메시지
                </label>
                <textarea name="message" id="message" rows="4" required className="mt-1 focus:ring-blue-500 focus:border-blue-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md" value={formData.message} onChange={handleChange}></textarea>
              </div>
              <div>
                <button type="submit" className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500">
                  문의 제출
                </button>
              </div>
            </form>
          </div>
        </div>

        <div className="bg-white shadow overflow-hidden sm:rounded-lg">
          <div className="px-4 py-5 sm:p-6">
            <h2 className="text-lg leading-6 font-medium text-gray-900 mb-4">연락처 정보</h2>
            <div className="space-y-4">
              <div className="flex items-center">
                <Mail className="h-5 w-5 text-gray-400 mr-2" />
                <span>support@example.com</span>
              </div>
              <div className="flex items-center">
                <Phone className="h-5 w-5 text-gray-400 mr-2" />
                <span>02-1234-5678</span>
              </div>
              <div className="flex items-center">
                <MapPin className="h-5 w-5 text-gray-400 mr-2" />
                <span>서울특별시 강남구 테헤란로 123 경매나라 빌딩 4층</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
