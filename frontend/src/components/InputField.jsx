import InValidAlert from "./InValidAlert";

export default function InputField({ name, label, type = "text", value, placeholder, handleChange, inValid }) {
  return (
    <div>
      <label htmlFor={name} className="block text-sm font-medium text-gray-700 mb-1">
        {label}
      </label>
      <input type={type} id={name} name={name} value={value} placeholder={placeholder} onChange={handleChange} className={`w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${inValid[name] ? "border-red-500" : "border-gray-300"}`} />
      <InValidAlert inValid={inValid[name]} message={inValid[name]} />
    </div>
  );
}
