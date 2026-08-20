import crypto from "node:crypto";
import pino from "pino";
import pinoHttp from "pino-http";

export const log = pino({
  level: process.env.LOG_LEVEL ? process.env.LOG_LEVEL.toLowerCase() : "info",
});

export function installLoggingMiddleware(app) {
  app.use(
    pinoHttp({
      logger: log,
      genReqId: (req) => req.headers["x-request-id"] ?? crypto.randomUUID(),
      customReceivedMessage: (req) => `→ ${req.method} ${req.url}`,
      customSuccessMessage: (req, res) =>
        `← ${req.method} ${req.url} ${res.statusCode}`,
      customErrorMessage: (req, res, err) =>
        `← ${req.method} ${req.url} ${res.statusCode} ${err.message}`,
      serializers: {
        req: (req) => ({
          id: req.id,
          method: req.method,
          url: req.url,
          userAgent: req.headers["user-agent"],
        }),
        res: (res) => ({
          statusCode: res.statusCode,
        }),
      },
    }),
  );
}
