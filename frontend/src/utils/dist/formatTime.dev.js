"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports["default"] = formatTime;

function formatTime(time) {
  var slice = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 4;
  if (time === 0) return "종료";
  var days = Math.floor(time / (24 * 60 * 60)); // 24시간

  var hours = Math.floor(time % (24 * 60 * 60) / (60 * 60)); // 60분

  var minutes = Math.floor(time % (60 * 60) / 60); // 60초

  var seconds = time % 60; // 초
  // 일, 시간, 분, 초 중 2개만 표시

  var timeParts = [];
  if (days > 0) timeParts.push("".concat(days, "\uC77C"));
  if (hours > 0) timeParts.push("".concat(hours, "\uC2DC\uAC04"));
  if (minutes > 0) timeParts.push("".concat(minutes, "\uBD84"));
  if (seconds > 0) timeParts.push("".concat(seconds, "\uCD08"));
  return timeParts.slice(0, slice).join(" ");
}