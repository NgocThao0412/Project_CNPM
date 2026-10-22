# REST contract mapped to the supplied source

## Existing group services (LIVE only)

| Service | Method / path | Request / response |
|---|---|---|
| auth-service :8081 | POST /api/auth/login | JSON username, password -> message, admin; Set-Cookie SGU_ADMIN_SESSION |
| auth-service | GET /api/auth/me | Cookie -> id, username, fullName, email; 401 if absent/expired |
| auth-service | POST /api/auth/logout | Revoke session |
| poi-service :8082 | GET /api/pois | Visible POIDTO array |
| poi-service | POST /api/admin/pois | Authenticated CreatePOIRequestDTO -> id, message |

POI JSON fields match original camelCase DTO: `id, poiCode, poiName, areaId, areaName, description, latitude, longitude, triggerRadiusM, imageUrl`.
No invented `/api/poi/list`, login email, JWT or Bearer token is required.

## Additions to poi-service (four separate source files)

GET `/api/admin/pois`: authenticated list including hidden POIs and `visible`.
GET `/api/campus-areas`: areas with id, areaCode, areaName, visible.
PUT `/api/admin/pois/{id}`: same CreatePOIRequestDTO fields as original POST.
PATCH `/api/admin/pois/{id}/visibility`: JSON `{"visible":false}`.
These reuse the original AuthServiceClient and JdbcTemplate, not a direct browser-to-MySQL connection.

## Narration :8083

GET `/api/languages` -> active languages (`id, languageCode, languageName`).
GET `/api/narrations?poiId=1&languageCode=vi` -> public narration list.
GET `/api/narrations/{id}` -> public item; missing/hidden -> 404.
GET `/api/admin/narrations[/id]` -> authenticated editor data.
POST `/api/admin/narrations`, PUT `/api/admin/narrations/{id}`:

```json
{"poiId":1,"languageId":1,"title":"A title","content":"Narration text","visible":true}
```

Existing narration cannot change its POI/language, matching the SQL trigger. Duplicate pair -> 409. No fallback to another language.

## Audio :8084

GET `/api/audio?contentId=1` -> active audio list with `id,contentId,fileName,durationSeconds,versionNumber,visible,audioUrl`.
GET `/api/audio?contentId=1&admin=1` -> authenticated preview metadata.
POST `/api/admin/audio?contentId=1&filename=guide.mp3`: cookie, Content-Type audio/mpeg, raw MP3 bytes, max 25 MiB -> 201 metadata.
Client browser uploads multipart form; Java Client parses it and sends the MP3 bytes to this API. Files are validated by actual Layer III frames, not filename alone.
GET / HEAD `/api/audio/{id}/stream`: MP3 or range. Supports `bytes=0-1`, `bytes=200-`, `bytes=-200`; returns 206 and Content-Range; invalid ranges -> 416. `admin=1` needs an authenticated session. Public access requires visible POI, area, content, language and audio.

## Client routes and security

Browser uses one origin :8080 and SGU_CLIENT (HttpOnly, SameSite=Lax). Backend cookie stays in a server-side Java session and is forwarded only to allowlisted services.
Login rotates Client session ID. All modifying browser forms require a CSRF token. Admin requests call auth/me, not merely a local logged-in flag.
`/media/audio/{id}` is the same-origin proxy; Range and HEAD preserved. Phones never need access to backend localhost addresses.
MP3 auto-play is deliberately absent: the visitor taps Play. Native controls vary by browser; no promise of identical pixel appearance across platforms.

## Scope and deployment limits

Separate runnable services but shared MySQL schema to preserve the user's supplied design. No database-per-service claim, service discovery, message broker, distributed transaction or production HA.
Gateway/frontend proxy buffers audio responses up to the configured upload bound; this is adequate for a course demo, not tuned for high concurrency.
Production needs TLS, private backend networking, hardened credential policy/rate limiting, file quotas, backups and monitoring. Do not expose the demo admin password on a public server.
