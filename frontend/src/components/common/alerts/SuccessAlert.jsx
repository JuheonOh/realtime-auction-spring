import { CheckCircle } from "lucide-react";

export default function SuccessAlert({ message, className }) {
  return (
    <div className={`mt-1 text-emerald-600 text-sm flex items-center ${className}`}>
      <CheckCircle className="w-4 h-4 mr-1" />
      {message.includes("\r\n")
        ? message.split("\r\n").map((line, index) => (
            <>
              {line}
              <br />
            </>
          ))
        : message}
    </div>
  );
}
