# Diseño de Quotely

## 1. Encargo

Quotely es una herramienta de mostrador para una empresa que vende **equipo de cómputo**. La usa una persona de ventas, de pie o en movimiento, desde el teléfono. Su trabajo principal es **cotizar rápido** y, cuando el cliente acepta, **convertir esa cotización en pedido sin perder los precios**.

- **Audiencia**: responsables de ventas y administradores de una distribuidora técnica.
- **Trabajo de la app**: registrar documentos comerciales claros, con totales confiables, y dejar rastro de quién los hizo.
- **Contexto de uso**: una mano, pantalla pequeña, a veces con prisa; se necesita leer el total y el estado de un vistazo.

## 2. Dirección visual: “Mostrador técnico”

La primera propuesta (fondo crema, serif de alto contraste y acento terracota) se descartó por ser un lugar común de diseño generado. En su lugar se eligió una identidad derivada del propio negocio: **etiquetas de especificaciones de hardware y tickets de punto de venta**.

La app se ve como un documento de mostrador: folios en monoespaciada, cifras alineadas, sellos de estado y una tira de código de barras. No es una app financiera genérica; se siente como el comprobante que se entrega al cliente.

### Paleta

| Token | Claro | Oscuro | Uso |
|---|---|---|---|
| Papel | `#F2F3EF` | `#0E1420` | Fondo general |
| Papel elevado | `#FAFBF8` | `#161E2C` | Tarjetas y formularios |
| Tinta | `#121A2B` | `#ECEFE7` | Texto principal |
| Cobalto | `#2447D8` | `#8CA4FF` | Acción, folios, estado emitido |
| Naranja señal | `#E8590C` | `#FF9A62` | Descuentos, vencimientos, pedidos pendientes |
| Verde éxito | `#1F8A5B` | `#5FD3A0` | Convertida / entregado |
| Acero | `#6B7280` | `#B6C0CF` | Texto secundario |
| Línea | `#D7DBD2` | `#334155` | Bordes, reglas punteadas |

El papel es frío, no crema. El cobalto se reserva para lo que se puede accionar y para los folios; el naranja solo aparece cuando hay algo que mirar con atención (un descuento o un vencimiento).

### Tipografía

| Rol | Familia | Uso |
|---|---|---|
| Titulares | **IBM Plex Sans Condensed** SemiBold/Bold | Títulos de pantalla, nombres de cliente, cifras de portada |
| Cuerpo | **IBM Plex Sans** Regular/Medium/SemiBold | Formularios, descripciones, botones |
| Datos | **IBM Plex Mono** Regular/Medium/SemiBold | Folios, fechas, cantidades, dinero |

Las tres familias vienen de la misma superfamilia, lo que da coherencia técnica sin monotonía. La condensada aporta energía de rótulo; la monoespaciada hace que dos documentos se puedan comparar cifra a cifra. Las fuentes están incluidas en `app/src/main/res/font` con su licencia OFL.

Escala principal: 36/32/28/23 sp para titulares, 16 sp para cuerpo, 14 sp para controles, 11 sp con tracking para etiquetas de sección, y cifras en `tnum` para alinear importes.

### Estructura y jerarquía

- **Barra inferior** de cuatro secciones: Inicio, Cotizaciones, Pedidos, Productos. Ajustes se abre desde el avatar del responsable.
- **Encabezado editorial**: una etiqueta en versalitas (OPERACIÓN, CATÁLOGO, ACCESOS) y un título condensado. No hay una *top app bar* genérica; el contexto se lee como el encabezado de un documento.
- **Tarjetas de documento**: lomo de color por estado, folio en mono, cliente en semibold, total a la derecha. La forma permite escanear una lista sin abrir cada documento.
- **Formularios**: filas tipo hoja de especificaciones, etiquetas arriba, campos de 12 dp de radio, totales fijos en la parte inferior.
- **Detalle**: tarjeta-ticket con muescas laterales, líneas punteadas, sello de estado, total destacado y código de barras. Es el único lugar donde la metáfora del ticket se lleva al extremo.

```
INICIO
┌──────────────────────────────┐
│ TURNO · 02 NOV 2026     [AR] │
│ Buen día, Ana                │
│ ┌──────────────────────────┐ │
│ │ COTIZADO ESTE MES   MXN  │ │
│ │ $110,819.21              │ │
│ │ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │ │
│ │ 02 vigentes  00 por vencer│ │
│ │ 01 pedido                 │ │
│ │ ▮▮▯▮▮▮▯▮▮▯▮▮▯▮▮▯▮▮▮     │ │
│ └──────────────────────────┘ │
│ [ Cotizar ]    [ Pedido ]    │
│ ÚLTIMOS MOVIMIENTOS          │
│ ▌COT-2026-0004  Cliente      │
│ ▌PED-2026-0002  Distribuidora│
└──────────────────────────────┘
```

```
DETALLE / TICKET
┌──────────────────────────────┐
│  COT-2026-0001       EMITIDA │
│  Corporativo Andares         │
│  emisión 02 nov · vence 15 nov│
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │
│  2 × Laptop ThinkPad  $37,980│
│  12 × Monitor Dell    $39,330│
│    volumen 5%         −$2,070│
│ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ │
│  Subtotal            $81,960 │
│  Descuento global 3% −$2,396 │
│  IVA 16%             $12,398 │
│  TOTAL               $89,892 │
│  ▮▮▮▯▮▮▯▮▮▮▯▮▮▯▮▮▯▮▮▮▮▯   │
└──────────────────────────────┘
```

### Firma visual

El elemento memorable es el **documento-ticket**:

1. **Muescas** laterales dibujadas con el color del fondo (solo en el detalle y en documentos, no en el tablero).
2. **Código de barras determinístico** generado a partir del folio; no es una imagen decorativa, siempre representa el mismo documento.
3. **Sello de estado** con color y texto, nunca solo color.
4. **Líneas punteadas** que separan cliente, conceptos, totales y responsable.

En el PDF la misma firma se traduce a un encabezado con barcode, reglas finas y una tabla monoespaciada. El tablero se mantiene limpio para que el ticket no compita consigo mismo.

## 3. Pantallas

### Login

- Marca “QUOTELY / MOSTRADOR TÉCNICO”, titular grande y una tarjeta de acceso.
- Credenciales de prueba en dos atajos que rellenan el formulario.
- Mensajes de error en línea, sin modales.
- Código de barras y nota de almacenamiento local como cierre.

### Inicio

- Saludo con el nombre del responsable y fecha del turno.
- Panel tipo ticket con el total cotizado del mes, tres estadísticas y barcode.
- Dos acciones primarias: **Cotizar** y **Pedido**.
- Últimos seis movimientos con estado y total.

### Cotizaciones

- Búsqueda por folio/cliente y filtros: Todas, Borradores, Vigentes, Por vencer, Vencidas, Convertidas, Anuladas.
- Chips con scroll horizontal; tarjetas con lomo de color, vigencia y total.
- FAB “Nueva” siempre visible.

### Formulario de cotización

- Cliente y contacto.
- Vigencia con dos selectores de fecha en español.
- Productos con selector en bottom sheet, búsqueda, stepper y descuento por volumen visible por línea.
- Descuento global con tres estados: sin descuento, porcentaje, monto fijo.
- Resumen de subtotal, descuentos, IVA y total.
- Barra inferior fija con total y dos salidas: **Borrador** / **Emitir cotización**.

### Detalle de cotización

- Ticket con folio, cliente, contacto, fechas, conceptos, descuentos, IVA, total, notas y responsable.
- Acciones contextuales: editar borrador, emitir, convertir a pedido, anular, eliminar borrador, compartir PDF.
- Confirmaciones explícitas para convertir, anular y eliminar.
- Banner cuando está vencida, explicando que debe usarse un pedido directo.

### Pedidos

- Misma gramática de lista que cotizaciones, con etiqueta de origen: “Precios de la cotización conservados” o “Precio base del catálogo”.
- Filtros por estado y FAB “Nuevo”.
- El detalle muestra el folio de la cotización de origen y un banner con la regla de precios.
- Acciones de avance de estado con confirmación de cancelación.

### Pedido directo

- Aviso visible: precios base, sin descuentos de cotización, precios congelados al registrar.
- Mismo selector de productos, stepper y resumen sin descuentos.
- Botón “Registrar pedido” a ancho completo.

### Productos

- Búsqueda, código en cobalto, nombre, descripción, precio/unidad.
- Interruptor activo/inactivo y icono de edición; toda la tarjeta abre el formulario.
- Formulario con código, nombre, descripción, precio, unidad (chips) y activo.

### Responsables

- Solo para administradores.
- Tarjetas con avatar de iniciales, usuario, rol y marca “TÚ”.
- Formulario con nombre, usuario, contraseña opcional en edición, rol y acceso activo.
- El switch propio se muestra activo pero bloqueado (“No puedes desactivar tu propio acceso”).

### Ajustes

- Perfil y sesión local.
- Acceso a Responsables (solo admin).
- Reglas de cálculo visibles.
- Base de datos: ruta, explicación, exportación `.db`.
- Acerca de, versión y logout con confirmación.

## 4. Movimiento

No hay animaciones decorativas. Se usan las transiciones estándar de Navigation Compose y los cambios de estado de Material 3. La información importante (total, estado) se actualiza en el lugar; no se mueve por la pantalla. Esto mantiene la sensación de documento estable y evita que una app de trabajo se vuelva un espectáculo.

## 5. Accesibilidad y modo oscuro

- Contraste alto entre tinta y papel; el cobalto se usa con texto blanco.
- Los estados combinan color + texto (“Emitida”, “Vencida”, “Convertida”); no dependen solo del color.
- Controles principales de 46–52 dp de alto; iconos con `contentDescription` cuando son acciones.
- Formularios con etiquetas visibles, no solo placeholders.
- Modo oscuro con paleta propia: fondo azul muy oscuro, superficies elevadas y acentos aclarados.
- Selector de fecha y toda la interfaz en español mediante locale `es-MX`.

## 6. Iteraciones y autocrítica

- Se descartó la paleta crema + serif + terracota por ser el resultado por defecto de cualquier brief.
- Se quitaron las muescas del panel de Inicio: competían con las estadísticas y hacían parecer que la tarjeta estaba rota. Las muescas quedaron solo donde el objeto es un documento.
- Se separó el icono de edición del interruptor en Productos y se colocó a su izquierda, en la misma línea, porque la pila vertical se leía como un solo control.
- Se añadió la etiqueta de origen en pedidos para que la regla de negocio (precio conservado vs. precio base) se entienda sin abrir el detalle.
- Se forzó el locale español para que el DatePicker de Material 3 no mezclara inglés y español.
- Se prefirió un PDF limpio, con barcode en el encabezado, en lugar de saturar la página con adornos.

## 7. Evidencia

Capturas en [`docs/capturas`](capturas):

- `login.png`
- `inicio.png`
- `inicio-oscuro.png`
- `cotizaciones.png`
- `cotizacion-detalle.png`
- `cotizacion-form.png`
- `pedido-form.png`
- `pedido-detalle.png`
- `productos.png`
- `responsables.png`
- `ajustes.png`
- `date-picker.png`
