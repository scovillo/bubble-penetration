import "dotenv/config";
import { createRequire } from "node:module";
import express from "express";
import { installApiV1 } from "./controller/v1/api.js";
import { installCorsMiddleware } from "./middlewares/cors.middleware.js";
import {
  installLoggingMiddleware,
  log,
} from "./middlewares/logging.middleware.js";
import { installParserMiddleware } from "./middlewares/parser.middleware.js";
import { installServiceRateLimits } from "./middlewares/rate-limit.middleware.js";

const require = createRequire(import.meta.url);
const packageMetadata = require("../package.json");

const app = express();
const trustedProxyHops = Number.parseInt(process.env.TRUST_PROXY_HOPS ?? "0", 10);
if (Number.isInteger(trustedProxyHops) && trustedProxyHops > 0) {
  app.set("trust proxy", trustedProxyHops);
}
installCorsMiddleware(app);
installParserMiddleware(app);
installLoggingMiddleware(app);
installServiceRateLimits(app);

app.use(express.static("src/public"));

app.get("/", (_request, response) => {
  response.status(200).json({
    service: packageMetadata.name,
    version: packageMetadata.version,
    apiVersions: ["v1"],
    endpoints: {
      health: "/health",
      api: "/api/v1",
    },
  });
});

app.get("/health", (_request, response) => {
  response.status(200).json({ status: "ok" });
});

installApiV1(app);

const serverPort = Number.parseInt(process.env.SERVER_PORT ?? "3000", 10);
app.listen(serverPort, "0.0.0.0", () =>
  log.info(
    "Bubble Penetration backend service listening at http://0.0.0.0:%d",
    serverPort,
  ),
);
