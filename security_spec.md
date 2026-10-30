# ShareDabba Firestore Security Specification (Stage 1)

## 1. Data Invariants
- ShareDabba's production user accounts, creator earnings, and wallet ledger remain exclusively on the primary ShareDabba website backend (no separate production user database or fake financial data in Firestore).
- Firestore is used strictly for authenticated Android client state synchronization: personal media bookmarks / saved stream notes under `/users/{userId}/bookmarks/{bookmarkId}`.
- Every bookmark document must belong to the authenticated user (`request.auth.uid == userId` and `incoming().userId == request.auth.uid`).
- All string fields have strict size limits (`title`: 1..160 chars, `streamUrl`: 1..500 chars, `category`: 1..60 chars).
- Timestamps (`createdAt`, `updatedAt`) must be server-generated (`is timestamp && <= request.time`), and `createdAt` + `userId` are immutable on update.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `GET /users/alice_123/bookmarks/bm_1` with `auth = null` -> REJECT.
2. **Unauthenticated Write**: `CREATE /users/alice_123/bookmarks/bm_1` with `auth = null` -> REJECT.
3. **Cross-User Read**: `GET /users/bob_456/bookmarks/bm_1` as `alice_123` -> REJECT.
4. **Cross-User List**: `LIST /users/bob_456/bookmarks` as `alice_123` -> REJECT.
5. **Identity Spoofing on Create**: `CREATE /users/alice_123/bookmarks/bm_1` with `userId: "bob_456"` as `alice_123` -> REJECT.
6. **Shadow Field Injection on Create**: Adding `"isVerifiedCreator": true` or `"walletBalance": 9999` -> REJECT via `hasOnly()`.
7. **Shadow Field Injection on Update**: Updating with an undeclared field `"adminOverride": true` -> REJECT via `affectedKeys().hasOnly()`.
8. **Immutable Field Mutation (`userId`)**: Updating `userId` from `"alice_123"` to `"bob_456"` -> REJECT.
9. **Immutable Field Mutation (`createdAt`)**: Updating `createdAt` to a new timestamp -> REJECT.
10. **Resource / Value Poisoning (`title`)**: Sending a 5,000-character string or integer for `title` -> REJECT via `isValidBookmark()`.
11. **Path ID Poisoning**: Document ID containing illegal characters or >128 chars -> REJECT via `isValidId()`.
12. **Future Timestamp Spoofing**: Sending `createdAt` in the future (`> request.time`) -> REJECT.
