import { createAuction } from "@apis/AuctionAPI";
import { getCategoryList } from "@apis/CommonAPI";
import InValidAlert from "@components/common/alerts/InValidAlert";
import InputField from "@components/common/inputs/InputField";
import { formatPrice, removeCommas } from "@utils/formatNumber";
import { Upload, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useDropzone } from "react-dropzone";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";

export default function AuctionCreatePage() {
  const navigate = useNavigate();
  const user = useSelector((state) => state.user.info);

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    categoryId: "",
    startPrice: "",
    buyNowPrice: "",
    auctionDuration: "",
    images: [],
  });

  const [previewImages, setPreviewImages] = useState([]);
  const [categoryList, setCategoryList] = useState([]);
  const [inValid, setInValid] = useState({});
  const [fileError, setFileError] = useState("");

  // 카테고리 목록 가져오기
  const fetchCategoryList = async () => {
    const response = await getCategoryList();
    setCategoryList(response.data);
  };

  // 카테고리 목록 가져오기
  useEffect(() => {
    fetchCategoryList();
  }, []);

  // 입력 필드 변경 처리
  const handleChange = (e) => {
    const { name, value } = e.target;

    let newValue = value;

    if (name === "startPrice" || name === "buyNowPrice") {
      newValue = formatPrice(value);
    }

    setInValid((prevInValid) => ({
      ...prevInValid,
      [name]: "",
    }));

    setFormData((prevData) => ({
      ...prevData,
      [name]: newValue,
    }));
  };

  // 이미지 업로드 드래그 앤 드롭
  const onDrop = useCallback((acceptedFiles) => {
    handleImageUpload(acceptedFiles);
  }, []);

  // 이미지 업로드 드래그 앤 드롭
  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "image/*": [".jpeg", ".jpg", ".png", ".gif", ".webp"],
    },
    maxSize: 10 * 1024 * 1024, // 10MB
    onDropRejected: (rejectedFiles) => {
      const oversizedFiles = rejectedFiles.filter((file) => file.errors[0]?.code === "file-too-large").map((file) => file.file.name);

      if (oversizedFiles.length > 0) {
        setFileError(`${oversizedFiles.length > 1 ? "파일들이" : "파일이"} 10MB 용량 제한을 초과했습니다. \r\n ${oversizedFiles.join("\r\n")} `);
      }
    },
  });

  // 이미지 업로드
  const handleImageUpload = (files) => {
    setFileError(""); // 에러 메시지 초기화
    const newImages = files.filter((file) => file.type.startsWith("image/"));
    setFormData((prevData) => ({
      ...prevData,
      images: [...prevData.images, ...newImages],
    }));

    const newPreviewImages = newImages.map((file) => URL.createObjectURL(file));
    setPreviewImages((prevImages) => [...prevImages, ...newPreviewImages]);
    setInValid((prevInValid) => ({
      ...prevInValid,
      images: "",
    }));
  };

  // 이미지 삭제
  const removeImage = (index) => {
    setFormData((prevData) => ({
      ...prevData,
      images: prevData.images.filter((_, i) => i !== index),
    }));
    setPreviewImages((prevImages) => prevImages.filter((_, i) => i !== index));
    setInValid((prevInValid) => ({
      ...prevInValid,
      images: "",
    }));
  };

  const validateForm = () => {
    const inValid = {};

    if (!formData.title.trim()) {
      inValid.title = "제목은 필수 입력 사항입니다.";
    }

    if (!formData.description.trim()) {
      inValid.description = "설명은 필수 입력 사항입니다.";
    }

    if (!formData.categoryId) {
      inValid.categoryId = "카테고리는 필수 선택 사항입니다.";
    }

    const startPrice = removeCommas(formData.startPrice);
    if (isNaN(startPrice) || startPrice <= 0) {
      inValid.startPrice = "유효한 경매 시작 가격을 입력하세요.";
    }

    if (startPrice < 1000) {
      inValid.startPrice = "경매 시작 가격은 최소 1,000원 이상이어야 합니다.";
    }

    const buyNowPrice = removeCommas(formData.buyNowPrice);
    if (buyNowPrice && buyNowPrice < startPrice && buyNowPrice !== 0) {
      inValid.buyNowPrice = "즉시 구매 가격은 경매 시작 가격보다 같거나 높아야 합니다.";
    }

    if (!formData.auctionDuration) {
      inValid.auctionDuration = "경매 기간은 필수 선택 사항입니다.";
    }

    if (formData.images.length === 0) {
      inValid.images = "이미지를 최소 1개 이상 업로드해야 합니다.";
    }

    setInValid(inValid);
    return Object.keys(inValid).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    const auctionData = {
      userId: user.id,
      title: formData.title,
      description: formData.description,
      categoryId: formData.categoryId === "" ? 0 : parseInt(formData.categoryId),
      startPrice: formData.startPrice === "" || formData.startPrice === 0 ? 0 : removeCommas(formData.startPrice),
      buyNowPrice: formData.buyNowPrice === "" || formData.buyNowPrice === 0 ? 0 : removeCommas(formData.buyNowPrice),
      auctionDuration: formData.auctionDuration === "" ? 0 : parseInt(formData.auctionDuration),
      images: formData.images,
    };

    try {
      const response = await createAuction(auctionData);

      if (response.status === 201) {
        alert("경매 상품이 등록되었습니다.");
        navigate(`/auctions/${response.data.auctionId}`);
      }
    } catch (err) {
      console.log(err);

      setInValid(err.response.data);
    }
  };

  return (
    <div className="bg-gray-100 min-h-screen py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-white rounded-lg shadow-lg overflow-hidden">
          <div className="p-6 sm:p-10">
            <h1 className="text-3xl font-bold mb-6">경매 등록</h1>
            <form onSubmit={handleSubmit} className="space-y-6">
              <InputField name="title" label="상품명" placeholder="상품명을 입력하세요." value={formData.title} handleChange={handleChange} inValid={inValid} />
              <div>
                <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                  상품 설명
                </label>
                <textarea id="description" name="description" value={formData.description} placeholder="상품에 대한 자세한 설명을 입력하세요." onChange={handleChange} rows="4" className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${inValid.description ? "border-red-500" : "border-gray-300"}`}></textarea>
                <InValidAlert inValid={inValid.description} message={inValid.description} />
              </div>
              <div>
                <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-1">
                  카테고리
                </label>
                <select id="category" name="categoryId" value={formData.categoryId} onChange={handleChange} className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${inValid.categoryId ? "border-red-500" : "border-gray-300"}`}>
                  <option value="">카테고리 선택</option>
                  {categoryList.map((category, index) => (
                    <option key={index} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
                <InValidAlert inValid={inValid.categoryId} message={inValid.categoryId} />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <InputField name="startPrice" label="경매 시작 가격 (원)" type="text" placeholder="1,000원 이상 입력하세요." value={formData.startPrice} handleChange={handleChange} inValid={inValid} />
                <InputField name="buyNowPrice" label="즉시 구매 가격 (원, 선택사항)" type="text" placeholder="경매 시작 가격과 같거나 높게 입력하세요." value={formData.buyNowPrice} handleChange={handleChange} inValid={inValid} />
              </div>

              <div>
                <label htmlFor="auctionDuration" className="block text-sm font-medium text-gray-700 mb-1">
                  경매 기간
                </label>
                <select id="auctionDuration" name="auctionDuration" value={formData.duration} onChange={handleChange} className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${inValid.auctionDuration ? "border-red-500" : "border-gray-300"}`}>
                  <option value="">경매 기간 선택</option>
                  <option value="1">1일</option>
                  <option value="3">3일</option>
                  <option value="5">5일</option>
                  <option value="7">7일</option>
                  <option value="10">10일</option>
                </select>
                <InValidAlert inValid={inValid.auctionDuration} message={inValid.auctionDuration} />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">상품 이미지</label>
                <div
                  {...getRootProps()}
                  className={`mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-dashed rounded-md transition-colors focus:outline-none focus:border-blue-500 focus:border-solid
                    ${isDragActive ? "border-blue-500 bg-blue-50" : ""} 
                    ${inValid.images || fileError ? "border-red-500" : "border-gray-300"}`}
                >
                  <div className="space-y-1 text-center">
                    <Upload className="mx-auto h-12 w-12 text-gray-400" />
                    <div className="flex text-sm text-gray-600">
                      <label htmlFor="images" className="relative cursor-pointer bg-white rounded-md font-medium text-blue-600 hover:text-blue-500 ">
                        <span>이미지 업로드</span>
                        <input {...getInputProps()} className="sr-only" />
                      </label>
                      <p className="pl-1">또는 드래그 앤 드롭</p>
                    </div>
                    <p className="text-xs text-gray-500">PNG, JPG, GIF (최대 10MB)</p>
                    <p className="text-xs text-gray-500">이미지 비율 추천 : 1:1</p>
                  </div>
                </div>
                <InValidAlert inValid={fileError} message={fileError} />
                <InValidAlert inValid={inValid.images} message={inValid.images} />
              </div>

              {previewImages.length > 0 && (
                <div className="mt-4 grid grid-cols-3 gap-4">
                  {previewImages.map((image, index) => (
                    <div key={index} className="relative w-full h-full max-h-96 aspect-square rounded-lg bg-gray-100 overflow-hidden">
                      <img src={image} alt={`업로드 한 ${index + 1}번째 이미지`} className="h-full absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 hover:scale-105 transition-transform duration-200" />
                      <button type="button" onClick={() => removeImage(index)} className="absolute top-2 right-2 bg-red-500 hover:bg-red-600 text-white rounded-full p-1.5 shadow-md transition-colors">
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <div>
                <button type="submit" className="w-full bg-blue-500 hover:bg-blue-600 text-white font-bold py-2 px-4 rounded-md transition-colors">
                  경매 등록하기
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
