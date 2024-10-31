import { AlertCircle, Bell, CheckCircle, Clock, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";

const mockNotifications = [
  { id: 1, type: "bid", message: "귀하의 입찰이 현재 최고가입니다.", time: "5분 전", read: false },
  { id: 2, type: "outbid", message: "다른 사용자가 더 높은 가격을 입찰했습니다.", time: "15분 전", read: false },
  { id: 3, type: "win", message: "축하합니다! 경매에서 낙찰되었습니다.", time: "1시간 전", read: true },
  { id: 4, type: "reminder", message: "관심 상품의 경매가 1시간 후 종료됩니다.", time: "2시간 전", read: true },
];

export default function NotificationComponent() {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState(mockNotifications);
  const notificationRef = useRef(null);

  const unreadCount = notifications.filter((n) => !n.read).length;

  useEffect(() => {
    function handleClickOutside(event) {
      if (notificationRef.current && !notificationRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const markAsRead = (id) => {
    setNotifications(notifications.map((n) => (n.id === id ? { ...n, read: true } : n)));
  };

  const removeNotification = (id) => {
    setNotifications(notifications.filter((n) => n.id !== id));
  };

  const getIcon = (type) => {
    switch (type) {
      case "bid":
      case "win":
        return <CheckCircle className="h-5 w-5 text-green-500" />;
      case "outbid":
        return <AlertCircle className="h-5 w-5 text-red-500" />;
      case "reminder":
        return <Clock className="h-5 w-5 text-blue-500" />;
      default:
        return <Bell className="h-5 w-5 text-gray-500" />;
    }
  };

  return (
    <div className="relative" ref={notificationRef}>
      <button className="p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors relative" onClick={() => setIsOpen(!isOpen)}>
        <Bell className="h-6 w-6 text-gray-600" />
        {unreadCount > 0 && <span className="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-red-500 rounded-full">{unreadCount}</span>}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-md shadow-lg overflow-hidden z-10">
          <div className="py-2">
            <div className="px-4 py-2 bg-gray-100 text-gray-800 font-semibold flex justify-between items-center">
              <span>알림</span>
              <button className="text-sm text-blue-600 hover:text-blue-800" onClick={() => setNotifications(notifications.map((n) => ({ ...n, read: true })))}>
                모두 읽음 표시
              </button>
            </div>
            {notifications.length === 0 ? (
              <div className="px-4 py-2 text-gray-500 text-center">새로운 알림이 없습니다.</div>
            ) : (
              <div className="max-h-96 overflow-y-auto">
                {notifications.map((notification) => (
                  <div key={notification.id} className={`px-4 py-2 hover:bg-gray-50 flex items-start ${notification.read ? "opacity-50" : ""}`}>
                    <div className="flex-shrink-0 mr-3">{getIcon(notification.type)}</div>
                    <div className="flex-grow">
                      <div className="text-sm">{notification.message}</div>
                      <div className="text-xs text-gray-500 mt-1">{notification.time}</div>
                    </div>
                    <div className="flex-shrink-0 ml-2">
                      {!notification.read && (
                        <button className="text-blue-500 hover:text-blue-700 text-xs" onClick={() => markAsRead(notification.id)}>
                          읽음
                        </button>
                      )}
                      <button className="ml-2 text-gray-400 hover:text-gray-600" onClick={() => removeNotification(notification.id)}>
                        <X className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
