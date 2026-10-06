# ms-cleanfresh-bff

Backend-for-Frontend de Clean&Fresh Manager. Spring Boot 4.1.1 / Java
21. Valida el JWT (access token) emitido por AWS Cognito en cada request,
aplica autorización por rol y republica los datos de los microservicios con
un contrato propio hacia el frontend. Los microservicios confían totalmente
en él: es el único punto que autentica y autoriza.

Proyecto individual de **DSY1107 Cloud Native 1** (DuocUC).

## Qué hace

- Valida issuer, firma y vigencia del JWT (Spring Security OAuth2 Resource
  Server) y, con `CognitoTokenValidator`, que sea un access token
  (`token_use`), emitido para esta app (`client_id`) y con el scope
  `https://api.cleanfresh.com/access_as_user`.
- Extrae los roles del claim `cognito:groups` y los mapea a authorities
  `ROLE_*`.
- Autoriza cada endpoint por rol con `@PreAuthorize`.
- Reenvía las peticiones a los microservicios con `RestClient`.

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
| GET | `/api/reportes` | Admin | Reporte por sucursal (hoy respuesta fija, EP2) |
| GET | `/api/auditoria` | Admin | Registro de auditoría (hoy respuesta fija, EP2) |

`ms-cleanfresh-notificaciones` no tiene ruta aquí: solo recibe mensajes de SQS.

## Requisitos

- Java 21 (`JAVA_HOME` apuntando a un JDK 21)
- Un User Pool de Cognito alcanzable: al arrancar consulta la configuración
  del emisor, así que sin red o con un `COGNITO_ISSUER_URI` inválido no inicia.
- Los microservicios que se vayan a usar: `orders` (:8081), `catalog` (:8082),
  `reportes` (:8084), `auditoria` (:8085).

## Configuración (variables de entorno)

```powershell
$env:COGNITO_ISSUER_URI = "https://cognito-idp.<region>.amazonaws.com/<user-pool-id>"
$env:COGNITO_CLIENT_ID  = "<client id del App Client>"
```

Opcionales:

```powershell
$env:COGNITO_REQUIRED_SCOPE = "https://api.cleanfresh.com/access_as_user"   # valor por defecto
$env:ORDERS_SERVICE_URL     = "http://localhost:8081"
$env:CATALOG_SERVICE_URL    = "http://localhost:8082"
$env:REPORTES_SERVICE_URL   = "http://localhost:8084"
$env:AUDITORIA_SERVICE_URL  = "http://localhost:8085"
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

## Tests

`.\mvnw.cmd test` no necesita red ni Cognito: reemplaza el `JwtDecoder` por un
mock y usa tokens simulados para comprobar la autorización por rol (por
ejemplo, `/api/reportes` responde 401 sin token, 403 a Operador y Cliente, y
200 a Admin).

## Arquitectura y decisiones técnicas

Ver [`CLAUDE.md`](CLAUDE.md) para el contexto del sistema y, en el repo del
frontend, `EP2/ARQUITECTURA.md` para la arquitectura objetivo de la entrega 2.
