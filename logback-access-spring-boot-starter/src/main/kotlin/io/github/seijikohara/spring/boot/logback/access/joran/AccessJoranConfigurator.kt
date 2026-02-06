package io.github.seijikohara.spring.boot.logback.access.joran

import ch.qos.logback.access.common.joran.JoranConfigurator
import ch.qos.logback.core.joran.spi.ElementSelector
import ch.qos.logback.core.joran.spi.RuleStore
import ch.qos.logback.core.model.processor.AppenderRefDependencyAnalyser
import ch.qos.logback.core.model.processor.DefaultProcessor
import org.springframework.core.env.Environment
import java.util.function.Supplier

/**
 * Extended [JoranConfigurator] that adds Spring-specific XML tags:
 * - `<springProperty>`: Sources Logback properties from the Spring environment
 * - `<springProfile>`: Conditionally includes configuration based on active profiles
 *
 * @see org.springframework.boot.logging.logback.SpringBootJoranConfigurator
 */
class AccessJoranConfigurator(
    private val environment: Environment,
) : JoranConfigurator() {
    override fun addElementSelectorAndActionAssociations(store: RuleStore): Unit =
        super.addElementSelectorAndActionAssociations(store).also {
            store.addRule(ElementSelector("configuration/springProperty")) { SpringPropertyAction() }
            store.addRule(ElementSelector("*/springProfile")) { SpringProfileAction() }
            store.addTransparentPathPart("springProfile")
        }

    override fun addModelHandlerAssociations(processor: DefaultProcessor): Unit =
        processor.run {
            addHandler(SpringPropertyModel::class.java) { ctx, _ -> SpringPropertyModelHandler(ctx, environment) }
            addHandler(SpringProfileModel::class.java) { ctx, _ -> SpringProfileModelHandler(ctx, environment) }
            super.addModelHandlerAssociations(this)
            // Register the dependency analyser so that <appender-ref> elements nested
            // inside <springProfile> are discovered during the dependency analysis phase.
            // Without this, AppenderModelHandler skips appender creation because
            // hasDependers() returns false for appenders referenced only from within
            // a springProfile block.
            addAnalyser(SpringProfileModel::class.java) { AppenderRefDependencyAnalyser(context) }
        }

    override fun buildModelInterpretationContext(): Unit =
        super.buildModelInterpretationContext().also {
            modelInterpretationContext.configuratorSupplier =
                Supplier { AccessJoranConfigurator(environment).also { it.context = context } }
        }
}
