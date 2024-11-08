import React, { useRef, useState, useEffect } from "react";

export default function ImageSlider({ images, selectedImage, onSelectImage }) {
  const imageContainerRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const [totalX, setTotalX] = useState(0);

  const onMouseDown = (e) => {
    setIsDragging(true);
    const x = e.clientX;
    if (imageContainerRef.current && "scrollLeft" in imageContainerRef.current) {
      setTotalX(x + imageContainerRef.current.scrollLeft);
    }
  };

  const onMouseMove = (e) => {
    if (!isDragging) return;
    const scrollLeft = totalX - e.clientX;
    if (imageContainerRef.current && "scrollLeft" in imageContainerRef.current) {
      imageContainerRef.current.scrollLeft = scrollLeft;
    }
  };

  const onMouseUp = () => {
    setIsDragging(false);
  };

  const onMouseLeave = () => {
    setIsDragging(false);
  };

  useEffect(() => {
    const container = imageContainerRef.current;
    if (!container) return;

    const onWheel = (e) => {
      e.preventDefault();
      imageContainerRef.current.scrollLeft += e.deltaY * 0.5;
    };

    container.addEventListener("wheel", onWheel, { passive: false });

    return () => container.removeEventListener("wheel", onWheel);
  }, []);

  return (
    <div
      className="flex p-2 gap-x-2 overflow-x-auto overflow-y-hidden image-scroll-container"
      ref={imageContainerRef}
      onMouseDown={onMouseDown}
      onMouseMove={onMouseMove}
      onMouseUp={onMouseUp}
      onMouseLeave={onMouseLeave}
    >
      {images.map((image, index) => (
        <div
          key={index}
          className={`w-24 h-24 flex-shrink-0 rounded-md overflow-hidden cursor-pointer ${
            selectedImage === index ? "ring-2 ring-blue-500" : "border border-gray-200"
          }`}
        >
          <img
            src={image.filePath}
            alt={image.fileName}
            className="w-full h-full object-cover"
            onClick={() => onSelectImage(index)}
            onDragStart={(e) => e.preventDefault()}
          />
        </div>
      ))}
    </div>
  );
}