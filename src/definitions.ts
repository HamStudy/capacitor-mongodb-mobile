declare module "@capacitor/core" {
  interface PluginRegistry {
    MongoDBMobile: MongoDBMobilePlugin;
  }
}

export interface MongoDBMobilePlugin {
  echo(options: { value: string }): Promise<{value: string}>;
}
