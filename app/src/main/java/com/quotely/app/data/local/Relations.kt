package com.quotely.app.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class CotizacionConItems(
    @Embedded val cotizacion: CotizacionEntity,
    @Relation(parentColumn = "id", entityColumn = "cotizacionId")
    val items: List<CotizacionItemEntity>
) {
    val itemsOrdenados: List<CotizacionItemEntity> get() = items.sortedBy { it.orden }
}

data class PedidoConItems(
    @Embedded val pedido: PedidoEntity,
    @Relation(parentColumn = "id", entityColumn = "pedidoId")
    val items: List<PedidoItemEntity>
) {
    val itemsOrdenados: List<PedidoItemEntity> get() = items.sortedBy { it.orden }
}
