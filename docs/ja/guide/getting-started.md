# はじめに

このガイドでは、Spring BootアプリケーションにHTTPアクセスロギングを追加する方法を説明します。

## 前提条件

- Java 21以上
- Spring Boot 4.0以上
- TomcatまたはJetty組み込みサーバー

## モジュール構成

このライブラリは2つのMavenアーティファクトで構成されています:

| アーティファクト | 説明 |
|---------------|------|
| `logback-access-spring-boot-starter` | 自動設定とサーバー連携（Tomcat、Jetty、Security、TeeFilter） |
| `logback-access-spring-boot-starter-core` | 公開APIクラス（推移的依存関係 — 個別に宣言不要） |

ほとんどのユーザーはスターター依存関係のみを宣言すれば十分です。coreモジュールは推移的依存関係として自動的に含まれます。

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

`src/main/resources`に`logback-access.xml`ファイルを作成します:

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

この設定は、Common Log Format（CLF）でアクセスログを出力します。

## パターン変数

以下のパターン変数が使用可能です:

| 変数 | 説明 |
|------|------|
| `%h` | リモートホスト（IPアドレス） |
| `%a` | リモートIPアドレス |
| `%A` | ローカルIPアドレス |
| `%p` | ローカルポート |
| `%l` | リモートログ名（常に`-`） |
| `%u` | リモートユーザー（認証から取得） |
| `%t` | リクエストタイムスタンプ |
| `%r` | リクエストライン（メソッド、URI、プロトコル） |
| `%s` | HTTPステータスコード |
| `%b` | レスポンスボディサイズ（バイト） |
| `%D` | リクエスト処理時間（ミリ秒） |
| `%T` | リクエスト処理時間（秒） |
| `%I` | スレッド名 |
| `%{xxx}i` | リクエストヘッダー`xxx` |
| `%{xxx}o` | レスポンスヘッダー`xxx` |
| `%{xxx}c` | Cookie値`xxx` |
| `%{xxx}r` | リクエスト属性`xxx` |
| `%queryString` | クエリ文字列（`?`プレフィックス付き、例: `?name=value`） |
| `%requestContent` | リクエストボディ（TeeFilterが必要） |
| `%responseContent` | レスポンスボディ（TeeFilterが必要） |

::: tip 代替構文
ヘッダー、Cookie、属性パターンについては、`%{name}i`と`%i{name}`の両形式がサポートされています。本ドキュメントの例では`%i{name}`形式を使用しています。
:::

## Combined Log Format

ApacheのCombined Log Formatに近い詳細な出力:

```xml
<pattern>%h %l %u [%t] "%r" %s %b "%i{Referer}" "%i{User-Agent}"</pattern>
```

## 動作確認

1. Spring Bootアプリケーションを起動
2. 任意のエンドポイントにHTTPリクエストを送信
3. コンソール出力でアクセスログを確認

出力例:

```
127.0.0.1 - - [01/Jan/2026:12:00:00 +0000] "GET /api/users HTTP/1.1" 200 1234
```

## 次のステップ

- [設定リファレンス](/ja/guide/configuration) - すべての設定オプションを学ぶ
- [Tomcat連携](/ja/guide/tomcat) - Tomcat固有の設定
- [Jetty連携](/ja/guide/jetty) - Jetty固有の設定
- [高度な設定](/ja/guide/advanced) - TeeFilter、URLフィルタリングなど
