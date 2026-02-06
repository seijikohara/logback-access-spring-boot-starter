package io.github.seijikohara.spring.boot.logback.access.joran

import ch.qos.logback.core.Context
import ch.qos.logback.core.joran.action.ActionUtil.stringToScope
import ch.qos.logback.core.joran.action.BaseModelAction
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext
import ch.qos.logback.core.model.Model
import ch.qos.logback.core.model.NamedModel
import ch.qos.logback.core.model.processor.ModelHandlerBase
import ch.qos.logback.core.model.processor.ModelInterpretationContext
import ch.qos.logback.core.model.util.PropertyModelHandlerHelper.setProperty
import org.springframework.core.env.Environment
import org.xml.sax.Attributes

/**
 * Joran `<springProperty>` tag support.
 *
 * Allows Logback-access properties to be sourced from the Spring [Environment].
 * Usage: `<springProperty name="propName" source="spring.property.key" defaultValue="fallback"/>`
 *
 * @see org.springframework.boot.logging.logback.SpringPropertyAction
 * @see org.springframework.boot.logging.logback.SpringPropertyModel
 * @see org.springframework.boot.logging.logback.SpringPropertyModelHandler
 */

internal class SpringPropertyAction : BaseModelAction() {
    override fun buildCurrentModel(
        ic: SaxEventInterpretationContext,
        name: String,
        attrs: Attributes,
    ): Model =
        SpringPropertyModel().apply {
            this.name = attrs.getValue(NAME_ATTRIBUTE)
            source = attrs.getValue("source")
            defaultValue = attrs.getValue("defaultValue")
            scope = attrs.getValue(SCOPE_ATTRIBUTE)
        }
}

internal class SpringPropertyModel : NamedModel() {
    var source: String? = null
    var defaultValue: String? = null
    var scope: String? = null
}

internal class SpringPropertyModelHandler(
    context: Context,
    private val environment: Environment,
) : ModelHandlerBase(context) {
    override fun handle(
        ic: ModelInterpretationContext,
        model: Model,
    ): Unit =
        (model as SpringPropertyModel).let { m ->
            val name = m.name
            val source = m.source
            when {
                name.isNullOrBlank() || source.isNullOrBlank() -> {
                    addError("""The "name" and "source" attributes of <springProperty> must be set""")
                }

                else -> {
                    setProperty(ic, name, environment.getProperty(source, m.defaultValue.orEmpty()), stringToScope(m.scope))
                }
            }
        }
}
