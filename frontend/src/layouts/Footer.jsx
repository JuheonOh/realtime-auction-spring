import React from "react";
import { Link } from "react-router-dom";

export default function Footer() {
  return (
    <footer className="bg-gray-800 text-white py-8">
        <div className="container mx-auto px-4">
          <div className="flex flex-wrap justify-between">
            <div className="w-full md:w-1/3 mb-6 md:mb-0">
              <h3 className="text-xl font-semibold mb-4">경매나라</h3>
              <p className="text-gray-400">신뢰할 수 있는 온라인 경매 플랫폼입니다.</p>
            </div>
            <div className="w-full md:w-1/3 mb-6 md:mb-0">
              <h4 className="text-lg font-semibold mb-4">빠른 링크</h4>
              <ul className="space-y-2">
                <li>
                  <Link to="#" className="text-gray-400 hover:text-white transition-colors">
                    회사 소개
                  </Link>
                </li>
                <li>
                  <Link to="#" className="text-gray-400 hover:text-white transition-colors">
                    자주 묻는 질문
                  </Link>
                </li>
                <li>
                  <Link to="/support" className="text-gray-400 hover:text-white transition-colors">
                    고객센터
                  </Link>
                </li>
                <li>
                  <Link to="#" className="text-gray-400 hover:text-white transition-colors">
                    이용약관
                  </Link>
                </li>
              </ul>
            </div>
            <div className="w-full md:w-1/3">
              <h4 className="text-lg font-semibold mb-4">뉴스레터</h4>
              <p className="text-gray-400 mb-4">최신 경매 정보와 뉴스를 받아보세요.</p>
              <div className="flex">
                <input type="email" placeholder="이메일 주소" className="flex-grow px-4 py-2 rounded-l-md focus:outline-none focus:ring-2 focus:ring-blue-500" />
                <button className="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-r-md transition-colors">구독하기</button>
              </div>
            </div>
          </div>
          <div className="mt-8 text-center text-gray-400">
            <p>&copy; Copyright 2024 경매나라. All rights reserved. Designed by v0.dev</p>
          </div>
        </div>
      </footer>
  );
}
