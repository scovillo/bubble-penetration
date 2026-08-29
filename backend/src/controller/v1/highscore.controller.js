import express from 'express';
import { log } from '../../middlewares/logging.middleware.js';
import { installRateLimits } from '../../middlewares/rate-limit.middleware.js';
import * as highscoreService from '../../services/highscore.service.js';

export const highscoreController = express.Router();
installRateLimits(highscoreController);

function toHighscoreResponse(row) {
  return {
    username: row.username,
    score: row.highscore,
    timestamp: row.timestamp.toLocaleDateString('de-DE'),
  };
}

highscoreController.post('/users', function (req, res, next) {
  const username = req.body?.username?.trim();

  if (!username) {
    return res.status(400).json({
      success: false,
      message: 'username must not be empty',
    });
  }

  highscoreService
    .findHighscore(username)
    .then((isExisting) => {
      if (isExisting) {
        return res.status(409).json({
          success: false,
          message: 'username already exists',
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

highscoreController.get('/highscores', function (req, res, next) {
  const isPaginated =
    req.query.username !== undefined || req.query.startRank !== undefined;
  if (!isPaginated) {
    return highscoreService
      .findAll()
      .then((highscores) => {
        res.json({ highscores: highscores.map(toHighscoreResponse) });
      })
      .catch((error) => {
        log.error(error);
        next(error);
      });
  }

  const requestedStartRank = Number.parseInt(req.query.startRank, 10);
  const startRank = Number.isNaN(requestedStartRank) ? 1 : requestedStartRank;
  const username = req.query.username?.trim();

  highscoreService
    .findPage({ startRank, username })
    .then((page) => {
      const body = {
        highscores: page.highscores.map((row, index) => ({
          ...toHighscoreResponse(row),
          rank: page.startRank + index,
        })),
        startRank: page.startRank,
        total: page.total,
        hasPrevious: page.hasPrevious,
        hasNext: page.hasNext,
      };
      res.json(body);
    })
    .catch((error) => {
      log.error(error);
      next(error);
    });
});

highscoreController.post('/highscores', function (req, res, next) {
  const { username, highscore } = req.body;

  if (!username?.trim()) {
    return res.status(400).json({
      success: false,
      message: 'username must not be empty',
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
