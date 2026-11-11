# ADR 010 — Gráficos con Chart.js, detrás de un envoltorio propio

**Estado:** aceptada · 2026-09-26

## Contexto
Los reportes necesitan líneas (ingresos, gastos y resultado por mes), barras (servicios más realizados) y un anillo (ingresos por categoría). Se evaluó dibujarlos a mano con SVG y CSS; se prefirió una librería por su variedad de gráficos, sus tooltips y su adaptación al tamaño.

## Decisión
- Se usa **Chart.js** directamente, sin `ng2-charts`. Solo se registran los componentes que se usan (barras, línea, anillo, ejes, leyenda y tooltip), para que el resto no entre al bundle.
- Toda la interacción con la librería está en `shared/components/chart`: `ChartFactory` (crea, actualiza y destruye un gráfico) y `ChartCanvas` (el componente `<app-chart>`). Las pantallas no importan Chart.js.
- Las **configuraciones** de cada gráfico son funciones puras (`features/reports/utils/chart-specs.ts`), que se prueban sin necesitar un canvas.
- En los tests se reemplaza `ChartFactory` por uno falso, porque el entorno de pruebas no tiene canvas real.
- El módulo de reportes se carga de forma diferida: Chart.js (unos 62 KB comprimidos) solo se descarga al entrar a Reportes.

## Accesibilidad
Un gráfico es una imagen para un lector de pantalla. Por eso cada gráfico lleva una descripción (`aria-label`) con lo principal que muestra, y **cada uno tiene sus mismos datos en una tabla o lista** (la evolución mensual, dentro de "Ver los datos en una tabla"). No se usa el color como único medio: el resultado positivo o negativo se explica con palabras, y los importes negativos llevan el signo.
En la evolución mensual, las tres líneas se distinguen además del color por la forma del punto (círculo, cuadrado y triángulo) y porque la del resultado va punteada.
Los gráficos respetan la preferencia del sistema de menos movimiento (sin animación).

## Consecuencias
- Cambiar de librería en el futuro toca solo `ChartFactory` y las funciones de `chart-specs.ts`.
- Los nombres largos se cortan en el eje del gráfico (con "…") y se ven completos en la tabla y en el tooltip.
