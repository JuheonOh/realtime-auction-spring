import React from "react";

const LoadingSpinner = ({ children }) => {
  return (
    <div className="fixed inset-0 flex items-center justify-center z-50">
      <div className="flex flex-col items-center">
        <div className="animate-spin rounded-full h-16 w-16 border-t-2 border-b-2 border-blue-500"></div>
        <p className="mt-4 text-white font-semibold">Loading...</p>
      </div>
    </div>
  );
};

export default LoadingSpinner;
