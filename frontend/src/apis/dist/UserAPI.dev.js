"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.deleteNotification = exports.patchNotification = exports.patchNotificationAll = exports.getNotificationStream = exports.getNotifications = exports.getUser = void 0;

var _constant = require("@utils/constant");

var _eventSourcePolyfill = require("event-source-polyfill");

var _HttpClientManager = _interopRequireDefault(require("./HttpClientManager"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

// API 인스턴스 생성
var UserApi = _HttpClientManager["default"].createApiInstance();

var getUser = function getUser() {
  var response;
  return regeneratorRuntime.async(function getUser$(_context) {
    while (1) {
      switch (_context.prev = _context.next) {
        case 0:
          _context.prev = 0;
          _context.next = 3;
          return regeneratorRuntime.awrap(UserApi.get("/api/users"));

        case 3:
          response = _context.sent;
          return _context.abrupt("return", response);

        case 7:
          _context.prev = 7;
          _context.t0 = _context["catch"](0);
          throw _context.t0;

        case 10:
        case "end":
          return _context.stop();
      }
    }
  }, null, null, [[0, 7]]);
};

exports.getUser = getUser;

var getNotifications = function getNotifications() {
  var response;
  return regeneratorRuntime.async(function getNotifications$(_context2) {
    while (1) {
      switch (_context2.prev = _context2.next) {
        case 0:
          _context2.prev = 0;
          _context2.next = 3;
          return regeneratorRuntime.awrap(UserApi.get("/api/users/notifications"));

        case 3:
          response = _context2.sent;
          return _context2.abrupt("return", response);

        case 7:
          _context2.prev = 7;
          _context2.t0 = _context2["catch"](0);
          throw _context2.t0;

        case 10:
        case "end":
          return _context2.stop();
      }
    }
  }, null, null, [[0, 7]]);
};

exports.getNotifications = getNotifications;

var getNotificationStream = function getNotificationStream(token, userId) {
  try {
    var EventSource = _eventSourcePolyfill.EventSourcePolyfill || _eventSourcePolyfill.NativeEventSource;
    var eventSource = new EventSource("".concat(_constant.API_BASE_URL, "/api/users/").concat(userId, "/notifications/stream"), {
      headers: {
        Authorization: _HttpClientManager["default"].getAuthHeader(token)
      }
    });
    return eventSource;
  } catch (error) {
    throw error;
  }
};

exports.getNotificationStream = getNotificationStream;

var patchNotificationAll = function patchNotificationAll() {
  var response;
  return regeneratorRuntime.async(function patchNotificationAll$(_context3) {
    while (1) {
      switch (_context3.prev = _context3.next) {
        case 0:
          _context3.prev = 0;
          _context3.next = 3;
          return regeneratorRuntime.awrap(UserApi.patch("/api/users/notifications/all"));

        case 3:
          response = _context3.sent;
          return _context3.abrupt("return", response);

        case 7:
          _context3.prev = 7;
          _context3.t0 = _context3["catch"](0);
          throw _context3.t0;

        case 10:
        case "end":
          return _context3.stop();
      }
    }
  }, null, null, [[0, 7]]);
};

exports.patchNotificationAll = patchNotificationAll;

var patchNotification = function patchNotification(notificationId) {
  var response;
  return regeneratorRuntime.async(function patchNotification$(_context4) {
    while (1) {
      switch (_context4.prev = _context4.next) {
        case 0:
          _context4.prev = 0;
          _context4.next = 3;
          return regeneratorRuntime.awrap(UserApi.patch("/api/users/notifications", {
            notificationId: notificationId
          }));

        case 3:
          response = _context4.sent;
          return _context4.abrupt("return", response);

        case 7:
          _context4.prev = 7;
          _context4.t0 = _context4["catch"](0);
          throw _context4.t0;

        case 10:
        case "end":
          return _context4.stop();
      }
    }
  }, null, null, [[0, 7]]);
};

exports.patchNotification = patchNotification;

var deleteNotification = function deleteNotification(notificationId) {
  var response;
  return regeneratorRuntime.async(function deleteNotification$(_context5) {
    while (1) {
      switch (_context5.prev = _context5.next) {
        case 0:
          _context5.prev = 0;
          _context5.next = 3;
          return regeneratorRuntime.awrap(UserApi["delete"]("/api/users/notifications", {
            data: {
              notificationId: notificationId
            }
          }));

        case 3:
          response = _context5.sent;
          return _context5.abrupt("return", response);

        case 7:
          _context5.prev = 7;
          _context5.t0 = _context5["catch"](0);
          throw _context5.t0;

        case 10:
        case "end":
          return _context5.stop();
      }
    }
  }, null, null, [[0, 7]]);
};

exports.deleteNotification = deleteNotification;