/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.descriptors.ValueClassEnum.Inline
import org.jetbrains.kotlin.descriptors.ValueClassEnum.MultiField
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

sealed class ValueClassRepresentation<Type : SimpleTypeMarker> {
    abstract val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>
    abstract fun containsPropertyWithName(name: Name): Boolean
    abstract fun propertyTypeByName(name: Name): Type?

    fun <Other : SimpleTypeMarker> mapUnderlyingType(transform: (Type) -> Other): ValueClassRepresentation<Other> = when (this) {
        is InlineClassRepresentation -> InlineClassRepresentation(underlyingPropertyName, transform(underlyingType))
        is MultiFieldValueClassRepresentation ->
            MultiFieldValueClassRepresentation(underlyingPropertyNamesToTypes.map { (name, type) -> name to transform(type) })
    }
}

fun SimpleTypeMarker.valueClassRepresentationTypeMarkersList(context: TypeSystemCommonBackendContext): List<Pair<Name, SimpleTypeMarker>>? =
    with(context) { this@valueClassRepresentationTypeMarkersList.typeConstructor().valueClassRepresentationTypeMarkersList() }

enum class ValueClassEnum { Inline, MultiField }

fun <Type : SimpleTypeMarker> jvmInlineLoweringMode(
    context: TypeSystemCommonBackendContext,
    fields: List<Pair<Name, Type>>,
): ValueClassEnum = when {
    fields.size > 1 -> MultiField
    fields.isEmpty() -> error("Value classes cannot have 0 fields")
    else -> {
        val type = fields.single().second
        with(context) {
            when {
                type.isNullableType() -> Inline
                !type.typeConstructor().isMultiFieldValueClass() -> Inline
                else -> MultiField
            }
        }
    }
}

fun <Type : SimpleTypeMarker> createValueClassRepresentation(context: TypeSystemCommonBackendContext, fields: List<Pair<Name, Type>>) =
    when (jvmInlineLoweringMode(context, fields)) {
        Inline -> InlineClassRepresentation(fields[0].first, fields[0].second)
        MultiField -> MultiFieldValueClassRepresentation(fields)
    }