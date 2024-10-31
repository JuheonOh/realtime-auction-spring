export default function LoadingSpinner({ isLoading, isError, message }) {
  return isLoading || isError ? (
    <div className="relative h-48">
      {isLoading ? (
        <div className="absolute top-0 left-0 w-full h-full flex items-center justify-center z-50">
          <div className="flex flex-col items-center">
            <div className="animate-spin rounded-full h-16 w-16 border-t-2 border-b-2 border-blue-500"></div>
            <p className="mt-4 text-gray-700 font-semibold">{message} 불러오는 중...</p>
          </div>
        </div>
      ) : isError ? (
        <div className="flex flex-col justify-center items-center h-full">
          <p className="mt-4 text-gray-700 font-semibold">{message} 불러오기 실패</p>
        </div>
      ) : null}
    </div>
  ) : null;
}
