# 🌐 get_set (Next.js) — Interacción con Contrato en Stellar / Soroban

Este proyecto es un **frontend desarrollado con Next.js** que demuestra cómo **conectarse a una wallet**, **leer (get)** y **actualizar (set)** datos en un contrato desplegado en la red **Stellar / Soroban**.

---

## 📍 Ubicación en el repositorio

`/web/get_set`

---

## 🎯 Objetivo

Este ejemplo tiene como propósito mostrar de forma sencilla:

- Cómo conectar una **wallet compatible** con Soroban (por ejemplo, **Freighter** 🦊).
- Cómo ejecutar operaciones **get** y **set** sobre un contrato.
- Cómo visualizar los resultados y manejar el flujo completo desde la interfaz web.

---

## ⚙️ Requisitos previos

Asegúrate de tener instalados los siguientes componentes:

- **Node.js** v16 o superior
- **npm** o **yarn**
- Una wallet compatible con **Stellar / Soroban** (por ejemplo **Freighter**)
- Variables de entorno configuradas en un archivo `.env.local`:

```bash
NEXT_PUBLIC_NETWORK=testnet
NEXT_PUBLIC_CONTRACT_ID=<ID_DEL_CONTRATO>
NEXT_PUBLIC_WALLET_ENDPOINT=<URL_WALLET>
```

---

## 🚀 Instalación y ejecución

1. **Clona el repositorio** y entra en la carpeta del proyecto:

```bash
git clone https://github.com/mdeonchain/20251108IntroStellarBootcamp2026CapIV.git
cd 20251108IntroStellarBootcamp2026CapIV/web/get_set
```

2. **Instala las dependencias:**

```bash
npm install
# o
yarn install
```

3. **Ejecuta el proyecto en modo desarrollo:**

```bash
npm run dev
# o
yarn dev
```

4. Abre tu navegador en 👉 [http://localhost:3000](http://localhost:3000)

---

## 🧩 Estructura del proyecto

| Carpeta / Archivo | Descripción |
|--------------------|-------------|
| `pages/index.tsx` | Página principal con conexión a la wallet y botones get/set |
| `components/WalletConnector.tsx` | Componente que maneja la conexión con la wallet |
| `lib/contract.ts` | Funciones para interactuar con el contrato (get/set) |
| `styles/` | Estilos del proyecto |
| `public/` | Recursos estáticos (logos, íconos, etc.) |
| `.env.local` | Variables de entorno (no subir a git) |

---

## 🔁 Flujo de interacción

1. El usuario abre la app y conecta su **wallet Freighter**.
2. La app ejecuta la función `getValue()` del contrato para mostrar el valor actual.
3. El usuario ingresa un nuevo valor y presiona **Set Value**.
4. La transacción se firma y envía desde la wallet.
5. Al confirmarse, la app llama nuevamente a `getValue()` para actualizar el valor mostrado.

---

## 🧠 Buenas prácticas

- 🔒 **Nunca** incluyas claves privadas en el código ni en variables de entorno públicas.
- 🧪 Usa **testnet** para pruebas antes de pasar a mainnet.
- 💡 Implementa manejo de estados: desconectado, cargando, éxito y error.
- 🛠 Asegúrate de que el contrato en `NEXT_PUBLIC_CONTRACT_ID` esté desplegado en la misma red que la wallet.

---

## 🧩 Problemas comunes

| Problema | Causa probable | Solución |
|-----------|----------------|-----------|
| La wallet no se conecta | No instalada o en red diferente | Instalar y cambiar a testnet |
| Error al hacer set | Contrato mal configurado o fondos insuficientes | Verificar ID del contrato y cuenta |
| No se actualiza el valor | Cache UI o no se reejecuta get | Forzar actualización tras transacción |

---

## 📜 Licencia

Este ejemplo se distribuye bajo la misma licencia del repositorio principal.

---

✨ *Proyecto educativo del Intro Stellar Bootcamp 2026 — Capítulo IV.* 🚀
