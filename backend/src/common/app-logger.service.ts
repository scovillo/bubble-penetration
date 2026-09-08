import { ConsoleLogger } from '@nestjs/common';
import { getCurrentRequestId } from './request-context';

export class AppLogger extends ConsoleLogger {
  protected formatContext(context: string): string {
    const base = super.formatContext(context);
    const requestId = getCurrentRequestId();

    if (!requestId) {
      return base;
    }

    return `${base}[reqId:${requestId}] `;
  }
}
