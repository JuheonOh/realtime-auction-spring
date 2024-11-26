import { getNotifications } from "@apis/UserAPI";
import { SET_INFO } from "@data/redux/store/User";
import { AlertCircle, Bell, CheckCircle, Clock, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { deleteNotification, getNotificationStream, getUser, patchNotification, patchNotificationAll } from "../../../apis/UserAPI";

const NOTIFICATION = {
  BID: {
    icon: <CheckCircle className="h-5 w-5 text-green-500" />,
    message: "고객님의 입찰이 현재 최고가입니다.",
  },
  OUTBID: {
    icon: <AlertCircle className="h-5 w-5 text-red-500" />,
    message: "다른 사용자가 더 높은 가격을 입찰했습니다.",
  },
  WIN: {
    icon: <CheckCircle className="h-5 w-5 text-green-500" />,
    message: "축하합니다! 경매에서 낙찰되었습니다.",
  },
  REMINDER: {
    icon: <Clock className="h-5 w-5 text-blue-500" />,
    message: "관심 상품의 경매가 1시간 후 종료됩니다.",
  },
};

export default function NotificationComponent() {
  const dispatch = useDispatch();
  const user = useSelector((state) => state.user);

  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const notificationRef = useRef(null);

  // 읽지 않은 알림 개수
  const unreadCount = notifications.filter((n) => !n.isRead).length;

  // 유저 정보
  useEffect(() => {
    if (!user.authenticated) return;

    const fetchUser = async () => {
      const response = await getUser();
      dispatch(SET_INFO(response.data));
    };

    fetchUser();
  }, [user.authenticated, dispatch]);

  // 초기 알림 데이터 가져오기
  useEffect(() => {
    if (!user.authenticated) return;

    const fetchNotifications = async () => {
      try {
        const response = await getNotifications();
        setNotifications(
          response.data.map((notification) => ({
            ...notification,
            message: NOTIFICATION[notification.type].message,
          }))
        );
      } catch (err) {
        console.error(err);
      }
    };

    fetchNotifications();
  }, [user.authenticated]);

  // 알림 스트림 가져오기
  useEffect(() => {
    if (!user.info.id) return;

    let eventSource = null;
    const fetchNotificationsStream = () => {
      try {
        if (eventSource) {
          eventSource.close();
        }

        // SSE 연결
        eventSource = getNotificationStream(user.info.id);

        // 연결 성공 이벤트
        eventSource.addEventListener("connect", (e) => {
          console.log(e.data);
        });

        // 알림 이벤트
        eventSource.addEventListener("notification", (e) => {
          let notification = JSON.parse(e.data);
          notification.message = NOTIFICATION[notification.type].message;
          setNotifications((prev) => [notification, ...prev]);
        });

        // SSE 연결 유지 이벤트
        eventSource.addEventListener("ping", (e) => {});

        // SSE 연결 에러 이벤트
        eventSource.addEventListener("error", (e) => {});

        // SSE 연결 타임아웃 이벤트
        eventSource.addEventListener("timeout", (e) => {});
      } catch (err) {
        console.error(err);

        setTimeout(() => {
          console.log("계정 알림 스트림 재연결 시도");
          fetchNotificationsStream();
        }, 1000);
      }

      return () => {
        if (eventSource) {
          eventSource.close();
          eventSource = null;
        }
      };
    };

    fetchNotificationsStream();
  }, [user.info.id]);

  // 외부 클릭 이벤트 처리
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

  const markAllAsRead = async () => {
    try {
      setNotifications(notifications.map((notification) => ({ ...notification, isRead: true })));
      await patchNotificationAll();
    } catch (err) {
      console.error(err);
    }
  };

  const markAsRead = async (id) => {
    try {
      setNotifications(notifications.map((notification) => (notification.id === id ? { ...notification, isRead: true } : notification)));
      await patchNotification(id);
    } catch (err) {
      console.error(err);
    }
  };

  const removeNotification = async (id) => {
    try {
      setNotifications(notifications.filter((notification) => notification.id !== id));
      await deleteNotification(id);
    } catch (err) {
      console.error(err);
    }
  };

  const getIcon = (type) => {
    return NOTIFICATION[type]?.icon || <Bell className="h-5 w-5 text-gray-500" />;
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
              {notifications.length > 0 && (
                <button className="text-sm text-blue-600 hover:text-blue-800" onClick={markAllAsRead}>
                  모두 읽음 표시
                </button>
              )}
            </div>
            {!user.authenticated ? (
              <div className="px-4 py-2 text-gray-500 text-center">로그인 후 알림을 확인할 수 있습니다.</div>
            ) : notifications.length === 0 ? (
              <div className="px-4 py-2 text-gray-500 text-center">새로운 알림이 없습니다.</div>
            ) : (
              <div className="max-h-96 overflow-y-auto">
                {notifications.map((notification) => (
                  <div key={notification.id} className={`px-4 py-2 hover:bg-gray-50 flex items-start ${notification.isRead ? "opacity-50" : ""}`}>
                    <div className="flex-shrink-0 mr-3">{getIcon(notification.type)}</div>
                    <div className="flex-grow">
                      <div className="text-sm">{notification.message}</div>
                      <div className="text-xs text-gray-500 mt-1">{notification.time}</div>
                    </div>
                    <div className="flex-shrink-0 ml-2">
                      {!notification.isRead && (
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
