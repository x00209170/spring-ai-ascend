---
name: hotel-booking
description: 酒店预订技能，从用户消息中提取业务参数并以 JSON 调用 Versatile 工作流工具。
---

# 酒店预订技能

当用户提出酒店预订需求时，从用户消息中提取以下字段，并组装为 JSON 对象调用
可用的 Versatile 工作流工具：

- **intent**: 业务意图（如 "订酒店"）
- **wap_userName**: 用户显示名称
- **person_name**: 入住人姓名
- **checkin_date**: 入住日期，格式 YYYY-MM-DD
- **checkout_date**: 退房日期，格式 YYYY-MM-DD
- **arrival_city**: 目的地城市

示例工具调用：
```json
{"intent":"订酒店","wap_userName":"张三","person_name":"李四","checkin_date":"2026-03-30","checkout_date":"2026-04-03","arrival_city":"北京"}
```

所有字段值必须从用户消息中提取，不要编造。如果用户消息中缺少某个字段，
用空字符串填充，不要猜测。
