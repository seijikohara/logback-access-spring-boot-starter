---
layout: home
title: ホーム
description: Spring Boot 4向けLogback AccessのHTTPロギング自動設定

hero:
  name: logback-access-spring-boot-starter
  text: Spring Boot向けHTTPアクセスロギング
  tagline: TomcatとJetty対応のLogback Access自動設定
  image:
    src: /logo.svg
    alt: logback-access-spring-boot-starter
  actions:
    - theme: brand
      text: はじめる
      link: /ja/guide/getting-started
    - theme: alt
      text: GitHubで見る
      link: https://github.com/seijikohara/logback-access-spring-boot-starter

features:
  - icon: ⚙️
    title: 自動設定
    details: TomcatとJettyの組み込みサーバーに対するゼロコンフィグセットアップ。依存関係を追加するだけでロギング開始。
  - icon: 🔐
    title: Spring Security連携
    details: Spring Securityによる認証済みユーザー名をアクセスログに自動記録。
  - icon: 📝
    title: リクエスト/レスポンスボディ記録
    details: オプションのTeeFilterによるリクエストとレスポンスのボディ内容のログ記録。
  - icon: 🎯
    title: URLフィルタリング
    details: 包含/除外URLパターンでログ記録対象リクエストを制御。
  - icon: 🌱
    title: Springプロファイル対応
    details: Springプロファイルによる環境別ロギング設定。
  - icon: 📊
    title: JSONロギング
    details: logstash-logback-encoderによるJSON出力。LogstashやELKスタックと互換。
---

## アーキテクチャ

```mermaid
flowchart TB
    subgraph starter["logback-access-spring-boot-starter"]
        direction TB
        A[HTTPリクエスト] --> B{組み込みサーバー}
        B -->|Tomcat| C[TomcatValve]
        B -->|Jetty| D[JettyRequestLog]
    end

    subgraph core["logback-access-spring-boot-starter-core"]
        E[LogbackAccessContext]
    end

    C --> E
    D --> E
    E --> F[logback-access.xml]
    F --> G[Appenders]
    G -->|Console| H[コンソール出力]
    G -->|File| I[ファイル出力]
    G -->|JSON| J[Logstash/ELK]

    subgraph optional["オプション連携"]
        K[Spring Security] -.->|ユーザー名| E
        L[TeeFilter] -.->|ボディキャプチャ| E
    end
```

## クイックスタート

プロジェクトに依存関係を追加:

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

`src/main/resources/logback-access.xml`を作成:

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

アプリケーションを起動すると、コンソールにアクセスログが出力されます。

## 要件

| コンポーネント | バージョン |
|---------------|-----------|
| Java | 21以上 |
| Spring Boot | 4.0以上 |

## ライセンス

このプロジェクトは[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)の下でライセンスされています。
