# はじめに

このガイドでは、Spring BootアプリケーションにHTTPアクセスロギングを追加する方法を説明します。

## 前提条件

- Java 17以上
- Spring Boot 4.0以上
- TomcatまたはJetty組み込みサーバー

## インストール

ビルドファイルに依存関係を追加します:

::: code-group

```kotlin [Gradle (Kotlin)]
implementation("io.github.seijikohara:logback-access-spring-boot-starter:1.0.0")
```

```groovy [Gradle (Groovy)]
implementation 'io.github.seijikohara:logback-access-spring-boot-starter:1.0.0'
```

```xml [Maven]
<dependency>
    <groupId>io.github.seijikohara</groupId>
    <artifactId>logback-access-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

:::

## 基本設定

`src/main/resources`に`logback-access.xml`ファイルを作成します:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%h %l %u %t "%r" %s %b</pattern>
        </encoder>
    </appender>
    <appender-ref ref="CONSOLE"/>
</configuration>
```

この設定は、Common Log Format（CLF）でアクセスログを出力します。

## パターン変数

以下のパターン変数が使用可能です:

| 変数 | 説明 |
|------|------|
| `%h` | リモートホスト（IPアドレス） |
| `%l` | リモートログ名（常に`-`） |
| `%u` | リモートユーザー（認証から取得） |
| `%t` | リクエストタイムスタンプ |
| `%r` | リクエストライン（メソッド、URI、プロトコル） |
| `%s` | HTTPステータスコード |
| `%b` | レスポンスボディサイズ（バイト） |
| `%D` | リクエスト処理時間（ミリ秒） |
| `%T` | リクエスト処理時間（秒） |
| `%I` | スレッド名 |

## Combined Log Format

ApacheのCombined Log Formatに近い詳細な出力:

```xml
<pattern>%h %l %u %t "%r" %s %b "%i{Referer}" "%i{User-Agent}"</pattern>
```

## 動作確認

1. Spring Bootアプリケーションを起動
2. 任意のエンドポイントにHTTPリクエストを送信
3. コンソール出力でアクセスログを確認

出力例:

```
127.0.0.1 - - [01/Jan/2024:12:00:00 +0000] "GET /api/users HTTP/1.1" 200 1234
```

## 次のステップ

- [設定リファレンス](/ja/guide/configuration) - すべての設定オプションを学ぶ
- [Tomcat連携](/ja/guide/tomcat) - Tomcat固有の設定
- [Jetty連携](/ja/guide/jetty) - Jetty固有の設定
- [高度な設定](/ja/guide/advanced) - TeeFilter、URLフィルタリングなど
