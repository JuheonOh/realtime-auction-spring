import { getAuctionDetail } from "@apis/AuctionAPI";
import httpClientManager from "@apis/HttpClientManager";
import { getUser } from "@apis/UserAPI";
import InValidAlert from "@components/common/alerts/InValidAlert";
import SuccessAlert from "@components/common/alerts/SuccessAlert";
import BlurOverlay from "@components/common/loading/BlurOverlay";
import LoadingSpinner from "@components/common/loading/LoadingSpinner";
import ImageSlider from "@components/common/sliders/ImageSlider";
import BidChart from "@components/features/auction/BidChart";
import { SET_ACCESS_TOKEN, SET_INFO } from "@data/redux/store/User";
import { clearCookie } from "@data/storage/Cookie";
import useInterval from "@hooks/useInterval";
import { IMAGE_URL, WS_BASE_URL } from "@utils/constant";
import { addCommas, formatNumberInput } from "@utils/formatNumber";
import formatTime from "@utils/formatTime";
import { Clock, Eye, Gavel, Heart, MessageSquareMore, Share2, Star, Tag, User } from "lucide-react";
import { useCallback, useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Link, useNavigate, useParams } from "react-router-dom";
import { toggleFavorite } from "@apis/AuctionAPI";

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

export default function AuctionDetailPage() {
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

  const [isFavorite, setIsFavorite] = useState(false);

  const [showCopyMessage, setShowCopyMessage] = useState(false);
  const [showBidSuccess, setShowBidSuccess] = useState(false);

  const [inValid, setInValid] = useState({});
  const [highestBidderNickname, setHighestBidderNickname] = useState("없음");
  const [isHighestBidder, setIsHighestBidder] = useState(false);

  const socketRef = useRef(null);
  const userIdRef = useRef(null);

  const navigate = useNavigate();

  // 계정 정보 불러오기
  useEffect(() => {
    const fetchUser = async () => {
      if (!user.accessToken) return;
      try {
        const userInfo = await getUser();
        dispatch(SET_INFO(userInfo.data));
        userIdRef.current = userInfo.data.id;
      } catch (err) {
        console.error(err);
        clearCookie();
      }
    };

    fetchUser();
  }, [user?.accessToken, dispatch]);

  // 경매 정보 불러오기
  const fetchInitialData = useCallback(async () => {
    try {
      const auctionDetail = await getAuctionDetail(auctionId);

      if (auctionDetail.data) {
        setAuction(auctionDetail.data);
        setIsFavorite(auctionDetail.data.isFavorite);
      }

      if (auctionDetail.data.transaction) {
        setTransaction(auctionDetail.data.transaction);
      }

      const bids = auctionDetail.data.bids.map((data) => ({ ...data, bidTime: new Date(data.bidTime) }));
      if (bids.length > 0) {
        setBidData(bids);
        setHighestBidderNickname(bids[bids.length - 1].nickname);
      }
    } catch (err) {
      console.error(err);
      setIsError(true);
    } finally {
      setIsLoading(false);
    }
  }, [auctionId]);

  // 최고입찰자 상태 업데이트
  useEffect(() => {
    if (bidData.length > 0 && user?.info?.id) {
      setIsHighestBidder(bidData[bidData.length - 1].userId === user.info.id);
    }
  }, [bidData, user?.info?.id]);

  // 1초마다 시간 감소
  useInterval(() => {
    if (auction?.auctionLeftTime > 0) {
      setAuction((prev) => ({ ...prev, auctionLeftTime: prev.auctionLeftTime - 1 }));
    }
  }, 1000);

  // 경매 입찰 소켓 연결
  useEffect(() => {
    if (!auctionId) return;

    const retryInterval = 3000; // 재시도 간격 3초

    const setupWebSocket = () => {
      try {
        // 이전 웹소켓이 있다면 연결 해제
        if (socketRef.current) {
          socketRef.current.close();
        }

        socketRef.current = new WebSocket(`${WS_BASE_URL}/ws/auctions/${auctionId}`);

        socketRef.current.onopen = (e) => {
          console.log("경매 입찰 소켓 연결 성공");
        };

        socketRef.current.onclose = (e) => {
          console.log("경매 입찰 소켓 연결 종료");

          // 정상적인 종료가 아닌 경우 재연결 시도
          if (!e.wasClean) {
            console.log(`${retryInterval / 1000}초 후 재연결 시도`);
            setTimeout(() => {
              setupWebSocket();
            }, retryInterval);
          }
        };

        socketRef.current.onerror = (e) => {};
      } catch (err) {
        console.error("웹소켓 설정 중 에러: ", err);

        // 입찰 소켓 재연결
        setTimeout(() => {
          setupWebSocket();
        }, retryInterval);
      }
    };

    setupWebSocket();
  }, [auctionId]);

  useEffect(() => {
    socketRef.current.onmessage = async (e) => {
      const res = JSON.parse(e.data);
      const type = res.type;
      const status = res.status;
      const data = res.data;

      // 본인의 액세스 토큰이 만료되었을 때 (token_expired)
      if (type === "token_expired") {
        try {
          // 리프레쉬 토큰으로 새 액세스 토큰 갱신
          const newAccessToken = await httpClientManager.refreshAccessToken();

          // 새 액세스 토큰으로 user store 업데이트
          dispatch(SET_ACCESS_TOKEN(newAccessToken));

          const message = {
            type: "bid",
            data: { bidAmount },
            accessToken: newAccessToken,
          };

          // 새 액세스 토큰으로 웹소켓 메시지 재전송
          socketRef.current.send(JSON.stringify(message));
        } catch (err) {
          console.log(err);
        }

        return;
      }

      // 남은 시간 데이터 받았을 때 (time)
      if (type === "time") {
        const auctionLeftTime = data.auctionLeftTime > 0 ? data.auctionLeftTime : 0;
        setAuction((prev) => ({ ...prev, auctionLeftTime }));
      }

      // 입찰 데이터 받았을 때 (bid)
      if (type === "bid") {
        const bidData = data?.bidData;

        // 입찰 실패 메시지
        if (status === 400) {
          setInValid({ bidAmount: data.message });
          setShowBidSuccess(false);
          return;
        }

        // 유효성 알림 초기화
        setInValid({});

        // 최고입찰자 닉네임 업데이트
        setHighestBidderNickname(bidData.nickname);

        // 입찰 성공이고 본인이 입찰한 경우 성공 알림 표시
        if (status === 201 && bidData.userId === userIdRef.current) {
          setShowBidSuccess(true);
          setIsHighestBidder(true);
        } else {
          // 입찰 성공 메시지 초기화
          setShowBidSuccess(false);
          setIsHighestBidder(false);
        }

        setBidData((prev) => [
          ...prev, // 이전 입찰 데이터 유지
          {
            id: bidData.id, // 입찰 id
            userId: bidData.userId, // 입찰자 id
            nickname: bidData.nickname, // 입찰자 닉네임
            bidAmount: bidData.bidAmount, // 입찰 금액
            bidTime: new Date(bidData.bidTime), // 입찰 시간
          },
        ]);
        setAuction((prev) => ({
          ...prev, // 이전 상태 유지
          currentPrice: bidData.bidAmount, // 현재 입찰가 업데이트
          bidCount: prev.bidCount + 1, // 입찰 횟수 업데이트
          auctionLeftTime: bidData.auctionLeftTime, // 남은 시간 업데이트
        }));
      }

      // 즉시 구매 데이터 받았을 때 (buy-now)
      if (type === "buy-now") {
        const buyNowData = data?.buyNowData;

        setTransaction((prev) => ({
          ...prev,
          userId: buyNowData.userId,
          nickname: buyNowData.nickname,
          status: buyNowData.status,
          finalPrice: buyNowData.buyNowPrice,
        }));

        setAuction((prev) => ({
          ...prev,
          status: "ENDED",
          auctionLeftTime: 0,
          successfulPrice: buyNowData.buyNowPrice,
        }));
      }

      // 경매 종료 데이터 받았을 때 (ended)
      if (type === "ended") {
        const transactionData = data?.transactionData;

        setTransaction((prev) => ({
          ...prev,
          userId: transactionData.userId,
          nickname: transactionData.nickname,
          status: transactionData.status,
          finalPrice: transactionData.finalPrice,
        }));
      }

      // error 데이터 받았을 때 (error)
      if (type === "error") {
        setInValid({ bidAmount: data.message });
        setShowBidSuccess(false);
      }
    };
  }, [auctionId, user.accessToken, dispatch, bidAmount]);

  // 초기 데이터 불러오기
  useEffect(() => {
    fetchInitialData();
  }, [fetchInitialData]);

  // 본인이 최고입찰자인 경우 최고입찰자 상태 업데이트
  useEffect(() => {
    if (bidData.length === 0) return;

    if (user?.info?.id) {
      setIsHighestBidder(bidData[bidData.length - 1].userId === user.info.id);
    }
  }, [bidData, user?.info?.id]);

  // 입찰 금액 단위 업데이트
  useEffect(() => {
    if (!auction?.startPrice || !auction?.currentPrice) return;

    // 첫 입찰자인 경우 시작가로 설정, 아니면 현재가 + 입찰단위
    setBidAmount(bidData.length === 0 ? auction.currentPrice : auction.currentPrice + bidUnit(auction.currentPrice));
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
      const message = {
        type: "bid",
        accessToken: user.accessToken,
        data: {
          bidAmount,
        },
      };

      // 입찰 요청
      socketRef.current.send(JSON.stringify(message));
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
        const message = {
          type: "buy-now",
          accessToken: user.accessToken,
        };

        socketRef.current.send(JSON.stringify(message));
      } catch (err) {
        setInValid({ bidAmount: err.response.data.message });
        console.error(err);
      }
    }
  };

  // 관심 등록 버튼 클릭 시 관심 등록 상태 업데이트
  const handleFavorite = () => {
    if (!user.authenticated) {
      return navigate("/auth/login");
    }

    setIsFavorite(!isFavorite);
    setAuction((prev) => ({
      ...prev,
      favoriteCount: prev.favoriteCount + (isFavorite ? -1 : 1),
    }));

    toggleFavorite(auctionId);
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

  return isLoading || isError ? (
    <div className="h-screen">
      <LoadingSpinner isLoading={isLoading} isError={isError} message="경매 상세" />
    </div>
  ) : (
    <div className="max-w-[1280px] mx-auto p-8">
      <div className="bg-white rounded-lg shadow-lg overflow-hidden p-10 flex flex-col gap-y-10">
        <div className="grid grid-cols-2 gap-x-8">
          {/* 이미지 */}
          <section className="flex flex-col gap-y-2">
            {/* 메인 이미지 */}
            <div className="aspect-square w-full rounded-lg relative border border-gray-200 bg-gray-100 overflow-hidden">
              <img src={`${IMAGE_URL}/${auction.images[selectedImage].filePath}`} alt={auction.images[selectedImage].fileName} className="w-full h-full object-contain" />
            </div>

            {/* 이미지 슬라이드 */}
            <ImageSlider
              images={auction.images.map((img) => ({
                filePath: `${IMAGE_URL}/${img.filePath}`,
                fileName: img.fileName,
              }))}
              selectedImage={selectedImage}
              onSelectImage={setSelectedImage}
            />
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
                    <span className={`font-bold text-blue-600 block text-right ${transaction.finalPrice.toString().length > 12 ? "text-xl" : "text-2xl"}`}>{transaction.finalPrice.toLocaleString()}원</span>
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
                    <Eye className={`w-5 h-5 ${isFavorite ? "text-red-500" : "text-gray-500"}`} />
                    <span className={isFavorite ? "text-red-500" : "text-gray-500"}>{auction.favoriteCount} 관심</span>
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
              ) : transaction && transaction.status === "COMPLETED" && transaction.userId === user.info.id ? (
                <BlurOverlay message="축하합니다! 낙찰되었습니다." className="text-blue-600" />
              ) : auction.auctionLeftTime <= 0 || (transaction && transaction.userId !== user.info.id) ? (
                <BlurOverlay message="종료된 경매입니다." className="text-gray-700" />
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
                onClick={handleFavorite}
                className={`flex items-center justify-center gap-2 py-2 rounded-lg border transition-all duration-200
                        ${isFavorite ? "text-red-500 border-red-500" : "text-gray-600 border-gray-200 hover:text-red-500 hover:border-red-500"}`}
              >
                <Heart className={`w-5 h-5 transition-transform duration-200 ${isFavorite ? "fill-current scale-110" : "scale-100"}`} />
                <span>{isFavorite ? "관심 등록됨" : "관심 등록"}</span>
              </button>
              <div className="relative">
                <button onClick={handleShare} className="w-full flex items-center justify-center gap-2 py-2 text-gray-600 hover:text-blue-500 rounded-lg border border-gray-200 hover:border-blue-500 transition-colors">
                  <Share2 className="w-5 h-5" />
                  <span>공유하기</span>
                </button>
                <div
                  className={`absolute -top-10 left-1/2 -translate-x-1/2 bg-gray-800 text-white px-3 py-1 rounded text-sm whitespace-nowrap
                          transition-all duration-300 transform
                          ${showCopyMessage ? "opacity-100 translate-y-0 z-10" : "opacity-0 translate-y-2 pointer-events-none"}`}
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
                    {/* <div className="flex items-center">
                      <Star className="h-4 w-4 text-yellow-400 mr-1" />
                      <span className="text-sm">4.8 (거래 132회)</span>
                    </div> */}
                  </div>
                </div>
                {/* <button className="border border-gray-300 text-gray-700 py-2 px-4 rounded-lg hover:bg-gray-50 transition duration-200">
                  <MessageSquareMore className="h-5 w-5 inline mr-2" />
                  문의하기
                </button> */}
              </div>
            </div>
          </section>
        </div>

        {/* 차트 */}
        <div>
          <BidChart bidData={bidData} startPrice={auction?.startPrice} userId={userIdRef.current} />
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
  );
}
