import { Db } from "./db";


export class Collection {
  constructor(private db: Db, public name: string) {
  }
}