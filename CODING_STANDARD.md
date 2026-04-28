# 办公室绿萝 Pro - 代码规范

## 一、核心原则

> ⚠️ **每行代码都必须有注释 - 硬性要求**

所有业务代码必须包含完整的中文注释，确保：
- 代码可读性
- 知识可传承
- 便于未来维护

---

## 二、注释规范

### 1. 类/接口注释

```java
/**
 * 叶片观测记录实体类
 * 
 * 负责存储每次叶片追踪的观测数据
 * 包括面积、颜色、位置等核心指标
 * 
 * @author 狼群团队
 * @version 3.0.0
 * @since 2026-04-28
 */
public class LeafObservation {
    // ...
}
```

### 2. 方法注释

```java
/**
 * 根据叶片ID获取其所有观测记录
 * 
 * 按观测时间倒序排列，保证最新的记录在前
 * 用于叶片详情页的历史数据展示
 * 
 * @param leafId 叶片唯一标识，如 "L0001"
 * @return 观测记录列表，按时间倒序
 */
public List<LeafObservation> getObservationsByLeafId(String leafId) {
    // ...
}
```

### 3. 变量注释

```java
// 绿色HSV范围下限 - H值35开始为绿色
private static final int LOWER_GREEN_H = 35;

// 绿色HSV范围上限 - H值85以内都是绿色
private static final int UPPER_GREEN_H = 85;

// 黄叶判定阈值 - H值超过65认为偏黄
private static final int YELLOW_LEAF_H_THRESHOLD = 65;
```

### 4. 关键逻辑注释

```java
// 计算叶片匹配分数：位置35% + 颜色30% + 面积15% + 轮廓20%
double positionScore = calculatePositionScore(current, previous);
double colorScore = calculateColorScore(current, previous);
double areaScore = calculateAreaScore(current, previous);
double contourScore = calculateContourScore(current, previous);

// 综合得分低于0.3认为不匹配（可能是新叶片或聚集叶片分离）
double totalScore = positionScore * 0.35 + colorScore * 0.30 
                  + areaScore * 0.15 + contourScore * 0.20;
if (totalScore < 0.3) {
    // 不匹配，标记为待确认
}
```

### 5. 代码块注释

```java
// ========== 叶片健康状态判断 ==========

// 1. 先检查是否是黄叶（H值超过65）
if (hValue > 65) {
    status = HealthStatus.YELLOW_LEAF;
}
// 2. 再检查是否枯萎发白（高亮度+低饱和度）
else if (brightness > 200 && saturation < 40) {
    status = HealthStatus.WILT;
}
// 3. 最后检查是否是中度枯萎预警
else if (brightness > 220 && saturation < 50) {
    status = HealthStatus.WILT_WARNING;
}
// 4. 以上都不是，则认为是健康的
else {
    status = HealthStatus.HEALTHY;
}

// ========== 叶片追踪匹配 ==========

// 使用匈牙利算法找最优匹配
// 确保每片当前叶片最多匹配一片历史叶片
```

---

## 三、禁止出现的情况

| 禁止 | 示例 | 正确 |
|------|------|------|
| 无注释的业务代码 | `int a = b + c;` | `// 计算总面积 = 宽 × 高` |
| 硬编码魔法数字 | `if (x > 65)` | `if (hValue > YELLOW_LEAF_H_THRESHOLD)` |
| 省略大括号 | `if (x) y = 1;` | `if (x) { y = 1; }` |
| 无意义的变量名 | `int a, b, c;` | `int leafArea, leafPerimeter, leafCircularity;` |

---

## 四、命名规范

### Java代码

```java
// 类名：UpperCamelCase，名词
public class LeafObservation { }

// 方法名：lowerCamelCase，动词
public void calculateLeafArea() { }

// 常量：UPPER_SNAKE_CASE
public static final int YELLOW_LEAF_THRESHOLD = 65;

// 变量：lowerCamelCase
int leafId;
double hValue;
```

### Vue前端

```javascript
// 变量：lowerCamelCase
const leafList = ref([]);
const currentLeafId = ref('');

// 方法：lowerCamelCase
function calculateArea() { }

// 常量：UPPER_SNAKE_CASE
const API_BASE_URL = 'http://localhost:8080/api';
```

---

## 五、Git提交规范

每次commit必须包含版本号和更新内容：

```
[v3.0.0] 迭代日志系统实现

- 新增 IterationLog 实体类和完整CRUD API
- 新增迭代日志管理页面 (Iterations.vue)
- 更新 CHANGELOG.md

注：所有代码均已添加完整中文注释
```

---

## 六、审查机制

每次PR/MR必须检查：
1. ✅ 所有新增代码是否有注释
2. ✅ 注释是否清晰表达了代码意图
3. ✅ 关键逻辑是否有块注释说明
4. ✅ 变量命名是否语义化

---

_规范制定: 2026-04-28_
_依据: 子龙硬性要求_
