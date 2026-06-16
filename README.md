# 商品秒杀系统
---

## 一、环境准备

### 1.1 确认 JDK 版本

打开终端，运行：

```powershell
java -version
```

看到以下内容（版本号 ≥ 17 即可）：

```
java version "21.0.7" 2025-04-15 LTS
```

### 1.2 确认 Docker 已安装并运行

```powershell
docker ps
```

看到如下表格（可能为空，但不能报错）：

```
CONTAINER ID   IMAGE     COMMAND   CREATED   STATUS    PORTS     NAMES
```

### 1.3 下载项目

```powershell
git clone <仓库地址>
cd seckill-demo
```

---

## 二、启动项目

### Step 1 — 启动中间件（Redis + RocketMQ）

```powershell
docker compose up -d
```

> 首次需要下载镜像，约 2-5 分钟

验证：

```powershell
docker compose ps
```

看到 4 个容器状态均为 `Up` 即为成功：

```
NAME                  STATUS
seckill-mysql         Up xx seconds
seckill-redis         Up xx seconds
seckill-rmq-namesrv   Up xx seconds
seckill-rmq-broker    Up xx seconds
```

---

### Step 2 — 编译项目

```powershell
# Windows
.\mvnw.cmd clean package -DskipTests

# Mac / Linux
./mvnw clean package -DskipTests
```

> 首次需要下载依赖，约 2-3 分钟。

看到 `BUILD SUCCESS` 即为成功。

---

### Step 3 — 启动应用

```powershell
java -jar target\seckill-demo-1.0.0.jar
```

看到以下输出说明启动成功：

```
Started SeckillApplication
库存预热完成，共加载3个商品
```

> 保持此窗口打开，不要关闭。下面测试在另一个终端窗口操作。

---

## 三、功能验证

> 以下命令在**另一个终端窗口**中执行。

### 3.1 商品列表（验证：MVC、SQL 查询、AOP）

浏览器直接打开，或终端执行：

```powershell
curl http://localhost:8080/api/products
```

预期输出（3 个商品）：

```json
{"records":[{"id":1,"name":"iPhone 15 Pro","stock":100,"price":8999.00}, ...]}
```

同时观察**应用终端日志**，应出现 AOP 输出：

```
请求开始: method=ProductController.list(..), args=[1, 10]
请求结束: method=ProductController.list(..), 耗时=30ms
```

---

### 3.2 商品详情 + 缓存（验证：Redis 缓存、依赖注入）

```powershell
curl http://localhost:8080/api/products/1
```

预期输出：

```json
{"id":1,"name":"iPhone 15 Pro","stock":100,"price":8999.00,...}
```

首次访问会查数据库并写入 Redis 缓存，再次访问直接从 Redis 读取。

---

### 3.3 查询 Redis 库存（验证：库存预热、Redis 读写）

```powershell
curl -H "token: test123" http://localhost:8080/api/seckill/stock/1
```

预期输出：

```
100
```

---

### 3.4 秒杀下单（验证：分布式锁、Lua 脚本、RocketMQ）

```powershell
curl -X POST http://localhost:8080/api/seckill/1 -H "token: test123" -H "X-User-Id: 12345"
```

预期输出：

```
秒杀成功，订单处理中
```

应用日志应出现：

```
秒杀成功: productId=1, userId=12345
订单消息发送成功: productId=1, userId=12345
```

---

### 3.5 秒杀后库存变化（验证：Lua 原子扣库存生效）

```powershell
curl -H "token: test123" http://localhost:8080/api/seckill/stock/1
```

预期输出：

```
99
```

> 从上一步的 100 变为 99，说明 Lua 脚本正确扣减了库存。

---

### 3.6 拦截器鉴权（验证：拦截器 HandlerInterceptor）

不带 token 访问：

```powershell
curl http://localhost:8080/api/seckill/stock/1
```

预期输出：

```json
{"code":401,"message":"未登录或token已过期"}
```

应用日志应出现：

```
WARN  AuthInterceptor: 请求缺少token: uri=/api/seckill/stock/1
```

---

### 3.7 重复下单防护（验证：Redisson 分布式锁 + Redis Set 防重）

再次用同一用户 ID 秒杀同一商品：

```powershell
curl -X POST http://localhost:8080/api/seckill/1 -H "token: test123" -H "X-User-Id: 12345"
```

预期输出：

```
您已经参与过该商品的秒杀，请勿重复下单
```

---

### 3.8 查看数据库

浏览器打开：`http://localhost:8080/h2-console`

| 参数 | 值 |
|------|-----|
| JDBC URL | `jdbc:h2:mem:seckill` |
| 用户名 | `sa` |
| 密码 | （留空） |

点击 Connect 后可以查看 `PRODUCT` 和 `SECKILL_ORDER` 表中的数据。

---

## 四、验证清单

| # | 验证项 | 预期结果 | 涉及知识点 |
|---|--------|---------|-----------|
| 1 | `GET /api/products` | 返回 3 个商品，日志有 AOP 输出 | MVC、SQL、AOP |
| 2 | `GET /api/products/1` | 返回商品详情，走 Redis 缓存 | Redis 缓存、DI |
| 3 | `GET /api/seckill/stock/1`（带 token） | 返回 100 | 库存预热、Redis 读 |
| 4 | `POST /api/seckill/1`（带 token + User-Id） | 返回"秒杀成功" | 分布式锁、Lua 脚本、RocketMQ |
| 5 | 再次查询库存 | 99（扣减了 1） | Lua 原子操作 |
| 6 | 不带 token 访问秒杀接口 | 返回 401 + 拦截器日志 | HandlerInterceptor |
| 7 | 同一用户重复秒杀 | 返回"请勿重复下单" | Redisson 分布式锁 |
| 8 | RocketMQ 消费者 | 日志中出现"订单消息发送成功" | RocketMQ 生产/消费、@Transactional |

---

## 常见问题

| 问题 | 解决办法 |
|------|---------|
| `javac` 找不到 | 确认 JDK >= 17，运行 `java -version` |
| Redis 连接失败 | 运行 `docker compose up -d`，确认 4 个容器都是 Up |
| RocketMQ 发送失败 | 启动后等待 30 秒，等 broker 完全就绪再测 |
| 端口 8080 被占用 | 修改 `application.yml` 中的 `server.port` |
| 想重置数据 | 重启应用即可（H2 是内存数据库） |

---

## 项目结构速览

```
controller/    → 接收 HTTP 请求（MVC）
service/       → 业务逻辑（秒杀核心、事务）
mapper/        → 数据库访问（MyBatis-Plus）
entity/        → 实体类（Product、SeckillOrder）
interceptor/   → Token 鉴权（HandlerInterceptor）
aspect/        → 接口日志（AOP @Around）
mq/            → 消息队列（RocketMQ 生产/消费）
config/        → 配置类（Redis、Redisson、拦截器注册）
runner/        → 启动预热（CommandLineRunner）
resources/lua/ → Redis Lua 脚本（原子扣库存）
```
