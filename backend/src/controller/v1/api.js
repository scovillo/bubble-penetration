import { highscoreController } from "./highscore.controller.js";

export function installApiV1(app) {
  app.use("/api/v1", highscoreController);
}
