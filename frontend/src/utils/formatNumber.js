// 숫자에 천단위 콤마 추가
export const addCommas = (value) => {
  if (!value) return "";

  return value
    .toString()
    .replace(/\D/g, "")
    .replace(/\B(?=(\d{3})+(?!\d))/g, ",");
};

// 콤마가 포함된 문자열을 숫자로 변환
export const removeCommas = (value) => {
  if (!value) return 0;
  return parseInt(value.replace(/,/g, ""));
};

// 입력 중인 값 포맷팅 (커서 위치 유지)
export const formatNumberInput = (value, prevValue, cursorPos) => {
  // 숫자만 추출
  const numericValue = value.replace(/[^\d]/g, '');
  
  if (!numericValue) {
    return {
      formattedValue: "",
      numericValue: 0,
      cursorPos: 0
    };
  }

  // 이전 값의 콤마 개수
  const prevCommaCount = (prevValue.slice(0, cursorPos).match(/,/g) || []).length;
  
  // 새로운 포맷팅된 값
  const formattedValue = numericValue.replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  
  // 새로운 값의 콤마 개수
  const newCommaCount = (formattedValue.slice(0, cursorPos).match(/,/g) || []).length;
  
  // 커서 위치 조정
  const newCursorPos = cursorPos + (newCommaCount - prevCommaCount);

  return {
    formattedValue,
    numericValue: parseInt(numericValue),
    cursorPos: newCursorPos
  };
};

// 가격 입력값 검증 및 포맷팅
export const formatPrice = (value, minPrice = 0) => {
  if (value < 0) return "";
  const numericValue = removeCommas(value);
  if (numericValue < minPrice) return addCommas(minPrice);
  return addCommas(value);
};
