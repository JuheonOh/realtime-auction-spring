"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.IMAGE_URL = exports.WS_BASE_URL = exports.API_BASE_URL = void 0;
// 서버 주소
var API_BASE_URL = "http://localhost:8080";
exports.API_BASE_URL = API_BASE_URL;
var WS_BASE_URL = "ws://localhost:8080";
exports.WS_BASE_URL = WS_BASE_URL;
var IMAGE_URL = "".concat(API_BASE_URL, "/auction/images");
exports.IMAGE_URL = IMAGE_URL;