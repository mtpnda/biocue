// BioCue Deno KV Database Layer
// Implements specs/16_DENO_AUTH_AND_DATA_PLANE.md

export interface UserProfile {
  uid: string;
  email: string;
  displayName: string;
  photoUrl?: string;
  modes: ("WEARER" | "CAREGIVER")[];
  pairingCode?: string;
  pairingCodeExpiresAt?: number;
  createdAt: number;
  updatedAt: number;
}

export interface PairingCodeEntry {
  code: string;
  wearerUid: string;
  wearerName: string;
  expiresAt: number;
}

export interface Pairing {
  id: string;
  wearerUid: string;
  caregiverUid: string;
  wearerName: string;
  caregiverName: string;
  status: "APPROVED" | "PENDING" | "REMOVED";
  createdAt: number;
  removedAt?: number;
}

export interface StressEvent {
  id: string;
  userId: string;
  startTime: number;
  endTime?: number;
  triggerType: "MANUAL" | "AUTO";
  label: string;
  isResolved: boolean;
}

export interface CaregiverReview {
  id: string;
  pairingId: string;
  eventId: string;
  caregiverUid: string;
  note: string;
  createdAt: number;
}

export interface DeviceRegistration {
  id: string;
  uid: string;
  token: string;
  platform: "ios" | "android";
  updatedAt: number;
}

export class Database {
  private kv: Deno.Kv;

  constructor(kv: Deno.Kv) {
    this.kv = kv;
  }

  static async init(): Promise<Database> {
    const kv = await Deno.openKv();
    return new Database(kv);
  }

  get rawKv(): Deno.Kv {
    return this.kv;
  }

  // --- Users ---
  async getUser(uid: string): Promise<UserProfile | null> {
    const res = await this.kv.get<UserProfile>(["users", uid]);
    return res.value;
  }

  async getUserByEmail(email: string): Promise<UserProfile | null> {
    const res = await this.kv.get<string>(["users_by_email", email.toLowerCase()]);
    if (!res.value) return null;
    return this.getUser(res.value);
  }

  async upsertUser(profile: Partial<UserProfile> & { uid: string; email: string }): Promise<UserProfile> {
    const now = Date.now();
    const existing = await this.getUser(profile.uid);
    const updated: UserProfile = {
      uid: profile.uid,
      email: profile.email,
      displayName: profile.displayName || existing?.displayName || profile.email.split("@")[0],
      photoUrl: profile.photoUrl || existing?.photoUrl,
      modes: profile.modes || existing?.modes || ["WEARER"],
      pairingCode: profile.pairingCode !== undefined ? profile.pairingCode : existing?.pairingCode,
      pairingCodeExpiresAt: profile.pairingCodeExpiresAt !== undefined ? profile.pairingCodeExpiresAt : existing?.pairingCodeExpiresAt,
      createdAt: existing?.createdAt || now,
      updatedAt: now,
    };

    await this.kv.atomic()
      .set(["users", updated.uid], updated)
      .set(["users_by_email", updated.email.toLowerCase()], updated.uid)
      .commit();

    return updated;
  }

  // --- Pairing Codes ---
  async createPairingCode(wearerUid: string, wearerName: string): Promise<string> {
    const code = Math.floor(100000 + Math.random() * 900000).toString();
    const now = Date.now();
    const expiresAt = now + 15 * 60 * 1000; // 15 minutes TTL

    const entry: PairingCodeEntry = { code, wearerUid, wearerName, expiresAt };

    await this.kv.atomic()
      .set(["pairing_codes", code], entry, { expireIn: 15 * 60 * 1000 })
      .set(["users_pairing_code", wearerUid], code)
      .commit();

    // Update user record with pairing code
    const user = await this.getUser(wearerUid);
    if (user) {
      user.pairingCode = code;
      user.pairingCodeExpiresAt = expiresAt;
      await this.kv.set(["users", wearerUid], user);
    }

    return code;
  }

  async claimPairingCode(caregiverUid: string, caregiverName: string, code: string): Promise<Pairing> {
    const codeRes = await this.kv.get<PairingCodeEntry>(["pairing_codes", code]);
    if (!codeRes.value) {
      throw new Error("Invalid or expired pairing code");
    }

    const { wearerUid, wearerName, expiresAt } = codeRes.value;
    if (Date.now() > expiresAt) {
      await this.kv.delete(["pairing_codes", code]);
      throw new Error("Pairing code has expired");
    }

    if (wearerUid === caregiverUid) {
      throw new Error("Cannot pair with yourself");
    }

    const pairingId = `pair_${wearerUid.slice(-6)}_${caregiverUid.slice(-6)}_${Date.now()}`;
    const pairing: Pairing = {
      id: pairingId,
      wearerUid,
      caregiverUid,
      wearerName,
      caregiverName,
      status: "APPROVED",
      createdAt: Date.now(),
    };

    // Atomic claim: delete single-use code, store pairing and reverse indices
    const commit = await this.kv.atomic()
      .delete(["pairing_codes", code])
      .delete(["users_pairing_code", wearerUid])
      .set(["pairings", pairingId], pairing)
      .set(["pairings_by_wearer", wearerUid, pairingId], true)
      .set(["pairings_by_caregiver", caregiverUid, pairingId], true)
      .commit();

    if (!commit.ok) {
      throw new Error("Failed to claim pairing code, please try again");
    }

    return pairing;
  }

  async getPairingsForUser(uid: string): Promise<Pairing[]> {
    const pairings: Pairing[] = [];

    // Search where user is wearer
    const wearerIter = this.kv.list({ prefix: ["pairings_by_wearer", uid] });
    for await (const entry of wearerIter) {
      const pairingId = entry.key[2] as string;
      const p = await this.kv.get<Pairing>(["pairings", pairingId]);
      if (p.value) pairings.push(p.value);
    }

    // Search where user is caregiver
    const caregiverIter = this.kv.list({ prefix: ["pairings_by_caregiver", uid] });
    for await (const entry of caregiverIter) {
      const pairingId = entry.key[2] as string;
      const p = await this.kv.get<Pairing>(["pairings", pairingId]);
      if (p.value && !pairings.some((existing) => existing.id === p.value!.id)) {
        pairings.push(p.value);
      }
    }

    return pairings;
  }

  async getPairing(pairingId: string): Promise<Pairing | null> {
    const res = await this.kv.get<Pairing>(["pairings", pairingId]);
    return res.value;
  }

  async revokePairing(pairingId: string, callerUid: string): Promise<Pairing> {
    const pairing = await this.getPairing(pairingId);
    if (!pairing) throw new Error("Pairing not found");

    if (pairing.wearerUid !== callerUid && pairing.caregiverUid !== callerUid) {
      throw new Error("Unauthorized to revoke this pairing");
    }

    pairing.status = "REMOVED";
    pairing.removedAt = Date.now();

    await this.kv.set(["pairings", pairingId], pairing);
    return pairing;
  }

  // --- Caregiver Reviews ---
  async addCaregiverReview(review: CaregiverReview): Promise<void> {
    await this.kv.set(["reviews", review.pairingId, review.id], review);
  }

  async getReviewsForPairing(pairingId: string): Promise<CaregiverReview[]> {
    const list: CaregiverReview[] = [];
    const iter = this.kv.list<CaregiverReview>({ prefix: ["reviews", pairingId] });
    for await (const entry of iter) {
      if (entry.value) list.push(entry.value);
    }
    return list;
  }

  // --- Stress Events ---
  async saveEvent(event: StressEvent): Promise<void> {
    await this.kv.atomic()
      .set(["events", event.userId, event.id], event)
      .set(["events_by_time", event.userId, event.startTime, event.id], event)
      .commit();
  }

  async getEvent(wearerUid: string, eventId: string): Promise<StressEvent | null> {
    const res = await this.kv.get<StressEvent>(["events", wearerUid, eventId]);
    return res.value;
  }

  async getEventsForWearer(wearerUid: string, limit = 50): Promise<StressEvent[]> {
    const events: StressEvent[] = [];
    const iter = this.kv.list<StressEvent>(
      { prefix: ["events_by_time", wearerUid] },
      { limit, reverse: true }
    );
    for await (const entry of iter) {
      if (entry.value) events.push(entry.value);
    }
    return events;
  }

  // --- Devices & Push Tokens ---
  async registerDevice(device: DeviceRegistration): Promise<void> {
    await this.kv.set(["devices", device.uid, device.id], device);
  }

  async getDevices(uid: string): Promise<DeviceRegistration[]> {
    const devices: DeviceRegistration[] = [];
    const iter = this.kv.list<DeviceRegistration>({ prefix: ["devices", uid] });
    for await (const entry of iter) {
      if (entry.value) devices.push(entry.value);
    }
    return devices;
  }

  async deleteDevice(uid: string, deviceId: string): Promise<void> {
    await this.kv.delete(["devices", uid, deviceId]);
  }

  // --- Push Logs ---
  async getPushLog(wearerUid: string, eventId: string): Promise<string[]> {
    const res = await this.kv.get<{ caregivers: string[]; sentAt: number }>(["push_logs", wearerUid, eventId]);
    return res.value?.caregivers || [];
  }

  async savePushLog(wearerUid: string, eventId: string, caregivers: string[]): Promise<void> {
    await this.kv.set(["push_logs", wearerUid, eventId], {
      caregivers,
      sentAt: Date.now(),
    });
  }
}
