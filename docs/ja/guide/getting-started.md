# はじめに

このガイドでは、Spring BootアプリケーションにHTTPアクセスログを追加する手順を説明します。

## 前提条件

- Java 21以上
- Spring Boot 4.0以上
- 組み込みサーバーとしてTomcatまたはJetty

## モジュール構成

このライブラリは2つのMavenアーティファクトとして公開されています。

| アーティファクト | 説明 |
|---------------|------|
| `logback-access-spring-boot-starter` | 自動設定とサーバー連携（Tomcat、Jetty、Spring Security、TeeFilter）。 |
| `logback-access-spring-boot-starter-core` | 公開APIとデータモデル。推移的依存関係として取り込まれる。APIを拡張する場合のみ明示的に宣言する。 |

通常はスターターのみを依存関係として宣言します。coreモジュールは推移的に取り込まれます。

## インストール

ビルドファイルに依存関係を追加します:

> `VERSION`を[Maven Centralの最新バージョン](https://central.sonatype.com/artifact/io.github.seijikohara/logback-access-spring-boot-starter)に置き換えてください。

::: code-group

```kotlin [Gradle (Kotlin)]
implementation("io.github.seijikohara:logback-access-spring-boot-starter:VERSION")
```

```groovy [Gradle (Groovy)]
implementation 'io.github.seijikohara:logback-access-spring-boot-starter:VERSION'
```

```xml [Maven]
<dependency>
    <groupId>io.github.seijikohara</groupId>
    <artifactId>logback-access-spring-boot-starter</artifactId>
    <version>VERSION</version>
</dependency>
```

:::

## 基本設定

`src/main/resources/logback-access.xml`を作成します。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u [%t] "%r" %s %b</pattern>
        </encoder>
    </appender>
    <appender-ref ref="console"/>
</configuration>
```

このパターンはNCSA Common Log Format（CLF）に相当します。スターターはこのファイルを自動的に読み込みます。全体の探索順序は[設定ファイルの解決](/ja/guide/configuration#設定ファイルの解決)を参照してください。

## パターン変数

パターンで使用可能な変換ワードは以下のとおりです。

| 変数 | 説明 |
|------|------|
| `%h` | リモートホスト。Jettyでは常にIPアドレス（逆引きDNSルックアップは行わない）。 |
| `%a` | リモートIPアドレス。 |
| `%A` | ローカルIPアドレス。 |
| `%p` | ローカルポート。クライアントが指定したポートとローカルインターフェースのポートの切り替えは[local-port-strategy](/ja/guide/configuration#プロパティリファレンス)で行う。 |
| `%l` | リモートログ名。常に`-`。 |
| `%u` | 認証済みユーザー名。匿名リクエストでは`-`。Spring Securityを使用したServletアプリケーションが必要。 |
| `%t` | リクエストタイムスタンプ。 |
| `%r` | リクエストライン: メソッド、URI（クエリ文字列含む）、プロトコル。 |
| `%s` | HTTPステータスコード。 |
| `%b` | レスポンスボディサイズ（バイト）。 |
| `%D` | リクエスト処理時間（ミリ秒）。 |
| `%T` | リクエスト処理時間（秒）。 |
| `%I` | リクエストを処理したスレッド名。 |
| `%{name}i` | リクエストヘッダー`name`の値。 |
| `%{name}o` | レスポンスヘッダー`name`の値。 |
| `%{name}c` | Cookie `name`の値。 |
| `%{name}r` | リクエスト属性`name`の値。 |
| `%queryString` | `?`を含むクエリ文字列。クエリがない場合は空。 |
| `%requestContent` | リクエストボディ。TeeFilterが有効な場合のみ出力（Tomcat限定）。 |
| `%responseContent` | レスポンスボディ。TeeFilterが有効な場合のみ出力（Tomcat限定）。 |

::: tip 代替構文
リクエスト/レスポンスヘッダー、Cookie、属性の変換ワードは`%{name}i`形式と`%i{name}`形式の両方を受け付けます。本ドキュメントでは`%{name}i`形式で統一しています。
:::

## Combined Log Format

ApacheのCombined Log Formatに対応するには、`Referer`と`User-Agent`ヘッダーを追加します。

```xml
<pattern>%h %l %u [%t] "%r" %s %b "%{Referer}i" "%{User-Agent}i"</pattern>
```

## 動作確認

1. Spring Bootアプリケーションを起動する。
2. 任意のエンドポイントにHTTPリクエストを送信する。
3. コンソールにアクセスログが出力されることを確認する。

出力例:

```
127.0.0.1 - - [01/Jan/2026:12:00:00 +0000] "GET /api/users HTTP/1.1" 200 1234
```

## 次のステップ

- [設定リファレンス](/ja/guide/configuration) — 全アプリケーションプロパティとXML設定。
- [Tomcat連携](/ja/guide/tomcat) — Tomcat固有の動作とリバースプロキシ設定。
- [Jetty連携](/ja/guide/jetty) — Jetty固有の動作と既知の制限事項。
- [高度な設定](/ja/guide/advanced) — TeeFilter、URLフィルタリング、JSONロギング、Spring Security。
