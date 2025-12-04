# 本地依赖 JAR 文件

## Residence-6.0.1.0.jar

Residence 是一个领地保护插件，本项目集成它来实现宗门领地系统。

### 如何获取

1. **从 Spigot 插件页面下载**
   - 访问: https://www.spigotmc.org/resources/residence.3380/
   - 下载 Residence 6.0.1.0 版本
   - 将 JAR 文件放在此目录

2. **或从编译的 Residence 项目获取**
   - 如果你有本地的 Residence 源代码
   - 运行 `mvn clean package -DskipTests`
   - 从 `target/` 文件夹复制编译后的 JAR

3. **或从你的服务器插件文件夹复制**
   - 如果你已经在运行的服务器中有 Residence 插件
   - 直接从 plugins 文件夹复制 `Residence.jar`
   - 重命名为 `Residence-6.0.1.0.jar`

### 文件名要求

必须命名为 `Residence-6.0.1.0.jar`，这样 Maven 才能找到它。

### 完成后

放置好 JAR 文件后，运行：
```bash
mvn clean compile
```

应该能够成功编译项目。
