# YOLOv8 模型文件目录

## 模型文件

将您的 YOLOv8 模型文件 (.onnx) 放在此目录。

## 推荐模型

### YOLOv8n (nano) - 轻量快速
- 参数量: 3.2M
- 推理速度: ~20ms/帧 (CPU)
- 适合边缘部署

### YOLOv8s (small) - 精度更高
- 参数量: 11.2M
- 推理速度: ~40ms/帧 (CPU)
- 适合服务器部署

## 文件命名

```
yolov8n-leaf.onnx    # 叶片检测模型
classes.txt          # 类别名称
```

## 模型训练

如需训练自定义叶片检测模型，请参考：

1. 收集绿萝叶片图片
2. 使用 LabelImg 标注
3. 使用 YOLOv8 训练
4. 导出 ONNX 格式

## 配置

在 application.yml 中配置：
```yaml
model:
  enabled: true
  path: models/yolov8n-leaf.onnx
  conf-threshold: 0.5
  iou-threshold: 0.45
```