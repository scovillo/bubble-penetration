import { MikroOrmModule } from '@mikro-orm/nestjs';
import { Module } from '@nestjs/common';
import { OriginAllowlistGuard } from '../common/origin-allowlist.guard';
import { BubbleGameController } from './bubble-game.controller';
import { BubbleGameService } from './bubble-game.service';
import { GameSession } from './entities/game-session.entity';
import { Highscore } from './entities/highscore.entity';
import { Player } from './entities/player.entity';
import { PlayerCredentialGuard } from './player/player-credential.guard';
import { PlayerController } from './player/player.controller';
import { PlayerService } from './player/player.service';
import { UsernameValidationModule } from './username-validation/username-validation.module';

@Module({
  imports: [
    MikroOrmModule.forFeature([Player, GameSession, Highscore]),
    UsernameValidationModule,
  ],
  providers: [
    BubbleGameService,
    PlayerCredentialGuard,
    PlayerService,
    OriginAllowlistGuard,
  ],
  controllers: [BubbleGameController, PlayerController],
})
export class BubbleGameModule {}
