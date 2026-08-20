import bodyParser from 'body-parser';

export function installParserMiddleware(app) {
    app.use(bodyParser.json());
}
