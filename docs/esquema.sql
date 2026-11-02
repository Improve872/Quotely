-- Quotely · esquema SQLite versión 2
-- Generado desde app/schemas/com.quotely.app.data.local.QuotelyDatabase/2.json
-- Referencia de consulta; Room gestiona la creación y las migraciones en tiempo de ejecución.

-- Tabla responsables
CREATE TABLE IF NOT EXISTS `responsables` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `usuario` TEXT NOT NULL, `passwordHash` TEXT NOT NULL, `salt` TEXT NOT NULL, `rol` TEXT NOT NULL, `activo` INTEGER NOT NULL, `creadoEn` INTEGER NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS `index_responsables_usuario` ON `responsables` (`usuario`);

-- Tabla productos
CREATE TABLE IF NOT EXISTS `productos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `codigo` TEXT NOT NULL, `nombre` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `precioBase` REAL NOT NULL, `unidad` TEXT NOT NULL, `activo` INTEGER NOT NULL, `creadoEn` INTEGER NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS `index_productos_codigo` ON `productos` (`codigo`);

-- Tabla cotizaciones
CREATE TABLE IF NOT EXISTS `cotizaciones` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `folio` TEXT NOT NULL, `cliente` TEXT NOT NULL, `clienteContacto` TEXT NOT NULL, `fechaEmision` INTEGER NOT NULL, `fechaCaducidad` INTEGER NOT NULL, `responsableId` INTEGER NOT NULL, `estado` TEXT NOT NULL, `subtotal` REAL NOT NULL, `descuentoVolumen` REAL NOT NULL, `descuentoGlobal` REAL NOT NULL, `tipoDescuentoGlobal` TEXT NOT NULL, `valorDescuentoGlobal` REAL NOT NULL, `iva` REAL NOT NULL, `total` REAL NOT NULL, `notas` TEXT NOT NULL, `creadoEn` INTEGER NOT NULL, `actualizadoEn` INTEGER NOT NULL, FOREIGN KEY(`responsableId`) REFERENCES `responsables`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT );
CREATE UNIQUE INDEX IF NOT EXISTS `index_cotizaciones_folio` ON `cotizaciones` (`folio`);
CREATE INDEX IF NOT EXISTS `index_cotizaciones_responsableId` ON `cotizaciones` (`responsableId`);
CREATE INDEX IF NOT EXISTS `index_cotizaciones_estado` ON `cotizaciones` (`estado`);

-- Tabla cotizacion_items
CREATE TABLE IF NOT EXISTS `cotizacion_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cotizacionId` INTEGER NOT NULL, `productoId` INTEGER, `codigo` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `unidad` TEXT NOT NULL, `precioUnitario` REAL NOT NULL, `cantidad` INTEGER NOT NULL, `descuentoVolumenPct` REAL NOT NULL, `descuentoVolumen` REAL NOT NULL, `importe` REAL NOT NULL, `orden` INTEGER NOT NULL, FOREIGN KEY(`cotizacionId`) REFERENCES `cotizaciones`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_cotizacion_items_cotizacionId` ON `cotizacion_items` (`cotizacionId`);

-- Tabla pedidos
CREATE TABLE IF NOT EXISTS `pedidos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `folio` TEXT NOT NULL, `cliente` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `responsableId` INTEGER NOT NULL, `cotizacionId` INTEGER, `origen` TEXT NOT NULL, `estado` TEXT NOT NULL, `subtotal` REAL NOT NULL, `descuentoVolumen` REAL NOT NULL, `descuentoGlobal` REAL NOT NULL, `tipoDescuentoGlobal` TEXT NOT NULL, `valorDescuentoGlobal` REAL NOT NULL, `iva` REAL NOT NULL, `total` REAL NOT NULL, `notas` TEXT NOT NULL, `creadoEn` INTEGER NOT NULL, `actualizadoEn` INTEGER NOT NULL, FOREIGN KEY(`responsableId`) REFERENCES `responsables`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`cotizacionId`) REFERENCES `cotizaciones`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL );
CREATE UNIQUE INDEX IF NOT EXISTS `index_pedidos_folio` ON `pedidos` (`folio`);
CREATE INDEX IF NOT EXISTS `index_pedidos_responsableId` ON `pedidos` (`responsableId`);
CREATE UNIQUE INDEX IF NOT EXISTS `index_pedidos_cotizacionId` ON `pedidos` (`cotizacionId`);
CREATE INDEX IF NOT EXISTS `index_pedidos_estado` ON `pedidos` (`estado`);

-- Tabla pedido_items
CREATE TABLE IF NOT EXISTS `pedido_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `pedidoId` INTEGER NOT NULL, `productoId` INTEGER, `codigo` TEXT NOT NULL, `descripcion` TEXT NOT NULL, `unidad` TEXT NOT NULL, `precioUnitario` REAL NOT NULL, `cantidad` INTEGER NOT NULL, `descuentoVolumenPct` REAL NOT NULL, `descuentoVolumen` REAL NOT NULL, `importe` REAL NOT NULL, `orden` INTEGER NOT NULL, FOREIGN KEY(`pedidoId`) REFERENCES `pedidos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_pedido_items_pedidoId` ON `pedido_items` (`pedidoId`);
