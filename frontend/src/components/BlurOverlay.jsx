export default function BlurOverlay({ message }) {
  return (
    <div className="absolute inset-0 bg-gray-100/70 backdrop-blur-sm rounded-lg flex items-center justify-center z-10">
      <p className="text-xl font-bold text-gray-700">{message}</p>
    </div>
  );
}
