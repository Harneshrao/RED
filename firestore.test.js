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

function buildProfilePayload(uid, createdAt, updatedAt) {
  return {
    userId: uid,
    displayName: "Alice Watchmaker",
    email: "alice@example.com",
    preferredTheme: "SYSTEM",
    scansCount: 0,
    createdAt,
    updatedAt,
  };
}

function buildScanPayload(uid, createdAt, updatedAt) {
  return {
    userId: uid,
    productBrand: "Seiko",
    productModel: "SKX007 Automatic Diver",
    category: "Watches",
    generationOrEra: "1996-2019",
    movementOrPowerType: "Automatic Mechanical (7S26)",
    confidenceScore: 0.91,
    confidenceLabel: "HIGH",
    damageNotes: "Light bezel edge wear",
    missingComponentNotes: "None detected",
    needsAdditionalAngle: false,
    suggestedNextAngle: "",
    dataVerificationStatus: "VERIFIED_DATASET",
    componentCount: 1,
    createdAt,
    updatedAt,
  };
}

function buildComponentPayload(uid, scanId, createdAt, updatedAt) {
  return {
    userId: uid,
    scanId,
    name: "Watch Strap Pin (Spring Bar)",
    categoryKey: "spring_bar",
    alternativeNames: ["Spring bar", "Lug pin"],
    primaryFunction: "Secures the watch strap or bracelet between the case lugs.",
    material: "316L Stainless Steel",
    dimensions: "20 mm lug width x 1.8 mm tube",
    partNumber: "SEIKO-C200FS",
    compatibilityStatus: "VERIFIED_COMPATIBLE",
    compatibilityReason: "Matches 20 mm drilled/undrilled lug geometry of SKX007.",
    replacementDifficulty: "EASY",
    visibleInPhoto: true,
    hiddenInViewer: false,
    layerOrder: 9,
    createdAt,
    updatedAt,
  };
}

function buildSavedItemPayload(uid, createdAt, updatedAt) {
  return {
    userId: uid,
    itemType: "COMPONENT",
    referenceId: "comp_spring_bar_20mm",
    title: "20 mm Double-Flange Spring Bar",
    subtitle: "Seiko SKX007 • 316L Stainless Steel",
    category: "Watches",
    compatibilityStatus: "VERIFIED_COMPATIBLE",
    notes: "Measure lug gap before ordering.",
    createdAt,
    updatedAt,
  };
}

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("scans").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("savedItems").doc("item_1").set(
      buildSavedItemPayload(ALICE_UID, new Date(Date.now() - 1000), new Date(Date.now() - 1000))
    )
  );
});

test("User profile: owner create/read/update succeeds, cross-user & ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(buildProfilePayload(ALICE_UID, ts, ts))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredTheme: "DARK",
      scansCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user PII read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ displayName: "Hijacked" }));

  // Ghost field & future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      ...buildProfilePayload(ALICE_UID, new Date(Date.now() + 60000), ts),
    })
  );
});

test("Scans & components: atomic WriteBatch succeeds, @DocumentId id key & orphan component fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  const scanRef = aliceDb.collection("users").doc(ALICE_UID).collection("scans").doc("scan_1");
  const compRef = scanRef.collection("components").doc("comp_1");

  const batch = aliceDb.batch();
  batch.set(scanRef, buildScanPayload(ALICE_UID, ts, ts));
  batch.set(compRef, buildComponentPayload(ALICE_UID, "scan_1", ts, ts));
  await assertSucceeds(batch.commit());

  // List queries matching Kotlin repositories succeed for owner
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("scans").get());
  await assertSucceeds(scanRef.collection("components").get());

  // Partial update on component visibility succeeds
  await assertSucceeds(
    compRef.update({
      hiddenInViewer: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    scanRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Including @DocumentId "id" field fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("scans").doc("scan_2").set({
      id: "scan_2",
      ...buildScanPayload(ALICE_UID, ts, ts),
    })
  );

  // Orphan component without parent scan fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("scans").doc("missing_scan")
      .collection("components").doc("comp_orphan")
      .set(buildComponentPayload(ALICE_UID, "missing_scan", ts, ts))
  );

  // Cross-user access fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("scans").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("scans").doc("scan_1").delete());
});

test("Saved items: owner create/list/update/delete succeeds, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  const itemRef = aliceDb.collection("users").doc(ALICE_UID).collection("savedItems").doc("saved_1");
  await assertSucceeds(itemRef.set(buildSavedItemPayload(ALICE_UID, ts, ts)));
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("savedItems").get());
  await assertSucceeds(
    itemRef.update({
      notes: "Verified 20mm with digital calipers.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("savedItems").get());
  await assertSucceeds(itemRef.delete());
});
