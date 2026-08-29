import { ipKeyGenerator, rateLimit } from "express-rate-limit";

function configuredLimit(name, fallback) {
  const value = Number.parseInt(process.env[name] ?? "", 10);
  return Number.isInteger(value) && value > 0 ? value : fallback;
}

const commonOptions = {
  windowMs: 60_000,
  standardHeaders: "draft-8",
  legacyHeaders: false,
  message: {
    success: false,
    message: "too many requests; please try again shortly",
  },
};

export function installServiceRateLimits(app) {
  const serviceLimiter = rateLimit({
    ...commonOptions,
    identifier: "service-read",
    limit: configuredLimit("RATE_LIMIT_SERVICE_PER_MINUTE", 120),
  });

  app.get("/", serviceLimiter);
  app.get("/health", serviceLimiter);
}

export function installRateLimits(router) {
  router.get(
    "/highscores",
    rateLimit({
      ...commonOptions,
      identifier: "highscore-read",
      limit: configuredLimit("RATE_LIMIT_READ_PER_MINUTE", 120),
    }),
  );

  router.post(
    "/users",
    rateLimit({
      ...commonOptions,
      identifier: "user-registration",
      limit: configuredLimit("RATE_LIMIT_REGISTRATIONS_PER_MINUTE", 10),
    }),
  );

  router.post(
    "/highscores",
    rateLimit({
      ...commonOptions,
      identifier: "highscore-write",
      limit: configuredLimit("RATE_LIMIT_SCORES_PER_MINUTE", 30),
      keyGenerator: (request) => {
        const username = request.body?.username?.trim().toLocaleLowerCase("en-US");
        return `${ipKeyGenerator(request.ip)}:${username || "anonymous"}`;
      },
    }),
  );
}
