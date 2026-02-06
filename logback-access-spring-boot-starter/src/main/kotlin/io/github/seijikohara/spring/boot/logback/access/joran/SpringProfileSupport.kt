package io.github.seijikohara.spring.boot.logback.access.joran

import ch.qos.logback.core.Context
import ch.qos.logback.core.joran.action.BaseModelAction
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext
import ch.qos.logback.core.model.Model
import ch.qos.logback.core.model.NamedModel
import ch.qos.logback.core.model.processor.ModelHandlerBase
import ch.qos.logback.core.model.processor.ModelInterpretationContext
import ch.qos.logback.core.util.OptionHelper.substVars
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.util.StringUtils.commaDelimitedListToStringArray
import org.springframework.util.StringUtils.trimArrayElements
import org.xml.sax.Attributes

/**
 * Joran `<springProfile>` tag support.
 *
 * Conditionally enables sections of Logback-access configuration
 * based on active Spring profiles.
 * Usage: `<springProfile name="dev,staging">...</springProfile>`
 *
 * @see org.springframework.boot.logging.logback.SpringProfileAction
 * @see org.springframework.boot.logging.logback.SpringProfileModel
 * @see org.springframework.boot.logging.logback.SpringProfileModelHandler
 */

internal class SpringProfileAction : BaseModelAction() {
    override fun buildCurrentModel(
        ic: SaxEventInterpretationContext,
        name: String,
        attrs: Attributes,
    ): Model =
        SpringProfileModel().apply {
            this.name = attrs.getValue(NAME_ATTRIBUTE)
        }
}

internal class SpringProfileModel : NamedModel()

internal class SpringProfileModelHandler(
    context: Context,
    private val environment: Environment,
) : ModelHandlerBase(context) {
    override fun handle(
        ic: ModelInterpretationContext,
        model: Model,
    ): Unit =
        (model as SpringProfileModel)
            .let { trimArrayElements(commaDelimitedListToStringArray(it.name)) }
            .map { substVars(it, ic, context) }
            .toTypedArray()
            .takeIf { it.isEmpty() || !environment.acceptsProfiles(Profiles.of(*it)) }
            ?.let { model.deepMarkAsSkipped() }
            ?: Unit
}
