const fs = require('fs');

function decode(value) {
    return Buffer.from(value, 'base64').toString('utf-8');
}

exports.getFullUrlFor = function getFullUrlFor(environment) {
    const parsed = JSON.parse(fs.readFileSync('./db/' + environment + '.json', 'UTF-8'));
    const host = decode(parsed.host);
    const username = decode(parsed.username);
    const password = decode(parsed.password);
    return "postgres://" + username + ":" + password + "@" + host;
}

exports.getPortFor = function getPortFor(environment) {
    if(environment === 'production') {
        return 8089
    }
    return 8088
}