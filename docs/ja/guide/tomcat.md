# Tomcat連携

このページでは、Tomcat固有の設定オプションと動作を説明します。

## 動作の仕組み

組み込みサーバーとしてTomcatを使用する場合、スターターはすべてのHTTPリクエストとレスポンスをインターセプトする`TomcatValve`を登録します。

```
HTTPリクエスト → Tomcat Connector → TomcatValve → アプリケーション
                                          ↓
                                   LogbackAccessContext
                                          ↓
                                   Appender (Console, Fileなど)
```

## Tomcat固有のプロパティ

```yaml
logback:
  access:
    tomcat:
      # 未設定時、RemoteIpValveの存在から自動判定
      request-attributes-enabled: true
```

### リクエスト属性

`request-attributes-enabled`が`true`の場合、以下のTomcatリクエスト属性が利用可能になります:

| 属性 | 説明 |
|------|------|
| `org.apache.catalina.AccessLog.RemoteAddr` | クライアントIPアドレス |
| `org.apache.catalina.AccessLog.RemoteHost` | クライアントホスト名 |
| `org.apache.catalina.AccessLog.Protocol` | HTTPプロトコルバージョン |
| `org.apache.catalina.AccessLog.ServerPort` | サーバーポート |
| `org.apache.tomcat.remoteAddr` | クライアントIPアドレス（代替） |

これらの属性はリバースプロキシの背後で使用する場合に便利です。

## パターン変数

Tomcatは標準のパターン変数に加えて以下をサポート:

| 変数 | 説明 |
|------|------|
| `%a` | リモートIPアドレス |
| `%A` | ローカルIPアドレス |
| `%p` | ローカルポート |
| `%{xxx}i` | リクエストヘッダー`xxx` |
| `%{xxx}o` | レスポンスヘッダー`xxx` |
| `%{xxx}c` | Cookie値`xxx` |
| `%{xxx}r` | リクエスト属性`xxx` |

## リバースプロキシの背後での使用

プロキシ（nginx、Apache、ロードバランサー）の背後で動作する場合、`RemoteIpValve`を設定して実際のクライアントIPを取得します:

```yaml
server:
  tomcat:
    remoteip:
      remote-ip-header: X-Forwarded-For
      protocol-header: X-Forwarded-Proto
```

アクセスログにはプロキシのIPではなく、実際のクライアントIPが表示されます。

## ローカルポート戦略

ログに記録するポートを制御:

```yaml
logback:
  access:
    local-port-strategy: server  # または 'local'
```

- `server`: サーバーポートを使用（例: 8080）
- `local`: ローカル接続ポートを使用

## Spring Security連携

Spring Securityがクラスパスにある場合、認証済みユーザー名が自動的にキャプチャされます:

```xml
<pattern>%h %l %u %t "%r" %s %b</pattern>
```

`%u`変数の表示:
- 認証済みリクエスト: ユーザー名
- 匿名リクエスト: `-`

## 設定例

本番Tomcat向けの完全な設定例:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProperty name="appName" source="spring.application.name"
                    defaultValue="app" scope="context"/>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/access.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%h %l %u %t "%r" %s %b "%i{Referer}" "%i{User-Agent}" %D</pattern>
        </encoder>
    </appender>

    <appender-ref ref="FILE"/>
</configuration>
```

アプリケーションプロパティ:

```yaml
logback:
  access:
    tomcat:
      request-attributes-enabled: true
    filter:
      exclude-url-patterns:
        - /actuator/.*
        - /health
        - /favicon.ico
```
