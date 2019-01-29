package stacrypt.stawallet.rest

import kotlin.reflect.KType

enum class ClientRole

interface Exportable<SCHEMA> {
    fun export(role: ClientRole? = null): SCHEMA?
}


data class EventResource(val id: Int)
