# ShareDabba Stage 2 — Website & Backend Integration Contract (Pending Repository / API Docs)

> **Status:** `AWAITING_REPOSITORY_OR_API_DOCS`
> The GitHub repository URL in the prompt was passed as the placeholder `[INSERT GITHUB REPOSITORY URL]`. Per Stage 2 instructions, **no endpoint names, Firebase collections, credentials, or response schemas have been invented or guessed**.

---

## 1. Required Website Architecture Discovery Checklist

Please provide the actual GitHub repository URL (if public) or paste/upload the relevant files (`package.json`, backend routes, database schema/rules, `.env.example`, and player components) so we can verify and lock in these 11 areas:

| # | Architecture Area | Discovery Status | Files / Details Needed from Website Repo |
|---|---|---|---|
| 1 | **Website Framework** | Pending Inspection | `package.json`, `vite.config.*` / `next.config.*` / framework entry files |
| 2 | **Backend Framework** | Pending Inspection | Server entry (`server.ts`/`index.js`), API routes, Cloud Functions, or Next.js/Express routes |
| 3 | **Database Provider** | Pending Inspection | Database client config (Firestore / Supabase / PostgreSQL / MongoDB / Prisma / Drizzle) |
| 4 | **Authentication Provider** | Pending Inspection | Auth config (Firebase Auth / NextAuth / Clerk / custom JWT / OAuth client IDs) |
| 5 | **File Storage Provider** | Pending Inspection | Video/thumbnail storage SDK (Firebase Storage / AWS S3 / Cloudflare R2 / BunnyStream / GCS) |
| 6 | **Video Playback Architecture** | Pending Inspection | Stream delivery format (HLS `.m3u8`, DASH `.mpd`, progressive MP4, signed CDN URLs, DRM) |
| 7 | **Existing Share-Link Format** | Pending Inspection | Route definitions for shared links (e.g., `/watch/:id`, `/v/:slug`, `/s/:code`, query params) |
| 8 | **Existing API Endpoints** | Pending Inspection | Route handlers, controllers, or OpenAPI/Swagger specs |
| 9 | **Admin Panel Architecture** | Pending Inspection | Admin role verification, moderation routes, and remote config/settings collections |
| 10 | **Advertising Integration** | Pending Inspection | Ad network SDKs, VAST/VMAP tag configuration, or custom ad-slot models |
| 11 | **Wallet & Earnings Architecture** | Pending Inspection | Creator view/impression counting, payout ledger tables/collections, and withdrawal endpoints |

---

## 2. API Endpoint Contract Specification Template

Once you provide the repository URL or route files, each existing endpoint will be documented here with its exact production signature:

### Endpoint Entry Template
- **Actual URL Path:** `<To be populated from website source>`
- **HTTP Method:** `GET | POST | PUT | PATCH | DELETE`
- **Authentication Requirements:** `<Bearer Firebase ID Token | Session Cookie | API Key>`
- **Request Headers & Fields:** `<Exact JSON body / query parameters / multipart fields>`
- **Response Fields (2xx):** `<Exact JSON payload structure>`
- **Error Formats (4xx/5xx):** `<Exact error status code and JSON error envelope>`

---

## 3. Android Integration Preparation Completed in Stage 2

While awaiting the actual repository URL or API documentation, the Android codebase has been prepared with:
1. **Zero Invented Endpoints:** Removed all placeholder endpoint paths from `ShareDabbaApiService` so no unverified routes are called.
2. **Authenticated HTTP Interceptor (`AuthHeaderInterceptor`):** Ready in `com.example.network` to attach the authenticated user's token (such as `FirebaseAuth.getInstance().currentUser?.getIdToken(false)`) to outbound requests once the website's auth header format is confirmed.
3. **Standardized Network Transport (`NetworkResult<T>`):** Explicit `Success`, `HttpError(code, rawBody)`, `NetworkFailure`, and `UnconfiguredContract` states in `com.example.network` for deterministic error handling.
4. **Integration Contract Registry (`IntegrationContractRegistry`):** Exposes the live readiness state of all 11 website integration pillars inside the Android app's **Settings & Diagnostics** screen without claiming the backend connection is complete.
