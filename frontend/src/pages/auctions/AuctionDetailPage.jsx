import { buyNowAuction, createBid, getAuctionBidStream, getAuctionDetail } from "@apis/AuctionAPI";
import { getUser } from "@apis/UserAPI";
import InValidAlert from "@components/common/alerts/InValidAlert";
import SuccessAlert from "@components/common/alerts/SuccessAlert";
import BlurOverlay from "@components/common/loading/BlurOverlay";
import LoadingSpinner from "@components/common/loading/LoadingSpinner";
import BidChart from "@components/features/auction/BidChart";
import { SET_INFO } from "@data/redux/store/User";
import { clearCookie } from "@data/storage/Cookie";
import useInterval from "@hooks/useInterval";
import { IMAGE_URL } from "@utils/constant";
import { addCommas, formatNumberInput } from "@utils/formatNumber";
import formatTime from "@utils/formatTime";
import { Clock, Eye, Gavel, Heart, MessageSquareMore, Share2, Star, Tag, User } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Link, useParams } from "react-router-dom";

// 입찰 단위
const bidUnit = (currentPrice) => {
  if (currentPrice < 1000) return 100;
  else if (currentPrice < 10000) return 1000;
  else if (currentPrice < 50000) return 2500;
  else if (currentPrice < 100000) return 5000;
  else if (currentPrice < 1000000) return 10000;
  else if (currentPrice < 5000000) return 25000;
  else if (currentPrice < 10000000) return 50000;
  else return 100000;
};

export default function AuctionDetail() {
  const auctionId = useParams().auctionId;

  const dispatch = useDispatch();
  const user = useSelector((state) => state.user);

  const [auction, setAuction] = useState();
  const [bidData, setBidData] = useState([]);
  const [transaction, setTransaction] = useState();

  const [selectedImage, setSelectedImage] = useState(0);
  const [bidAmount, setBidAmount] = useState(0);

  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  const [isWatched, setIsWatched] = useState(false);

  const [showCopyMessage, setShowCopyMessage] = useState(false);
  const [showBidSuccess, setShowBidSuccess] = useState(false);
  const [showBuyNowSuccess, setShowBuyNowSuccess] = useState(false);

  const [inValid, setInValid] = useState({});
  const [highestBidderNickname, setHighestBidderNickname] = useState("없음");
  const [isHighestBidder, setIsHighestBidder] = useState(false);

  // 계정 정보 불러오기
  useEffect(() => {
    const fetchUser = async () => {
      if (!user.accessToken) return;
      try {
        const userInfo = await getUser();
        dispatch(SET_INFO(userInfo.data));
      } catch (err) {
        console.error(err);
        clearCookie();
      }
    };

    fetchUser();
  }, [user?.accessToken, dispatch]);

  // 초기 데이터 불러오기
  useEffect(() => {
    // 경매 정보 불러오기
    const fetchAuctionDetail = async () => {
      try {
        const auctionDetail = await getAuctionDetail(auctionId);
        if (auctionDetail.data) {
          setAuction(auctionDetail.data);
        }

        if (auctionDetail.data.transaction) {
          setTransaction(auctionDetail.data.transaction);
        }

        const bids = auctionDetail.data.bids.map((data) => ({ ...data, createdAt: new Date(data.createdAt) }));
        if (bids.length > 0) {
          setBidData(bids);
          setIsHighestBidder(bids[bids.length - 1].userId === user.info.id);
          setHighestBidderNickname(bids[bids.length - 1].nickname);
        }
      } catch (err) {
        console.error(err);
        setIsError(true);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAuctionDetail();
  }, [auctionId, user.info.id]);

  // 1초마다 시간 감소
  useInterval(() => {
    if (auction?.auctionLeftTime > 0) {
      setAuction((prev) => ({ ...prev, auctionLeftTime: prev.auctionLeftTime - 1 }));
    }
  }, 1000);

  // 입찰 스트림 불러오기
  useEffect(() => {
    if (!auctionId) return;

    let bidStream = null;
    const userId = user?.info?.id || null;

    const fetchBidStream = async () => {
      try {
        // 기존 연결이 있다면 닫기
        if (bidStream) {
          bidStream.close();
        }

        // SSE 연결
        bidStream = await getAuctionBidStream(auctionId);

        // 연결 성공 이벤트
        bidStream.addEventListener("connect", (e) => {
          console.log(e.data);
        });

        // 서버 시간 수신 이벤트
        bidStream.addEventListener("time", (e) => {
          const auctionLeftTime = e.data > 0 ? e.data : 0;
          setAuction((prev) => ({ ...prev, auctionLeftTime }));
        });

        // 입찰 데이터 수신 이벤트
        bidStream.addEventListener("bid", (e) => {
          const bid = JSON.parse(e.data);

          // 입찰자가 본인이 아닌 경우 입찰 데이터 업데이트
          // 본인이 입찰한 경우 업데이트 하지 않음
          // 중복 입찰 방지를 위해 본인이 입찰한 경우 업데이트 하지 않음
          if (bid.userId !== userId) {
            setBidData((prev) => [...prev, { id: bid.id, userId: bid.userId, nickname: bid.nickname, bidAmount: bid.bidAmount, createdAt: new Date(bid.createdAt) }]);
            setAuction((prev) => ({
              ...prev,
              currentPrice: bid.bidAmount,
              bidCount: prev.bidCount + 1,
            }));

            // 유효성 알림 초기화
            setInValid({});

            // 입찰 성공 메시지 초기화
            setShowBidSuccess(false);
            setIsHighestBidder(false);
          } else {
            // 본인이 입찰한 경우 최고입찰자 상태 업데이트
            setIsHighestBidder(true);
          }

          // 남은 시간 업데이트
          setAuction((prev) => ({ ...prev, auctionLeftTime: bid.auctionLeftTime }));

          // 최고입찰자 닉네임 업데이트
          setHighestBidderNickname(bid.nickname);
        });

        // 경매 종료 알림 이벤트 (즉시 구매 시 사용)
        bidStream.addEventListener("buy-now", (e) => {
          const buyNow = JSON.parse(e.data);

          setTransaction((prev) => ({
            ...prev,
            userId: buyNow.userId,
            nickname: buyNow.nickname,
            status: buyNow.status,
          }));

          setAuction((prev) => ({
            ...prev,
            status: "ENDED",
            auctionLeftTime: 0,
            successfulPrice: buyNow.finalPrice,
          }));
        });

        // SSE 연결 타임아웃 이벤트
        bidStream.addEventListener("timeout", (e) => {});

        // SSE 연결 에러 이벤트
        bidStream.addEventListener("error", (e) => {});
      } catch (err) {
        console.error(err);

        // 입찰 스트림 재연결
        setTimeout(() => {
          console.log("입찰 스트림 재연결");
          fetchBidStream();
        }, 1000);
      }
    };

    fetchBidStream();

    // 다른 페이지로 이동 시 SSE 연결 해제
    return () => {
      if (bidStream) {
        bidStream.close();
        bidStream = null;
      }
    };
  }, [auctionId, user.info.id]);

  // 입찰 금액 단위 업데이트
  useEffect(() => {
    if (!auction?.startPrice || !auction?.currentPrice) return;

    // 첫 입찰자인 경우 시작가로 설정, 아니면 현재가 + 입찰단위
    setBidAmount(bidData.length === 0 ? auction.startPrice : auction.currentPrice + bidUnit(auction.currentPrice));
  }, [auction?.startPrice, auction?.currentPrice, bidData.length]);

  // 입찰 금액 유효성 검사
  const validateBidAmount = () => {
    if (!auction?.startPrice || !auction?.currentPrice) return false;

    const inValid = {};

    // 첫 입찰자 체크
    if (bidData.length === 0) {
      //시작가와 동일하거나 높아야 함
      if (bidAmount < auction.startPrice) {
        inValid.bidAmount = "첫 입찰은 시작 금액과 동일하거나 높아야 합니다.";
        setInValid(inValid);
        return false;
      }
    } else {
      // 입찰 금액이 현재 입찰가보다 높아야 함
      if (bidAmount <= auction.currentPrice) {
        inValid.bidAmount = "입찰 금액은 현재 입찰가보다 높아야 합니다.";
        setInValid(inValid);
        return false;
      }

      // 입찰 단위 체크
      const currentBidUnit = bidUnit(auction.currentPrice);
      if (bidAmount - auction.currentPrice < currentBidUnit) {
        inValid.bidAmount = `${addCommas(currentBidUnit)}원 단위로 입찰해야 합니다.`;
        setInValid(inValid);
        return false;
      }
    }

    setInValid({});
    return Object.keys(inValid).length === 0;
  };

  // 입찰 금액 변경 시
  const handleBidAmountChange = (e) => {
    const input = e.target;
    const { formattedValue, numericValue, cursorPos } = formatNumberInput(input.value, input.value, input.selectionStart);

    // 입찰 성공 메시지 초기화
    setShowBidSuccess(false);

    // 입찰 금액 업데이트
    setBidAmount(numericValue);
    input.value = formattedValue;

    // 커서 위치 복원
    input.setSelectionRange(cursorPos, cursorPos);
  };

  // 입찰 버튼 클릭 시
  const handleBid = async (e) => {
    e.preventDefault();
    if (auction.auctionLeftTime <= 0 || auction.status === "ENDED" || !user.authenticated) return;
    if (!validateBidAmount()) return;

    if (user.info.id === auction.userId) {
      setInValid({ bidAmount: "본인이 등록한 경매에는 입찰할 수 없습니다." });
      return;
    }

    // 최고입찰자가 본인인 경우 입찰 불가
    if (bidData.length > 0 && bidData[bidData.length - 1].userId === user.info.id) {
      setInValid({ bidAmount: "현재 고객님이 최고입찰자입니다." });
      setShowBidSuccess(false);
      return;
    }

    try {
      // 입찰 요청
      const res = await createBid(auctionId, bidAmount);

      // 입찰 성공 시 입찰가 업데이트
      if (res?.status === 201) {
        setAuction((prev) => ({
          ...prev,
          currentPrice: bidAmount,
          bidCount: prev.bidCount + 1,
        }));

        // 입찰 데이터 업데이트
        setBidData([...bidData, { id: res.data.id, userId: user.info.id, nickname: user.info.nickname, bidAmount, createdAt: new Date() }]);

        // 입찰 성공 메시지 표시
        setShowBidSuccess(true);
      }
    } catch (err) {
      console.error(err);
      setInValid({ bidAmount: err.response.data.message });
    }
  };

  // 즉시 구매 버튼 클릭 시 즉시 구매
  const handleBuyNow = async () => {
    if (auction.auctionLeftTime <= 0 || auction.status === "ENDED" || !user.authenticated) return;

    if (user.info.id === auction.userId) {
      setInValid({ bidAmount: "본인이 등록한 경매에는 즉시 구매할 수 없습니다." });
      return;
    }

    // 즉시 구매 요청
    if (window.confirm("즉시 구매하시겠습니까?")) {
      try {
        const res = await buyNowAuction(auctionId);
        if (res?.status === 201) {
          setShowBuyNowSuccess(true);

          setTransaction((prev) => ({
            ...prev,
            userId: user.info.id,
            nickname: user.info.nickname,
            finalPrice: auction.buyNowPrice,
            status: "COMPLETED",
          }));

          setAuction((prev) => ({
            ...prev,
            status: "ENDED",
            auctionLeftTime: 0,
            successfulPrice: prev.buyNowPrice,
          }));
        }
      } catch (err) {
        setInValid({ bidAmount: err.response.data.message });
        console.error(err);
      }
    }
  };

  // 관심 등록 버튼 클릭 시 관심 등록 상태 업데이트
  const toggleWatch = () => {
    setIsWatched(!isWatched);
    setAuction((prev) => ({
      ...prev,
      watchCount: prev.watchCount + (isWatched ? -1 : 1),
    }));
  };

  // 공유 버튼 클릭 시 링크 복사
  const handleShare = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setShowCopyMessage(true);
      setTimeout(() => setShowCopyMessage(false), 2000); // 2초 후 메시지 숨김
    } catch (err) {
      console.error("클립보드 복사 실패:", err);
      alert("링크 복사에 실패했습니다.");
    }
  };

  // 이미지 슬라이드 기능
  const imageContainerRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const [totalX, setTotalX] = useState(0);

  const onMouseDown = (e) => {
    setIsDragging(true);

    const x = e.clientX;

    if (imageContainerRef.current && "scrollLeft" in imageContainerRef.current) {
      setTotalX(x + imageContainerRef.current.scrollLeft);
    }
  };
  const onMouseMove = (e) => {
    if (!isDragging) return;

    const scrollLeft = totalX - e.clientX;

    if (imageContainerRef.current && "scrollLeft" in imageContainerRef.current) {
      imageContainerRef.current.scrollLeft = scrollLeft;
    }
  };
  const onMouseUp = (e) => {
    setIsDragging(false);
  };

  const onMouseLeave = (e) => {
    setIsDragging(false);
  };

  // 이미지 슬라이드 기능
  useEffect(() => {
    const container = imageContainerRef.current;
    if (!container) return;

    const onWheel = (e) => {
      e.preventDefault();
      imageContainerRef.current.scrollLeft += e.deltaY * 0.5;
    };

    container.addEventListener("wheel", onWheel, { passive: false });

    return () => container.removeEventListener("wheel", onWheel);
  }, []);

  return isLoading || isError ? (
    <LoadingSpinner isLoading={isLoading} isError={isError} message="경매 상세" />
  ) : (
    <div className="bg-gray-100 min-h-screen py-8">
      <div className="max-w-[1280px] mx-auto px-8">
        <div className="bg-white rounded-lg shadow-lg overflow-hidden p-10 flex flex-col gap-y-10">
          <div className="grid grid-cols-2 gap-x-8">
            {/* 이미지 */}
            <section className="flex flex-col gap-y-2">
              {/* 메인 이미지 */}
              <div className="aspect-square w-full rounded-lg relative border border-gray-200 bg-gray-100 overflow-hidden">
                <img src={`${IMAGE_URL}/${auction.images[selectedImage].filePath}`} alt={auction.images[selectedImage].fileName} className="w-full h-full object-contain" />
              </div>

              {/* 이미지 슬라이드 */}
              <div className="flex p-2 gap-x-2 overflow-x-auto overflow-y-hidden image-scroll-container" ref={imageContainerRef} onMouseDown={onMouseDown} onMouseMove={onMouseMove} onMouseUp={onMouseUp} onMouseLeave={onMouseLeave}>
                {auction.images.map((image, index) => (
                  <div key={index} className={`w-24 h-24 flex-shrink-0 rounded-md overflow-hidden cursor-pointer ${selectedImage === index ? "ring-2 ring-blue-500" : "border border-gray-200"}`}>
                    <img src={`${IMAGE_URL}/${image.filePath}`} alt={image.fileName} className="w-full h-full object-cover" onClick={(e) => setSelectedImage(index)} onDragStart={(e) => e.preventDefault()} />
                  </div>
                ))}
              </div>
            </section>

            {/* 상품 정보 섹션 */}
            <section className="flex flex-col gap-y-8">
              {/* 상품 제목 */}
              <h1 className="text-2xl font-bold">{auction.title}</h1>

              {/* 가격 및 시간 정보 */}
              <div className="bg-gray-50 border rounded-lg shadow-md p-4">
                <div className="grid grid-cols-2 gap-6">
                  {/* 경매 완료 시 최종 거래가 표시 */}
                  {transaction && transaction.status === "COMPLETED" ? (
                    <div className="flex flex-col justify-between gap-y-4 border-b border-gray-300 p-2">
                      <div className="flex items-center gap-2">
                        <Gavel className="w-5 h-5 text-blue-500" />
                        <span className="font-bold text-lg text-gray-700">최종 거래가</span>
                      </div>
                      <span className={`font-bold text-blue-600 block text-right ${auction.buyNowPrice.toString().length > 12 ? "text-xl" : "text-2xl"}`}>{auction.buyNowPrice.toLocaleString()}원</span>
                    </div>
                  ) : (
                    // 현재 입찰가 표시
                    <div className="flex flex-col justify-between gap-y-4 border-b border-gray-300 p-2">
                      <div className="flex items-center gap-2">
                        <Gavel className="w-5 h-5 text-blue-500" />
                        <span className="font-bold text-lg text-gray-700">현재 입찰가</span>
                      </div>
                      <span className={`font-bold text-blue-600 block text-right ${auction.currentPrice.toString().length > 12 ? "text-xl" : "text-2xl"}`}>{auction.currentPrice.toLocaleString()}원</span>
                    </div>
                  )}

                  {/* 즉시 구매가 */}
                  <div className="flex flex-col justify-between gap-y-4 border-b border-gray-300 p-2">
                    <div className="flex items-center gap-2">
                      <Tag className="w-5 h-5 text-red-500" />
                      <span className="font-bold text-lg text-gray-700">즉시 구매가</span>
                    </div>
                    <span className={`font-bold text-red-600 block text-right ${auction.buyNowPrice.toString().length > 12 ? "text-xl" : "text-2xl"}`}>{auction.buyNowPrice > 0 ? `${auction.buyNowPrice.toLocaleString()}원` : "없음"}</span>
                  </div>

                  {/* 최종 낙찰자 */}
                  {transaction && transaction.status === "COMPLETED" ? (
                    <div className="flex flex-col justify-between gap-y-4 border-b border-gray-300 p-2">
                      <div className="flex items-center gap-x-2">
                        <User className="w-5 h-5 text-gray-500" />
                        <span className="font-bold text-lg text-gray-700">최종 낙찰자</span>
                      </div>
                      <span className={`font-semibold text-lg text-right ${transaction.userId === user.info.id ? "text-emerald-500" : ""}`}>{transaction.userId === user.info.id ? `${user.info.nickname} (본인)` : transaction.nickname}</span>
                    </div>
                  ) : (
                    // 최고 입찰자
                    <div className="flex flex-col justify-between gap-y-4 border-b border-gray-300 p-2">
                      <div className="flex items-center gap-x-2">
                        <User className="w-5 h-5 text-gray-500" />
                        <span className="font-bold text-lg text-gray-700">최고 입찰자</span>
                      </div>
                      <span className={`font-semibold text-lg text-right ${isHighestBidder ? "text-emerald-500" : ""}`}>{isHighestBidder ? `${user.info.nickname} (본인)` : highestBidderNickname}</span>
                    </div>
                  )}

                  {/* 남은 시간 */}
                  <div className="flex flex-col justify-between gap-y-4 border-b border-gray-300 p-2">
                    <div className="flex items-center gap-x-2">
                      <Clock className="w-5 h-5 text-black" />
                      <span className="font-bold text-lg text-gray-700">남은 시간</span>
                    </div>
                    <span className="font-bold text-xl block text-right">{formatTime(auction.auctionLeftTime)}</span>
                  </div>

                  {/* 입찰/관심 현황 */}
                  <div className="flex col-span-2 gap-x-4">
                    <div className="flex items-center gap-2">
                      <Gavel className="w-5 h-5 text-gray-500" />
                      <span className="text-gray-500">{auction.bidCount} 입찰</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <Eye className={`w-5 h-5 ${isWatched ? "text-red-500" : "text-gray-500"}`} />
                      <span className={isWatched ? "text-red-500" : "text-gray-500"}>{auction.watchCount} 관심</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* 입찰 영역 */}
              <div className="relative">
                {/* 경매 종료 시 오버레이 */}
                {!user.authenticated ? (
                  <Link to="/auth/login">
                    <BlurOverlay message="로그인 후 이용할 수 있습니다." />
                  </Link>
                ) : transaction && transaction.status === "COMPLETED" ? (
                  <BlurOverlay message="종료된 경매입니다." />
                ) : (
                  user.info.id === auction.userId && <BlurOverlay message="본인이 등록한 경매에는 입찰할 수 없습니다." />
                )}
                <div className="border rounded-lg shadow-md p-6">
                  <form onSubmit={handleBid} disabled={auction.auctionLeftTime <= 0 || auction.status === "ENDED"}>
                    <label htmlFor="bidAmount" className="block text-md font-bold text-gray-700 mb-3">
                      입찰금액
                    </label>
                    <div className="flex gap-x-2">
                      <input type="text" id="bidAmount" className="w-full min-w-0 flex-1 px-4 border border-gray-300 rounded-lg text-lg focus:outline-none focus:ring-2 focus:ring-blue-500" value={addCommas(bidAmount)} onChange={handleBidAmountChange} disabled={auction.auctionLeftTime <= 0} />
                      <button type="submit" className="flex-none px-8 py-3 bg-blue-500 text-white font-bold rounded-lg hover:bg-blue-600 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed" disabled={auction.auctionLeftTime <= 0}>
                        입찰하기
                      </button>
                    </div>
                    <InValidAlert inValid={inValid.bidAmount} message={inValid.bidAmount} className="mt-4" />
                    {showBidSuccess && <SuccessAlert message="입찰이 완료되었습니다." className="mt-4" />}
                    {showBuyNowSuccess && <SuccessAlert message="즉시 구매가 완료되었습니다." className="mt-4" />}
                  </form>
                  {auction.buyNowPrice > 0 && (
                    <button className="w-full py-3 mt-4 bg-rose-600 text-white font-bold text-lg rounded-lg hover:bg-rose-700 transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed" title={`${auction.buyNowPrice.toLocaleString()}원에 즉시 구매`} disabled={auction.auctionLeftTime <= 0} onClick={handleBuyNow}>
                      {addCommas(auction.buyNowPrice)}원에 즉시 구매하기
                    </button>
                  )}
                </div>
              </div>

              {/* 관심/공유 버튼 */}
              <div className="grid grid-cols-2 gap-4">
                <button
                  onClick={toggleWatch}
                  className={`flex items-center justify-center gap-2 py-2 rounded-lg border transition-all duration-200
                        ${isWatched ? "text-red-500 border-red-500" : "text-gray-600 border-gray-200 hover:text-red-500 hover:border-red-500"}`}
                >
                  <Heart className={`w-5 h-5 transition-transform duration-200 ${isWatched ? "fill-current scale-110" : "scale-100"}`} />
                  <span>{isWatched ? "관심 등록됨" : "관심 등록"}</span>
                </button>
                <div className="relative">
                  <button onClick={handleShare} className="w-full flex items-center justify-center gap-2 py-2 text-gray-600 hover:text-blue-500 rounded-lg border border-gray-200 hover:border-blue-500 transition-colors">
                    <Share2 className="w-5 h-5" />
                    <span>공유하기</span>
                  </button>
                  <div
                    className={`absolute -top-10 left-1/2 -translate-x-1/2 bg-gray-800 text-white px-3 py-1 rounded text-sm whitespace-nowrap
                          transition-all duration-300 transform
                          ${showCopyMessage ? "opacity-100 translate-y-0" : "opacity-0 translate-y-2 pointer-events-none"}`}
                  >
                    링크가 복사되었습니다
                    <div className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-2 h-2 bg-gray-800 rotate-45"></div>
                  </div>
                </div>
              </div>

              {/* 판매자 정보 */}
              <div className="border rounded-lg shadow-md px-4 py-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-x-4">
                    <img src={`${IMAGE_URL}/placeholder.svg`} alt="판매자 프로필" className="rounded-full w-14 h-14" />
                    <div>
                      <h2 className="font-semibold">{auction.nickname}</h2>
                      <div className="flex items-center">
                        <Star className="h-4 w-4 text-yellow-400 mr-1" />
                        <span className="text-sm">4.8 (거래 132회)</span>
                      </div>
                    </div>
                  </div>
                  <button className="border border-gray-300 text-gray-700 py-2 px-4 rounded-lg hover:bg-gray-50 transition duration-200">
                    <MessageSquareMore className="h-5 w-5 inline mr-2" />
                    문의하기
                  </button>
                </div>
              </div>
            </section>
          </div>

          {/* 차트 */}
          <div>
            <BidChart bidData={bidData} startPrice={auction?.startPrice} />
          </div>

          {/* 상품 설명 */}
          <div>
            <h2 className="text-2xl font-bold mb-4">상품 설명</h2>
            <p className="text-gray-600 mb-4">
              {auction.description.split("\r\n").map((line, index) => (
                <span key={index}>
                  {line}
                  <br />
                </span>
              ))}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
