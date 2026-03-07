package com.francisco.calculadorapedidos.data.db

import androidx.room.TypeConverter
import com.francisco.calculadorapedidos.data.ClientType

class Converters {
    @TypeConverter
    fun fromClientType(value: ClientType): String = value.name

    @TypeConverter
    fun toClientType(value: String): ClientType = enumValueOf(value)
}