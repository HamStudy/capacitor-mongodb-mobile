import { Plugins } from '@capacitor/core';
import { MongoDBMobilePlugin } from './definitions';

import {
  setMongoMobilePlugin
} from './Wrapper';

const MongoDBMobile = Plugins.MongoDBMobile as MongoDBMobilePlugin;

setMongoMobilePlugin(MongoDBMobile);

export * from './definitions';
export * from './web';
