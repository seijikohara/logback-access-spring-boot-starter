package io.github.seijikohara.spring.boot.logback.access.joran

import ch.qos.logback.access.common.spi.AccessContext
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.mock.env.MockEnvironment
import java.io.ByteArrayInputStream

class AccessJoranConfiguratorSpec :
    FunSpec({
        context("springProperty") {
            test("resolves value from environment") {
                val environment =
                    MockEnvironment().apply {
                        setProperty("test.key", "test-value")
                    }
                val context = AccessContext()
                val configurator = AccessJoranConfigurator(environment)
                configurator.context = context

                val xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <configuration>
                        <springProperty name="myProp" source="test.key" scope="context"/>
                        <appender name="list" class="ch.qos.logback.core.read.ListAppender"/>
                        <appender-ref ref="list"/>
                    </configuration>
                    """.trimIndent()

                configurator.doConfigure(ByteArrayInputStream(xml.toByteArray()))

                context.getProperty("myProp") shouldBe "test-value"
            }

            test("uses defaultValue when property not found") {
                val environment = MockEnvironment()
                val context = AccessContext()
                val configurator = AccessJoranConfigurator(environment)
                configurator.context = context

                val xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <configuration>
                        <springProperty name="myProp" source="nonexistent.key" defaultValue="fallback" scope="context"/>
                        <appender name="list" class="ch.qos.logback.core.read.ListAppender"/>
                        <appender-ref ref="list"/>
                    </configuration>
                    """.trimIndent()

                configurator.doConfigure(ByteArrayInputStream(xml.toByteArray()))

                context.getProperty("myProp") shouldBe "fallback"
            }

            test("uses empty string when property not found and no defaultValue") {
                val environment = MockEnvironment()
                val context = AccessContext()
                val configurator = AccessJoranConfigurator(environment)
                configurator.context = context

                val xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <configuration>
                        <springProperty name="myProp" source="nonexistent.key" scope="context"/>
                        <appender name="list" class="ch.qos.logback.core.read.ListAppender"/>
                        <appender-ref ref="list"/>
                    </configuration>
                    """.trimIndent()

                configurator.doConfigure(ByteArrayInputStream(xml.toByteArray()))

                context.getProperty("myProp") shouldBe ""
            }
        }

        context("springProfile") {
            test("includes content when profile matches") {
                val environment =
                    MockEnvironment().apply {
                        setActiveProfiles("dev")
                    }
                val context = AccessContext()
                val configurator = AccessJoranConfigurator(environment)
                configurator.context = context

                val xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <configuration>
                        <springProfile name="dev">
                            <appender name="devAppender" class="ch.qos.logback.core.read.ListAppender"/>
                            <appender-ref ref="devAppender"/>
                        </springProfile>
                        <springProfile name="prod">
                            <appender name="prodAppender" class="ch.qos.logback.core.read.ListAppender"/>
                            <appender-ref ref="prodAppender"/>
                        </springProfile>
                    </configuration>
                    """.trimIndent()

                configurator.doConfigure(ByteArrayInputStream(xml.toByteArray()))
                context.start()

                assertSoftly {
                    context.getAppender("devAppender").shouldNotBeNull()
                    context.getAppender("prodAppender").shouldBeNull()
                }
            }

            test("skips content when profile does not match") {
                val environment =
                    MockEnvironment().apply {
                        setActiveProfiles("prod")
                    }
                val context = AccessContext()
                val configurator = AccessJoranConfigurator(environment)
                configurator.context = context

                val xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <configuration>
                        <springProfile name="dev">
                            <appender name="devAppender" class="ch.qos.logback.core.read.ListAppender"/>
                            <appender-ref ref="devAppender"/>
                        </springProfile>
                        <springProfile name="prod">
                            <appender name="prodAppender" class="ch.qos.logback.core.read.ListAppender"/>
                            <appender-ref ref="prodAppender"/>
                        </springProfile>
                    </configuration>
                    """.trimIndent()

                configurator.doConfigure(ByteArrayInputStream(xml.toByteArray()))
                context.start()

                assertSoftly {
                    context.getAppender("devAppender").shouldBeNull()
                    context.getAppender("prodAppender").shouldNotBeNull()
                }
            }

            test("supports multiple profiles in name attribute") {
                val environment =
                    MockEnvironment().apply {
                        setActiveProfiles("staging")
                    }
                val context = AccessContext()
                val configurator = AccessJoranConfigurator(environment)
                configurator.context = context

                val xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <configuration>
                        <springProfile name="dev, staging">
                            <appender name="nonProdAppender" class="ch.qos.logback.core.read.ListAppender"/>
                            <appender-ref ref="nonProdAppender"/>
                        </springProfile>
                    </configuration>
                    """.trimIndent()

                configurator.doConfigure(ByteArrayInputStream(xml.toByteArray()))
                context.start()

                context.getAppender("nonProdAppender").shouldNotBeNull()
            }

            test("handles negation in profile expression") {
                val environment =
                    MockEnvironment().apply {
                        setActiveProfiles("dev")
                    }
                val context = AccessContext()
                val configurator = AccessJoranConfigurator(environment)
                configurator.context = context

                val xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <configuration>
                        <springProfile name="!prod">
                            <appender name="notProdAppender" class="ch.qos.logback.core.read.ListAppender"/>
                            <appender-ref ref="notProdAppender"/>
                        </springProfile>
                    </configuration>
                    """.trimIndent()

                configurator.doConfigure(ByteArrayInputStream(xml.toByteArray()))
                context.start()

                context.getAppender("notProdAppender").shouldNotBeNull()
            }
        }

        context("combined springProperty and springProfile") {
            test("springProperty works inside springProfile") {
                val environment =
                    MockEnvironment().apply {
                        setActiveProfiles("dev")
                        setProperty("app.name", "test-app")
                    }
                val context = AccessContext()
                val configurator = AccessJoranConfigurator(environment)
                configurator.context = context

                val xml =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <configuration>
                        <springProfile name="dev">
                            <springProperty name="appName" source="app.name" scope="context"/>
                            <appender name="list" class="ch.qos.logback.core.read.ListAppender"/>
                            <appender-ref ref="list"/>
                        </springProfile>
                    </configuration>
                    """.trimIndent()

                configurator.doConfigure(ByteArrayInputStream(xml.toByteArray()))
                context.start()

                assertSoftly {
                    context.getProperty("appName") shouldBe "test-app"
                    context.getAppender("list").shouldNotBeNull()
                }
            }
        }
    })
