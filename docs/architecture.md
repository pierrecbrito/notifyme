# NotifyMe — Especificações de Arquitetura e Requisitos

Documento de referência rápida para o desenvolvimento e evolução dos serviços do NotifyMe.

## 1. Requisitos do Sistema

### Requisitos Funcionais
- **Receber avisos do YouTube:** Identificar automaticamente quando um canal posta vídeo novo (WebSub/PubSubHubbub).
- **Seguir canais:** Permitir ao usuário escolher de quais criadores quer receber alertas.
- **Enviar notificações:** Disparar alertas via Push, E-mail e SMS.
- **Escolha de canal de envio:** Usuário define preferência de canal de envio (ex.: apenas Push ou apenas E-mail).
- **Histórico:** Exibir notificações passadas dentro do aplicativo.

### Requisitos Não Funcionais
- **Escalabilidade:** Suportar milhões de notificações simultâneas em picos de grandes canais.
- **Baixa Latência:** Entrega do aviso ao usuário final em menos de 5 segundos.
- **Confiabilidade:** Nenhuma notificação perdida (garantias at-least-once com idempotência).
- **Segurança:** Validar autenticidade via assinatura HMAC da notificação do YouTube (evitar fakes).
- **Disponibilidade:** Manter fluxo de envio ativo mesmo em indisponibilidade do banco de histórico.

---

## 2. Visão Arquitetural

1. **Ingestão & Validação (Webhook Handler)**
   - API Gateway / Load Balancer + Webhook Validator.
   - Valida assinatura HMAC do payload XML do YouTube WebSub.
   - Retorna `200 OK` em < 100ms.
   - Publica evento na fila `New Video Queue`.

2. **Descoberta & Fan-out (Fan-out Service)**
   - Fan-out Processor consome `New Video Queue`.
   - Consulta banco particionado de inscritos (`User Subscriptions Sharded` por `channel_id`).
   - Divide inscritos em lotes (*chunks* de 500) e envia para `Delivery Tasks Queue`.

3. **Mensageria & Cache**
   - Filas: RabbitMQ / Kafka / AWS SQS.
   - Cache: Redis Cache (User Settings) para consultas de preferências de notificação em baixa latência.

4. **Execução & Entrega (Delivery Workers)**
   - Workers com autoescala (1..N).
   - Busca configurações do usuário no Redis.
   - Roteia para os provedores adequados.
   - Resiliência: Retry com Exponential Backoff e Dead Letter Queue (DLQ).

5. **Provedores Externos**
   - Push: Firebase FCM
   - E-mail: SendGrid / AWS SES
   - SMS: Twilio
