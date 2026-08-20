-- CreateTable
CREATE TABLE IF NOT EXISTS "Highscores"
(
    "username" VARCHAR
(
    10
) NOT NULL,
    "highscore" INTEGER NOT NULL DEFAULT 0,
    "timestamp" TIMESTAMP
(
    3
) NOT NULL,
    CONSTRAINT "Highscores_pkey" PRIMARY KEY
(
    "username"
)
    );
