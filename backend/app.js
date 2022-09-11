const express = require('express');
const bodyParser = require('body-parser')
const pg = require('pg');
const fs = require('fs');
const app = express();

app.use(bodyParser.json())

const environment = app.settings.env;
const config = require('./config');
const dbUrl = config.getFullUrlFor(environment);
const dbClient = new pg.Client(dbUrl);
dbClient.connect();
createDb();

app.all('*', function(req, res, next) {
    res.header('Access-Control-Allow-Origin', '*');
    res.header('Access-Control-Allow-Methods', 'PUT, GET, POST, DELETE, OPTIONS');
    res.header('Access-Control-Allow-Headers', 'Content-Type');
    next();
});

app.post('/username', function (req, res, next) {
    const username = req.body.username
    saveHighscore(username, 0).then(isSuccess => {
        if (isSuccess) {
            return res.end('true')
        } else {
            return res.end('false');
        }
    }).catch(error => {
        console.error(error)
        next(error)
    });
})

app.get('/highscores', function (req, res, next) {
    getHighscoresFromDb()
        .then(result => {
            result.rows.forEach(row => row.timestamp = row.timestamp.toLocaleDateString('de-DE'))
            const body = {
                highscores: result.rows
            }
            res.end(JSON.stringify(body))
        })
        .catch(error => {
            console.error(error)
            next(error)
        });
})

app.post('/highscores', function (req, res, next) {
    const username = req.body.username
    const highscore = req.body.highscore
    saveHighscore(username, highscore).then(isHighscore => {
        if (isHighscore) {
            return res.end('true')
        } else {
            return res.end('false');
        }
    }).catch(error => {
        console.error(error);
        next(error)
    });
})

const server = app.listen(config.getPortFor(environment), function () {
    const host = server.address().address
    const port = server.address().port
    console.log("Environment: " + environment)
    console.log("Bubble Penetration backend service listening at http://%s:%s", host, port)
});

function getHighscoresFromDb() {
    return dbClient.query("SELECT * from highscores ORDER BY highscore DESC");
}

function getHighscoresFromDbFor(name) {
    const values = [name];
    return dbClient.query('SELECT * from highscores WHERE username=$1 ORDER BY highscore DESC', values);
}

function saveHighscore(name, highscore) {
    return getHighscoresFromDbFor(name).then(result => {
        const values = [name, highscore, new Date()];
        if (result.rows.length === 0) {
            return dbClient.query('INSERT INTO highscores (username, highscore, timestamp) values ($1, $2, $3)', values)
                .then(() => true)
                .catch(error => {
                    console.error(error);
                    return false;
                });
        } else {
            if (result.rows[0].highscore < highscore) {
                return dbClient.query('UPDATE highscores SET highscore=$2, timestamp=$3 WHERE username=$1', values)
                    .then(() => true)
                    .catch(error => {
                        console.error(error);
                        return false;
                    })
            }
            return false;
        }
    }).catch(error => console.error(error));
}

function createDb() {
    const sql = fs.readFileSync('./db/db-setup.sql').toString();
    dbClient.query(sql)
        .then(() => true)
        .catch(error => {
            console.error(error);
            process.exit(1);
        });
}
