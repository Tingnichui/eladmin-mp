# Repository Guidelines

## 项目结构与模块组织

本仓库是前后端分离的 ELADMIN 应用。`eladmin/` 是 Maven 多模块 Spring Boot 后端，主要模块包括：`eladmin-system` 作为启动入口，`eladmin-common` 存放通用工具与配置，`eladmin-logging` 负责日志与审计，`eladmin-tools` 存放第三方集成能力，`eladmin-generator` 提供 CRUD 代码生成。业务扩展模块包括 `eladmin-gym`、`eladmin-invest`、`eladmin-media-crawler` 和 `eladmin-other`。

后端各模块采用标准 Maven 目录：`src/main/java`、`src/main/resources`、`src/test/java`。`eladmin-web/` 是 Vue 2 管理端，源码位于 `src/`，常用目录有 `api/`、`views/`、`components/`、`router/`、`store/`、`utils/`、`assets/`。数据库初始化脚本位于 `sql/`，包括 `eladmin.sql` 和 `quartz.sql`。

## 构建、测试与本地开发命令

- `cd eladmin && mvn clean package`：构建所有后端模块；当前 Surefire 配置默认跳过测试。
- `cd eladmin && mvn -DskipTests=false test`：需要时运行后端测试。
- `cd eladmin-web && npm install`：按 `package-lock.json` 安装前端依赖。
- `cd eladmin-web && npm run dev`：启动 Vue 开发服务，地址通常为 `localhost:8013`。
- `cd eladmin-web && npm run build:prod`：生成生产构建到 `dist/`。
- `cd eladmin-web && npm run lint`：检查 `src/**/*.js` 与 `src/**/*.vue`。
- `cd eladmin-web && npm run test:unit`：清理 Jest 缓存并运行单元测试。

## 编码风格与命名约定

Java 代码目标版本为 Java 8，并沿用现有 `me.zhengjie` 包结构。Spring 组件按功能模块组织，通用能力放入 `eladmin-common`。Vue/JavaScript 使用 ESLint：2 空格缩进、单引号、无分号、Vue 组件名使用 PascalCase，变量尽量使用 `camelCase`。提交前端代码前运行 `npm run lint`；Husky/lint-staged 会自动修复已暂存的 JS/Vue 文件。

## 测试规范

后端测试使用 Spring Boot/JUnit，放在各模块 `src/test/java` 下，命名示例：`StringUtilsTest`、`EladminSystemApplicationTests`。修改哪个模块，就优先在该模块补充聚焦测试。前端测试使用 Jest 与 Vue Test Utils，新增测试建议使用 `*.spec.js` 命名。涉及数据库变更时，说明所需 SQL，并核对 `sql/` 下的脚本。

## 提交与 Pull Request 规范

近期提交信息较短，常用中文描述。提交信息应简洁、面向动作；必要时带上模块名，例如 `eladmin-web: 修复订单筛选` 或 `eladmin: 更新爬虫任务`。PR 应包含变更摘要、关联 issue 或任务、已运行的测试/构建命令、数据库迁移说明；涉及界面变更时附截图或录屏。

## 安全与配置提示

不要提交密钥、真实凭据、生成日志或本地 IDE 元数据。环境差异配置应保留在本地 profile 中。导入 SQL 时按 README 提示使用 UTF-8/UTF8MB4 编码。

## 新会话检查清单

每个新聊天开始处理代码任务前，先确认以下本机开发环境路径是否存在：

- 快速查看 `docs/README.md`，了解 `docs/` 下有哪些文档分类；只需确认目录用途，不需要细读每份文档。
- Maven 安装目录：`C:\MyProgram\develop\Maven\apache-maven-3.5.4`
- Maven 配置文件：`C:\MyProgram\develop\Maven\apache-maven-3.5.4\conf\settings.xml`
- Maven 本地仓库：`C:\MyProgram\develop\Maven\apache-maven-3.5.4\repo`
- Java 8 安装目录：`C:\MyProgram\develop\Java\java1.8_8u361`

如需构建或测试后端，优先使用上述 Maven 与 Java 路径。若任一路径不存在，必须先说明缺失项并询问用户如何处理；等待用户回复后，再继续执行构建、测试或代码修改。

启动 Java 后端项目前，必须向用户确认 `JASYPT_ENCRYPTOR_PASSWORD` 的值；这是后端配置解密密钥，未确认前不要启动后端服务。

所有文件读写、终端命令、构建测试和数据库导入导出都必须使用 UTF-8 字符集。若工具支持显式编码参数，优先指定 UTF-8。
