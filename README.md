<h1 align="center">LUKA APP — Plataforma SaaS de Gestión Financiera Inteligente</h1>

<p align="center">
  <img src="https://skillicons.dev/icons?i=java,spring,python,angular,docker,rabbitmq,github&theme=dark" alt="Tech Stack" />
</p>

<p align="center">
  <strong> Estado: En Desarrollo</strong>
</p>

---

## Índice

- [Descripción del Proyecto](#descripción-del-proyecto)
- [Características Principales](#características-principales)
- [Tecnologías Utilizadas](#tecnologías-utilizadas)
- [Arquitectura y Estructura](#arquitectura-y-estructura)
- [Configuración Local](#configuración-local)
- [Personas Desarrolladoras](#personas-desarrolladoras)
- [Licencia](#licencia)

---

## Descripción del Proyecto

**LUKA APP** es una solución integral de gestión financiera diseñada bajo una arquitectura de microservicios escalable. La plataforma no solo gestiona transacciones, sino que utiliza **Inteligencia Artificial (Google Gemini)** para actuar como un coach financiero proactivo, detectando patrones de gasto y brindando proyecciones personalizadas para la salud económica del usuario.

> Actualmente, el sistema cuenta con la integración core de Usuario, Gestión Financiera, Seguridad- Próximamente se integrarán módulos de facturación, reportes e integración con WhatsApp / IA .

---

## Características Principales

- **Coach Financiero IA:** Consejos personalizados basados en historial y perfil de riesgo.
- **Análisis de Gastos Hormiga:** Identificación automática de fugas de capital y categorías.
- **Arquitectura Resiliente:** Comunicación asíncrona mediante **RabbitMQ** para auditoría inmutable.
- **Gestión de Suscripciones (SaaS):** Diferenciación de servicios para roles `FREE`, `PREMIUM` y `ADMIN`.
- **Service Discovery:** Gobernanza dinámica de servicios mediante **Eureka-Netflix**.

---

## Tecnologías Utilizadas

### Backend (Polyglot)

- **Java 21 & Spring Boot 3.4.4** — Núcleo transaccional, seguridad (JWT) y orquestación.
- **Python 3.12 & FastAPI** — Procesamiento de IA, lógica de prompts y análisis predictivo.
- **RabbitMQ** — Message Broker para el desacoplamiento de eventos y auditoría.
- **PostgreSQL** — Almacenamiento relacional bajo el patrón *Database per Service*.
- **Google Gemini API** — Motor de IA Generativa para coaching personalizado.

### Frontend

- **Angular 17** — Framework para una interfaz de usuario SPA (Single Page Application).
- **Tailwindcss** — Librerías de componentes UI y diseño responsivo.

### DevOps & Herramientas

- **Docker & Docker Compose** — Contenerización y orquestación local del ecosistema.
- **Eureka Server** — Registro y descubrimiento de microservicios.
- **Spring Cloud Gateway** — Gateway centralizado para filtrado de seguridad y ruteo.
- **GitHub** — Controlador de versiones.

---

## Arquitectura y Estructura

**Arquitectura Poligrota:** Backend construido con Java (Spring Boot). El proyecto utiliza un patrón de microservicios con **Service Discovery (Eureka)** y **API Gateway**.

El proyecto se organiza como un **Monorepo** para facilitar la gestión del ciclo de vida del software:

```
/estructura-backend    → Microservicios Spring Boot
/estructura-frontend   → Cliente web Angular
```

---

**Clonación del repositorio**
```bash
git clone https://github.com/CristinaAstocaza/LUKA--APP-.git
```

## Personas Desarrolladoras

| Nombre | Rol | Perfil |
|---|---|---|
| Cristina Astocaza | Project Leader & Software Architect | [GitHub](https://github.com/CristinaAstocaza) |
| Anyelo Palomino | Frontend Lead | [GitHub](https://github.com/Angelo-PC1) |
| Casafranca Jousef | Frontend Lead | [GitHub](https://github.com/Jousef30) |
| Flor Caceres | Frontend Lead | [GitHub](https://github.com/flor23-caceres) |
