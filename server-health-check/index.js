const mariadb = require("mariadb");
const axios = require("axios");
const fs = require("fs");

const config = {
  spring: {
    url: "http://localhost:8080",
    healthEndpoint: "/actuator/health",
    checkInterval: 3000, // 3초
  },
  database: {
    host: "localhost",
    user: "auction",
    password: "1234",
    database: "auction_db",
    port: 3306,
  },
};

class HealthChecker {
  constructor() {
    this.lastStatus = null;
    this.db = null;
  }

  async dbConnectionInitialize() {
    try {
      this.db = await mariadb.createConnection(config.database);
      console.log("데이터베이스 연결 성공");
    } catch (error) {
      console.error("데이터베이스 연결 실패");
    }
  }

  async checkHealth() {
    try {
      const response = await axios.get(`${config.spring.url}${config.spring.healthEndpoint}`);
      const status = response.data.status;
      return {
        timestamp: new Date(),
        status: status,
        isUp: status === "UP",
      };
    } catch (error) {
      return {
        timestamp: new Date(),
        status: "DOWN",
        isUp: false,
        error: error.message,
      };
    }
  }

  async updateServerStatus(isUp, timestamp) {
    // 상태가 변경되지 않았으면 리턴 (한번만 로깅해야 하기때문)
    if (this.lastStatus === isUp) return;

    const logMessage = `${formatKSTDate(timestamp)} - 서버 상태: ${isUp ? "UP" : "DOWN"}`;
    console.log(logMessage); // 로깅
    fs.appendFileSync("server_status.log", logMessage + "\n"); // 데이터베이스가 연결되지 않은 경우를 대비 로컬 파일에 기록

    this.lastStatus = isUp; // 상태 업데이트

    // 서버가 다운되었고 데이터베이스가 연결된 경우
    if (!isUp && this.db) {
      try {
        // 서버 생명주기 테이블에서 마자막 행 조회 (가장 최근에 종료된 시간)
        const query = `SELECT * FROM server_lifecycle ORDER BY ID DESC LIMIT 1`;
        const lastLifecycle = await this.db.query(query);

        // 마지막 행의 shutdown_time, startup_time 둘다 있으면
        // 스프링 서버에서 비정상 종료되어 서버 종료 시간을 기록하지 못한 경우임
        // 따라서 현재 시간을 기록함
        if (lastLifecycle[0].shutdown_time && lastLifecycle[0].startup_time) {
          console.log(`${formatKSTDate(timestamp)} - 서버가 비정상 종료되었습니다. 현재 시간을 기록합니다.`);
          try {
            // 현재 시간을 기록
            const query = `INSERT INTO server_lifecycle (shutdown_time) VALUES (?)`;
            await this.db.query(query, [timestamp]);
          } catch (error) {
            console.error("데이터베이스 삽입 오류:", error);
          }
        }
      } catch (error) {
        console.error("데이터베이스 검색 오류:", error);
      }
    }
  }

  async start() {
    console.log("서버 상태 체크 시작...");

    await this.dbConnectionInitialize();

    while (true) {
      const healthData = await this.checkHealth();

      // 현재 서버 상태 업데이트
      await this.updateServerStatus(healthData.isUp, healthData.timestamp);

      // 다음 체크까지 대기
      await new Promise((resolve) => setTimeout(resolve, config.spring.checkInterval));
    }
  }

  async stop() {
    if (this.db) {
      await this.db.end();
    }
  }
}

function formatKSTDate(date) {
  const kstDate = new Date(date.getTime() + 9 * 60 * 60 * 1000);
  return kstDate.toISOString().replace("Z", "+09:00");
}

async function main() {
  const healthChecker = new HealthChecker();

  // 헬스 체크 시작
  await healthChecker.start();
}

main();
