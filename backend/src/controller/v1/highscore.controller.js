import express from "express";
import { log } from "../../middlewares/logging.middleware.js";
import * as highscoreService from "../../services/highscore.service.js";

export const highscoreController = express.Router();

highscoreController.post("/username", function (req, res, next) {
  const { username } = req.body;

  highscoreService
    .findHighscore(username)
    .then((isExisting) => {
      if (isExisting) {
        return res.end("false");
      } else {
        highscoreService.create(username, 0).then(() => res.end("true"));
      }
    })
    .catch((error) => {
      log.error(error);
      next(error);
    });
});

highscoreController.get("/highscores", function (req, res, next) {
  highscoreService
    .findAll()
    .then((highscores) => {
      highscores.forEach(
        (row) => (row.timestamp = row.timestamp.toLocaleDateString("de-DE")),
      );
      const body = {
        highscores,
      };
      res.json(body);
    })
    .catch((error) => {
      log.error(error);
      next(error);
    });
});

highscoreController.post("/highscores", function (req, res, next) {
  const { username, highscore } = req.body;
  highscoreService
    .findHighscore(username)
    .then((existingHighscore) => {
      const highscoreBefore = existingHighscore
        ? existingHighscore.highscore
        : 0;

      return highscoreService
        .save(username, highscore)
        .then((savedHighscore) => {
          if (savedHighscore.highscore > highscoreBefore) {
            return res.end("true");
          }

          return res.end("false");
        });
    })
    .catch((error) => {
      log.error(error);
      next(error);
    });
});
