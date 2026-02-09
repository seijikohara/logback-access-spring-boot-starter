# Jetty連携

このページでは、Jetty固有の設定オプションと動作を説明します。

## 動作の仕組み

組み込みサーバーとしてJettyを使用する場合、スターターはHTTPリクエストとレスポンスデータをキャプチャする`JettyRequestLog`を登録します。

```
HTTPリクエスト → Jetty Server → JettyRequestLog → アプリケーション
                                          ↓
                                   LogbackAccessContext
                                          ↓
                                   Appender (Console, Fileなど)
```

## Jettyの使用

TomcatではなくJettyを使用するには、Tomcatを除外してJettyを追加します:

::: code-group

```kotlin [Gradle (Kotlin)]
implementation("org.springframework.boot:spring-boot-starter-webmvc") {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}
implementation("org.springframework.boot:spring-boot-starter-jetty")
implementation("io.github.seijikohara:logback-access-spring-boot-starter:VERSION")
```

```groovy [Gradle (Groovy)]
implementation('org.springframework.boot:spring-boot-starter-webmvc') {
    exclude group: 'org.springframework.boot', module: 'spring-boot-starter-tomcat'
}
implementation 'org.springframework.boot:spring-boot-starter-jetty'
implementation 'io.github.seijikohara:logback-access-spring-boot-starter:VERSION'
```

```xml [Maven]
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
<dependency>
    <groupId>io.github.seijikohara</groupId>
    <artifactId>logback-access-spring-boot-starter</artifactId>
    <version>VERSION</version>
</dependency>
```

:::

## Jetty 12互換性

このライブラリはJetty 12（Spring Boot 4にバンドルされているバージョン）と互換性があります。

## パターン変数

標準のパターン変数については[はじめに — パターン変数](/ja/guide/getting-started#パターン変数)を参照してください。

Jetty固有の注意事項:

- **Cookie** (`%{xxx}c`): スターターはJettyネイティブAPIの`Request.getCookies()`を使用してCookieを抽出します。
- **リクエスト属性** (`%{xxx}r`): 標準のServletリクエスト属性は利用可能ですが、Tomcat固有の`AccessLog`属性（例: `org.apache.catalina.AccessLog.RemoteAddr`）はサポートされません。
- **リモートホスト** (`%h`): 常にIPアドレスを返します（逆引きDNSルックアップは実行されません）。
- **リクエストパラメータ**: `requestParameterMap`はリクエストボディの消費を避けるため、空のマップを返します。

## 既知の制限事項

### リモートホスト解決

Jettyはデフォルトで逆引きDNSルックアップを実行しません。`%h`変数はホスト名ではなくIPアドレスを表示します。これはパフォーマンス上の理由から意図的な動作です。

### リクエストパラメータ

パフォーマンスと互換性の理由から、`requestParameterMap`は全リクエストで空のマップを返します。これはリクエストボディの消費を避けるための意図的な動作です。

### TeeFilter

::: warning Jetty 12非対応
TeeFilterはJetty 12ではサポートされていません。JettyのRequestLog APIはServlet APIとは別のコアサーバーレベルで動作します。TeeFilterはServletリクエストにリクエスト属性を設定しますが、RequestLogからは参照できません。TomcatでのTeeFilter使用方法は[高度な設定 — TeeFilter](/ja/guide/advanced#teefilter)を参照してください。
:::

## ローカルポート戦略

ログに記録するポートを制御:

```yaml
logback:
  access:
    local-port-strategy: server
```

- `server`: 設定されたサーバーポートを使用
- `local`: ローカル接続ポートを使用

## リバースプロキシの背後での使用

転送ヘッダーを処理するようにJettyを設定:

```yaml
server:
  forward-headers-strategy: native
```

またはより詳細な制御:

```yaml
server:
  forward-headers-strategy: framework
```

## Spring Security連携

Spring Securityがクラスパスにある場合、スターターは認証済みユーザー名を`%u`変数で自動的にキャプチャします。

## 設定例

本番Jetty向けの完全な設定例:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="file" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b "%i{Referer}" "%i{User-Agent}" %D</pattern>
        </encoder>
    </appender>

    <appender-ref ref="file"/>
</configuration>
```

アプリケーションプロパティ:

```yaml
logback:
  access:
    filter:
      exclude-url-patterns:
        - /actuator/.*
        - /health
```

## 関連ページ

- [設定リファレンス](/ja/guide/configuration) — 全プロパティリファレンスとXML設定
- [高度な設定](/ja/guide/advanced) — TeeFilter、URLフィルタリング、JSONロギング、Spring Security
