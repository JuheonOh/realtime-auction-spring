import { addCommas } from "@utils/formatNumber";
import {
  CategoryScale,
  Chart as ChartJS,
  Filler, // Filler 플러그인 추가
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip,
} from "chart.js";
import { Line } from "react-chartjs-2";

// Chart.js 등록 (Filler 추가)
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  Filler // Filler 플러그인 등록
);

// 차트 데이터 필터링 함수
const filterChartData = (data, maxPoints = 20) => {
  if (data.length <= maxPoints) return data;

  // 전체 데이터를 maxPoints 개수만큼 균등하게 선택
  const interval = (data.length - 1) / (maxPoints - 1);
  const result = [];

  for (let i = 0; i < maxPoints; i++) {
    const index = Math.round(i * interval);
    result.push(data[Math.min(index, data.length - 1)]);
  }

  return result;
};

export default function BidChart({ bidData, startPrice }) {
  // const filteredData = filterChartData(bidData);
  const filteredData = bidData;

  // 만 단위 올림 함수
  const ceilToThousand = (value) => Math.ceil(value / 1000) * 1000;

  // 최소값과 최대값 계산 (만 단위 올림)
  const minPrice = startPrice;
  const maxPrice = (filteredData.length > 0 ? ceilToThousand(Math.max(...filteredData.map((data) => data.bidAmount))) : ceilToThousand(startPrice)) * 1.1;

  // 눈금 간격 계산
  const range = maxPrice - minPrice;
  const step = Math.ceil(range / 6); // 7개의 눈금을 위해 6등분
  const roundedStep = ceilToThousand(step); // 만 단위로 올림

  // 차트 옵션
  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: "top",
        labels: {
          font: {
            size: 14,
          },
        },
      },
      tooltip: {
        backgroundColor: "rgba(0, 0, 0, 0.8)",
        padding: 12,
        titleFont: {
          size: 14,
        },
        bodyFont: {
          size: 14,
        },
        displayColors: false, // 범례 색상 표시 제거
        position: "nearest", // 툴팁 위치 설정
        yAlign: "bottom", // 툴팁을 항상 데이터 포인트 위에 표시
        caretPadding: 15, // 툴팁과 데이터 포인트 사이의 간격
        callbacks: {
          title: function (context) {
            if (context[0].label === "경매 시작") {
              return "경매 시작";
            }

            return new Date(context[0].label).toLocaleString("ko-KR", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit", hour12: false });
          },
          label: function (context) {
            const dataPoint = filteredData[context.dataIndex];
            const isFirstData = context.dataIndex === 0;

            return [
              // 첫 데이터는 '시작가'로, 나머지는 '입찰가'로 표시
              `${isFirstData ? "시작가" : "입찰가"}: ${addCommas(dataPoint.bidAmount)}원`,
              // 첫 데이터는 입찰자 정보를 표시하지 않음
              ...(!isFirstData ? [`입찰자: ${dataPoint.nickname || "알 수 없음"}`] : []),
            ];
          },
        },
      },
    },
    interaction: {
      intersect: false, // 선 위에서도 툴팁 표시
      mode: "nearest", // 가장 가까운 포인트 선택
    },
    scales: {
      x: {
        grid: { display: true },
        ticks: {
          padding: 10,
          callback: (value) => {
            return chartConfig.labels[value].toLocaleString("ko-KR", { hour: "2-digit", minute: "2-digit", hour12: false });
          },
        },
      },
      y: {
        beginAtZero: false,
        min: minPrice,
        max: minPrice + roundedStep * 7,
        ticks: {
          callback: (value) => addCommas(value) + "원",
          stepSize: roundedStep,
        },
        grid: {
          color: "rgba(0, 0, 0, 0.1)",
        },
      },
    },
    layout: {
      padding: { left: 25, right: 25 },
    },
    animation: {
      duration: 750,
      easing: "easeInOutQuart",
      animations: {
        numbers: {
          type: "number",
          duration: 750,
          delay: (ctx) => ctx.dataIndex * 100, // 순차적 애니메이션
        },
        x: {
          type: "number",
          easing: "easeOutElastic", // 탄성 효과
        },
        y: {
          type: "number",
          easing: "easeOutBounce", // 바운스 효과
        },
      },
    },
  };

  // 차트 데이터
  const chartConfig = {
    labels: filteredData.map((data, index) => (index === 0 ? "경매 시작" : data.bidTime)),
    datasets: [
      {
        label: "입찰가",
        data: filteredData.map((data) => data.bidAmount),
        borderColor: "rgb(75, 192, 192)",
        backgroundColor: (ctx) => {
          const gradient = ctx.chart.ctx.createLinearGradient(0, 0, 0, 400);
          gradient.addColorStop(0, "rgba(75, 192, 192, 0.4)");
          gradient.addColorStop(1, "rgba(75, 192, 192, 0)");
          return gradient;
        },
        tension: 0.4,
        pointRadius: 6,
        pointHoverRadius: 10,
        pointHitRadius: 20,
        pointBackgroundColor: (ctx) => {
          // 첫 번째 포인트는 다른 색상으로 표시
          return ctx.dataIndex === 0 ? "#FFD700" : "#fff";
        },
        pointBorderColor: (ctx) => {
          // 첫 번째 포인트는 다른 색상으로 표시
          return ctx.dataIndex === 0 ? "#FFA500" : "rgb(75, 192, 192)";
        },
        borderWidth: 3,
        fill: true,
        fillOpacity: 0.3,
        cubicInterpolationMode: "monotone", // 부드러운 상승 곡선
        segment: {
          borderColor: "rgba(75, 192, 192, 0.8)", // 항상 상승 색상 사용
          borderWidth: (ctx) => {
            // 입찰 금액 차이에 따라 선 두께 변화
            const prev = ctx.p0.parsed.y;
            const curr = ctx.p1.parsed.y;
            const increase = curr - prev;
            return increase > 50000 ? 4 : 3; // 큰 폭 상승 시 더 두꺼운 선
          },
        },
        spanGaps: true,
      },
    ],
  };

  // 컴포넌트 스타일 개선
  return (
    <>
      <h2 className="text-2xl font-bold mb-4 flex items-center">
        <span>실시간 입찰 현황</span>
        <div className="ml-3 flex items-center gap-1">
          <span className="animate-pulse inline-block w-2 h-2 bg-red-500 rounded-full"></span>
          <span className="animate-pulse inline-block w-2 h-2 bg-red-500 rounded-full" style={{ animationDelay: "0.2s" }}></span>
          <span className="animate-pulse inline-block w-2 h-2 bg-red-500 rounded-full" style={{ animationDelay: "0.4s" }}></span>
        </div>
      </h2>
      <div className="h-96 rounded-lg p-4 shadow-lg bg-white transition-all duration-300 hover:shadow-xl">
        <Line options={chartOptions} data={chartConfig} />
      </div>
    </>
  );
}
