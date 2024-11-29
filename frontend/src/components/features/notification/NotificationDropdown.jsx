import { SET_INFO } from "@data/redux/store/User";
import { AlertCircle, Bell, CheckCircle, Clock, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Link } from "react-router-dom";
import httpClientManager from "../../../apis/HttpClientManager";
import { deleteNotification, deleteNotificationAll, getNotificationStream, getUser, patchNotification, patchNotificationAll } from "../../../apis/UserAPI";
import { SET_ACCESS_TOKEN } from "../../../data/redux/store/User";
import Tooltip from "./Tooltip";

const NOTIFICATION = {
  BID: {
    icon: <CheckCircle className="h-5 w-5 text-green-500" />,
    message: "고객님의 입찰이 현재 최고가 입니다.",
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

  const [detailNotification, setDetailNotification] = useState(null);

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
        eventSource = getNotificationStream(user.accessToken, user.info.id);

        // 연결 성공 이벤트
        eventSource.addEventListener("connect", (e) => {
          const notifications = JSON.parse(e.data);

          console.log(`(사용자 ID : ${user.info.id}) 알림 스트림 연결 성공`);

          setNotifications(
            notifications.map((notification) => ({
              ...notification,
              message: NOTIFICATION[notification.type].message,
            }))
          );
        });

        // 알림 이벤트
        eventSource.addEventListener("notification", (e) => {
          const notification = JSON.parse(e.data);

          setNotifications((prev) => {
            // BID 타입일 경우 같은 경매의 OUTBID 알림 제거
            let filterNotifications = [...prev];
            if (notification.type === "BID") {
              filterNotifications = filterNotifications.filter((n) => n.type !== "OUTBID" || n.auctionInfo.id !== notification.auctionInfo.id);
            }

            // 동일한 type과 auctionId를 가진 알림이 있는지 확인
            const duplicateIndex = filterNotifications.findIndex(
              (n) => n.type === notification.type && n.auctionInfo.id === notification.auctionInfo.id
            );

            // 중복된 알림이 없는 경우 새로운 알림 추가
            if (duplicateIndex === -1) {
              return [
                {
                  ...notification,
                  message: NOTIFICATION[notification.type].message,
                },
                ...filterNotifications,
              ];
            }

            // 중복된 알림이 있는 경우 해당 알림 업데이트
            const updatedNotifications = [...filterNotifications];
            updatedNotifications[duplicateIndex] = {
              ...notification,
              message: NOTIFICATION[notification.type].message,
            };
            
            // 업데이트된 알림을 맨 앞으로 이동
            updatedNotifications.splice(duplicateIndex, 1);

            // 새로운 알림을 맨 앞에 추가
            return [
              {
                ...notification,
                message: NOTIFICATION[notification.type].message,
              },
              ...updatedNotifications,
            ];
          });
        });

        // SSE 연결 유지 이벤트
        eventSource.addEventListener("ping", (e) => {});

        // SSE 연결 에러 이벤트
        eventSource.addEventListener("error", async (res) => {
          if (res.status === 401) {
            try {
              console.log("토큰 만료");
              let newAccessToken = await httpClientManager.refreshAccessToken();
              dispatch(SET_ACCESS_TOKEN(newAccessToken));
              console.log("계정 알림 스트림 재연결 시도");
            } catch (err) {
              console.log(err);
            }
          }
        });

        // SSE 연결 타임아웃 이벤트
        eventSource.addEventListener("timeout", (e) => {});
      } catch (err) {
        console.error(err);

        setTimeout(() => {
          console.log("계정 알림 스트림 재연결 시도");
          fetchNotificationsStream();
        }, 1000);
      }
    };

    fetchNotificationsStream();

    return () => {
      if (eventSource) {
        eventSource.close();
        eventSource = null;
      }
    };
  }, [user.info.id, user.accessToken, dispatch]);

  // 외부 클릭 이벤트 처리
  useEffect(() => {
    function handleClickOutside(event) {
      if (notificationRef.current && !notificationRef.current.contains(event.target)) {
        setIsOpen(false);
        setDetailNotification(null);
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

  const removeAllNotifications = async () => {
    try {
      setNotifications([]);
      await deleteNotificationAll();
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

  const handleNotificationClick = (notification) => {
    markAsRead(notification.id);
    setIsOpen(false);
  };

  const handleMouseEnter = (notification) => {
    setDetailNotification(notification);
  };

  return (
    <div className="relative" ref={notificationRef}>
      <button className="p-2 rounded-full bg-gray-200 hover:bg-gray-300 transition-colors relative" onClick={() => setIsOpen(!isOpen)}>
        <Bell className="h-6 w-6 text-gray-600" />
        {unreadCount > 0 && <span className="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none text-white transform translate-x-1/2 -translate-y-1/2 bg-red-500 rounded-full">{unreadCount}</span>}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-md shadow-lg z-10">
          <div className="px-4 py-2 bg-gray-100 text-gray-800 font-semibold flex justify-between items-center">
            <span>알림</span>
            {notifications.length > 0 && (
              <div className="flex gap-2">
                <button className="text-sm text-blue-600 hover:text-blue-800" onClick={removeAllNotifications}>
                  전체 삭제
                </button>
                <button className="text-sm text-blue-600 hover:text-blue-800" onClick={markAllAsRead}>
                  모두 읽음
                </button>
              </div>
            )}
          </div>
          {!user.authenticated ? (
            <div className="py-2 text-gray-500 text-center">로그인 후 알림을 확인할 수 있습니다.</div>
          ) : notifications.length === 0 ? (
            <div className="py-2 text-gray-500 text-center">새로운 알림이 없습니다.</div>
          ) : (
            <div className="max-h-96 overflow-y-auto">
              {detailNotification && <Tooltip detailNotification={detailNotification} />}
              {notifications.map((notification) => (
                <Link to={`/auctions/${notification.auctionInfo.id}`} key={notification.id} className={`px-2 py-2 hover:bg-blue-50 flex items-start ${notification.isRead ? "opacity-50" : ""}`} onClick={() => handleNotificationClick(notification)} onMouseEnter={() => handleMouseEnter(notification)}>
                  <div className="px-2 flex-shrink-0 justify-self-center self-center">{getIcon(notification.type)}</div>
                  <div className="px-1 flex-grow">
                    <div className="text-sm">{notification.message}</div>
                    <div className="text-xs text-gray-500 mt-1">{notification.time}</div>
                  </div>
                  <div className="px-1 flex-shrink-0">
                    <button
                      className="text-gray-400 hover:text-gray-600"
                      onClick={(e) => {
                        e.preventDefault();
                        e.stopPropagation();
                        removeNotification(notification.id);
                      }}
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
