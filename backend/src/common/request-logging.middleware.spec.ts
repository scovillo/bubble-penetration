import { Logger } from '@nestjs/common';
import { EventEmitter } from 'node:events';
import type { NextFunction, Request, Response } from 'express';
import { RequestLoggingMiddleware } from './request-logging.middleware';

describe('RequestLoggingMiddleware', () => {
  it('does not log health checks without a request ID', () => {
    const middleware = new RequestLoggingMiddleware();
    const request = {
      headers: {},
      originalUrl: '/health/ready?full=true',
      path: '/',
      method: 'GET',
    } as Request;
    const response = Object.assign(new EventEmitter(), {
      setHeader: jest.fn(),
      statusCode: 200,
    }) as unknown as Response;
    const warn = jest.spyOn(Logger.prototype, 'warn');
    const log = jest.spyOn(Logger.prototype, 'log');

    middleware.use(request, response, jest.fn<NextFunction>());
    response.emit('finish');

    expect(warn).not.toHaveBeenCalled();
    expect(log).not.toHaveBeenCalled();
    expect(response.setHeader).toHaveBeenCalledWith(
      'X-Request-Id',
      expect.any(String),
    );
  });
});
