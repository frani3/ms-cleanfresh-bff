# ms-cleanfresh-bff

Backend-for-Frontend de Clean&Fresh Manager. Spring Boot 4.1.1 / Java
21. Valida el JWT (idToken) emitido por Azure Entra External ID (CIAM)
en cada request, aplica autorización por rol, y republica los datos de
`ms-cleanfresh-orders` y `ms-cleanfresh-catalog` con un contrato propio
hacia el frontend.

Proyecto individual para **EP1** de **DSY1107 Cloud Native 1** (DuocUC).

## Qué hace

- Valida issuer, audience, firma y vigencia del JWT (auto-config nativa
  de Spring Security OAuth2 Resource Server — sin código manual).
- Extrae roles del claim `roles` del token y los mapea a authorities
  `ROLE_*`.
- Autoriza cada endpoint por rol con `@PreAuthorize`.
- Reenvía las peticiones (GET, y un POST de creación de órdenes) a los
  dos microservicios vía `RestClient`.

## Endpoints

| Método | Ruta | Roles | Descripción |
|---|---|---|---|
| GET | `/actuator/health` | público | Health check de infraestructura |
| GET | `/api/health` | autenticado | Health check + info del JWT (usado por Admin) |
| GET | `/api/catalog` | Admin, Operador, Cliente | Catálogo completo |
| GET | `/api/catalog/{id}` | Admin, Operador, Cliente | Un servicio |
| GET | `/api/catalog/disponibles` | Admin, Operador, Cliente | Servicios disponibles |
| GET | `/api/orders` | Admin, Operador, Cliente | Órdenes (Operador se filtra por sucursal) |
| GET | `/api/orders/{id}` | Admin, Operador, Cliente | Una orden |
| GET | `/api/orders/estado/{estado}` | Admin, Operador | Órdenes por estado |
| POST | `/api/orders` | Admin, Cliente | Crea una orden (el `cliente` se resuelve del JWT, no del body) |

## Requisitos

- Java 21 (`JAVA_HOME` apuntando a un JDK 21)
- `ms-cleanfresh-orders` corriendo en `:8081`
- `ms-cleanfresh-catalog` corriendo en `:8082`

## Configuración

`src/main/resources/application.yaml` ya trae el `issuer-uri` y
`audiences` del tenant CIAM del proyecto. Antes de levantar, exportar:

```powershell
$env:AZURE_TENANT_ID = "<tenant id>"
$env:AZURE_API_CLIENT_ID = "<client id de la app registration del API>"
```

Opcional, si los microservicios no corren en los puertos por defecto:

```powershell
$env:ORDERS_SERVICE_URL = "http://localhost:8081"
$env:CATALOG_SERVICE_URL = "http://localhost:8082"
```

## Levantar en local

```powershell
.\mvnw.cmd spring-boot:run
```

O compilar y correr el jar:

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\ms-cleanfresh-bff-0.0.1-SNAPSHOT.jar
```

Corre en `http://localhost:8080`.

## Probar sin el frontend

```bash
# Sin token -> 401
curl -i http://localhost:8080/api/orders

# Token invalido -> 401
curl -i http://localhost:8080/api/orders -H "Authorization: Bearer invalido"

# Publico, sin auth
curl -i http://localhost:8080/actuator/health
```

## Arquitectura y decisiones técnicas

Ver [`CLAUDE.md`](CLAUDE.md) para el detalle completo del sistema (los
4 repos, configuración de Azure, y la pauta de evaluación de EP1).
