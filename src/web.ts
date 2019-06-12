import { WebPlugin } from '@capacitor/core';
import { MongoDBMobilePlugin } from './definitions';

export class MongoDBMobileWeb extends WebPlugin implements MongoDBMobilePlugin {
  constructor() {
    super({
      name: 'MongoDBMobile',
      platforms: ['web']
    });
  }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  }
}

const MongoDBMobile = new MongoDBMobileWeb();

export { MongoDBMobile };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(MongoDBMobile);
