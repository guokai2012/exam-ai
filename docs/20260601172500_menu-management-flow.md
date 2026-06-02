# 菜单管理与扫描权限联动流程

## 功能目标
核心内置菜单结构由开发人员通过迁移脚本维护，后台菜单管理允许管理员新增、编辑和删除扩展菜单。`sys_menu` 不保存前端组件标识，前端路由组件由代码路由表绑定；`path = NULL` 表示分组菜单，分组菜单不能设置 `api_path`。

## 参与角色
- 管理员：新增扩展菜单，维护菜单展示字段、叶子菜单 `path`、权限码和 `api_path`，删除没有子节点的菜单。
- 开发人员：通过可重复迁移脚本维护菜单结构、页面 `path`、权限码、图标初始值、排序初始值和默认 `api_path`。
- 登录用户：根据自身扫描权限看到可访问菜单。
- 系统：按当前用户权限过滤启用菜单，并移除没有可见子菜单的分组。

## 主流程
1. 开发人员新增或调整前端页面时，同步提交菜单迁移脚本。
2. 迁移脚本维护 `sys_menu`，其中 `path = NULL` 表示分组菜单，叶子菜单配置非空页面 `path`。
3. 管理员进入 `/admin/menus`，前端调用 `GET /api/admin/menus` 查询完整菜单树。
4. 前端调用 `GET /api/admin/menus/api-path-options`，下拉选项来自 Controller API 根路径扫描，名称取 `@Tag(name)`。
5. 管理员可新建根菜单或子菜单；分组菜单不填写 `path`，叶子菜单填写页面 `path`、权限码并可选择 `api_path`。
6. 管理员可编辑菜单名称、图标、排序、状态；叶子菜单可从下拉中选择 `api_path`，分组菜单不能设置 `api_path`。
7. 系统启动、后台定时任务或管理员点击“扫描权限”时，从 Controller 注解全量同步权限。
8. 权限扫描以 Controller 类级 `@RequestMapping` 根路径反查菜单 `api_path`，命中后使用菜单名称作为权限分组名称；同一 `api_path` 绑定多个菜单时使用 `/` 合并菜单名称，未命中时回退到 `@Tag(name)`。
9. 菜单 `permission_code` 绑定扫描生成的动作权限码，例如 `admin:menu:list`、`document:list`。
10. 登录用户进入后台布局时，前端调用 `GET /api/menus/me`。
11. 后端根据用户权限过滤启用菜单，并移除没有可见子菜单的空分组。
12. 前端渲染左侧菜单；分组只展开不跳转，叶子菜单按 `path` 跳转，页面请求优先使用菜单 `api_path`。

## 异常流程
- 管理员给分组菜单提交 `api_path`：后端返回业务异常，前端保存失败。
- 管理员删除仍有子菜单的菜单：后端返回业务异常，要求先删除或迁移子菜单。
- 管理员新增重复页面 `path`：后端返回业务异常，避免同一路由出现多个叶子菜单。
- 菜单接口失败：前端使用最小兜底菜单和默认 API 路径，保证基础导航可用。
- 权限不足：对应叶子菜单不会出现在当前用户菜单树中；分组没有可见子菜单时一并隐藏。
- 菜单绑定了不存在或未授权的权限码：菜单不会对当前用户展示，需要通过迁移脚本修正权限码或在角色授权中补齐。

## Mermaid 业务流程图
```mermaid
flowchart TD
    A["开发人员提交页面代码"] --> B["同步菜单迁移脚本"]
    B --> C["维护 sys_menu 结构和默认 api_path"]
    D["管理员进入 /admin/menus"] --> E["GET /api/admin/menus"]
    D --> F["GET /api/admin/menus/api-path-options"]
    E --> G["新增/编辑菜单"]
    F --> H["选择叶子菜单 api_path"]
    G --> I["PUT /api/admin/menus/{id}"]
    H --> I
    I --> J["保存允许编辑字段"]
    K["权限扫描"] --> L["从 Controller 注解生成权限"]
    L --> M["角色授权绑定 ACTION 权限"]
    J --> N["用户进入后台布局"]
    N --> O["GET /api/menus/me"]
    O --> P["后端按权限码过滤菜单"]
    P --> Q["返回菜单树"]
    Q --> R["前端渲染左侧菜单并解析 api_path"]
```

## 前后端交互点
- 页面：`/admin/menus`、`/admin/permissions`、后台布局左侧菜单。
- 接口：`GET /api/menus/me`、`GET /api/admin/menus`、`POST /api/admin/menus`、`PUT /api/admin/menus/{id}`、`DELETE /api/admin/menus/{id}`、`GET /api/admin/menus/api-path-options`、`POST /api/admin/permissions/scan`。
- 权限关系：菜单只消费扫描权限，不生产权限；`sys_permission` 的数据来源是 Controller 扫描。
- 分组命名：权限扫描用 Controller 根路径匹配菜单 `api_path`，菜单存在时权限分组名称跟随菜单名称，菜单缺失时使用 Controller `@Tag(name)`。
- 数据关系：`path = NULL` 是分组菜单判定规则；叶子菜单通过 `api_path` 绑定页面主资源 API 根路径。
