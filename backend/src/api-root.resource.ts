import { ApiProperty } from '@nestjs/swagger';
import 'dotenv/config';
import packageMetadata from '../package.json';

export class ApiRootResource {
  @ApiProperty()
  name: string;

  @ApiProperty()
  version: string;

  @ApiProperty()
  apiVersions: { version: string; baseUrl: string }[];

  @ApiProperty()
  healthEndpoint: string;

  constructor() {
    this.name = packageMetadata.name;
    this.version = packageMetadata.version;
    this.apiVersions = [{ version: 'v2', baseUrl: '/api/v2' }];
    this.healthEndpoint = '/health';
  }
}
