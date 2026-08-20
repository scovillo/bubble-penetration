import express from "express";
import { log } from "../../middlewares/logging.middleware.js";
import * as highscoreService from "../../services/highscore.service.js";

export const highscoreController = express.Router();

function toHighscoreResponse(row) {
  return {
    username: row.username,
    score: row.highscore,
    timestamp: row.timestamp.toLocaleDateString("de-DE"),
  };
}

highscoreController.post("/username", function (req, res, next) {
  const username = req.body?.username?.trim();

  if (!username) {
    return res.status(400).json({
      success: false,
      message: "username must not be empty",
    });
  }

  highscoreService
    .findHighscore(username)
    .then((isExisting) => {
      if (isExisting) {
        return res.json({
          success: false,
          message: "username already exists",
        });
      } else {
        return highscoreService.create(username, 0).then((createdUser) =>
          res.status(201).json({
            success: true,
            user: {
              username: createdUser.username,
            },
          }),
        );
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
      const body = {
        highscores: highscores.map(toHighscoreResponse),
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

  if (!username?.trim()) {
    return res.status(400).json({
      success: false,
      message: "username must not be empty",
    });
  }

  highscoreService
    .findHighscore(username)
    .then((existingHighscore) => {
      const highscoreBefore = existingHighscore
        ? existingHighscore.highscore
        : 0;

      return highscoreService
        .save(username, highscore)
        .then((savedHighscore) => {
          const isNewHighscore = savedHighscore.highscore > highscoreBefore;
          return res.status(existingHighscore ? 200 : 201).json({
            success: true,
            isNewHighscore,
            highscore: toHighscoreResponse(savedHighscore),
          });
        });
    })
    .catch((error) => {
      log.error(error);
      next(error);
    });
});
