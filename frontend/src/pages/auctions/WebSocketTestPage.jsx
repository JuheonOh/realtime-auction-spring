import { WS_BASE_URL } from "@utils/constant";
import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

export default function WebSocketTestPage() {
  const { auctionId } = useParams();
  const [socket, setSocket] = useState(null);

  const sendMessage = () => {
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(auctionId);
    }
  };

  useEffect(() => {
    const newSocket = new WebSocket(`${WS_BASE_URL}/api/auctions/${auctionId}/ws`);

    newSocket.onopen = (event) => {
      console.log("소켓 연결 성공", event);
    };

    newSocket.onmessage = (event) => {
      console.log("소켓 메시지 수신", event);
      console.log(event.data);
    };

    newSocket.onerror = (event) => {
      console.error("소켓 에러", event);
    };

    newSocket.onclose = (event) => {
      console.log("소켓 연결 종료", event);
    };

    setSocket(newSocket);

    // 컴포넌트 언마운트 시 웹 소켓 연결 해제
    return () => {
      if (newSocket) newSocket.close();
    };
  }, [auctionId]);

  return (
    <div className="bg-gray-100 min-h-screen py-8">
      <div className="max-w-[1280px] mx-auto px-8">
        <div className="bg-white rounded-lg shadow-lg overflow-hidden p-10 flex flex-col gap-y-10">
          <button className="bg-blue-500 text-white px-4 py-2 rounded-md" onClick={sendMessage}>
            전송
          </button>
        </div>
      </div>
    </div>
  );
}
