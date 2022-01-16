package com.github.su23.dynamic.property.catalog

import kotlin.reflect.KClass

data class DynamicProperty internal constructor(
    val name: String,
    val type: KClass<*>,
    val ownerType: KClass<*>,
    val userDefined: Boolean,
    val metadata: DynamicPropertyMetadata?
) {
    companion object {
        private val registry: IDynamicPropertyRegistry = DynamicPropertyRegistry.value

        fun registerProperty(
            name: String,
            type: KClass<*>,
            ownerType: KClass<*>,
            defaultMetadata: DynamicPropertyMetadata? = null
        ): DynamicProperty {
            return registry.registerProperty(name, type, ownerType, defaultMetadata)
        }
    }
}

data class DynamicPropertyMetadata(
    val onPropertyChanged: ((dynamicObject: IDynamicObject, e: DynamicPropertyChangedEventArgs) -> Unit)? = null,
    val defaultValue: Any? = null
) {
    fun raisePropertyChanged(dynamicObject: IDynamicObject, e: DynamicPropertyChangedEventArgs) {
        onPropertyChanged?.invoke(dynamicObject, e)
    }
}

data class DynamicPropertyChangedEventArgs(val oldValue: Any?, val newValue: Any?, val property: DynamicProperty)

interface IDynamicObject {
    fun getValue(property: DynamicProperty): Any?
    fun setValue(property: DynamicProperty, value: Any?)
    fun clearValue(property: DynamicProperty)
}

interface IHasDynamicProperty {
    fun register()
}

class DynamicPropertyContainer(private val owner: IDynamicObject) : IDynamicObject {
    private companion object {
        val propertyRegistry: IDynamicPropertyRegistry = DynamicPropertyRegistry.value


    }

    private val entityType: KClass<*> = owner::class
    private val dynamicTypeMap = mutableMapOf<KClass<*>, Any?>()


    override fun getValue(property: DynamicProperty): Any? {
        checkProperty(property)

        return getValueInternal(property)
    }

    private fun <T> getValueInternal(property: DynamicProperty): T? {
        checkProperty(property)

        val properties = getValueMap(property) as? Map<DynamicProperty, T>

        if (properties != null) {
            val currentValue = properties[property]
            if (currentValue != null) {
                return currentValue
            }
        }

        if (property.metadata?.defaultValue != null) {
            return property.metadata.defaultValue as? T
        }

        return null
    }

    override fun setValue(property: DynamicProperty, value: Any?) {
        setValueInternal(property, value)
    }

    private fun <T> setValueInternal(property: DynamicProperty, value: T?) {
        checkProperty(property)

        val currentValue = getValueInternal<T>(property)
        if (currentValue == value) {
            return
        }

        val properties = getOrCreateValueMap(property) as MutableMap<DynamicProperty, T?>
        properties[property] = value
        property.metadata?.raisePropertyChanged(owner, DynamicPropertyChangedEventArgs(currentValue, value, property))
    }

    override fun clearValue(property: DynamicProperty) {
        checkProperty(property)

        val oldValue = getValue(property) ?: return

        val properties = getValueMap(property) as? MutableMap<DynamicProperty, Any?>
        if (properties != null && properties.containsKey(property)) {
            properties.remove(property)
            property.metadata?.raisePropertyChanged(owner, DynamicPropertyChangedEventArgs(oldValue, null, property))
        }
    }

    private fun getValueMap(property: DynamicProperty) = dynamicTypeMap[property.type]
    private fun getOrCreateValueMap(property: DynamicProperty): Any {
        val currentValue = getValueMap(property)
        if (currentValue != null)
            return currentValue


        val map = mutableMapOf<DynamicProperty, Nothing>()
        dynamicTypeMap[property.type] = map
        return map
    }


    private fun checkProperty(property: DynamicProperty) {
        if (!propertyRegistry.isRegistered(property.name, entityType)) {
            throw IllegalArgumentException("A property named ${property.name} is not exists on ${entityType.simpleName}")
        }
    }
}

interface IDynamicPropertyRegistry {
    fun getAllDynamicTypes(): List<KClass<*>>

    fun registerProperty(
        name: String,
        propertyType: KClass<*>,
        ownerType: KClass<*>,
        defaultMetadata: DynamicPropertyMetadata? = null
    ): DynamicProperty

    fun registerUserProperty(
        name: String,
        propertyType: KClass<*>,
        ownerType: KClass<*>,
        defaultMetadata: DynamicPropertyMetadata? = null
    ): DynamicProperty

    fun isRegistered(name: String, ownerType: KClass<*>): Boolean
}


