export default function formatTime(time, slice = 4) {
  if (time === 0) return "종료";

  const days = Math.floor(time / (24 * 60 * 60)); // 24시간
  const hours = Math.floor((time % (24 * 60 * 60)) / (60 * 60)); // 60분
  const minutes = Math.floor((time % (60 * 60)) / 60); // 60초
  const seconds = time % 60; // 초

  // 일, 시간, 분, 초 중 2개만 표시
  const timeParts = [];
  if (days > 0) timeParts.push(`${days}일`);
  if (hours > 0) timeParts.push(`${hours}시간`);
  if (minutes > 0) timeParts.push(`${minutes}분`);
  if (seconds > 0) timeParts.push(`${seconds}초`);

  return timeParts.slice(0, slice).join(" ");
}
