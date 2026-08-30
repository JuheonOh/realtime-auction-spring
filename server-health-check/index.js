const mariadb = require("mariadb");
const axios = require("axios");
const fs = require("fs/promises");
const path = require("path");

const CHECK_INTERVAL_MS = 3000;
const HTTP_TIMEOUT_MS = 5000;
const DATABASE_CONNECT_TIMEOUT_MS = 5000;
const MAX_RECONNECT_DELAY_MS = 30000;
const MAX_LOG_QUEUE_SIZE = 100;
const MAX_LOG_SIZE_BYTES = 1024 * 1024;
const LOG_FILE = path.join(__dirname, "server_status.log");

function requiredEnvironment(name) {
  const value = process.env[name];
  if (!value || value.trim() === "") {
    throw new Error(
      `Missing required environment variable ${name}. Set DB_USER, DB_PASSWORD, and DB_NAME for a dedicated least-privilege database account; do not use an administrator account.`,
    );
  }
  return value;
}

function loadConfig() {
  const dbPort = Number.parseInt(process.env.DB_PORT || "3306", 10);
  const springPort = Number.parseInt(process.env.SPRING_PORT || "8080", 10);

  if (!Number.isInteger(dbPort) || dbPort <= 0 || !Number.isInteger(springPort) || springPort <= 0) {
    throw new Error("DB_PORT and SPRING_PORT must be positive integer port numbers.");
  }

  return {
    spring: {
      host: process.env.SPRING_HOST || "localhost",
      port: springPort,
      healthEndpoint: "/actuator/health",
    },
    database: {
      host: process.env.DB_HOST || "localhost",
      port: dbPort,
      user: requiredEnvironment("DB_USER"),
      password: requiredEnvironment("DB_PASSWORD"),
      database: requiredEnvironment("DB_NAME"),
      connectTimeout: DATABASE_CONNECT_TIMEOUT_MS,
    },
  };
}

class StatusLogger {
  constructor() {
    this.queue = [];
    this.writing = false;
    this.closed = false;
    this.drainPromise = Promise.resolve();
  }

  log(message) {
    if (this.closed) return;

    if (this.queue.length >= MAX_LOG_QUEUE_SIZE) {
      this.queue.shift();
      console.error("Status log queue full; dropping the oldest status message.");
    }

    this.queue.push(`${message}\n`);
    if (!this.writing) {
      this.drainPromise = this.drain();
    }
  }

  async drain() {
    if (this.writing) return;

    this.writing = true;
    try {
      while (this.queue.length > 0) {
        const entry = this.queue.shift();
        try {
          await this.rotateIfNeeded(Buffer.byteLength(entry));
          await fs.appendFile(LOG_FILE, entry, "utf8");
        } catch (error) {
          console.error("Status log write failed:", error.message);
        }
      }
    } finally {
      this.writing = false;
    }
  }

  async rotateIfNeeded(nextEntrySize) {
    try {
      const current = await fs.stat(LOG_FILE);
      if (current.size + nextEntrySize <= MAX_LOG_SIZE_BYTES) return;
      await fs.rm(`${LOG_FILE}.1`, { force: true });
      await fs.rename(LOG_FILE, `${LOG_FILE}.1`);
    } catch (error) {
      if (error.code !== "ENOENT") throw error;
    }
  }

  async close() {
    this.closed = true;
    await this.drainPromise;
  }
}

class HealthChecker {
  constructor(config) {
    this.config = config;
    this.lastStatus = null;
    this.db = null;
    this.dbReconnectAttempts = 0;
    this.nextDbConnectAt = 0;
    this.running = false;
    this.timer = null;
    this.logger = new StatusLogger();
  }

  async ensureDatabaseConnection() {
    if (this.db) return true;
    if (Date.now() < this.nextDbConnectAt) return false;

    try {
      this.db = await mariadb.createConnection(this.config.database);
      this.dbReconnectAttempts = 0;
      this.nextDbConnectAt = 0;
      console.log("Database connection established.");
      return true;
    } catch (error) {
      this.dbReconnectAttempts += 1;
      const delay = Math.min(1000 * 2 ** (this.dbReconnectAttempts - 1), MAX_RECONNECT_DELAY_MS);
      this.nextDbConnectAt = Date.now() + delay;
      console.error(`Database connection failed; retrying in ${delay}ms:`, error.message);
      return false;
    }
  }

  async disconnectDatabase() {
    const connection = this.db;
    this.db = null;
    if (!connection) return;

    try {
      await connection.end();
    } catch (error) {
      console.error("Database close failed:", error.message);
    }
  }

  async checkHealth() {
    try {
      const response = await axios.get(
        `http://${this.config.spring.host}:${this.config.spring.port}${this.config.spring.healthEndpoint}`,
        { timeout: HTTP_TIMEOUT_MS, validateStatus: (status) => status >= 200 && status < 300 },
      );
      const status = response.data && typeof response.data === "object" && !Array.isArray(response.data)
        ? response.data.status
        : null;

      if (typeof status !== "string") {
        throw new Error("Health endpoint returned an invalid response body.");
      }

      return { timestamp: new Date(), status, isUp: status === "UP" };
    } catch (error) {
      return { timestamp: new Date(), status: "DOWN", isUp: false, error: error.message };
    }
  }

  async updateServerStatus(isUp, timestamp) {
    if (this.lastStatus === isUp) return;

    const logMessage = `${formatKSTDate(timestamp)} - Server status: ${isUp ? "UP" : "DOWN"}`;
    console.log(logMessage);
    this.logger.log(logMessage);
    this.lastStatus = isUp;

    if (isUp || !(await this.ensureDatabaseConnection())) return;

    try {
      const lastLifecycle = await this.db.query("SELECT * FROM server_lifecycle ORDER BY ID DESC LIMIT 1");
      const latest = lastLifecycle[0];
      if (latest && latest.shutdown_time && latest.startup_time) {
        console.log(`${formatKSTDate(timestamp)} - Server stopped unexpectedly; recording shutdown time.`);
        await this.db.query("INSERT INTO server_lifecycle (shutdown_time) VALUES (?)", [timestamp]);
      }
    } catch (error) {
      console.error("Database lifecycle update failed:", error.message);
      await this.disconnectDatabase();
    }
  }

  async poll() {
    try {
      await this.ensureDatabaseConnection();
      const healthData = await this.checkHealth();
      await this.updateServerStatus(healthData.isUp, healthData.timestamp);
    } catch (error) {
      console.error("Health polling failed:", error.message);
    } finally {
      if (this.running) {
        this.timer = setTimeout(() => this.poll(), CHECK_INTERVAL_MS);
      }
    }
  }

  start() {
    if (this.running) return;
    this.running = true;
    console.log("Server health monitoring started.");
    void this.poll();
  }

  async stop() {
    if (!this.running) return;
    this.running = false;
    if (this.timer) clearTimeout(this.timer);
    await this.disconnectDatabase();
    await this.logger.close();
    console.log("Server health monitoring stopped.");
  }
}

function formatKSTDate(date) {
  const kstDate = new Date(date.getTime() + 9 * 60 * 60 * 1000);
  return kstDate.toISOString().replace("Z", "+09:00");
}

async function main() {
  let config;
  try {
    config = loadConfig();
  } catch (error) {
    console.error(`Configuration error: ${error.message}`);
    process.exitCode = 1;
    return;
  }

  const healthChecker = new HealthChecker(config);
  const shutdown = async (signal) => {
    console.log(`${signal} received; shutting down.`);
    try {
      await healthChecker.stop();
      process.exit(0);
    } catch (error) {
      console.error("Graceful shutdown failed:", error.message);
      process.exit(1);
    }
  };

  process.once("SIGINT", () => void shutdown("SIGINT"));
  process.once("SIGTERM", () => void shutdown("SIGTERM"));
  healthChecker.start();
}

void main();
