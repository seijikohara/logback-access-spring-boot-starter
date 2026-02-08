# 高度な設定

このページでは、高度な機能と設定について説明します。

## TeeFilter

TeeFilterはロギング用にリクエストとレスポンスのボディ内容をキャプチャします。

::: tip Servletアプリケーション限定
TeeFilterはServletベースのWebアプリケーション（Spring MVC）が必要です。リアクティブアプリケーション（Spring WebFlux）では使用できません。
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

| プロパティ | 説明 |
|-----------|------|
| `enabled` | ボディキャプチャの有効/無効 |
| `include-hosts` | 含めるホストのカンマ区切りリスト |
| `exclude-hosts` | 除外するホストのカンマ区切りリスト |

### ボディ内容へのアクセス

`%requestContent`と`%responseContent`パターンを使用:

```xml
<pattern>%h "%r" %s %requestContent %responseContent</pattern>
```

### パフォーマンスへの影響

::: warning
ボディキャプチャはメモリ使用量を増加させ、パフォーマンスに影響を与える可能性があります。ホストフィルタリングを使用して、特定の環境に限定してキャプチャしてください。
:::

## URLフィルタリング

包含パターンと除外パターンを使用して、ログに記録するURLを制御します。

### 除外パターン

ヘルスチェックとアクチュエーターエンドポイントを除外:

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

APIエンドポイントのみをログに記録:

```yaml
logback:
  access:
    filter:
      include-url-patterns:
        - /api/.*
```

### パターン評価順序

1. 包含パターンが定義されている場合、URLは少なくとも1つにマッチする必要がある
2. 除外パターンが定義されている場合、マッチするURLは除外される
3. 両方にマッチする場合、除外が優先

### 正規表現構文

パターンはJava正規表現を使用:

| パターン | マッチ対象 |
|---------|----------|
| `/api/.*` | `/api/`で始まる任意のURL |
| `/users/[0-9]+` | `/users/123`、`/users/456` |
| `.*\\.json` | `.json`で終わる任意のURL |

## JSONロギング

ログ集約システム向けにアクセスログをJSON形式で出力します。

### Logstash Encoderの使用

依存関係を追加:

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
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder"/>
    </appender>
    <appender-ref ref="JSON"/>
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
  "@timestamp": "2026-01-01T12:00:00.000Z",
  "@version": 1,
  "method": "GET",
  "uri": "/api/users",
  "status": 200,
  "elapsed_time": 45,
  "remote_addr": "192.168.1.100",
  "user_agent": "Mozilla/5.0...",
  "service": "my-app",
  "environment": "production"
}
```

## Spring Security連携

Spring Securityがクラスパスにある場合、認証済みユーザー名が自動的にキャプチャされます。

::: tip Servletアプリケーション限定
ユーザー名の自動キャプチャにはServletベースのWebアプリケーション（Spring MVC）が必要です。リアクティブアプリケーション（Spring WebFlux）ではアクセスロギングは動作しますが、`%u`変数は`-`を表示します。
:::

### 動作の仕組み

ライブラリは認証済みプリンシパルの`SecurityContextHolder`をチェックします:

1. 認証済みリクエスト: ユーザー名が`%u`でキャプチャされる
2. 匿名リクエスト: `-`が表示される

### カスタムプリンシパル抽出

ライブラリはデフォルトで`SecurityContextHolder.getContext().getAuthentication().getName()`を使用します。

## 複数Appender

ログを複数の宛先に送信:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log</fileNamePattern>
        </rollingPolicy>
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>

    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashAccessEncoder"/>
    </appender>

    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
    <appender-ref ref="JSON"/>
</configuration>
```

## パフォーマンスのヒント

アクセスロギングのパフォーマンスを最適化するには:

1. 本番環境のファイルロギングにはサイズ制限付きの`RollingFileAppender`を使用
2. [URLフィルタリング](#urlフィルタリング)を有効にしてログ量を削減
3. JSONロギングには、[logstash-logback-encoder](https://github.com/logfellow/logstash-logback-encoder)が独自の非同期機能を提供
4. ボディキャプチャが不要な場合はTeeFilterを無効化

## トラブルシューティング

### ログが表示されない

1. `logback.access.enabled`が`true`であることを確認
2. 設定ファイルが存在し、有効なXMLであることを確認
3. Appender名のタイプミスをチェック

### ユーザー名が表示されない

1. Spring Securityがクラスパスにあることを確認
2. ユーザーが実際に認証されていることを確認
3. ログフォーマットに`%u`パターンが含まれていることを確認

### パフォーマンスの問題

1. ファイルロギングには非同期Appenderを使用
2. URLフィルタリングを有効にしてログ量を削減
3. 不要な場合はボディキャプチャ（TeeFilter）を無効化
4. サイズ制限付きのローリングファイルAppenderを使用
