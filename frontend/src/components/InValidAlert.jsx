import { AlertCircle } from "lucide-react";

const InValidAlert = ({ inValid, message, className }) => {
  return (
    inValid && (
      <div className={`mt-1 text-red-500 text-sm flex items-center ${className}`}>
        <AlertCircle className="w-4 h-4 mr-1" />
        {message}
      </div>
    )
  );
};

export default InValidAlert;