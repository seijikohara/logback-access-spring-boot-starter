# 設定

このページでは、logback-access-spring-boot-starterのすべての設定オプションを一覧します。

## アプリケーションプロパティ

`application.yml`または`application.properties`でスターターを設定します。

```yaml
logback:
  access:
    enabled: true
    # config-location: classpath:custom-access.xml  # classpath: および file: プレフィックスに対応
    local-port-strategy: server
    tomcat:
      # request-attributes-enabled: true  # RemoteIpValveの存在から自動判定
    tee-filter:
      enabled: false
      # include-hosts: localhost,example.com
      # exclude-hosts: internal.example.com
      # max-payload-size: 65536
      # allowed-content-types:
      #   - "text/*"
      #   - "application/json"
    filter:
      # include-url-patterns:
      #   - /api/.*
      exclude-url-patterns:
        - /actuator/.*
        - /health
```

### プロパティリファレンス

| プロパティ | デフォルト | 説明 |
|-----------|----------|------|
| `logback.access.enabled` | `true` | アクセスロギングを有効/無効にする。 |
| `logback.access.config-location` | 自動検出 | logback-access設定ファイルへのパス。`classpath:`および`file:` URLプレフィックスに対応。 |
| `logback.access.local-port-strategy` | `server` | アクセスログに記録するポート: `server`（クライアントが指定したポート。`RemoteIpValve`使用時は`X-Forwarded-Port`を反映）または`local`（接続を受け付けたローカルインターフェースのポート）。 |
| `logback.access.tomcat.request-attributes-enabled` | 自動検出 | `RemoteIpValve`が設定するアクセスログ属性を反映する。未設定時、パイプラインに`RemoteIpValve`が存在すれば自動的に有効化する。 |
| `logback.access.tee-filter.enabled` | `false` | リクエスト/レスポンスボディキャプチャを有効にする（Tomcat Servlet限定）。 |
| `logback.access.tee-filter.include-hosts` | `null`（全ホスト） | フィルタを適用するホスト名のカンマ区切りリスト。 |
| `logback.access.tee-filter.exclude-hosts` | `null`（なし） | フィルタを適用しないホスト名のカンマ区切りリスト。 |
| `logback.access.tee-filter.max-payload-size` | `65536` | ログ出力に含まれる最大ペイロードサイズ（バイト）。超過分はセンチネル値に置換される。 |
| `logback.access.tee-filter.allowed-content-types` | `null` | ボディキャプチャを許可するContent-Typeパターン。指定するとデフォルト一覧を完全に置き換える（上書きモード）。 |
| `logback.access.filter.include-url-patterns` | `null`（全URL） | Java正規表現パターン。リクエストURIが少なくとも1つにマッチする必要がある。部分一致のため、完全一致は`^...$`を使う。 |
| `logback.access.filter.exclude-url-patterns` | `null`（なし） | Java正規表現パターン。マッチしたリクエストURIはログに記録されない。両方指定時は除外が優先される。 |

## 設定ファイルの解決

`logback.access.config-location`が設定されている場合、スターターはそのパスを直接読み込み、フォールバックは行いません。ファイルが存在しない場合はアプリケーションの起動に失敗します。

未設定時は、以下の順序でクラスパスを検索し、最初に存在するリソースを使用します。

1. `classpath:logback-access-test.xml` — テスト時のみ採用される。
2. `classpath:logback-access.xml` — メインの設定ファイル。
3. `classpath:logback-access-test-spring.xml` — `<springProfile>` / `<springProperty>` を利用するテスト用バリアント。
4. `classpath:logback-access-spring.xml` — `<springProfile>` / `<springProperty>` を利用する本番用バリアント。
5. スターターに同梱された組み込みフォールバック設定。リクエストを`common`形式でコンソールに出力する。

## XML設定

### 基本構造

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Appenderはログの出力先を定義 -->
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>

    <!-- Appenderを参照して有効化 -->
    <appender-ref ref="console"/>
</configuration>
```

### Springプロパティの使用

Springの`Environment`から値を取り込むには`<springProperty>`を使用します。この拡張機能を利用するには、設定ファイル名を`logback-access-spring.xml`（またはテスト用の`-test-spring.xml`）にする必要があります。

```xml
<configuration>
    <springProperty name="appName" source="spring.application.name"
                    defaultValue="app" scope="context"/>

    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[${appName}] %h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>
    <appender-ref ref="console"/>
</configuration>
```

::: warning デフォルトスコープ
`<springProperty>`のデフォルトスコープは`LOCAL`です。`LOCAL`スコープのプロパティはXML解析時の変数置換（`${varName}`）でのみ参照可能です。`context.getProperty()`でプログラム的に値を取得する場合は`scope="context"`を指定してください。
:::

### Springプロファイルの使用

環境ごとにAppenderを切り替えるには`<springProfile>`を使用します。

```xml
<configuration>
    <springProfile name="dev">
        <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
            <encoder>
                <pattern>%h %l %u [%t] "%r" %s %b %D</pattern>
            </encoder>
        </appender>
        <appender-ref ref="console"/>
    </springProfile>

    <springProfile name="prod">
        <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
            <file>logs/access.log</file>
            <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
                <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log</fileNamePattern>
                <maxHistory>30</maxHistory>
            </rollingPolicy>
            <encoder>
                <pattern>%h %l %u [%t] "%r" %s %b</pattern>
            </encoder>
        </appender>
        <appender-ref ref="file"/>
    </springProfile>
</configuration>
```

### プロファイル式

`<springProfile>`は否定とカンマ区切りの複数プロファイル指定をサポートします。

```xml
<!-- "prod"がアクティブでないときに有効 -->
<springProfile name="!prod">
    ...
</springProfile>

<!-- "dev"または"staging"のいずれかがアクティブなときに有効 -->
<springProfile name="dev, staging">
    ...
</springProfile>
```

## File Appender

アクセスログをファイルに出力します。

```xml
<appender name="file" class="ch.qos.logback.core.FileAppender">
    <file>logs/access.log</file>
    <encoder>
        <pattern>%h %l %u [%t] "%r" %s %b</pattern>
    </encoder>
</appender>
```

## Rolling File Appender

時間とサイズの両方でログをローテーションします。

```xml
<appender name="rolling" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/access.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/access.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>100MB</maxFileSize>
        <maxHistory>30</maxHistory>
        <totalSizeCap>3GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%h %l %u [%t] "%r" %s %b</pattern>
    </encoder>
</appender>
```

## アクセスロギングの無効化

スターター全体を無効化します。

```yaml
logback:
  access:
    enabled: false
```

特定のプロファイルだけで無効化するには、`logback.access.enabled`と`spring.config.activate.on-profile`を組み合わせます。

```yaml
spring:
  profiles:
    active: test

---
spring:
  config:
    activate:
      on-profile: test

logback:
  access:
    enabled: false
```

## 関連ページ

- [Tomcat連携](/ja/guide/tomcat) — Tomcat固有のプロパティとリバースプロキシ設定。
- [Jetty連携](/ja/guide/jetty) — Jetty固有の動作と既知の制限事項。
- [高度な設定](/ja/guide/advanced) — TeeFilter、URLフィルタリング、JSONロギング、Spring Security。
