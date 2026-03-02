import { IMAGE_URL } from "@utils/constant";

export default function Tooltip(props) {
  const { detailNotification } = props;
  const { auctionInfo, myBidInfo, previousBidInfo } = detailNotification;

  const formatPrice = (price) => {
    if (price === null || price === undefined) return "-";
    return `${price.toLocaleString()} 원`;
  };

  const detailContent = (notificationType) => {
    let content = null;

    switch (notificationType) {
      case "BID":
        let previousBidInfoContent = null;

        if (previousBidInfo) {
          previousBidInfoContent = (
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-gray-600">이전 입찰가</span>
              <span className="text-gray-600">{formatPrice(previousBidInfo?.bidAmount)}</span>
            </div>
          );
        }

        content = (
          <>
            {previousBidInfoContent}
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-blue-600">내가 입찰한 가격</span>
              <span className="text-blue-600">{formatPrice(myBidInfo?.bidAmount)}</span>
            </div>
          </>
        );
        break;
      case "OUTBID":
        content = (
          <>
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-rose-600">현재 입찰가</span>
              <span className="text-rose-600">{formatPrice(auctionInfo?.currentPrice)}</span>
            </div>
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-blue-600">내가 입찰한 가격</span>
              <span className="text-blue-600">{formatPrice(myBidInfo?.bidAmount)}</span>
            </div>
          </>
        );
        break;
      case "WIN":
      case "BUY_NOW_WIN":
        content = (
          <div className="flex font-bold justify-between items-center">
            <span className="text-sm text-emerald-600">낙찰된 가격</span>
            <span className="text-emerald-600">{formatPrice(auctionInfo?.successfulPrice)}</span>
          </div>
        );
        break;
      case "REMINDER":
        content = (
          <>
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-blue-600">현재 가격</span>
              <span className="text-blue-600">{formatPrice(auctionInfo?.currentPrice)}</span>
            </div>
          </>
        );
        break;
      case "ENDED":
      case "ENDED_TIME":
        content = (
          <>
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-rose-600">낙찰된 가격</span>
              <span className="text-rose-600">{formatPrice(auctionInfo?.successfulPrice)}</span>
            </div>
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-blue-600">내가 입찰한 가격</span>
              <span className="text-blue-600">{formatPrice(myBidInfo?.bidAmount)}</span>
            </div>
          </>
        );
        break;
      default:
        break;
    }

    return content;
  };

  return (
    detailNotification && (
      <div className="absolute right-full top-0 z-50 w-64 mr-2 rounded-md shadow-lg bg-gray-50 border border-gray-300">
        <h3 className="px-3 py-2 bg-gray-100 border-b rounded font-bold">{auctionInfo.title}</h3>
        <div className="flex flex-col gap-y-2 text-black p-3">
          <div className="w-full h-48 border border-gray-300 rounded-md bg-white">
            <img src={`${IMAGE_URL}/${auctionInfo.filePath}`} alt={auctionInfo.fileName} className="w-full h-full object-contain" />
          </div>
          {detailContent(detailNotification.type)}
          <div className="flex justify-between text-sm">
            <span className="text-gray-900">경매 종료 시간</span>
            <span className="text-xs text-end text-gray-900">
              {new Date(auctionInfo.auctionEndTime).toLocaleString("ko-KR", { year: "numeric", month: "long", day: "numeric" })}
              <br />
              {new Date(auctionInfo.auctionEndTime).toLocaleString("ko-KR", { hour: "2-digit", minute: "2-digit" })}
            </span>
          </div>
        </div>
      </div>
    )
  );
}
