import { IMAGE_URL } from "@utils/constant";

export default function Tooltip(props) {
  const { detailNotification } = props;
  const { auctionInfo, myBidInfo, previousBidInfo } = detailNotification;

  // {
  //   "id": 244,
  //   "type": "OUTBID",
  //   "isRead": false,
  //   "time": "2일 전",
  //   "auctionInfo": {
  //       "id": 136,
  //       "title": "경매 종료 스케쥴러 테스트용2",
  //       "currentPrice": 104000,
  //       "auctionLeftTime": 792074,
  //       "successfulPrice": null,
  //       "filePath": "b5c63ac6-9fd7-442e-8b79-d63c6531209a_frontend roadmap.png",
  //       "fileName": "frontend roadmap.png"
  //   },
  //   "myBidInfo": {
  //       "id": 615,
  //       "bidAmount": 99000
  //   },
  //   message: "다른 사용자가 더 높은 가격을 입찰했습니다."
  // }

  const detailContent = (notificationType) => {
    let content = null;

    switch (notificationType) {
      case "BID":
        let previousBidInfoContent = null;

        if (previousBidInfo) {
          previousBidInfoContent = (
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-gray-600">이전 입찰가</span>
              <span className="text-gray-600">{previousBidInfo?.bidAmount.toLocaleString()} 원</span>
            </div>
          );
        }

        content = (
          <>
            {previousBidInfoContent}
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-blue-600">내가 입찰한 가격</span>
              <span className="text-blue-600">{myBidInfo?.bidAmount.toLocaleString()} 원</span>
            </div>
          </>
        );
        break;
      case "OUTBID":
        content = (
          <>
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-rose-600">현재 입찰가</span>
              <span className="text-rose-600">{auctionInfo?.currentPrice.toLocaleString()} 원</span>
            </div>
            <div className="flex font-bold justify-between items-center">
              <span className="text-sm text-blue-600">내가 입찰한 가격</span>
              <span className="text-blue-600">{myBidInfo?.bidAmount.toLocaleString()} 원</span>
            </div>
          </>
        );
        break;
      case "WIN":
        content = (
          <div className="flex font-bold justify-between items-center">
            <span className="text-lg text-green-600">낙찰된 가격</span>
            <span className="text-lg text-green-600">{auctionInfo?.successfulPrice.toLocaleString()} 원</span>
          </div>
        );
        break;
      case "REMINDER":
        content = (
          <>
            <div className="flex font-bold justify-between items-center">
              <span className="text-lg text-blue-600">현재 가격</span>
              <span className="text-lg text-blue-600">{auctionInfo?.currentPrice.toLocaleString()} 원</span>
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
          <div className="flex justify-between items-center text-sm">
            <span className="text-gray-900">종료 시간</span>
            <span className="text-gray-900">{new Date(auctionInfo.auctionEndTime).toLocaleString()}</span>
          </div>
        </div>
      </div>
    )
  );
}
