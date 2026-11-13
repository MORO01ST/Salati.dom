const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowTs() {
  return new Date(Date.now() - 2000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("circles").where("isPublic", "==", true).get());
  await assertFails(unauthDb.collection("achievements").where("isPublic", "==", true).get());
});

test("User profile: owner can create and update valid profile, cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "أحمد",
      friendCode: "SALATI1",
      totalPrayersLogged: 5,
      totalTasbihCount: 100,
      totalWirdPages: 4,
      currentStreak: 1,
      bestStreak: 1,
      calculationMethod: "UMM_AL_QURA",
      cityName: "الرياض",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Cross-user read & write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "مخترق",
      updatedAt: nowTs(),
    })
  );

  // Ghost field shadow update blocked
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: nowTs(),
    })
  );
});

test("DailyLogs: owner can create, update, and list own logs; cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").doc("2026-11-13").set({
      userId: ALICE_UID,
      dateKey: "2026-11-13",
      fajrDone: true,
      dhuhrDone: true,
      asrDone: false,
      maghribDone: false,
      ishaDone: false,
      wirdPagesDone: 2,
      wirdTargetPages: 4,
      dhikrCount: 33,
      morningAdhkarDone: true,
      eveningAdhkarDone: false,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dailyLogs").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("dailyLogs").get()
  );
});

test("Bookmarks: owner can create and list own bookmarks; invalid itemType blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("dua_1").set({
      userId: ALICE_UID,
      itemType: "DUA",
      title: "دعاء الصباح",
      subtitle: "أذكار الصباح",
      referenceKey: "dua_morning_1",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").doc("dua_2").set({
      userId: ALICE_UID,
      itemType: "INVALID",
      title: "bad",
      referenceKey: "bad",
      createdAt: nowTs(),
    })
  );
});

test("Circles & Members: create, public query, join, and member progress update work securely", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("circles").doc("circle_mulk").set({
      ownerId: ALICE_UID,
      title: "حلقة سورة الملك",
      surahName: "سورة الملك",
      targetVerses: 30,
      inviteCode: "MULK99",
      isPublic: true,
      memberCount: 1,
      totalMemorizedVerses: 5,
      memberUids: [ALICE_UID],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Aligned public query succeeds
  await assertSucceeds(
    bobDb.collection("circles").where("isPublic", "==", true).get()
  );

  // Unaligned list query without filter fails
  await assertFails(bobDb.collection("circles").get());

  // Bob joins public circle
  await assertSucceeds(
    bobDb.collection("circles").doc("circle_mulk").update({
      memberUids: [ALICE_UID, BOB_UID],
      memberCount: 2,
      updatedAt: nowTs(),
    })
  );

  // Bob writes own member entry
  await assertSucceeds(
    bobDb.collection("circles").doc("circle_mulk").collection("members").doc(BOB_UID).set({
      userId: BOB_UID,
      circleId: "circle_mulk",
      circleOwnerId: ALICE_UID,
      isPublic: true,
      displayName: "بكر",
      memorizedVerses: 10,
      lastSurahNote: "الآيات ١-١٠",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Bob cannot overwrite Alice's member entry
  await assertFails(
    bobDb.collection("circles").doc("circle_mulk").collection("members").doc(ALICE_UID).set({
      userId: ALICE_UID,
      circleId: "circle_mulk",
      circleOwnerId: ALICE_UID,
      isPublic: true,
      displayName: "بكر",
      memorizedVerses: 10,
      createdAt: nowTs(),
    })
  );
});

test("Achievements: author creates, friends query public and send Ameen increment, cross-user delete blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("achievements").doc("ach_1").set({
      userId: ALICE_UID,
      authorName: "أحمد",
      badgeTitle: "إتمام ورد اليوم",
      description: "أتممت ٤ صفحات من القرآن الكريم اليوم",
      category: "QURAN",
      streakDays: 7,
      ameenCount: 0,
      isPublic: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    bobDb.collection("achievements").where("isPublic", "==", true).get()
  );

  // Bob increments ameenCount by 1
  await assertSucceeds(
    bobDb.collection("achievements").doc("ach_1").update({
      ameenCount: 1,
      updatedAt: nowTs(),
    })
  );

  // Bob cannot jump ameenCount by +10 or delete Alice's achievement
  await assertFails(
    bobDb.collection("achievements").doc("ach_1").update({
      ameenCount: 11,
      updatedAt: nowTs(),
    })
  );
  await assertFails(bobDb.collection("achievements").doc("ach_1").delete());
});

test("CustomSupplications: owner can create, list, and delete personal adhkar/duas; cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customSupplications").doc("sup_1").set({
      userId: ALICE_UID,
      supplicationType: "DUA",
      title: "دعائي الخاص",
      arabicText: "اللهم بارك لي في أهلي ومالي",
      occasionNote: "مناسبات شخصية",
      targetCount: 3,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customSupplications").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customSupplications").get()
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("customSupplications").doc("sup_1").delete()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customSupplications").doc("sup_1").delete()
  );
});
