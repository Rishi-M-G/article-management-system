# FAANG Backend Interview Questions
### Spring Boot · REST · Security · System Design · Testing · Observability
*Export this file as PDF from any Markdown viewer (VS Code → Right-click → Open Preview → Print to PDF)*

---

## Section 1 — Exception Handling & Error Design

---

**Q1. An engineer wants to return `"Email not found"` vs `"Wrong password"` on login for better UX. How do you respond?**

**Never do this.** The principle at stake is **User Enumeration**, classified under **OWASP A07 — Identification and Authentication Failures**.

Differential error messages let an attacker feed a list of leaked emails (these lists have billions of entries, freely available) against your login endpoint and determine which emails have accounts on your platform — before attempting a single password. That's the setup phase for a **credential stuffing attack**.

But there's a second layer most developers miss: **timing attacks**. Even with identical messages, `BCryptPasswordEncoder.matches()` takes ~80ms. If you short-circuit on "email not found" and skip the bcrypt call, your endpoint returns in 2ms for invalid emails and 80ms for valid ones. An attacker measuring response latency can enumerate valid accounts even when messages are identical.

**The fix:**
1. Return identical message for both cases: `"Invalid credentials"` — no indication of which field is wrong.
2. Always run the bcrypt comparison, even for non-existent users — compare against a dummy pre-computed hash to burn the same time.
3. Return the same HTTP status code for both (401 or 404 — pick one, be consistent).

```java
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new ResourceNotFoundException("User", "credentials"));

if (!passwordEncoder.matches(password, user.getPassword())) {
    throw new ResourceNotFoundException("User", "credentials"); // same exception, same message
}
```

---

**Q2. What is the difference between HTTP 400 Bad Request and 422 Unprocessable Entity?**

These are semantically distinct and frequently confused.

**400 Bad Request** — the server could not parse or understand the request at all. The *structure* is broken.
- Malformed JSON (`{name: }`)
- Wrong Content-Type header
- `"rating": "five"` when an integer is expected — type mismatch

**422 Unprocessable Entity** — the server understood the request perfectly; it's structurally valid. But the *business logic* rejects it.
- `"rating": 99` where max is 5 — valid JSON, valid type, but violates a business rule
- Registering with an email that already exists — request is well-formed, but the operation can't be completed
- Submitting a rating for a non-existent article — request structure is fine; the referenced resource doesn't exist in a way that violates a rule

**Rule of thumb:** Syntax/structure failure → 400. Semantic/business rule failure → 422.

In Spring Boot, `MethodArgumentNotValidException` (from `@Valid`) triggers on constraint violations like `@Max(5)` — this should return 422, not 400. Spring defaults to 400 for this; you need to override it in `GlobalExceptionHandler`.

---

**Q3. Your `GlobalExceptionHandler` has a catch-all `Exception` handler. Is this the right design? What are the trade-offs?**

Yes, the catch-all is the right design, but the reasoning matters more than the answer.

**What it does:** Any exception not handled by a more specific `@ExceptionHandler` bubbles up to the catch-all. It returns a generic 500 with no internal details — no class names, no stack traces, no error message from the code.

**Why this is correct:** Returning stack traces and internal class names is **OWASP A05 — Security Misconfiguration / Information Disclosure**. An attacker who sees `NullPointerException at com.yourcompany.service.ArticleService.getArticleById:43` now knows your package structure, your service names, and the exact line causing the issue. That's reconnaissance for free.

**What you do instead:** Log the full stack trace internally, tagged with a `traceId`:
```java
@ExceptionHandler(Exception.class)
public ProblemDetail handleGeneric(Exception ex) {
    logger.error("Unhandled exception traceId={}", MDC.get("traceId"), ex); // full stack trace in logs
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred"); // nothing useful to client
    pd.setProperty("traceId", MDC.get("traceId")); // client gets the ID to report
    return pd;
}
```

**Trade-off of having it:** Prevents Spring's default `BasicErrorController` from leaking internals. Clean 500s always.

**Trade-off against:** During development, clean 500s can make bugs feel less visible. Discipline in checking logs solves this — never trust a clean 500 during development.

---

**Q4. What is RFC 9457 `application/problem+json` and why is it preferred over a custom error envelope?**

RFC 9457 (Problem Details for HTTP APIs) is an IETF standard that defines a common shape for HTTP error responses:

```json
{
  "type": "https://api.yourapp.com/errors/not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Article not found: 42",
  "instance": "/articles/42"
}
```

**Why it's preferred over custom envelopes like `{ success: false, message: "...", errorCode: 42 }`:**

1. **Interoperability** — Any HTTP client library that understands RFC 9457 can parse your errors without custom code. AWS, Stripe, Google all converge on this shape.
2. **Single parser** — Your frontend writes one error handler for every endpoint, every status code, forever.
3. **Extensible without breaking** — You add custom fields (`traceId`, `resourceType`) as additional properties; the standard fields remain stable.
4. **`type` as a contract** — The `type` URI is a stable identifier for the error class. Clients can write `if (error.type === 'https://.../not-found')` logic that survives message wording changes.

Spring Boot 3 ships native `ProblemDetail` support — enable it with `spring.mvc.problemdetails.enabled: true` and it's the default response shape for all `@RestControllerAdvice` handlers.

---

**Q5. What is `@RestControllerAdvice` and how is it different from putting `@ExceptionHandler` directly on a controller?**

`@ExceptionHandler` on a controller class applies only to exceptions thrown *within that specific controller*. If `ArticleController` has `@ExceptionHandler(ResourceNotFoundException.class)`, exceptions from `UserController` are not caught by it.

`@RestControllerAdvice` (= `@ControllerAdvice` + `@ResponseBody`) is a **global interceptor** — it applies across all controllers in the application. One class, one place, all exceptions. This is the **Single Responsibility Principle** applied to error handling: one class is responsible for translating domain exceptions to HTTP responses.

`ResponseEntityExceptionHandler` (the class you extend) pre-handles all Spring MVC internal exceptions (`MethodArgumentNotValidException`, `HttpMessageNotReadableException`, etc.) and gives you override hooks so you can customize their shape while keeping the default behavior for anything you don't override.

---

## Section 2 — REST API Design & HTTP

---

**Q6. What makes an HTTP endpoint RESTful? What are the most common REST violations you see in real codebases?**

REST (Representational State Transfer) has six constraints. The ones that matter most in practice:

**Resources, not actions** — URLs identify resources, not operations.
- Wrong: `POST /createUser`, `GET /getUserById?id=5`, `POST /deleteArticle`
- Right: `POST /users`, `GET /users/5`, `DELETE /articles/5`

**HTTP methods carry the verb** — `GET` reads, `POST` creates, `PUT` full-replaces, `PATCH` partial-updates, `DELETE` removes. Using `POST` for everything is the most common violation.

**Stateless** — every request must contain all information needed to process it. No session state on the server. JWTs are the stateless auth mechanism.

**Consistent status codes** — 201 for creation, 200 for successful reads/updates, 204 for successful deletes with no body, 404 for not found, 409 for conflicts, 422 for validation failures.

**Most common violations in real codebases:**
- `GET /articles/all` — "all" is not a resource, it's a query modifier. Use `GET /articles`.
- `@RequestParam` for resource IDs — `GET /articles?id=5` should be `GET /articles/5`.
- Returning 200 with `{ success: false }` — if it failed, use a 4xx or 5xx status code.
- camelCase in URLs — `/updateProfile` should be `/profiles/{id}` via `PUT`.

---

**Q7. Explain idempotency. Which HTTP methods must be idempotent? How would you implement an idempotency key for payments or critical writes?**

**Idempotency** means calling the same operation multiple times produces the same result as calling it once. `f(f(x)) = f(x)`.

**HTTP method idempotency:**
- `GET`, `HEAD`, `OPTIONS`, `DELETE`, `PUT` — must be idempotent by spec
- `POST` — not idempotent by spec (submitting a form twice creates two records)
- `PATCH` — not idempotent by default (though it can be designed to be)

**Why it matters in production:** Networks fail. Clients retry. A payment endpoint that isn't idempotent charges a customer twice on retry. A rating endpoint that isn't idempotent double-counts ratings (which is exactly the bug in the codebase we're working on — `updateRating()` increments `ratingCount` every call).

**Idempotency key pattern (used by Stripe, PayPal):**
```
POST /payments
Idempotency-Key: a3f9b2c1-d4e5-...
```
Server stores `(idempotencyKey, response)` in a database or Redis. On retry, if the key is seen again, return the stored response without executing the operation again. Key expires after 24-48 hours.

For your rating endpoint: the unique constraint on `(user_id, article_id)` is your idempotency mechanism at the DB layer. Add the application-layer check (`existsByUserIdAndArticlesId`) to return 409 instead of letting the DB constraint throw.

---

**Q8. What is cursor-based pagination and when does offset-based pagination break down?**

**Offset-based:** `SELECT * FROM articles OFFSET 100 LIMIT 20`

Works fine at small scale. Breaks down when:
- **Data changes during pagination** — if items are inserted between page 1 and page 2, records shift. A user paginating through 10 pages will see duplicates or miss records. At high write volumes, this is constant noise.
- **Deep offsets are expensive** — `OFFSET 50000 LIMIT 20` forces the DB to scan and discard 50,000 rows before returning 20. At 1M rows, this is a full table scan every page request.

**Cursor-based:** `SELECT * FROM articles WHERE created_at < :cursor ORDER BY created_at DESC LIMIT 20`

The cursor is an opaque pointer to the last seen record (usually a timestamp or encoded ID). Each response includes a `nextCursor` token. The query uses an indexed column, so it's always O(log n) regardless of depth. Social feeds (Twitter, Instagram) use cursor pagination because they have high write rates and deep pagination.

**Trade-off:** Cursor pagination doesn't support random access ("go to page 47"). For admin dashboards where random access matters, offset is fine. For user-facing infinite scroll feeds with high write rates, cursor is mandatory.

---

**Q9. What is the difference between 401 Unauthorized and 403 Forbidden?**

These are consistently confused, even by experienced developers.

**401 Unauthorized** — the request lacks valid authentication credentials. "I don't know who you are." The correct response is to authenticate and try again. Despite the name "Unauthorized," it actually means *Unauthenticated*.
- No `Authorization` header
- Expired JWT token
- Malformed token

**403 Forbidden** — the request is authenticated (the server knows who you are) but you don't have permission to perform this action. "I know who you are, and you can't do this."
- A user trying to edit another user's article
- A regular user trying to access an admin endpoint
- A valid JWT but missing the required role

**In Spring Security:**
- `.authenticationEntryPoint(...)` handles 401 — fires when no authentication exists
- `.accessDeniedHandler(...)` handles 403 — fires when authentication exists but authorization fails

---

**Q10. How do you version an API without breaking existing clients?**

Three main strategies, each with real trade-offs.

**URL path versioning:** `/v1/articles`, `/v2/articles`
- Most common, most visible. Easy to test in a browser. Violates REST purity (the version is not part of the resource identity) but industry has converged on this.
- Used by: Stripe, Twilio, GitHub

**Header versioning:** `Accept: application/vnd.articlemanager.v2+json`
- Keeps URLs clean. Harder to test. Can't bookmark versioned URLs.

**Query param:** `/articles?version=2`
- Easy to implement. Not recommended — query params are for filtering, not API version.

**The real skill is making breaking changes rare:**
- Additive changes (new fields) are never breaking — old clients ignore unknown fields
- Removing or renaming fields IS breaking — requires a version bump
- **Expand-contract pattern:** First expand (add the new field alongside the old one). Let clients migrate. Then contract (remove the old field in v2). Never remove without a version.
- **Sunset headers:** `Deprecation: Sat, 01 Jan 2027 00:00:00 GMT` — tell clients programmatically before you kill an endpoint.

---

**Q11. What is the difference between `PUT` and `PATCH`?**

**PUT** replaces the entire resource. If your `Articles` entity has 10 fields and you `PUT` with 3 fields, the other 7 are set to null/default. PUT must include the complete representation of the resource.

**PATCH** applies a partial update. Only send the fields you want to change. The server merges the changes into the existing resource.

Your `updateProfile` endpoint accepts `firstName`, `lastName`, `email`. If you implement it as `PUT`, a client that only wants to change `firstName` must also send `lastName` and `email` or risk clearing them. That's a bug. It should be `PATCH`.

**Idempotency:** `PUT` is idempotent (applying the same full replacement twice produces the same result). `PATCH` is not necessarily idempotent — `PATCH { "count": +1 }` applied twice increments twice.

---

**Q12. What is HATEOAS? When does it make sense and when is it overkill?**

**HATEOAS** (Hypermedia As The Engine Of Application State) — responses include links to related actions, so clients discover the API dynamically rather than having its structure hardcoded.

```json
{
  "id": 42,
  "heading": "My Article",
  "_links": {
    "self": { "href": "/articles/42" },
    "author": { "href": "/users/7" },
    "rate": { "href": "/articles/42/ratings", "method": "POST" }
  }
}
```

**When it makes sense:** Public APIs with many diverse clients that you don't control — clients can navigate the API without reading documentation. Useful for long-lived APIs where endpoint URLs might change (clients follow links rather than hardcoding paths).

**When it's overkill:** Internal APIs where you control both client and server. You know the URL structure; you can change both ends simultaneously. HATEOAS adds serialization overhead and response size for no practical benefit. Most startups and internal systems skip it entirely.

---

## Section 3 — Security

---

**Q13. What is CSRF? Why is it disabled in stateless JWT APIs?**

**CSRF (Cross-Site Request Forgery)** — an attacker tricks a logged-in user's browser into making a request to your API using the user's stored credentials (session cookie) without the user's knowledge. The browser automatically sends cookies with every request to the target domain, including malicious cross-origin requests.

**Why it works with cookies:** Browser automatically attaches `session_id` cookie to every request to `yourapp.com` — even if the request was initiated by `evil.com`.

**Why it doesn't apply to JWT in Authorization headers:** Browsers do NOT automatically attach `Authorization` headers to cross-origin requests. An attacker's site can't set the `Authorization: Bearer <token>` header because they don't have the token. CSRF relies on automatic credential attachment — JWTs in headers opt out of that mechanism entirely.

**The rule:** If you use cookies for authentication, you need CSRF tokens. If you use JWT in `Authorization` headers, CSRF protection is irrelevant (but make sure your JWT is NOT in a cookie — storing JWT in `HttpOnly` cookies reintroduces the CSRF vulnerability).

---

**Q14. Explain the JWT structure. What should and should NOT be in the payload?**

JWT = `base64(header).base64(payload).signature`

**Header:** Algorithm + token type
```json
{ "alg": "HS256", "typ": "JWT" }
```

**Payload (Claims):**
```json
{
  "sub": "user@email.com",
  "userId": 42,
  "iat": 1716000000,
  "exp": 1716000900
}
```

**SHOULD be in payload:**
- `sub` (subject — typically email or user ID)
- `userId` (to avoid a DB lookup on every request)
- `iat` (issued at — for audit)
- `exp` (expiration — for security)
- Roles/permissions if you need RBAC on stateless validation

**SHOULD NOT be in payload:**
- **Passwords** — even hashed. The payload is base64-encoded, not encrypted. Anyone with the token can decode and read it.
- **Sensitive PII** — address, phone number, SSN
- **Large objects** — JWTs are sent on every request. 5KB payload = 5KB overhead on every API call.
- **Mutable data you need to be fresh** — if you put `{ "plan": "free" }` in the JWT, a user who upgrades to paid still has "free" in their token until it expires.

**Key insight:** JWT is signed (integrity guaranteed) but NOT encrypted (confidentiality not guaranteed). For sensitive payload, use JWE (JSON Web Encryption) or just store the data server-side and look it up.

---

**Q15. Why should JWTs be short-lived (15 minutes)? How do you handle token refresh without forcing re-login every 15 minutes?**

**Why short-lived:** JWTs are stateless — once issued, the server has no way to revoke them before expiry without maintaining a blacklist (which defeats the purpose of stateless auth). If a JWT is stolen (XSS attack, log leak, man-in-the-middle), the attacker can use it until it expires. A 15-minute window limits the blast radius. A 30-day token is essentially a permanent credential if stolen.

**Refresh token pattern:**
- **Access token:** Short-lived (15 min), stateless JWT, sent with every API request in `Authorization` header
- **Refresh token:** Long-lived (7-30 days), opaque random string, stored in DB, sent ONLY to `/auth/refresh` endpoint — ideally in an `HttpOnly` cookie so JavaScript can't access it

When the access token expires, the client silently calls `/auth/refresh` with the refresh token. Server validates the refresh token against its DB (this is the one stateful lookup), issues a new access token. User never sees a login prompt.

**Revocation:** You can invalidate a refresh token server-side (on logout, on password change, on suspicious activity). This gives you the best of both worlds: stateless requests for 99.9% of traffic, revocability when needed.

---

**Q16. Can you get SQL injection with JPA/Hibernate? How?**

Yes. JPA protects you from SQL injection *when you use JPQL parameterized queries or Spring Data derived methods*. It does NOT protect you when you concatenate strings into native queries.

**Safe:**
```java
@Query("SELECT a FROM Articles a WHERE a.slug = :slug")
Optional<Articles> findBySlug(@Param("slug") String slug);
```

**Vulnerable:**
```java
@Query(value = "SELECT * FROM articles WHERE slug = '" + slug + "'", nativeQuery = true)
```

String concatenation into `nativeQuery = true` is textbook OWASP A03 SQL injection, even inside JPA. The parameterized form with `:slug` is safe because the driver separates the query structure from the data — the data can never be interpreted as SQL syntax.

---

**Q17. What is defense in depth?**

**Defense in depth** is a security principle: never rely on a single control. Layer multiple independent defenses so that if one fails, others still protect the system.

In your codebase:
1. **Validation layer:** `@NotNull`, `@Max(5)` on DTOs — catch bad input before it reaches the service
2. **Service layer:** Business rule checks before DB operations (`existsByUserIdAndArticlesId`)
3. **Database layer:** `CHECK (rating >= 0 AND rating <= 5)`, `UNIQUE (user_id, article_id)` constraints
4. **Authentication layer:** JWT validation on protected endpoints
5. **Authorization layer:** Verify the authenticated user owns the resource they're modifying

Each layer can fail (misconfiguration, code bug, missing annotation) — but an attacker has to defeat all of them. Validate on both client and server for the same reason. Defense in depth is why OWASP's recommended approach is "validate on the boundary AND in the domain."

---

## Section 4 — Spring Boot & JPA

---

**Q18. What does `@Transactional` actually do under the hood in Spring?**

Spring wraps your method in a **proxy**. When you call `articleService.addArticle(...)`, you're actually calling a Spring-generated proxy class that wraps your method. The proxy:

1. Opens a JDBC connection and begins a transaction (`BEGIN`)
2. Calls your actual method
3. If the method returns normally: commits (`COMMIT`) and closes the connection
4. If the method throws a `RuntimeException`: rolls back (`ROLLBACK`) and closes the connection

**What this means practically:**
- `@Transactional` only works when called from *outside* the class. If method A calls `@Transactional` method B in the same class, the proxy is bypassed — no transaction. This is the most common `@Transactional` bug.
- The transaction wraps at the method boundary. DB operations inside the method share a single connection and are atomic.
- Dirty checking: Hibernate tracks entity state within the transaction. You don't need to call `save()` on entities you loaded within the transaction — mutations are flushed automatically at commit.

---

**Q19. What is the N+1 query problem? How does it relate to `FetchType.EAGER`?**

N+1 is one of the most common performance bugs in JPA applications.

If you load 100 articles, and each article has an `@ManyToOne` author with `FetchType.EAGER`, Hibernate executes:
- 1 query: `SELECT * FROM articles LIMIT 100`
- 100 queries: `SELECT * FROM users WHERE id = ?` — once per article, for each author

That's 101 queries instead of 1 (or 2 with a JOIN). At 1,000 articles it's 1,001 queries. This is the N+1 problem.

**`FetchType.EAGER` makes it automatic and invisible** — Hibernate loads the association immediately without you asking, so you don't see the queries unless `show-sql` is on.

**Fixes:**
- `@EntityGraph` — tell Spring Data to JOIN FETCH the association in a specific query
- `@Query("SELECT a FROM Articles a JOIN FETCH a.author")` — explicit join fetch
- `FetchType.LAZY` + load within the transaction — default to lazy, only fetch what you need

Your `Articles.author` is currently `FetchType.EAGER`. The `getAllArticles()` endpoint with no limit loads all articles + fires N user queries. That's the specific N+1 in your codebase.

---

**Q20. What is OSIV (Open Session in View) and why should it be disabled?**

OSIV keeps the JPA `EntityManager` (Hibernate session) open for the entire lifecycle of an HTTP request — including during JSON serialization by Jackson, which happens after your controller method returns.

**The problem:** Jackson can trigger lazy-load database queries during serialization, completely outside your `@Transactional` boundaries. You think your transaction is closed, but Hibernate is still hitting the DB. You get invisible DB queries you didn't write, in a layer that shouldn't touch the DB. These queries don't benefit from your caching, your transaction optimizations, or your error handling.

**What happens when you disable it (`spring.jpa.open-in-view: false`):** Any lazy association accessed outside a transaction throws `LazyInitializationException`. This is actually good — it forces you to load everything you need within the transaction boundary and map to DTOs before the transaction closes. Your code becomes explicit about what it fetches.

**The rule:** Disable OSIV. Let `LazyInitializationException` be your guide to where you're missing explicit fetches.

---

**Q21. What is Hibernate dirty checking?**

Within a `@Transactional` method, every entity you load from the DB is tracked by Hibernate's **first-level cache** (the `EntityManager` session). At commit time, Hibernate compares the current state of every tracked entity against the snapshot it took when it was loaded. If any field changed, Hibernate generates and executes an `UPDATE` automatically — you never called `save()`.

This is why `articleRepository.save(article)` inside a `@Transactional` method is often redundant — the dirty check would have flushed the change anyway. Calling `save()` explicitly is harmless but shows you understand what's happening.

**The footgun:** Without `@Transactional`, there is no session, no dirty checking, no flush. Changes to entity fields are silently lost. This is exactly the bug in `ArticleService.updateRating()` before we added `@Transactional`.

---

**Q22. What is HikariCP and why does connection pool sizing matter?**

HikariCP is Spring Boot's default JDBC connection pool. It maintains a pre-created pool of database connections so requests don't pay the overhead of creating a new TCP connection to PostgreSQL on every query (~100-300ms).

**Why sizing matters:**

- Too few connections: requests queue up waiting for an available connection. Under load, this becomes a bottleneck — all threads block on DB access. You'll see `Connection is not available, request timed out` exceptions.
- Too many connections: PostgreSQL has a hard connection limit (default 100). Each connection uses ~5-10MB of PostgreSQL RAM. 200 connections from 10 application instances = 2,000 connections and likely PostgreSQL OOM.

**The counterintuitive rule (from HikariCP's own documentation):** For most OLTP workloads, `maximum-pool-size = (2 * CPU cores) + number_of_spindles`. On a 4-core machine, that's 9. More connections beyond that don't improve throughput — they increase context-switching overhead and PostgreSQL memory pressure.

**In `application.yaml`:**
```yaml
spring.datasource.hikari:
  maximum-pool-size: 10
  minimum-idle: 2
  connection-timeout: 30000   # 30s — fail fast rather than hang forever
```

---

## Section 5 — System Design

---

**Q23. Design a rate limiter for an API endpoint.**

The goal: limit each user to N requests per time window (e.g., 100 requests per minute).

**Two main algorithms:**

**Token Bucket:** Each user has a bucket with capacity N. A token is added every `(window / N)` seconds. Each request consumes a token. If the bucket is empty, the request is rejected. Allows short bursts up to capacity N.

**Sliding Window Counter:** Store request count per user in Redis with a TTL of the window size. Increment on each request. If count > N, reject. More memory efficient, smoother rate limiting.

**Implementation with Redis (production approach):**
```
INCR rate_limit:{userId}:{minute_bucket}
EXPIRE rate_limit:{userId}:{minute_bucket} 60
```

**Spring + Bucket4j library** handles this at the filter level without Redis for single-instance apps.

**What an interviewer is really asking:** Do you know about the distributed case? A rate limiter that works on a single instance fails when you have 10 app servers — each has its own counter. You need Redis as a shared counter. Also: do you return 429 Too Many Requests with a `Retry-After` header? That's the correct HTTP behavior.

---

**Q24. How would you handle a schema migration on a 100-million-row table with zero downtime?**

`ALTER TABLE` with `ADD COLUMN NOT NULL` on a 100M row table takes an exclusive lock for minutes. During that time, all reads and writes to the table fail. In production, this is a service outage.

**The expand-contract pattern (the production answer):**

**Phase 1 — Expand:** Add the new column as nullable, no default:
```sql
ALTER TABLE articles ADD COLUMN new_column VARCHAR(255);  -- instant, no lock
```

**Phase 2 — Backfill:** Update existing rows in batches to avoid lock escalation:
```sql
UPDATE articles SET new_column = 'default' 
WHERE id BETWEEN 1 AND 10000;  -- batch by primary key range
```
Run this in a script with `pg_sleep(0.01)` between batches to give the DB breathing room.

**Phase 3 — Constrain:** Add the NOT NULL constraint after all rows are populated:
```sql
ALTER TABLE articles ALTER COLUMN new_column SET NOT NULL;
```

**Phase 4 — Contract (later):** Remove the old column once all code is migrated.

This is why you use Flyway and write new migrations instead of editing old ones. Each phase is a separate versioned migration file.

---

**Q25. What is the CAP theorem? What does your PostgreSQL-backed application provide?**

CAP theorem: In a distributed system experiencing a network partition, you can guarantee at most two of three properties:
- **C — Consistency:** Every read gets the most recent write
- **A — Availability:** Every request receives a response (not necessarily the latest data)
- **P — Partition tolerance:** The system continues operating despite network partitions

**The key insight:** You must always tolerate partitions in distributed systems (networks fail). So the real choice is CP vs AP when a partition occurs.

**Your PostgreSQL application:** Single-node Postgres is **CA** — no partition tolerance because there's no distribution. When you add replication (primary + read replicas), it becomes **CP** — you sacrifice availability to maintain consistency (a primary failure causes downtime until failover) or you choose eventual consistency (reads from replicas may be slightly stale = AP).

**Practical relevance for your app:** Your current single-RDS setup is CP. If the RDS goes down, your app returns errors (not stale data). AWS Multi-AZ gives you automatic failover (~30s downtime). Aurora Global Database gives you a read replica in another region — reads become eventually consistent (usually milliseconds behind).

---

**Q26. Design a caching layer for your article reads.**

The question tests layered thinking: where to cache, what to cache, when to invalidate.

**Layer 1 — HTTP caching:** `Cache-Control: public, max-age=300` on `GET /articles/{id}` responses. CDN (CloudFront) caches the response for 5 minutes. Zero application code, massive scale reduction. Trade-off: published changes take up to 5 minutes to propagate.

**Layer 2 — Application cache (Redis):** Cache `ArticleResponseDTO` in Redis by article ID. TTL = 5-10 minutes.
```java
@Cacheable(value = "articles", key = "#id")
public ArticleResponseDTO getArticleById(Long id) { ... }

@CacheEvict(value = "articles", key = "#id")
public void updateArticle(Long id, ...) { ... }
```

**Cache invalidation (the hard part):** What invalidates the cache?
- Article updated → evict by ID ✓
- Author's name changed → every article by that author is stale. You'd need `@CacheEvict(allEntries = true)` or a smarter key strategy.
- Rating changes every submission → don't cache ratings separately; recompute from the article's denormalized `averageRatings` field.

**The senior answer:** Caching is a trade-off between **read latency** and **consistency**. TTL-based caching accepts eventual consistency (data can be stale for up to TTL). Event-driven invalidation (`@CacheEvict`) achieves strong consistency but requires every write path to correctly evict — one missed `@CacheEvict` means stale data forever. Pick based on your consistency requirements.

---

**Q27. How would you design the rating system to be correct under concurrent writes?**

Your current rating flow:
1. Check if user already rated (`existsByUserIdAndArticlesId`)
2. Save the rating row
3. Update `averageRatings` and `ratingCount` on the article

**The race condition:** Two requests for the same `(userId, articleId)` land simultaneously. Both pass the existence check (step 1), both save a rating row (the second one hits the UNIQUE constraint and fails), but now your article stats are wrong.

**Fix 1 — Database unique constraint as the source of truth:** The UNIQUE constraint is your real idempotency guarantee. The application-level check is an optimization to return 409 cleanly. Wrap the `ratingRepository.save()` in a try-catch for `DataIntegrityViolationException` as a safety net.

**Fix 2 — Optimistic locking on the article:** Add `@Version Long version` to `Articles`. Hibernate will add `WHERE version = :expected` to every UPDATE. If two requests try to update the article stats simultaneously, one succeeds and the other throws `OptimisticLockingFailureException`. Retry the failed request.

**Fix 3 — Atomic DB update:** Instead of load-mutate-save:
```sql
UPDATE articles 
SET average_ratings = (average_ratings * rating_count + :newRating) / (rating_count + 1),
    rating_count = rating_count + 1
WHERE id = :articleId
```
A single atomic statement — no read-modify-write race condition possible.

Fix 3 is the production answer. It's a single SQL statement that PostgreSQL executes atomically.

---

**Q28. What is eventual consistency? Give an example relevant to your article management system.**

**Eventual consistency** means that, given no new updates, all replicas of a piece of data will converge to the same value — eventually. There is a window (typically milliseconds to seconds) where different clients may see different values for the same data.

**In your system:** If you add a read replica to RDS for scaling reads, the replication lag is typically 10-100ms. A user who rates an article and immediately reads it back might read from the replica and not see their rating counted. They experience a **read-your-writes consistency violation** — I just wrote this, why can't I read it?

**Handling it:**
- **Session consistency** — route reads from the same user to the primary for a short window after a write.
- **Sticky routing** — for the author's own view of their article stats, always read from primary.
- **Accept and design for it** — show "Your rating was submitted" in the UI optimistically, refresh the rating count asynchronously.

Kleppmann's *Designing Data-Intensive Applications* (Chapter 5) covers consistency models in detail — required reading for any senior backend engineer.

---

## Section 6 — Testing

---

**Q29. What is the test pyramid? What happens when teams invert it?**

**Test pyramid:**
```
        / E2E \        (few, slow, expensive)
       /  Integ \      (some, medium)
      /   Unit   \     (many, fast, cheap)
```

**Unit tests** (base): Test a single class in isolation, no Spring context, no DB. Run in milliseconds. Mockito, JUnit 5. Catch logic bugs instantly.

**Integration tests** (middle): Test multiple layers together — controller + service + repository + real DB (Testcontainers). Run in seconds. Catch wiring bugs, SQL bugs, migration bugs.

**E2E tests** (top): Test the full system including UI, browser, external services. Run in minutes. Catch user journey bugs.

**Inverted pyramid (the anti-pattern):** Teams that write mostly E2E tests and few unit tests. The CI pipeline takes 45 minutes, failures are non-deterministic (flaky), and when a test fails it's hard to know *what* broke because the blast radius is the entire system.

**FAANG teams** heavily weight the base. Fast feedback is the goal — a unit test that runs in 20ms gives feedback before you even push.

---

**Q30. What is Testcontainers? Why is it better than H2 for Spring Boot integration tests?**

**Testcontainers** spins up a real Docker container (PostgreSQL, Redis, Kafka — anything) for the duration of your test suite, then tears it down.

**Why H2 is insufficient:**
1. Your V2 Flyway migration creates `CREATE TYPE article_status AS ENUM (...)` — this is PostgreSQL syntax. H2 doesn't support custom ENUM types. Your migrations will fail against H2.
2. H2's SQL dialect differs from PostgreSQL's. `TIMESTAMPTZ`, `BIGSERIAL`, `ON CONFLICT` syntax — all PostgreSQL-specific. H2 silently accepts or rejects them differently.
3. PostgreSQL's transaction isolation, locking behavior, and constraint checking are different from H2. Tests that pass against H2 can fail against real PostgreSQL.

**Trade-off:** Testcontainers requires Docker to be running. Tests take ~15-30s for first container start (subsequent tests reuse the container). H2 starts in milliseconds. The speed trade-off is worth it for the fidelity.

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
}
```

---

**Q31. What is the AAA pattern in testing?**

**Arrange — Act — Assert.** Every test should follow this structure, and each section should be clearly separated.

```java
@Test
void registerUser_throwsDuplicateResource_whenEmailExists() {
    // Arrange
    RegisterRequestDTO request = buildRequest("existing@test.com");
    when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

    // Act + Assert
    assertThatThrownBy(() -> userService.registerUser(request))
        .isInstanceOf(DuplicateResourceException.class)
        .hasMessageContaining("existing@test.com");

    // Assert side effects
    verify(userRepository, never()).save(any());
}
```

**Why it matters:** Tests without clear structure become impossible to debug. When a test fails, you need to know: what state was set up (Arrange), what action triggered the failure (Act), and what was expected vs actual (Assert). Tests that mix setup and assertions in random order are maintenance nightmares.

---

**Q32. What are the different types of test doubles? When would you use each?**

A **test double** is any object that replaces a real dependency in a test. There are five types (from Meszaros' *xUnit Test Patterns*):

- **Dummy** — passed but never used. Satisfies a parameter requirement. `null` often works.
- **Stub** — returns hardcoded responses. `when(repo.findById(1L)).thenReturn(Optional.of(article))`. Doesn't verify it was called.
- **Mock** — like a stub, but also records interactions for verification. `verify(repo).save(any())`. Mockito's `mock()` creates mocks.
- **Spy** — wraps a real object and delegates to it, but records calls. `spy(new UserService(...))` — you can override some methods while using real implementations for others.
- **Fake** — a real working implementation but simpler. An in-memory `HashMap`-backed repository instead of a real DB. Expensive to build but useful for complex scenarios.

**For unit tests:** Mostly stubs and mocks (Mockito). For integration tests: fakes or real containers (Testcontainers). Never mock what you're testing — the test double replaces *dependencies*, not the *subject under test*.

---

## Section 7 — Observability

---

**Q33. What are the three pillars of observability?**

**Logs, Metrics, Traces.** Each answers a different question about your system.

**Logs** — what happened. Discrete events with context. "User 42 rated article 7 at 14:23:01 with rating 5." Use structured JSON logs so they're queryable. Good for debugging specific incidents.

**Metrics** — how the system is behaving over time. Counters, gauges, histograms. "Average response time is 120ms. Error rate is 0.3%. CPU is at 78%." Good for dashboards, alerting, and trend analysis. The **RED method** for services: Rate (requests/sec), Errors (error rate), Duration (latency distribution).

**Traces** — how a request flows through multiple services. A trace is a tree of spans — each span represents an operation (DB query, HTTP call, cache lookup). The `traceId` in your MDC filter is the trace ID that links all log lines for a single request. In a microservices system, one trace spans multiple services via the W3C `traceparent` header.

**The gap between them:** Logs tell you something went wrong. Metrics tell you how often. Traces tell you where in the call chain. You need all three to debug a production incident efficiently.

---

**Q34. What is structured logging and why does plain-text logging break down at scale?**

**Plain-text logging:**
```
2024-01-15 14:23:01 INFO ArticleService - 🔥🔥🔥 All articles are fetched
```

**Structured JSON logging:**
```json
{
  "timestamp": "2024-01-15T14:23:01Z",
  "level": "INFO",
  "logger": "ArticleService",
  "message": "articles.list.fetched",
  "count": 47,
  "traceId": "a3f9b2c1"
}
```

**Why plain text breaks at scale:**
- Not machine-readable — you can't query `count > 100` because `count` is embedded in a sentence
- Emoji and special characters break log parsers (Logstash, Datadog, Fluentd)
- No consistent fields — `"user id 42"` vs `"userId=42"` vs `"for user: 42"` — you can't aggregate
- Can't set alerts on specific fields

**Why structured matters:** With JSON logs, Datadog/Elasticsearch index every field automatically. You can write: *"Alert me when `level=ERROR AND logger=RatingService` exceeds 10/minute"*. That's not possible with unstructured text logs without expensive regex parsing.

---

**Q35. What is the RED method for monitoring APIs?**

**RED** is a monitoring methodology specifically for services/APIs:

- **Rate** — requests per second. "What is the current throughput of this endpoint?"
- **Errors** — requests failing per second (as a count or %). "What is the error rate of `POST /articles`?"
- **Duration** — distribution of request latency. Not just average — **p50, p95, p99**. Average is a lie: if 95% of requests complete in 50ms but 5% take 10 seconds, your average might look fine while users are experiencing timeouts.

**The p99 trap:** If your p99 is 5 seconds, 1 in 100 users waits 5 seconds. At 1,000 requests/minute, that's 10 users/minute having a bad experience. At 1M requests/minute, it's 10,000 users/minute.

**Spring Boot Actuator** exposes these via Micrometer metrics automatically for every `@RestController` endpoint. Pair with Prometheus + Grafana or Datadog to visualize them.

---

## Section 8 — Transactions & Concurrency

---

**Q36. What is database transaction isolation? Explain `READ_COMMITTED` vs `REPEATABLE_READ`.**

Isolation levels control what one transaction can see from concurrent transactions.

**READ_COMMITTED (PostgreSQL default):** A transaction only sees data that has been committed by other transactions. If transaction B updates a row and commits, transaction A (still running) will see the new value on its *next read* of that row. This means you can get **non-repeatable reads** — reading the same row twice in the same transaction can return different values.

**REPEATABLE_READ:** Once you read a row in a transaction, subsequent reads of that row within the same transaction return the same value — even if another transaction committed a change. PostgreSQL implements this via MVCC (snapshot isolation).

**Why it matters for your app:** `rateArticle()` reads the article's `ratingCount`, computes a new average, and updates. Between the read and the update, another transaction could commit a different rating. Under `READ_COMMITTED`, you'd compute the average based on a stale count. This is the **lost update** problem — one of the updates is silently discarded. The atomic SQL UPDATE in Q27's Fix 3 avoids this entirely by doing the math inside the DB in a single operation.

---

**Q37. What is optimistic vs pessimistic locking? When would you use each?**

**Pessimistic locking:** Lock the row when you read it. No one else can modify it until you release the lock.
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Articles> findById(Long id);
```
Pros: Guarantees no concurrent modification. Cons: Reduces concurrency — other transactions queue up. Risk of deadlock if two transactions lock rows in different orders.

**Optimistic locking:** No lock on read. When you write, verify no one else changed the row since you read it. If they did, fail and retry.
```java
@Version
private Long version;  // on the entity
```
Hibernate adds `WHERE version = :expected` to every UPDATE. If the version changed, `OptimisticLockingFailureException` is thrown.

**When to use which:**
- **High contention** (many writes to the same row) → pessimistic. Everyone is fighting over the same data; optimistic locking would cause constant retries.
- **Low contention** (reads heavily outweigh writes, rare conflicts) → optimistic. Most operations succeed on first try; no lock overhead for the common case.

For your `Articles` rating update: low contention (rare for two users to rate the same article simultaneously), so optimistic locking or the atomic SQL approach are both appropriate.

---

**Q38. What is a deadlock? How would you design code to avoid it?**

**Deadlock:** Transaction A locks row 1 and waits for row 2. Transaction B locks row 2 and waits for row 1. Both wait forever.

**How it happens in your codebase:** If you have two operations that lock rows in different orders:
- Operation 1: Lock `users` row → Lock `articles` row
- Operation 2: Lock `articles` row → Lock `users` row

If both execute simultaneously, deadlock.

**Prevention strategies:**
1. **Consistent lock ordering** — always acquire locks in the same order (e.g., always lock the lower-ID row first). Neither transaction can wait on a resource the other holds.
2. **Keep transactions short** — the longer a transaction holds locks, the more time for another transaction to conflict. Load data, compute, write, commit — don't do external API calls inside a transaction.
3. **Avoid locking multiple tables** — redesign to operate on one table at a time where possible.
4. **Use `SELECT ... FOR UPDATE SKIP LOCKED`** — for work queues, skip rows another transaction has locked rather than waiting.

PostgreSQL detects deadlocks and automatically kills one transaction, returning error code `40P01`. Your `@Transactional` code should handle this and retry.

---

## Section 9 — Database Design

---

**Q39. When would you denormalize data? What are the trade-offs?**

Your `Articles` table has `average_ratings` and `rating_count` columns — this is denormalization. The normalized form would be: compute the average by querying the `Ratings` table every time. You're storing a derived value.

**Why denormalize:**
- `SELECT AVG(rating) FROM ratings WHERE article_id = :id` on every article read hits the `Ratings` table on every request. With 1M ratings and 10K reads/second, that's a lot of aggregate queries.
- Denormalized `average_ratings` is a single column read — O(1).

**Trade-offs:**
- **Write complexity** — every new rating must update both the `Ratings` table and `Articles.average_ratings`. Two writes instead of one. If one succeeds and one fails (without `@Transactional`), data is inconsistent. This is why `rateArticle()` must be `@Transactional`.
- **Data drift** — if a bug causes the update to be skipped, `average_ratings` drifts from the true average. You need periodic reconciliation jobs.
- **Storage** — minor, but you're storing computed values.

**Rule:** Denormalize for read performance when you're read-heavy and the write complexity is manageable. Normalize first, denormalize when you have evidence of a bottleneck.

---

**Q40. What is a database index? When should you add one vs not?**

An index is a separate data structure (B-tree by default in PostgreSQL) that maps column values to row locations. Without an index, a query `WHERE email = 'x@y.com'` scans every row — O(n). With an index on `email`, it's O(log n).

**When to add:**
- Columns used in `WHERE` clauses frequently
- Columns used in `JOIN ON` clauses (foreign keys should always be indexed)
- Columns used in `ORDER BY` with large result sets

**Your current migrations already add:**
- Index on `users.email` (used in login lookup)
- Index on `articles.author_id` (foreign key)
- Index on `articles.created_at` (used in ordering)

**When NOT to add:**
- High-cardinality columns with no query pattern — indexes take space and slow down writes (every `INSERT`/`UPDATE`/`DELETE` must update the index)
- Boolean columns — an index on `is_active` with 99% true values is useless. The planner won't use it.
- Small tables — PostgreSQL's planner will choose a sequential scan over an index for tables under a few hundred rows

The cost of an index: every write pays O(log n) to update it. If a table has 10 indexes, every insert updates 10 data structures. Write-heavy tables with too many indexes become bottlenecks.

---

## Section 10 — Architecture & Code Quality

---

**Q41. What is the Single Responsibility Principle? Show a violation in a Spring Boot codebase.**

SRP: A class should have one, and only one, reason to change.

**Your `UserService` violation (before we fixed it):**
```java
import org.springframework.web.bind.annotation.RequestBody;   // Web concern
import org.springframework.web.bind.annotation.RequestParam;  // Web concern
import jakarta.validation.Valid;                               // Web/validation concern
import io.swagger.v3.oas.annotations.parameters.RequestBody;  // Documentation concern
```

A service class has one reason to change: business logic changes. The moment it imports web-layer annotations, it now also changes when your HTTP contract changes, when your documentation framework changes, and when your validation strategy changes. Four reasons to change = SRP violation.

**The controller has one reason to change:** HTTP contract changes (new endpoint, different status code, different parameter names).

**The service has one reason to change:** Business logic changes (new validation rule, different domain behavior).

**The repository has one reason to change:** Data access strategy changes (new query, different fetch strategy).

Layer purity is SRP applied to architecture.

---

**Q42. What is Dependency Inversion? How does Spring's `@Autowired` relate to it?**

**Dependency Inversion Principle:** High-level modules (services) should not depend on low-level modules (repositories). Both should depend on abstractions (interfaces).

Without it:
```java
public class ArticleService {
    private final ArticleRepository repo = new ArticleRepositoryImpl();  // hard dependency
}
```
`ArticleService` can't be tested without a real `ArticleRepositoryImpl`. It's coupled to the implementation.

With DI:
```java
public class ArticleService {
    private final ArticleRepository repo;  // depends on the interface, not the implementation

    public ArticleService(ArticleRepository repo) {  // injected by Spring
        this.repo = repo;
    }
}
```

Spring's `@Autowired` (or `@RequiredArgsConstructor` with Lombok) implements this at runtime — Spring decides which implementation to inject. In tests, you inject a `Mockito.mock(ArticleRepository.class)`. In production, Spring injects the real JPA implementation. **The service never knows which one it gets.**

This is also why `ArticleService` in unit tests doesn't need Spring, a database, or Flyway — you inject mocks directly. That's the whole point.

---

**Q43. What is the difference between `@Component`, `@Service`, `@Repository`, `@Controller`?**

All four are stereotypes that tell Spring to create a bean. The only functional difference is `@Repository`:

- `@Component` — generic Spring bean. No special behavior.
- `@Service` — semantically a service. No additional behavior over `@Component`. Exists for readability and potential AOP pointcut targeting.
- `@Controller` / `@RestController` — registers the class with Spring MVC's handler mapping.
- `@Repository` — same as `@Component` **plus** Spring wraps database exceptions in `DataAccessException` hierarchy. A `org.postgresql.util.PSQLException` from Postgres becomes a `DataIntegrityViolationException` — a Spring type your `GlobalExceptionHandler` can catch without knowing which DB vendor you're using.

This means you can switch from PostgreSQL to MySQL and your exception handling code doesn't change — `@Repository`'s exception translation abstracts the vendor-specific exceptions.

---

**Q44. What is SOLID? Which principles do you see violated most in real Spring Boot codebases?**

**S** — Single Responsibility. Classes should have one reason to change.
**O** — Open/Closed. Open for extension, closed for modification. Add new behavior by extending, not editing.
**L** — Liskov Substitution. Subtypes must be substitutable for their base types.
**I** — Interface Segregation. Many specific interfaces are better than one general-purpose interface.
**D** — Dependency Inversion. Depend on abstractions, not concretions.

**Most violated in Spring Boot codebases:**
1. **SRP** — "God Services" that handle 20 different things. `UserService` that handles registration, login, profile management, password reset, email verification, admin operations. Split them.
2. **OCP** — Services with giant `if/else` chains based on `type` fields. Adding a new type requires editing the service. Replace with a strategy pattern — add a new `Strategy` implementation instead.
3. **DIP** — Services directly instantiating dependencies (`new SomeUtil()`) instead of injecting them, making the service impossible to unit test.

---

**Q45. What is the Repository Pattern? Why not just call JPA directly from controllers?**

The Repository Pattern creates an abstraction layer between your domain/business logic and your data access mechanism. The service calls `articleRepository.findBySlug(slug)` — it doesn't know (or care) whether that comes from PostgreSQL, MongoDB, a remote API, or an in-memory cache.

**Why not call JPA directly from controllers:**
1. **Testability** — controllers become integration tests that require a real database. With repositories, you mock the repository and unit-test the controller's HTTP behavior independently.
2. **Transaction boundaries** — `@Transactional` belongs on service methods, not controller methods. If two repository operations must be atomic, that logic lives in a service, not a controller.
3. **Business logic leakage** — `SELECT * FROM articles WHERE author_id = :id AND status = 'PUBLISHED' AND rating > 4.0` is business logic (what constitutes a "publishable" article to show). That belongs in the service, not scattered across controller query parameters.
4. **Single point to change** — if you add caching to article reads, you change the repository (or add `@Cacheable` to the service). You don't hunt through 12 controllers.

---

## Section 11 — Bonus: Career & Code Review

---

**Q46. A pull request removes `@Transactional` from a method saying "we're only reading, we don't need a transaction." Approve or reject?**

**Reject, with explanation.**

Read-only transactions exist for two reasons:

1. **Consistency** — within a `@Transactional(readOnly = true)` boundary, Hibernate uses the same DB snapshot for all queries. Without a transaction, two `findById` calls in the same service method can see different data if a concurrent write committed between them.

2. **Optimization** — `readOnly = true` tells Hibernate to skip dirty checking (no need to track entity state changes) and tells the JDBC driver/pool that this transaction can be routed to a read replica.

The correct fix is not to remove `@Transactional` but to change it:
```java
@Transactional(readOnly = true)  // not removing it
public ArticleResponseDTO getArticleById(Long id) { ... }
```

---

**Q47. How do you handle secrets (DB password, JWT secret) in a production Spring Boot application?**

**Never in code or config files checked into git.** That includes `application.yaml`. The RDS endpoint is in your YAML — that URL is in your git history forever.

**The 12-Factor App approach (Factor 3: Config):** All environment-specific config via environment variables, injected at runtime.

**Production secret management:**
- **AWS Secrets Manager / Parameter Store** — secrets are stored encrypted in AWS. Spring Boot can pull them via Spring Cloud AWS or the Secrets Manager SDK. Secrets rotate automatically; your app gets the new value without redeployment.
- **HashiCorp Vault** — vendor-neutral, can run anywhere.
- **Kubernetes Secrets** — env vars injected into pods at deploy time. Not encrypted at rest by default (needs external KMS integration).

**The rule:** At no point should a developer need to know the production DB password to deploy. CI/CD pulls it from the secret manager. The app never logs it. It never appears in process listings or core dumps.

---

**Q48. What is the difference between horizontal and vertical scaling?**

**Vertical scaling (scale up):** Give your single server more resources — more CPU, more RAM, faster disk. Simple, no code changes. Hard ceiling: you can only make one machine so big. Single point of failure: if that machine goes down, everything goes down. AWS RDS `db.t3.micro` → `db.r6g.4xlarge` is vertical scaling.

**Horizontal scaling (scale out):** Add more instances. 1 app server → 10 app servers behind a load balancer. Theoretically unlimited. Requires your application to be **stateless** — any request can go to any instance. If instance 1 handles login and stores session state in memory, instance 2 doesn't know that user is logged in. This is why JWT (stateless auth) is essential for horizontal scaling.

**Your app is ready for horizontal scaling** because JWTs are stateless. Your DB is the only shared state, and Postgres handles concurrent connections via HikariCP. Adding more app instances requires nothing but a load balancer in front.

**The bottleneck shifts:** Once you scale app servers horizontally, the database becomes the bottleneck. That's when you add read replicas (route `SELECT` queries to replicas, `INSERT/UPDATE/DELETE` to primary) and eventually connection pooling proxies like PgBouncer.

---

**Q49. You deploy a new version and the error rate spikes to 30%. Walk me through your incident response.**

This tests whether you think systematically under pressure.

1. **Assess blast radius** — is this 100% of requests or a specific endpoint? Check the error rate by endpoint in your dashboards.
2. **Read the errors** — what HTTP status codes? What `type` field in the ProblemDetail responses? What does the log say for those `traceId`s?
3. **Check what changed** — what did this deployment change? If it's a schema migration, check Flyway logs. If it's a code change, look at the stack trace.
4. **Rollback if unclear** — if you can't identify the root cause in 5 minutes and users are affected, rollback first, investigate second. The order is: stop the bleeding, then diagnose.
5. **Canary deployments prevent this** — deploy to 1% of traffic first. Monitor the RED metrics. Only promote to 100% if error rate stays flat. This is the production deployment strategy: blue/green (two environments, instant switch) or canary (gradual rollout).

**The senior answer:** The most important prep is *before* the incident — your Actuator health endpoints are live, your structured logs are searchable, your `traceId` is in every error response so affected users can report it. Without these, incident response is guesswork.

---

**Q50. If you could add one thing to this codebase that would have the highest impact on reliability, what would it be?**

There's no single right answer — this tests your judgment and priorities. A strong answer for your current codebase:

**Integration tests with Testcontainers.** Here's why: you currently have zero tests that catch real bugs. The context load test only verifies Spring wires up — it won't catch a broken `@Transactional`, a wrong SQL query, a Flyway migration that fails on the actual Postgres ENUM type, or a null pointer in a service method. A single `ArticleApiIntegrationTest` that spins up real Postgres, runs all migrations, and tests the golden path of `register → login → create article → rate article` would catch every regression in every layer simultaneously. The ROI is higher than any other single addition because it validates the entire vertical slice.

Other defensible answers: distributed tracing with correlation IDs (because you're flying blind in production without them), JWT authentication (because the app literally can't protect data without it), or Actuator health endpoints (because Kubernetes can't orchestrate a service it can't health-check).

---

*Total: 50 questions covering Exception Handling, REST API Design, Security, Spring Boot/JPA, System Design, Testing, Observability, Transactions, Database, and Architecture.*

*References: OWASP Top 10, RFC 9457, 12-Factor App, Designing Data-Intensive Applications (Kleppmann), Effective Java (Bloch), Spring Boot documentation, HikariCP documentation.*
