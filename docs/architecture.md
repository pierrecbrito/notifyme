# NotifyMe — Architecture & Requirements Specification

Technical reference for system design, layering, and service evolution.

## 1. System Requirements

### Functional Requirements
- **YouTube Updates Ingestion:** Automatically detect when a channel publishes a new video (WebSub/PubSubHubbub).
- **Follow Channels:** Allow users to choose which creators they want to follow.
- **Multi-Channel Delivery:** Dispatch alerts via Push Notifications, Email, and SMS.
- **Delivery Channel Selection:** Enable users to configure preferred communication channels (e.g., Push only, Email only).
- **History Log:** View past notifications within the application.

### Non-Functional Requirements
- **Scalability:** Support millions of simultaneous notifications during large creator traffic spikes.
- **Low Latency:** End-to-end notification delivery in under 5 seconds.
- **Reliability:** At-least-once delivery guarantee with idempotency and Dead Letter Queues (DLQ).
- **Security:** Validate YouTube HMAC-SHA1 signatures to prevent spoofed/fake notifications.
- **High Availability:** Maintain notification processing pipeline even during transient outages of secondary services.

---

## 2. Architectural Overview

1. **Ingestion & Validation (Webhook Handler)**
   - API Gateway / Load Balancer + Webhook Controller & Service.
   - Validates HMAC-SHA1 signature from YouTube WebSub XML payload.
   - Responds `200 OK` in < 100ms.
   - Publishes `VideoPublishedEvent` to `notifyme.video.published` queue.

2. **Discovery & Fan-out (Fan-out Service)**
   - `FanoutService` consumes `notifyme.video.published`.
   - Acquires 24h atomic Redis lock for video deduplication.
   - Queries partitioned subscriber records from DynamoDB (`channel_id` partition key).
   - Slices subscribers into batches (chunks of 500) and dispatches tasks to `notifyme.delivery.tasks`.

3. **Messaging & Cache**
   - Broker: RabbitMQ (Topic/Direct Exchanges, DLQ routing, retry backoff).
   - Cache: Redis Cache for user settings in-memory lookup (< 1ms).

4. **Execution & Delivery (Delivery Workers)**
   - Autoscaled consumers listening to `notifyme.delivery.tasks`.
   - Reads contact details from Redis and routes to active channel providers.
   - Resilience: Channel-isolated execution with Exponential Backoff and DLQ routing.

5. **External Providers**
   - Push: Firebase Cloud Messaging (FCM)
   - Email: SendGrid / AWS SES
   - SMS: Twilio
