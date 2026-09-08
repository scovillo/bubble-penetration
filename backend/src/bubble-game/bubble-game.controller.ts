import {
  Body,
  Controller,
  Get,
  Param,
  Patch,
  Post,
  Query,
  UseGuards,
} from '@nestjs/common';
import { minutes, Throttle } from '@nestjs/throttler';
import { OriginAllowlistGuard } from '../common/origin-allowlist.guard';
import type {
  CreatedGameSession,
  HighscorePage,
  SubmittedHighscore,
} from './bubble-game.service';
import { BubbleGameService } from './bubble-game.service';
import { SubmitScoreDto } from './dto/submit-score.dto';
import { Player } from './entities/player.entity';
import { CurrentPlayer } from './player/current-player.decorator';
import { PlayerCredentialGuard } from './player/player-credential.guard';

@Controller('bubble-game')
export class BubbleGameController {
  constructor(private readonly bubbleGameService: BubbleGameService) {}

  @UseGuards(OriginAllowlistGuard, PlayerCredentialGuard)
  @Throttle({ default: { limit: 10, ttl: minutes(1) } })
  @Post('sessions')
  createSession(@CurrentPlayer() player: Player): Promise<CreatedGameSession> {
    return this.bubbleGameService.createSession(player);
  }

  @UseGuards(OriginAllowlistGuard, PlayerCredentialGuard)
  @Throttle({ default: { limit: 10, ttl: minutes(1) } })
  @Patch('sessions/:sessionId')
  submitScore(
    @CurrentPlayer() player: Player,
    @Param('sessionId') sessionId: string,
    @Body() submitScoreDto: SubmitScoreDto,
  ): Promise<SubmittedHighscore> {
    return this.bubbleGameService.submitScore(
      player,
      sessionId,
      submitScoreDto,
    );
  }

  @Get('highscores')
  getHighscorePage(
    @Query('username') username?: string,
    @Query('startRank') startRank?: string,
    @Query('limit') limit?: string,
  ): Promise<HighscorePage> {
    return this.bubbleGameService.getHighscorePage({
      username,
      startRank: startRank ? Number.parseInt(startRank, 10) : undefined,
      limit: limit ? Number.parseInt(limit, 10) : undefined,
    });
  }
}
