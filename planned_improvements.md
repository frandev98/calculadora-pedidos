# 🚀 PROYECTO: Fuxion Business Assistant (Roadmap & Especificación)

**Versión del Documento:** 1.0
**Estado:** En Desarrollo (Fase 1 Completada)
**Visión:** Transformar la "Calculadora de Pedidos" en un "Asistente de Negocio Inteligente" que guíe al socio Fuxion día a día, entendiendo el contexto temporal (Periodos/Semanas) y sus metas específicas (Pro1 645 vs Base 540).

---

## 🗺️ RESUMEN EJECUTIVO: El Gran Plan (3 Fases)

Para evitar la "sobre-ingeniería" y mantener el desarrollo ágil, el proyecto se divide en 3 Sprints o Fases evolutivas.

| Fase | Nombre Clave | Objetivo Principal | Estado |
| :--- | :--- | :--- | :--- |
| **1** | **El Cerebro Matemático** | Algoritmo de distribución robusto (Francotirador/Constructor). | ✅ **COMPLETADO** |
| **2** | **El Contexto Temporal** | Implementar el calendario Fuxion (Periodos y Semanas) y el Dashboard. | 🔄 **SIGUIENTE PASO** |
| **3** | **Modo Táctico Semanal** | Calcular pedidos pequeños para la semana actual y Persistencia de datos. | 📅 PLANIFICADO |

---

## ✅ FASE 1: El Núcleo Lógico (COMPLETADO)

El motor de cálculo ya es funcional y estable.

### Características Implementadas

1. **Algoritmo Híbrido (Monte Carlo):** 20,000 iteraciones para encontrar la mejor combinación.
2. **Estrategia Dual:**
    * **Modo Bajo (< 645 pts):** Prioridad "Francotirador". Busca llenar P1, P2, P3 a **60 pts** exactos.
    * **Modo Alto (>= 645 pts):** Prioridad "Constructor". Busca llenar S1, S2, S3 a **180 pts**.
3. **Manejo de Excepciones:** Tolerancia a desbordes controlados (ej: llegar a 68 pts si no hay productos de 10).

---

## 🔄 FASE 2: El Contexto Temporal (CONTEXTO & CALENDARIO)

**Objetivo:** Que la aplicación deje de ser "ciega" al tiempo y sepa en qué momento del ciclo de negocio se encuentra el usuario.

### 1. Motor de Calendario (Lógica Matemática)

No usaremos servidores externos (evitar sobre-ingeniería). Usaremos una **Fecha Ancla**.

* **Configuración Inicial (Onboarding):**
  * El usuario ingresa **una sola vez**: *"Fecha de Inicio del Periodo 1 del Año Actual"*.
* **Cálculo Automático:**
  * `Días Transcurridos = (Hoy - FechaAncla)`
  * `Semana Actual = (Días Transcurridos / 7) % 4 + 1`
  * `Periodo Actual = (Semanas Transcurridas / 4) + 1`

### 2. Configuración de Meta por Periodo

El usuario define su estrategia al inicio de cada periodo, no cada vez que calcula.

* **Selector:** *"Para el Periodo X, ¿cuál es tu meta?"*
  * 🔘 **Meta Base (540 pts):** El sistema asumirá objetivos semanales de **120 pts** y bonos de **60 pts**.
  * 🔘 **Meta PRO (645 pts):** El sistema asumirá objetivos semanales de **180 pts**.

### 3. El Dashboard Inteligente (Rediseño Completo)

**Estructura Jerárquica:**

1. **Vista Anual (Home):**
    * Lista de los 13 Periodos del año.
    * Indicador visual del **Periodo Actual** (Color/Highlight).
    * Estado de cada periodo (Futuro, En Curso, Pasado).
2. **Vista de Periodo (Detalle):**
    * Al tocar un periodo, se abre su detalle.
    * Muestra las 4 Semanas con sus fechas.
    * **Selector de Meta:** "Este periodo voy por 540 o 645 pts" (Editable siempre).
    * Progreso acumulado de puntos.
3. **Vista de Semana (Calculadora):**
    * Al tocar una semana, se abre la calculadora/lista de productos.
    * Si es futura: Permite planificar.
    * Si es pasada: Muestra lo registrado (historial).

### 4. Sistema de Alertas

- Notificaciones locales no invasivas.
* "Nueva Semana iniciada".
* "Cierre de Periodo en 2 días".

---

## 📅 FASE 3: Modo Táctico y Memoria (PERSISTENCIA)

**Objetivo:** Permitir el trabajo "hormiga" (semana a semana) y guardar el historial.

### 1. Funcionalidad: "Cálculo Solo Esta Semana"

Opción para usuarios que no compran todo el paquete de 540 pts de golpe.

* **Input:** El usuario selecciona productos solo para *ahora*.
* **Lógica:**
  * El algoritmo reutiliza `DistributionCalculator` pero se limita a llenar **1 sola cubeta** (la semana actual).
  * El `Target` (Objetivo) lo toma del Dashboard (120 o 180).
  * **Resultado:** Muestra solo la lista de compras para hoy.

### 2. Base de Datos Local (Room)

Implementación de persistencia para guardar el historial.

* **Entidades (Tablas):**
  * `Periodo`: (ID, Numero, Meta_Elegida, Estado).
  * `Pedido`: (ID, Periodo_ID, Semana, Lista_Productos, Puntos_Totales).
* **Historial Visual:**
  * Línea de tiempo donde el usuario puede ver qué pidió en el Periodo 1 o 2.
  * Barra de progreso real acumulada (Sumando los pedidos guardados vs la Meta del Periodo).

---

## 🛡️ CRITERIOS DE "NO SOBRE-INGENIERÍA"

Reglas estrictas para mantener el proyecto viable y ágil:

1. **Cero Backend:** Toda la lógica vive en el celular. No hay servidores, ni login, ni bases de datos en la nube.
2. **Fechas Locales:** El calendario se calcula matemáticamente desde una fecha local. No se consulta una API de Fuxion.
3. **Validación Manual:** Si Fuxion cambia las fechas de cierre por un feriado, el usuario puede ajustar manualmente la fecha de cierre en la app (más fácil que programar excepciones complejas).

---

## 📝 ORDEN DE EJECUCIÓN TÉCNICA (Sprint Actual)

Para avanzar ordenadamente, ejecutaremos la **Fase 2** en este orden:

1. Crear clase `FuxionCalendarLogic` (Kotlin).
2. Diseñar pantalla de `OnboardingScreen` (Solo primera vez: Pedir fecha inicio Periodo 1).
3. Guardar esa fecha en `DataStore` (Preferencias locales).
4. Rediseñar `HomeScreen` para mostrar el Dashboard con la info calculada.
