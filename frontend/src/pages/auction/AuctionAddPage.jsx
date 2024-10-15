import { Upload, X } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { useDropzone } from "react-dropzone";
import { useSelector } from "react-redux";
import { addAuction, getCategoryList } from "../../apis/AuctionAPI";
import { useNavigate } from "react-router-dom";

export default function AuctionAddPage() {
  const navigate = useNavigate();
  const user = useSelector((state) => state.user.info);

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    categoryId: undefined,
    startPrice: 0,
    buyNowPrice: 0,
    auctionDuration: "3",
    images: [],
  });

  const [previewImages, setPreviewImages] = useState([]);
  const [categoryList, setCategoryList] = useState([]);

  // 카테고리 목록 가져오기
  const fetchCategoryList = async () => {
    const categoryList = await getCategoryList();
    setCategoryList(categoryList);
  };

  // 카테고리 목록 가져오기
  useEffect(() => {
    fetchCategoryList();
  }, []);

  // 입력 필드 변경 처리
  const handleChange = (e) => {
    if (e.target.name === "startPrice" || e.target.name === "buyNowPrice") {
      if (e.target.value < 0) {
        return;
      }

      // 1000 단위마다 콤마 추가
      e.target.value = e.target.value.replace(/\D/g, "").replace(/\B(?=(\d{3})+(?!\d))/g, ",");
    }

    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
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
      "image/*": [".jpeg", ".jpg", ".png", ".gif"],
    },
    maxSize: 10 * 1024 * 1024, // 10MB
  });

  // 이미지 업로드
  const handleImageUpload = (files) => {
    const newImages = files.filter((file) => file.type.startsWith("image/"));
    setFormData((prevData) => ({
      ...prevData,
      images: [...prevData.images, ...newImages],
    }));

    const newPreviewImages = newImages.map((file) => URL.createObjectURL(file));
    setPreviewImages((prevImages) => [...prevImages, ...newPreviewImages]);
  };

  // 이미지 삭제
  const removeImage = (index) => {
    setFormData((prevData) => ({
      ...prevData,
      images: prevData.images.filter((_, i) => i !== index),
    }));
    setPreviewImages((prevImages) => prevImages.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const auctionData = {
      userId: user.id,
      title: formData.title,
      description: formData.description,
      categoryId: parseInt(formData.categoryId),
      startPrice: parseInt(formData.startPrice.replace(/,/g, "")),
      buyNowPrice: formData.buyNowPrice === "" ? 0 : parseInt(formData.buyNowPrice.replace(/,/g, "")),
      auctionDuration: parseInt(formData.auctionDuration),
      images: formData.images,
    };

    if (auctionData.buyNowPrice !== 0 && auctionData.buyNowPrice < auctionData.startPrice) {
      alert("즉시 구매 가격은 시작 가격보다 높아야 합니다.");
      return;
    }

    try {
      const response = await addAuction(auctionData);
      
      if (response.status === 201) {
        alert("경매 상품이 등록되었습니다.");
        navigate("/auction");
      }
    } catch (err) {
      console.log(err);
    }
  };

  return (
    <div className="bg-gray-100 min-h-screen py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="bg-white rounded-lg shadow-lg overflow-hidden">
          <div className="p-6 sm:p-10">
            <h1 className="text-3xl font-bold mb-6">경매 상품 등록</h1>
            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label htmlFor="title" className="block text-sm font-medium text-gray-700 mb-1">
                  상품명
                </label>
                <input type="text" id="title" name="title" value={formData.title} onChange={handleChange} required className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" />
              </div>

              <div>
                <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
                  상품 설명
                </label>
                <textarea id="description" name="description" value={formData.description} onChange={handleChange} required rows="4" className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"></textarea>
              </div>

              <div>
                <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-1">
                  카테고리
                </label>
                <select id="category" name="categoryId" value={formData.categoryId} onChange={handleChange} required className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500">
                  <option value="">카테고리 선택</option>
                  {categoryList.map((category, index) => (
                    <option key={index} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="startPrice" className="block text-sm font-medium text-gray-700 mb-1">
                    시작 가격
                  </label>
                  <input type="text" id="startPrice" name="startPrice" value={formData.startPrice} onChange={handleChange} required className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
                <div>
                  <label htmlFor="buyNowPrice" className="block text-sm font-medium text-gray-700 mb-1">
                    즉시 구매 가격 (선택사항)
                  </label>
                  <input type="text" id="buyNowPrice" name="buyNowPrice" value={formData.buyNowPrice} onChange={handleChange} className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" />
                </div>
              </div>

              <div>
                <label htmlFor="auctionDuration" className="block text-sm font-medium text-gray-700 mb-1">
                  경매 기간
                </label>
                <select id="auctionDuration" name="auctionDuration" value={formData.duration} onChange={handleChange} required className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500">
                  <option value="3">3일</option>
                  <option value="5">5일</option>
                  <option value="7">7일</option>
                  <option value="10">10일</option>
                </select>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">상품 이미지</label>
                <div {...getRootProps()} className={`mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-dashed rounded-md transition-colors ${isDragActive ? `border-blue-500 bg-blue-50` : ``}`}>
                  <div className="space-y-1 text-center">
                    <Upload className="mx-auto h-12 w-12 text-gray-400" />
                    <div className="flex text-sm text-gray-600">
                      <label htmlFor="images" className="relative cursor-pointer bg-white rounded-md font-medium text-blue-600 hover:text-blue-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-blue-500">
                        <span>이미지 업로드</span>
                        <input {...getInputProps()} className="sr-only" />
                      </label>
                      <p className="pl-1">또는 드래그 앤 드롭</p>
                    </div>
                    <p className="text-xs text-gray-500">PNG, JPG, GIF up to 10MB</p>
                  </div>
                </div>
                {previewImages.length > 0 && (
                  <div className="mt-4 grid grid-cols-3 gap-4">
                    {previewImages.map((image, index) => (
                      <div key={index} className="relative">
                        <img src={image} alt={`Preview ${index + 1}`} className="h-24 w-full object-cover rounded-md" />
                        <button type="button" onClick={() => removeImage(index)} className="absolute top-0 right-0 bg-red-500 text-white rounded-full p-1">
                          <X className="h-4 w-4" />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

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
