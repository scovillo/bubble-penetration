const fs = require('fs');

function decode(value) {
    return Buffer.from(value, 'base64').toString('utf-8');
}

exports.getFullUrlFor = function getFullUrlFor(environment) {
    const parsed = JSON.parse(fs.readFileSync('./db/' + environment + '.json', 'UTF-8'));
    return decode(parsed.url);
}

exports.getPortFor = function getPortFor(environment) {
    if(environment === 'production') {
        return 8089
    }
    return 8088
}