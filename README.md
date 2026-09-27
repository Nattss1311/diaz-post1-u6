# Post-contenido — Unidad 6: Antipatrones de Diseño

## Descripción
Repositorio del post-contenido de la Unidad 6 de Patrones de Diseño de Software — Sexto Semestre. Un único proyecto Spring Boot (`pedidos-service/`) con dos partes: diagnóstico y refactorización de un antipatrón combinado en `GestorPedidos`, y diagnóstico y corrección de un segundo antipatrón introducido al hacer crecer el mismo proyecto con tres campañas de descuento.

---

## Análisis y Decisiones de Diseño — Parte 1

### Análisis `GestorPedidos.java`

El análisis estático del archivo `GestorPedidos.java` revela múltiples violaciones a los principios fundamentales de diseño orientado a objetos (SOLID) y la presencia de antipatrones reconocidos.

#### 1. ¿Cuántas razones distintas tiene esta clase para cambiar? (Violación del Principio de Responsabilidad Única - SRP)

La clase `GestorPedidos` actúa como un **God Object (Objeto Todopoderoso)**, agrupando 6 responsabilidades independientes dentro de la misma clase y ejecución secuencial:

* **Validación de Inventario y Stock (Líneas 33–44):** Comprueba que la lista de ítems no sea nula y consulta individualmente el stock de cada producto en la base de datos.
* **Validación de Cliente y Reglas de Morosidad (Líneas 47–63):** Consulta la categoría del cliente, suma facturas no pagadas y aplica una regla de negocio sensible al horario del sistema para permitir o rechazar la transacción.
* **Cálculo Financiero y Precios (Líneas 66–93):** Consulta precios unitarios de productos, calcula el subtotal, evalúa la política de descuentos según el perfil de cliente y computa la tasa de impuestos (19%).
* **Persistencia de Datos SQL Directa (Líneas 96–111):** Inserta el registro del pedido en la tabla `pedidos`, ejecuta una llamada dialectal `CALL IDENTITY()` para extraer el ID, registra el detalle en `detalle_pedido` y actualiza físicamente las existencias en inventario.
* **Composición y Formateo de Mensajes (Líneas 114–122):** Concatena cadenas de texto para generar el asunto y el cuerpo del correo en formato plano.
* **Manejo e Integración con Servicios de Notificación (Líneas 123–128):** Intenta invocar el servicio de correo e intercepta excepciones generales de infraestructura (`catch (Exception e)`) para silenciar errores de envío.

**Efecto de la acumulación:**  
Un cambio en el porcentaje de impuesto (línea 92), una modificación en el esquema SQL (líneas 96–111) o un ajuste en la plantilla del correo (líneas 115–122) obligan a modificar exactamente el mismo archivo, aumentando exponencialmente la probabilidad de generar efectos secundarios (*side effects*) y regresiones.

#### 2. ¿Cuántos niveles de anidamiento condicional alcanza el cálculo de descuento y la validación de mora? (Spaghetti Code)

La estructura condicional del método muestra una alta complejidad ciclomática con bifurcaciones profundamente anidadas:

* **Bloque de Cálculo de Descuento (Líneas 74–90):** Alcanza 3 niveles de anidamiento condicional.
  * **Nivel 1:** Evaluación del tipo de cliente (`if (tipoCliente.equals("VIP"))` / `else if (tipoCliente.equals("FRECUENTE"))`).
  * **Nivel 2:** Evaluación de montos (`subtotal > 1_000_000`) o consultas a la base de datos sobre historial (`pedidosPrevios > 10`).
  * **Nivel 3:** Evaluaciones secundarias de rangos (`else if (subtotal > 500_000)` o `else if (pedidosPrevios > 3)`).
* **Bloque de Validación de Mora (Líneas 51–62):** Alcanza 3 niveles de anidamiento condicional.
  * **Nivel 1:** Comprobación de categoría morosa (`else if (tipoCliente.equals("MOROSO"))`).
  * **Nivel 2:** Verificación de saldo de deuda (`if (deudaPendiente != null && deudaPendiente > 0)`).
  * **Nivel 3:** Regla de horario límite (`if (ahora.isBefore(LocalTime.of(20, 0)))`).

#### 3. ¿En cuántos niveles de abstracción distintos opera el método al mismo tiempo? (Violación del principio SLAP)

El método `procesarPedido` viola el Principio de Un Solo Nivel de Abstracción (SLAP), ya que entremezcla operaciones de muy bajo nivel técnico con decisiones de negocio de alto nivel en una única secuencia lineal:

* **Bajo Nivel (Infraestructura / BD / E/S):**
  * Sentencias SQL en texto plano embebidas directamente mediante JDBC (`SELECT stock FROM inventario WHERE...` en la línea 39, `INSERT INTO pedidos...` en las líneas 96–99).
  * Gestión manual del dialecto SQL con `CALL IDENTITY()` (línea 101).
  * Construcción manual de texto mediante `StringBuilder` concatenando caracteres de formato `\n` (líneas 116–122).
* **Alto Nivel (Reglas de Dominio y Negocio):**
  * Flujo de decisión del pedido (confirmado vs. rechazado).
  * Política de exención de mora por horario nocturno (después de las 20:00).
  * Lógica comercial para la jerarquía de descuentos VIP y Frecuente.

#### 4. Impacto de Extensibilidad: ¿Qué ocurre al agregar un nuevo tipo de cliente? (Violación del Principio Abierto/Cerrado - OCP)

Si el negocio requiere incorporar una nueva categoría de cliente (por ejemplo, `CORPORATIVO` con un 12% de descuento fijo o un nuevo umbral):

* **Impacto Directo:** Es obligatorio abrir la clase `GestorPedidos.java` y modificar el bloque condicional ubicado entre las líneas 74 y 90.
* **Riesgo de Regresión:** Como las variables de subtotal, descuento, impuesto y total son compartidas en la misma secuencia de código (líneas 66–93), alterar las sentencias `if/else` existentes implica re-evaluar e inspeccionar todas las rutas condicionales de clientes VIP y FRECUENTE.
* **Ausencia de Polimorfismo:** No es posible extender el comportamiento añadiendo una nueva clase sin tocar el código fuente existente, lo cual evidencia una arquitectura rígida.

### Refactorización y Aplicación de Patrones — Parte 1

#### Validación de Pedidos: Chain of Responsibility (Cadena de Responsabilidad)
* **Patrón Elegido:** `Chain of Responsibility`.
* **Alternativa Considerada:** Método `validarTodo()` invocando una lista de `Predicate<ContextoPedido>`.
* **Justificación:** Se eligió `Chain of Responsibility` para la secuencia de validaciones debido a que existe una dependencia de orden real y la necesidad de un corte anticipado (*short-circuit*). Si `ValidadorStock` rechaza el pedido por falta de existencias, no tiene sentido consultar la base de datos para validar la morosidad en `ValidadorCliente`. La alternativa de usar predicados evaluaría todas las condiciones de manera innecesaria o requeriría lógica condicional explícita para detenerse. La cadena permite que cada eslabón decida si delega al siguiente o interrumpe el procesamiento.

#### Cálculo de Descuentos: Strategy + Simple Factory / Selector
* **Patrón Elegido:** `Strategy` encapsulado con `SelectorEstrategiaDescuento`.
* **Alternativa Considerada:** Agregar el cálculo de descuento como un eslabón adicional en la Chain of Responsibility.
* **Justificación:** A diferencia de las validaciones, los descuentos no dependen de una secuencia ni de la posibilidad de cortar el flujo; siempre se aplica exactamente una regla determinada por la categoría del cliente. Modelarlo como parte de la cadena habría introducido mecanismos artificiales para evitar que múltiples eslabones modifiquen el descuento. `Strategy` junto con un mapa de selección directa (`SelectorEstrategiaDescuento`) resuelve el problema con menor indirección, eliminando por completo los `if/else` anidados sin acoplar las estrategias entre sí.

---

## Análisis y Decisiones de Diseño — Parte 2: Antipatrón Golden Hammer

Al incorporar las tres nuevas campañas promocionales (`BLACK_FRIDAY`, `CORPORATIVO` y `VOLUMEN`), se incurrió en el antipatrón **Golden Hammer (Martillo de Oro)**.

### 1. Diagnóstico del Antipatrón (Golden Hammer)

**Definición y causa:**  
El antipatrón ocurre cuando se fuerza el uso de un patrón o herramienta familiar (`Chain of Responsibility`) para resolver un problema de naturaleza completamente diferente (aplicación de promociones/descuentos), simplemente porque "ya existía en el proyecto y funcionó en la etapa anterior", sin evaluar si era la solución conceptual y técnicamente adecuada.

### 2. Evidencia en el Código

* **Violación del contrato de abstracción:**  
  Las clases `PromocionBlackFriday`, `PromocionCorporativo` y `PromocionVolumen` heredan de `ValidadorPedido`. Sin embargo, ninguna de estas clases evalúa o interrumpe el flujo; nunca ejecutan un rechazo de pedido. Su único propósito es escribir un valor mutable en `contexto.aplicarDescuentoCampana(...)`.
* **Ausencia de necesidad de orden o corte anticipado (*short-circuit*):**  
  A diferencia de `ValidadorStock` y `ValidadorCliente` (donde si el stock es insuficiente no tiene sentido validar al cliente), las promociones son independientes entre sí. Ejecutar `PromocionVolumen` antes que `PromocionCorporativo` no altera en absoluto el resultado final.
* **Mutabilidad artificial del contexto:**  
  Se alteró la clase `ContextoPedido` agregando el campo `descuentoCampana` y la lógica `if (valor > this.descuentoCampana)`, convirtiendo la cadena de validación en una tubería de mutación de estado con acoplamiento implícito.
* **Incompatibilidad con nuevas reglas de combinación:**  
  Si el negocio decide cambiar la regla de "el mayor descuento gana" por "sumar los descuentos acumulables", la estructura basada en cadena exige reescribir la lógica mutable del contexto o agregar hacks en los eslabones, generando ambigüedad sobre qué eslabón tiene prioridad o cuál debe ejecutarse primero.

### 3. Decisión de Diseño y Refactorización (Solución)

#### Strategy en vez de más eslabones de cadena
Se corrigió modelando las tres campañas promocionales como implementaciones de `EstrategiaDescuento` y no como validadores dentro de la cadena existente. Al igual que `DescuentoVip` y `DescuentoFrecuente`, estas campañas calculan un porcentaje sin depender de un orden de evaluación estricto ni requerir un mecanismo para "cortar" el flujo de procesamiento del pedido (propiedad exclusiva de `ValidadorStock` y `ValidadorCliente`). La alternativa de mantenerlas en la cadena fue descartada por ser el origen directo del antipatrón **Golden Hammer**.

#### Eliminar, no comentar, el código descartado
Se eliminó cualquier clase o atributo temporal (`PromocionBlackFriday`, `PromocionCorporativo`, `PromocionVolumen` y el campo `descuentoCampana`) en lugar de conservarlos comentados como referencia histórica. Comentar código "por si se necesita después" es el mecanismo por el cual nace el antipatrón **Lava Flow**, generando confusión sobre si las líneas cumplen alguna función activa. El historial de commits de Git es el mecanismo correcto para mantener la trazabilidad del código previo.

---

## Cómo ejecutar

```bash
$mvn spring-boot:run$ mvn test
```
## Herramientas utilizadas
Java 17, Spring Boot, Spring JDBC, Maven, H2 Database

VS Code / IntelliJ IDEA, Git, GitHub

## Conclusiones
El análisis y refactorización de esta arquitectura permitió evidenciar cómo los antipatrones God Object y Golden Hammer degradan la mantenibilidad y cohesión del software. Comprender la naturaleza del problema fue clave para seleccionar el patrón correcto: Chain of Responsibility demostró ser ideal para flujos de validación secuenciales con corte anticipado, mientras que Strategy resultó óptimo para reglas de cálculo independientes y extensibles. Finalmente, eliminar por completo el código obsoleto en lugar de comentarlo previno la aparición del antipatrón Lava Flow, garantizando un código limpio, desacoplado y listo para evolucionar.