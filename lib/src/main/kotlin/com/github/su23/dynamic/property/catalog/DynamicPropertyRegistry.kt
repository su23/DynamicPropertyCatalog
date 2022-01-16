package com.github.su23.dynamic.property.catalog

import kotlin.reflect.KClass
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.superclasses

class DynamicPropertyRegistry private constructor(): IDynamicPropertyRegistry {
    companion object {
        val value = DynamicPropertyRegistry()
        private val propertyDeclarations = mutableMapOf<KClass<*>, MutableSet<DynamicProperty>>()
    }

    override fun getAllDynamicTypes(): List<KClass<*>> {
        TODO("Not yet implemented")
    }

    override fun registerProperty(
        name: String,
        propertyType: KClass<*>,
        ownerType: KClass<*>,
        defaultMetadata: DynamicPropertyMetadata?
    ): DynamicProperty {
        val property = DynamicProperty(name, propertyType, ownerType, false, defaultMetadata)
        register(property)
        return property
    }

    override fun registerUserProperty(
        name: String,
        propertyType: KClass<*>,
        ownerType: KClass<*>,
        defaultMetadata: DynamicPropertyMetadata?
    ): DynamicProperty {
        return registerProperty(name, propertyType, ownerType, defaultMetadata)
    }

    override fun isRegistered(name: String, ownerType: KClass<*>): Boolean {
        var type: KClass<*>? = ownerType

        val objType = IDynamicObject::class
        while (type != null && type.isSubclassOf(objType)) {
            val declarations = propertyDeclarations[type]
            if (declarations != null) {
                if (declarations.any { it.name == name && it.ownerType == type }) {
                    return true
                }
            }

            type = type.superclasses.firstOrNull()
        }

        return false
    }

    private fun register(property: DynamicProperty) {
        if (property.ownerType.java.isInterface) {
            throw IllegalArgumentException("Property ${property.name} can't be registered on interface ${property.ownerType.simpleName}")
        }

        registerClassProperties(property.ownerType)

        if (isRegistered(property.name, property.ownerType)) {
            val message = "A property named ${property.name} already exists in ${property.ownerType.simpleName} or in its parent"
            throw IllegalArgumentException(message)
        }

        val typeDeclarations = propertyDeclarations.getOrPut(property.ownerType) { mutableSetOf() }
        typeDeclarations.add(property)

        onPropertyRegistered(property)
    }

    private fun registerClassProperties(type: KClass<*>) {
        (type.companionObjectInstance as? IHasDynamicProperty)?.register()
    }

    private fun onPropertyRegistered(property: DynamicProperty) {
    }
}