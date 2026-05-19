# 高度な設定

このページでは、TeeFilterによるボディキャプチャ、URLフィルタリング、JSON出力、Spring Security連携、トラブルシューティングを説明します。

## TeeFilter

TeeFilterはLogback Accessのコンポーネントで、リクエストとレスポンスのボディをバッファリングし、`%requestContent`と`%responseContent`パターン変数から参照できるようにします。

::: tip Tomcat Servletアプリケーション限定
スターターは、Tomcatがクラスパスに存在し、アプリケーションがServletベース（Spring MVC）である場合にのみTeeFilterを登録します。Jettyやリアクティブアプリケーション（Spring WebFlux）では利用できません。
:::

### TeeFilterの有効化

```yaml
logback:
  access:
    tee-filter:
      enabled: true
      include-hosts: localhost,example.com
      exclude-hosts: internal.example.com
```

### 設定オプション

| プロパティ | 説明 | デフォルト |
|-----------|------|-----------|
| `enabled` | ボディキャプチャを有効/無効にする。 | `false` |
| `include-hosts` | フィルタを適用するホスト名のカンマ区切りリスト。 | 全ホスト |
| `exclude-hosts` | フィルタを適用しないホスト名のカンマ区切りリスト。 | なし |
| `max-payload-size` | ログ出力に含まれる最大ペイロードサイズ（バイト）。超過分はセンチネル値に置換される。 | `65536` |
| `allowed-content-types` | ボディキャプチャを許可するContent-Typeパターン。指定するとデフォルト一覧を完全に置き換える。 | 下記参照 |

### ボディ内容へのアクセス

キャプチャしたボディはパターン変数`%requestContent`と`%responseContent`から参照します。

```xml
<pattern>%h "%r" %s %requestContent %responseContent</pattern>
```

### ボディキャプチャポリシー

キャプチャしたバイト列をログ出力に含める前に、スターターはレスポンスの`Content-Type`とペイロードサイズに基づきキャプチャポリシーを評価します。バイナリコンテンツや巨大なペイロードはセンチネル値に置換されます。

**デフォルトで許可されるコンテンツタイプ:**

- `text/*`（text/plain、text/htmlなど）
- `application/json`
- `application/xml`
- `application/*+json`（application/vnd.api+jsonなど）
- `application/*+xml`（application/atom+xmlなど）
- `application/x-www-form-urlencoded`

**センチネル値:**

| 条件 | センチネル |
|------|-----------|
| 画像コンテンツ（`image/*`） | `[IMAGE CONTENTS SUPPRESSED]` |
| その他のバイナリコンテンツ | `[BINARY CONTENT SUPPRESSED]` |
| ペイロードが`max-payload-size`を超過 | `[CONTENT TOO LARGE]` |

**カスタムコンテンツタイプ:**

```yaml
logback:
  access:
    tee-filter:
      enabled: true
      max-payload-size: 131072
      allowed-content-types:
        - "text/*"
        - "application/json"
        - "application/pdf"
```

`allowed-content-types`を指定するとデフォルト一覧は完全に置き換えられます（上書きモード）。キャプチャするタイプはすべて明示的に列挙してください。

::: warning
`max-payload-size`はログ出力に含まれるサイズのみを制御します。TeeFilterはこの値に関係なく、リクエスト/レスポンスの全ボディをメモリにバッファリングします。本番環境では`include-hosts` / `exclude-hosts`でキャプチャ範囲を限定してください。
:::

::: info
`tee-filter.enabled`が`false`（デフォルト）の場合、`%requestContent`と`%responseContent`は常に空を返します。これにより、`application/x-www-form-urlencoded`リクエストのフォームデータ再構成パスも抑制されます。TeeFilterを明示的に有効化しない限り、フォームに送信された認証情報などがアクセスログに漏洩することはありません。
:::

### 文字エンコーディング

スターターは`Content-Type`ヘッダーに宣言されたcharsetを使ってキャプチャしたバイト列をデコードします。charsetが省略されている、またはサポートされていない場合はUTF-8にフォールバックします。

クライアントまたはサーバーが`Content-Type`に適切な`charset`パラメータを設定していれば、Shift_JISやISO-8859-1などの非ASCIIペイロードも正しくデコードされます。

### パフォーマンスへの影響

::: warning
ボディキャプチャは各リクエスト/レスポンスをメモリにバッファリングします。`include-hosts` / `exclude-hosts`でキャプチャ範囲を限定し、コストが必要な環境にとどまるようにしてください。
:::

## URLフィルタリング

包含パターンと除外パターンで、ログに記録するリクエストURIを選択します。両リストともJava正規表現を受け付けます。

### 除外パターン

ヘルスチェックやアクチュエーターをアクセスログから除外します。

```yaml
logback:
  access:
    filter:
      exclude-url-patterns:
        - /actuator/.*
        - /health
        - /ready
        - /favicon.ico
```

### 包含パターン

APIエンドポイントのみをログに記録します。

```yaml
logback:
  access:
    filter:
      include-url-patterns:
        - /api/.*
```

### パターン評価順序

1. `include-url-patterns`が定義されている場合、リクエストURIは少なくとも1つにマッチしなければならない。マッチしないURIは記録されない。
2. `exclude-url-patterns`が定義されている場合、マッチしたURIは記録されない。
3. 両方が定義されている場合、除外が優先される。記録されるのは包含パターンにマッチし、かつどの除外パターンにもマッチしないURIのみ。

### パターンマッチングの動作

パターンはJava正規表現を使用し、**部分一致**で評価します。パターンがリクエストURIの任意の位置に見つかればマッチします。完全一致には`^`と`$`でアンカーします。

| パターン | マッチする | マッチしない |
|---------|----------|------------|
| `/api/.*` | `/api/users`, `/v2/api/data` | `/apiary` |
| `/health` | `/health`, `/api/health-check` | `/heal` |
| `^/health$` | `/health` | `/api/health`, `/health/check` |
| `/users/[0-9]+` | `/users/123`, `/users/456` | `/users/abc` |
| `.*\\.json` | `/data.json`, `/api/config.json` | `/json-data` |

::: tip
完全一致にはアンカー付きパターンを使用してください。例えば、`^/actuator/health$`は`/actuator/health`のみにマッチし、`/actuator/health/liveness`にはマッチしません。
:::

## JSONロギング

ログ集約システム（Logstash、OpenSearchなど）向けにアクセスログをJSON形式で出力します。

### Logstash Encoderの使用

`logstash-logback-encoder`を依存関係に追加します。

::: code-group

```kotlin [Gradle (Kotlin)]
implementation("net.logstash.logback:logstash-logback-encoder:9.0")
```

```xml [Maven]
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>9.0</version>
</dependency>
```

:::

エンコーダーを設定:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="json" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder"/>
    </appender>
    <appender-ref ref="json"/>
</configuration>
```

### カスタムJSONフィールド

JSON出力にカスタムフィールドを追加:

```xml
<encoder class="net.logstash.logback.encoder.LogstashAccessEncoder">
    <customFields>{"service":"my-app","environment":"production"}</customFields>
</encoder>
```

### 出力例

```json
{
  "@timestamp": "2026-01-01T12:00:00.000+09:00",
  "@version": "1",
  "message": "GET /api/users HTTP/1.1",
  "method": "GET",
  "protocol": "HTTP/1.1",
  "status_code": 200,
  "requested_url": "GET /api/users HTTP/1.1",
  "requested_uri": "/api/users",
  "remote_host": "192.168.1.100",
  "remote_user": "-",
  "content_length": 1234,
  "elapsed_time": 45,
  "service": "my-app",
  "environment": "production"
}
```

## Spring Security連携

Spring Securityがクラスパスにある場合、スターターは`SecurityContextHolder`から認証済みユーザー名を解決し、`%u`ログ変数に書き込みます。

::: tip Servletアプリケーション限定
ユーザー名のキャプチャにはServletベースのWebアプリケーション（Spring MVC）が必要です。リアクティブアプリケーション（Spring WebFlux）ではアクセスロギング自体は動作しますが、`%u`は常に`-`になります。
:::

### 動作の仕組み

スターターは、Spring Securityのフィルタチェーンの後段で動作する内部Servletフィルタを登録します。

1. 認証済みリクエスト: フィルタが`Authentication.getName()`をリクエスト属性に書き込み、アクセスイベントソースがそれを`%u`変数にコピーする。
2. 匿名リクエスト: 属性は書き込まれず、`%u`は`-`になる。

フィルタは`AuthenticationTrustResolver`を参照して匿名トークン（`AnonymousAuthenticationToken`など）を真に認証されたリクエストから区別します。アクセスログには認証されたユーザーのみが記録されます。

### 信頼解決のカスタマイズ

スターターは、`AuthenticationTrustResolver`型のBeanが他に存在しない場合、デフォルトの`AuthenticationTrustResolverImpl` Beanを登録します。同型のBeanを宣言することで上書きできます。

```java
@Bean
public AuthenticationTrustResolver authenticationTrustResolver() {
    return new MyCustomTrustResolver();
}
```

## 複数Appender

ログを複数の出力先に同時に送信します。

```xml
<configuration>
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>

    <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log</fileNamePattern>
        </rollingPolicy>
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>

    <appender name="json" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder"/>
    </appender>

    <appender-ref ref="console"/>
    <appender-ref ref="file"/>
    <appender-ref ref="json"/>
</configuration>
```

## パフォーマンスのヒント

- 本番環境のファイル出力にはサイズと履歴制限を設定した`RollingFileAppender`を使う。
- ログ量が多く価値の低いエンドポイント（ヘルスチェック、メトリクス）は[URLフィルタリング](#urlフィルタリング)で除外する。
- JSON出力が必要な場合、`logstash-logback-encoder`が独自の非同期Appenderを提供している。
- ボディの内容がアクセスログに本当に必要でない限り、TeeFilterは無効のままにする。

## トラブルシューティング

### ログが出力されない

1. `logback.access.enabled`が`true`であることを確認する。
2. 設定ファイルがクラスパス上に存在し、有効なXMLであることを確認する。
3. `<appender-ref>`のAppender名にタイプミスがないかを確認する。

### ユーザー名が出力されない

1. Spring Securityがクラスパスにあることを確認する。
2. リクエストが認証済みであることを確認する（匿名トークンではないこと）。
3. パターンに`%u`が含まれていることを確認する。
4. アプリケーションがServletベースであることを確認する。リアクティブアプリケーションでは`%u`は常に`-`になる。

### パフォーマンスの問題

1. ファイルAppenderを`AsyncAppender`でラップし、I/Oをリクエストスレッドから切り離す。
2. [URLフィルタリング](#urlフィルタリング)でログ量を制限する。
3. ボディキャプチャが不要な場合はTeeFilterを無効化する。
4. `RollingFileAppender`にサイズと履歴の制限を設定する。

## 関連ページ

- [Tomcat連携](/ja/guide/tomcat) — Tomcat固有のプロパティとリバースプロキシ設定。
- [Jetty連携](/ja/guide/jetty) — Jetty固有の動作と既知の制限事項。
- [設定リファレンス](/ja/guide/configuration) — 全プロパティリファレンスとXML設定。
