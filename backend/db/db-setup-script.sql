DROP SCHEMA IF EXISTS public;
CREATE SCHEMA development;
CREATE SCHEMA production;

create table development.highscores
(
    username  varchar(10) not null
        constraint highscores_pkey
            primary key,
    highscore INT,
    timestamp TIMESTAMP
);

create table production.highscores
(
    username  varchar(10) not null
        constraint highscores_pkey
            primary key,
    highscore INT,
    timestamp TIMESTAMP
);