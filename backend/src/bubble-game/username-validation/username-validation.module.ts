import { Module } from '@nestjs/common';
import { UsernameValidationService } from './username-validation.service';

@Module({
  providers: [UsernameValidationService],
  exports: [UsernameValidationService],
})
export class UsernameValidationModule {}
