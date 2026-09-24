import { MikroOrmModule } from '@mikro-orm/nestjs';
import { Module } from '@nestjs/common';
import { Player } from '../entities/player.entity';
import { UsernameAvailabilityService } from './username-availability.service';
import { UsernameValidationService } from './username-validation.service';

@Module({
  imports: [MikroOrmModule.forFeature([Player])],
  providers: [UsernameValidationService, UsernameAvailabilityService],
  exports: [UsernameValidationService, UsernameAvailabilityService],
})
export class UsernameValidationModule {}
