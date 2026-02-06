import { defineConfig } from 'vitepress'
import { withMermaid } from 'vitepress-plugin-mermaid'

export default withMermaid(
  defineConfig({
    title: 'logback-access-spring-boot-starter',
    description: 'Spring Boot 4 auto-configuration for Logback Access HTTP logging',

    // GitHub Pages subdirectory
    base: '/logback-access-spring-boot-starter/',

    // SEO: Clean URLs
    cleanUrls: true,

    // SEO: Sitemap
    sitemap: {
      hostname: 'https://seijikohara.github.io/logback-access-spring-boot-starter'
    },

    // Head: Favicon, OGP
    head: [
      ['link', { rel: 'icon', href: '/logback-access-spring-boot-starter/logo.svg', type: 'image/svg+xml' }],
      ['meta', { property: 'og:type', content: 'website' }],
      ['meta', { property: 'og:site_name', content: 'logback-access-spring-boot-starter' }],
      ['meta', { property: 'og:image', content: 'https://seijikohara.github.io/logback-access-spring-boot-starter/og-image.png' }],
      ['meta', { name: 'twitter:card', content: 'summary_large_image' }],
    ],

    // i18n
    locales: {
      root: {
        label: 'English',
        lang: 'en',
      },
      ja: {
        label: '日本語',
        lang: 'ja',
        description: 'Spring Boot 4向けLogback AccessのHTTPロギング自動設定',
        themeConfig: {
          siteTitle: 'Logback Access',
          nav: [
            { text: 'ガイド', link: '/ja/guide/getting-started' },
          ],
          sidebar: {
            '/ja/guide/': [
              {
                text: 'ガイド',
                items: [
                  { text: 'はじめに', link: '/ja/guide/getting-started' },
                  { text: '設定', link: '/ja/guide/configuration' },
                  { text: 'Tomcat連携', link: '/ja/guide/tomcat' },
                  { text: 'Jetty連携', link: '/ja/guide/jetty' },
                  { text: '高度な設定', link: '/ja/guide/advanced' },
                ]
              }
            ]
          },
          outlineTitle: '目次',
          returnToTopLabel: 'トップに戻る',
          sidebarMenuLabel: 'メニュー',
          darkModeSwitchLabel: 'ダークモード',
          langMenuLabel: '言語',
        }
      }
    },

    themeConfig: {
      logo: '/logo.svg',
      siteTitle: 'Logback Access',

      nav: [
        { text: 'Guide', link: '/guide/getting-started' },
      ],

      sidebar: {
        '/guide/': [
          {
            text: 'Guide',
            items: [
              { text: 'Getting Started', link: '/guide/getting-started' },
              { text: 'Configuration', link: '/guide/configuration' },
              { text: 'Tomcat Integration', link: '/guide/tomcat' },
              { text: 'Jetty Integration', link: '/guide/jetty' },
              { text: 'Advanced Topics', link: '/guide/advanced' },
            ]
          }
        ]
      },

      socialLinks: [
        { icon: 'github', link: 'https://github.com/seijikohara/logback-access-spring-boot-starter' }
      ],

      footer: {
        message: 'Released under the Apache 2.0 License.',
        copyright: 'Copyright © 2024 Seiji Kohara'
      },

      search: {
        provider: 'local'
      },

      editLink: {
        pattern: 'https://github.com/seijikohara/logback-access-spring-boot-starter/edit/main/docs/:path',
        text: 'Edit this page on GitHub'
      }
    },

    // Mermaid configuration
    mermaid: {
      theme: 'neutral',
      themeVariables: {
        primaryColor: '#3b82f6',
        primaryTextColor: '#1e293b',
        primaryBorderColor: '#60a5fa',
        lineColor: '#64748b',
        secondaryColor: '#f1f5f9',
        tertiaryColor: '#e2e8f0'
      }
    },

    // SEO: transformPageData for canonical and OGP
    transformPageData(pageData) {
      const canonicalUrl = `https://seijikohara.github.io/logback-access-spring-boot-starter/${pageData.relativePath}`
        .replace(/index\.md$/, '')
        .replace(/\.md$/, '')

      pageData.frontmatter.head ??= []
      pageData.frontmatter.head.push(
        ['link', { rel: 'canonical', href: canonicalUrl }],
        ['meta', { property: 'og:url', content: canonicalUrl }],
        ['meta', { property: 'og:title', content: pageData.title + ' | logback-access-spring-boot-starter' }],
        ['meta', { property: 'og:description', content: pageData.description || 'Spring Boot 4 auto-configuration for Logback Access HTTP logging' }],
      )
    }
  })
)
