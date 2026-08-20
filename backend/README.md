# Bubble Penetration Backend

The backend for the game Bubble Penetration, which manages the high scores.

## Configuration

### Environment Variables

The following environment variables can be set to configure the application:

| Name              | Description                                                                     |
| ----------------- | ------------------------------------------------------------------------------- |
| SERVER_PORT       | Port on which the server is hosted. **\*\*Default is '3000'.\*\***              |
| LOG_LEVEL         | Global log level. **\*\*Default is 'INFO'.\*\***                                |
| POSTGRES_HOST     | PostgreSQL hostname. **\*\*Default is '0.0.0.0'.\*\***                          |
| POSTGRES_PORT     | PostgreSQL port. **\*\*Default is '5432'.\*\***                                 |
| POSTGRES_USER     | PostgreSQL username.                                                            |
| POSTGRES_PASSWORD | Password of the PostgreSQL user.                                                |
| POSTGRES_DB       | PostgreSQL database to connect to. **\*\*Default is 'bubble_penetration'.\*\*** |
| POSTGRES_SCHEMA   | PostgreSQL database schema to connect to. **\*\*Default is 'public'.\*\***      |
