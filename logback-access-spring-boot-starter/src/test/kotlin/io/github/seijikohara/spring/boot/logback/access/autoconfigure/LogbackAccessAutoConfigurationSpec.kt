package io.github.seijikohara.spring.boot.logback.access.autoconfigure

import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties
import io.kotest.core.spec.style.FunSpec
import org.apache.catalina.startup.Tomcat
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.mock.env.MockEnvironment
import org.springframework.security.web.SecurityFilterChain

/**
 * Auto-configuration regression tests following the Spring Boot 4.0 official testing patterns.
 *
 * @see <a href="https://docs.spring.io/spring-boot/4.0/reference/features/developing-auto-configuration.html">
 *     Testing Your Auto-configuration</a>
 */
class LogbackAccessAutoConfigurationSpec :
    FunSpec({
        val autoConfiguration =
            AutoConfigurations.of(
                LogbackAccessAutoConfiguration::class.java,
            )

        fun baseRunner() =
            WebApplicationContextRunner()
                .withConfiguration(autoConfiguration)
                .withPropertyValues(
                    "logback.access.config-location=${LogbackAccessProperties.FALLBACK_CONFIG}",
                )

        context("enabled property") {
            test("creates LogbackAccessContext when enabled is not set (matchIfMissing)") {
                baseRunner().run { context ->
                    assertThat(context).hasSingleBean(LogbackAccessContext::class.java)
                }
            }

            test("creates LogbackAccessContext when enabled is true") {
                baseRunner()
                    .withPropertyValues("logback.access.enabled=true")
                    .run { context ->
                        assertThat(context).hasSingleBean(LogbackAccessContext::class.java)
                    }
            }

            test("does not create beans when enabled is false") {
                baseRunner()
                    .withPropertyValues("logback.access.enabled=false")
                    .run { context ->
                        assertThat(context).doesNotHaveBean(LogbackAccessContext::class.java)
                    }
            }
        }

        context("web application condition") {
            test("does not create beans in non-web context") {
                ApplicationContextRunner()
                    .withConfiguration(autoConfiguration)
                    .withPropertyValues(
                        "logback.access.config-location=${LogbackAccessProperties.FALLBACK_CONFIG}",
                    ).run { context ->
                        assertThat(context).doesNotHaveBean(LogbackAccessContext::class.java)
                    }
            }
        }

        context("custom bean back-off") {
            test("backs off when user defines LogbackAccessContext") {
                baseRunner()
                    .withUserConfiguration(CustomLogbackAccessContextConfiguration::class.java)
                    .run { context ->
                        assertThat(context).hasSingleBean(LogbackAccessContext::class.java)
                        assertThat(context)
                            .getBean("customLogbackAccessContext")
                            .isSameAs(context.getBean(LogbackAccessContext::class.java))
                    }
            }
        }

        context("Tomcat detection") {
            test("creates Tomcat customizer when Tomcat is on classpath") {
                baseRunner().run { context ->
                    assertThat(context).hasBean("logbackAccessTomcatCustomizer")
                }
            }

            test("does not create Tomcat customizer when Tomcat is absent") {
                baseRunner()
                    .withClassLoader(FilteredClassLoader(Tomcat::class.java))
                    .run { context ->
                        assertThat(context).doesNotHaveBean("logbackAccessTomcatCustomizer")
                    }
            }
        }

        context("Jetty detection") {
            test("creates Jetty customizer when Jetty is on classpath") {
                baseRunner().run { context ->
                    assertThat(context).hasBean("logbackAccessJettyCustomizer")
                }
            }

            test("does not create Jetty customizer when Jetty is absent") {
                baseRunner()
                    .withClassLoader(FilteredClassLoader(Server::class.java))
                    .run { context ->
                        assertThat(context).doesNotHaveBean("logbackAccessJettyCustomizer")
                    }
            }
        }

        context("Security detection") {
            test("creates security filter when Spring Security is on classpath") {
                baseRunner().run { context ->
                    assertThat(context).hasBean("logbackAccessSecurityFilter")
                    assertThat(context).hasBean("logbackAccessAuthenticationTrustResolver")
                }
            }

            test("does not create security filter when Spring Security is absent") {
                baseRunner()
                    .withClassLoader(FilteredClassLoader(SecurityFilterChain::class.java))
                    .run { context ->
                        assertThat(context).doesNotHaveBean("logbackAccessSecurityFilter")
                        assertThat(context).doesNotHaveBean("logbackAccessAuthenticationTrustResolver")
                    }
            }
        }

        context("TeeFilter detection") {
            test("creates TeeFilter bean when tee-filter.enabled is true") {
                baseRunner()
                    .withPropertyValues("logback.access.tee-filter.enabled=true")
                    .run { context ->
                        assertThat(context).hasBean("logbackAccessTeeFilter")
                    }
            }

            test("does not create TeeFilter bean when tee-filter.enabled is not set") {
                baseRunner().run { context ->
                    assertThat(context).doesNotHaveBean("logbackAccessTeeFilter")
                }
            }
        }
    })

@Configuration(proxyBeanMethods = false)
private class CustomLogbackAccessContextConfiguration {
    @Bean
    fun customLogbackAccessContext(): LogbackAccessContext =
        LogbackAccessContext(
            properties =
                LogbackAccessProperties(
                    enabled = true,
                    configLocation = LogbackAccessProperties.FALLBACK_CONFIG,
                    localPortStrategy = io.github.seijikohara.spring.boot.logback.access.LocalPortStrategy.SERVER,
                    tomcat = LogbackAccessProperties.TomcatProperties(requestAttributesEnabled = null),
                    teeFilter =
                        LogbackAccessProperties.TeeFilterProperties(
                            enabled = false,
                            includeHosts = null,
                            excludeHosts = null,
                            maxPayloadSize = 65536L,
                            allowedContentTypes = null,
                        ),
                    filter = LogbackAccessProperties.FilterProperties(includeUrlPatterns = null, excludeUrlPatterns = null),
                ),
            resourceLoader = DefaultResourceLoader(),
            environment = MockEnvironment(),
        )
}
