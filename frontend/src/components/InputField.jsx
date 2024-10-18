import { AlertCircle } from "lucide-react";

export default function InputField({ name, label, type = "text", value, placeholder, handleChange, inValid }) {
  return (
    <div>
      <label htmlFor={name} className="block text-sm font-medium text-gray-700 mb-1">
        {label}
      </label>
      <input type={type} id={name} name={name} value={value} placeholder={placeholder} onChange={handleChange} className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${inValid[name] ? "border-red-500" : "border-gray-300"}`} />
      {inValid[name] && (
        <div className="mt-1 text-red-500 text-sm flex items-center">
          <AlertCircle className="w-4 h-4 mr-1" />
          {inValid[name]}
        </div>
      )}
    </div>
  );
}
