import { AlertCircle } from "lucide-react";

export default function InValidAlert({ inValid, message, className }) {
  return (
    inValid && (
      <div className={`mt-1 text-red-500 text-sm flex items-center ${className}`}>
        <AlertCircle className="w-4 h-4 mr-1" />
        {message.includes("\r\n")
          ? message.split("\r\n").map((line, index) => (
              <>
                {line}
                <br />
              </>
            ))
          : message}
      </div>
    )
  );
}