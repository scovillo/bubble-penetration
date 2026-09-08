import { Body, Controller, Get, Post, UseGuards } from '@nestjs/common';
import { Throttle, hours } from '@nestjs/throttler';
import { OriginAllowlistGuard } from '../../common/origin-allowlist.guard';
import type { CreatedPlayer, PlayerProfile } from '../bubble-game.service';
import { CreatePlayerDto } from '../dto/create-player.dto';
import { Player } from '../entities/player.entity';
import { CurrentPlayer } from './current-player.decorator';
import { PlayerCredentialGuard } from './player-credential.guard';
import { PlayerService } from './player.service';

@Controller('players')
export class PlayerController {
  constructor(private readonly playerService: PlayerService) {}

  @UseGuards(OriginAllowlistGuard)
  @Throttle({ default: { limit: 3, ttl: hours(1) } })
  @Post()
  createPlayer(
    @Body() createPlayerDto: CreatePlayerDto,
  ): Promise<CreatedPlayer> {
    return this.playerService.createPlayer(createPlayerDto.username);
  }

  @UseGuards(PlayerCredentialGuard)
  @Get('me')
  getCurrentPlayer(@CurrentPlayer() player: Player): PlayerProfile {
    return this.playerService.getPlayerProfile(player);
  }

  @UseGuards(OriginAllowlistGuard, PlayerCredentialGuard)
  @Throttle({ default: { limit: 2, ttl: hours(1) } })
  @Post('me/credentials')
  rotateCredential(
    @CurrentPlayer() player: Player,
  ): Promise<{ credential: string }> {
    return this.playerService.rotateCredential(player);
  }
}
