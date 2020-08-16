create table highscores
(
    username  varchar(10) not null
        constraint highscores_pkey
            primary key,
    highscore INT,
    timestamp TIMESTAMP
);
