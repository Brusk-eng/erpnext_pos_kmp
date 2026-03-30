package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

// TODO: Mover
object IntAsBooleanSerializer : KSerializer<Boolean> {
  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("BooleanAsInt", PrimitiveKind.INT)

  override fun serialize(encoder: Encoder, value: Boolean) {
    encoder.encodeInt(if (value) 1 else 0)
  }

  override fun deserialize(decoder: Decoder): Boolean {
    if (decoder is JsonDecoder) {
      return decodeJsonPrimitive(decoder.decodeJsonElement() as? JsonPrimitive)
    }

    return runCatching { decoder.decodeInt() != 0 }
        .recoverCatching { decoder.decodeBoolean() }
        .recoverCatching { parseStringValue(decoder.decodeString()) }
        .getOrElse { false }
  }

  private fun decodeJsonPrimitive(value: JsonPrimitive?): Boolean {
    if (value == null) return false
    value.booleanOrNull?.let { return it }
    value.intOrNull?.let { return it != 0 }
    return parseStringValue(value.contentOrNull)
  }

  private fun parseStringValue(value: String?): Boolean {
    return when (value?.trim()?.lowercase()) {
      "1", "true", "yes", "y", "on" -> true
      else -> false
    }
  }
}

@Serializable
data class ItemDto(
    @SerialName("item_code") val itemCode: String,
    @SerialName("item_name") val itemName: String,
    @SerialName("item_group") val itemGroup: String,
    @SerialName("description") val description: String,
    @SerialName("brand") val brand: String? = null,
    @SerialName("image") val image: String? = null,
    @SerialName("disabled")
    @Serializable(with = IntAsBooleanSerializer::class)
    val disabled: Boolean = false,
    @SerialName("stock_uom") val stockUom: String,
    @SerialName("standard_rate") val standardRate: Double = 0.0,
    @SerialName("is_stock_item")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isStockItem: Boolean = false,
    @SerialName("is_sales_item")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isSalesItem: Boolean = true,
)

@Serializable data class BarcodeChild(@SerialName("barcode") val barcode: String)
