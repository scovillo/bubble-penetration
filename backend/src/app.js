import "dotenv/config";
import express from "express";
import { installApiV1 } from "./controller/v1/api.js";
import { installCorsMiddleware } from "./middlewares/cors.middleware.js";
import {
  installLoggingMiddleware,
  log,
} from "./middlewares/logging.middleware.js";
import { installParserMiddleware } from "./middlewares/parser.middleware.js";

const app = express();
installCorsMiddleware(app);
installParserMiddleware(app);
installLoggingMiddleware(app);

app.use(express.static("src/public"));

installApiV1(app);

const serverPort = Number.parseInt(process.env.SERVER_PORT ?? "3000", 10);
app.listen(serverPort, "0.0.0.0", () =>
  log.info(
    "Bubble Penetration backend service listening at http://0.0.0.0:%d",
    serverPort,
  ),
);
