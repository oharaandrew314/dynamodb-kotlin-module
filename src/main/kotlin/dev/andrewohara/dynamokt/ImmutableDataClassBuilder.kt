package dev.andrewohara.dynamokt

import java.lang.IllegalArgumentException
import kotlin.reflect.KFunction

internal class ImmutableDataClassBuilder(private val constructor: KFunction<Any>) {
    private val values = mutableMapOf<String, Any?>()

    fun build(): Any {
        val params = constructor.parameters.associateWith { values[it.name] }
            .mapNotNull { (param, value) -> if (value == null && param.isOptional) null else param to value } // filter out null optional constructor values
            .toMap()

        return try {
            constructor.callBy(params)
        } catch (e: Throwable) {
            throw IllegalArgumentException("Could not map item to ${constructor.returnType}", e)
        }
    }

    operator fun get(name: String) = values[name]
    operator fun set(name: String, value: Any?) = values.set(name, value)
}