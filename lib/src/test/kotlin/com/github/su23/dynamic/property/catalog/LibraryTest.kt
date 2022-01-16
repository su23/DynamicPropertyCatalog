/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.github.su23.dynamic.property.catalog

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.assertThrows

class LibraryTest : FreeSpec({
    "Test dynamic property callback" {
        val defaultValue = 123456
        val value = 7890
        var callBackInvoked = false
        var callbackSender: IDynamicObject? = null
        var callbackEventArgs: DynamicPropertyChangedEventArgs? = null
        val onPropertyChanged: (IDynamicObject, DynamicPropertyChangedEventArgs) -> Unit = {
            d: IDynamicObject, e: DynamicPropertyChangedEventArgs ->
            run {
                callbackSender = d
                callbackEventArgs = e
                callBackInvoked = true
            }
        }

        val dynamicObjectMock = ParentTestObject()
        val dp = DynamicProperty.registerProperty("name", Int::class, dynamicObjectMock::class, DynamicPropertyMetadata(onPropertyChanged, defaultValue))
        val propertyContainer = DynamicPropertyContainer(dynamicObjectMock)

        propertyContainer.setValue(dp, value)

        callBackInvoked shouldBe true
        callbackSender shouldBe dynamicObjectMock
        callbackEventArgs shouldNotBe null
        callbackEventArgs!!.oldValue shouldBe defaultValue
        callbackEventArgs!!.newValue shouldBe value
        callbackEventArgs!!.property shouldBe dp
    }

    "Register method must throw exception for duplicated property names" {
        val name = "Name"
        DynamicProperty.registerProperty(name, String::class, ParentTestObject::class)

        assertThrows<IllegalArgumentException> {
            DynamicProperty.registerProperty(name, String::class, ParentTestObject::class)
        }
    }

    "Register method must throw exception for duplicated property names inherited from base class" {
        val name = "1111"
        DynamicProperty.registerProperty(name, Int::class, ParentTestObject::class)

        assertThrows<IllegalArgumentException> {
            DynamicProperty.registerProperty(name, Int::class, ChildTestObject::class)
        }
    }
}) {
    open class ParentTestObject : IDynamicObject {
        override fun getValue(property: DynamicProperty): Any? {
            TODO("Not yet implemented")
        }

        override fun setValue(property: DynamicProperty, value: Any?) {
            TODO("Not yet implemented")
        }

        override fun clearValue(property: DynamicProperty) {
            TODO("Not yet implemented")
        }
    }

    class ChildTestObject : ParentTestObject()
}
