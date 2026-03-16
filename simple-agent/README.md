🏥 智慧健康助手 - 老年人健康管理系统
https://img.shields.io/badge/license-MIT-blue.svg
https://img.shields.io/badge/Spring%2520Boot-3.x-green
https://img.shields.io/badge/Java-17%252B-orange
https://img.shields.io/badge/Frontend-HTML%252FCSS%252FJavaScript-yellow

一个专为老年人设计的智能健康管理助手，提供全天候的健康咨询、用药提醒、紧急情况处理和多媒体健康服务。

✨ 主要功能
🤖 AI健康助手
智能对话：基于大语言模型的自然对话，理解老年人健康需求

多轮对话记忆：支持上下文理解，提供连贯的咨询服务

紧急情况检测：自动识别紧急医疗关键词，提供即时指导

个性化建议：结合用户健康档案提供定制化健康建议

📱 用户管理
简化的注册登录：仅需用户名即可快速登录，适合老年人使用

健康档案管理：记录个人基本信息、慢性病史、过敏史等

访客模式：无需注册即可体验基本功能

🏥 健康管理
健康记录追踪：记录血压、血糖、心率等健康指标

用药提醒系统：设置和管理用药提醒，支持定时提醒

紧急联系人：存储紧急联系人信息，紧急情况一键联系

🛠️ 智能工具
语音转文字：支持音频上传，自动转换为文字内容

OCR文字识别：识别药品说明书、体检报告等图片中的文字

文字转语音：将文字内容转换为语音，方便视力不便的老年人

实时天气查询：获取当地天气信息，提供出行建议

医院搜索：查找附近医院信息，提供联系方式

🚨 紧急服务
紧急医疗识别：自动检测心脏病、中风等紧急情况关键词

一键急救指导：提供详细的急救步骤和紧急联系电话

联系人自动通知：紧急情况下可快速联系家人或救护车

🚀 快速开始
环境要求
Java 17+

Maven 3.6+

MySQL 8.0+（可选，用于数据持久化）

现代浏览器（Chrome 90+、Firefox 88+、Edge 90+）

安装步骤
克隆项目

bash
git clone https://github.com/yourusername/smart-health-assistant.git
cd smart-health-assistant
配置应用

bash
# 复制配置文件
cp src/main/resources/application.properties.example src/main/resources/application.properties

# 编辑配置文件，设置API密钥等
vim src/main/resources/application.properties
配置API密钥
在 application.properties 中配置：

properties
# 硅基流动API配置
siliconflow.api.key=您的API密钥
siliconflow.api.url=https://api.siliconflow.cn/v1/chat/completions

# 高德地图API（医院搜索）
amap.api.key=您的高德API密钥
编译运行

bash
# 使用Maven编译
mvn clean package

# 运行应用
java -jar target/smart-health-assistant-1.0.0.jar

# 或直接使用Maven运行
mvn spring-boot:run
访问应用

打开浏览器访问：http://localhost:8080

使用示例用户快速登录：

用户名：zhangsan、lisi、wangwu

或创建新用户

📁 项目结构
text
smart-health-assistant/
├── src/main/java/com/example/simpleagent/
│   ├── config/           # 配置类
│   ├── controller/       # REST控制器
│   ├── service/         # 业务逻辑层
│   ├── model/          # 数据模型
│   ├── tool/           # 工具类实现
│   └── repository/     # 数据访问层
├── src/main/resources/
│   ├── static/         # 静态资源
│   ├── templates/      # 模板文件
│   └── application.properties
└── src/main/webapp/
    └── index.html      # 前端主页面
🔧 技术栈
后端
Spring Boot 3.x：后端框架

Spring MVC：Web层框架

Spring Data JPA：数据持久化

Jackson：JSON处理

RestTemplate：HTTP客户端

前端
HTML5/CSS3：页面结构和样式

JavaScript (ES6+)：前端逻辑

Font Awesome 6：图标库

响应式设计：支持移动端访问

第三方服务
硅基流动API：AI模型服务

高德地图API：地理位置和医院搜索

Windows TTS：文字转语音（Windows系统）

wttr.in：天气查询服务

📖 使用说明
基本流程
登录/注册：输入用户名即可登录，新用户自动注册

完善健康档案：填写个人健康信息，获得个性化服务

开始对话：与AI健康助手进行自然语言对话

使用工具：根据需要使用语音、OCR等多媒体功能

管理健康：记录健康数据，设置用药提醒

特色功能详解
🎤 语音转文字
支持上传MP3、WAV、M4A、FLAC格式音频

最大支持50MB文件

识别结果可直接发送给AI助手

🖼️ OCR文字识别
支持JPG、PNG、BMP、GIF、WebP格式图片

可识别药品说明书、体检报告等

提取关键信息并格式化显示

🔊 文字转语音
将文字内容转换为语音文件（WAV格式）

支持下载和在线播放

使用Windows系统语音引擎

🔒 安全性
会话管理：基于HTTP Session的用户认证

输入验证：所有用户输入都经过验证和清理

文件上传限制：严格的文件类型和大小限制

API密钥保护：敏感配置存储在配置文件中

📊 数据管理
存储方式
内存存储：对话历史、用户会话（重启后丢失）

文件存储：上传的音频、图片文件（./uploads/目录）

数据库：用户信息、健康记录、用药提醒

数据清理
临时文件自动清理（超过1小时）

会话超时自动清理（30分钟无活动）

可配置的数据保留策略

🧪 测试账号
项目内置了测试账号，方便快速体验：

用户名	真实姓名	年龄	预设健康信息
zhangsan	张三	75	高血压、糖尿病
lisi	李四	68	心脏病
wangwu	王五	72	关节炎、骨质疏松
zhaoliu	赵六	80	失眠、高血压
test	测试用户	65	无慢性病史
🚨 紧急情况处理
系统可以自动识别以下紧急情况关键词：

❤️ 心脏病相关：心脏病、心梗、胸痛、胸闷

🧠 中风相关：中风、脑梗、面瘫、言语不清

😫 呼吸困难：呼吸困难、气喘、窒息

🩸 出血相关：大出血、流血不止

🤢 中毒相关：中毒、食物中毒、药物过量

检测到紧急情况时，系统会：

显示醒目的紧急警报

提供详细的急救步骤

建议立即拨打120

显示用户的紧急联系人信息

🔧 自定义配置
调整对话记忆长度
properties
# 最大历史记录数
app.conversation.max-history=20
调整文件大小限制
properties
# 音频文件最大大小（默认50MB）
app.upload.max-audio-size=52428800

# 图片文件最大大小（默认20MB）
app.upload.max-image-size=20971520
调整会话超时时间
properties
# 会话超时时间（秒）
server.servlet.session.timeout=1800
🤝 贡献指南
欢迎贡献代码或报告问题！

Fork 项目

创建功能分支 (git checkout -b feature/AmazingFeature)

提交更改 (git commit -m 'Add some AmazingFeature')

推送到分支 (git push origin feature/AmazingFeature)

开启 Pull Request

📄 许可证
本项目采用 MIT 许可证 - 查看 LICENSE 文件了解详情。

📞 支持与联系
如有问题或建议，请通过以下方式联系：

📧 邮箱：support@health-assistant.com

🐛 提交Issue

💬 在线帮助：应用内AI助手

🙏 致谢
感谢以下开源项目和服务：

Spring Boot

硅基流动 提供AI模型服务

高德地图 提供地理位置服务

Font Awesome 提供图标

wttr.in 提供天气服务

免责声明：本系统为健康管理辅助工具，不能替代专业医疗建议。紧急情况请立即拨打120或联系专业医生。